package edu.rit.csh.devin.presentation

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.phone.interactions.authentication.OAuthRequest
import androidx.wear.phone.interactions.authentication.OAuthResponse
import androidx.wear.phone.interactions.authentication.RemoteAuthClient
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.rotaryinput.rotaryWithScroll
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.okta.authfoundation.InternalAuthFoundationApi
import com.okta.authfoundation.client.OidcClient
import com.okta.authfoundation.client.OidcClientResult
import com.okta.authfoundation.client.OidcConfiguration
import com.okta.authfoundation.credential.CredentialDataSource
import com.okta.authfoundation.credential.CredentialDataSource.Companion.createCredentialDataSource
import com.okta.authfoundation.credential.Token
import com.okta.authfoundationbootstrap.CredentialBootstrap
import com.okta.oauth2.AuthorizationCodeFlow.Companion.createAuthorizationCodeFlow
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import edu.rit.csh.devin.presentation.theme.FlaskTheme
import edu.rit.csh.devin.shared.GatekeeperViewModel
import edu.rit.csh.devin.shared.model.Api
import edu.rit.csh.devin.shared.model.DrinkMachineReturn
import edu.rit.csh.devin.shared.model.DropPayload
import edu.rit.csh.devin.shared.model.ThinDrinkMachine
import edu.rit.csh.devin.shared.model.UserPayload
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()

    super.onCreate(savedInstanceState)

    setTheme(android.R.style.Theme_DeviceDefault)

    setContent {
      WearApp()
    }
  }
}

@Suppress("NAME_SHADOWING")
@Module
@InstallIn(SingletonComponent::class)
class AuthViewModel : ViewModel() {
  var accessToken: StateFlow<Token?> = _accessToken.asStateFlow()

  private val client: OidcClient

  init {
    val oidcConfiguration = OidcConfiguration(
      clientId = "devin2",
      defaultScope = "openid email groups profile drink_balance gatekeeper_provision",
    )
    client = OidcClient.createFromDiscoveryUrl(
      oidcConfiguration,
      "https://sso.csh.rit.edu/auth/realms/csh/.well-known/openid-configuration".toHttpUrl()
    )
  }

  companion object {
    private var _credentialDataSource: CredentialDataSource? = null
    private val _accessToken = MutableStateFlow(null as Token?)
  }

  fun initToken(@ApplicationContext context: Context) {
    getCredentialDataSource(context)
  }

  private fun getCredentialDataSource(context: Context): CredentialDataSource {
    if (_credentialDataSource == null) {
      val credentialDataSource = client.createCredentialDataSource(context)
      _credentialDataSource = credentialDataSource
      CredentialBootstrap.initialize(credentialDataSource)
      viewModelScope.launch {
        val credential = CredentialBootstrap.defaultCredential()
        credential.refreshToken()
        val accessToken = credential.getAccessTokenIfValid()
        accessToken?.let {
          _accessToken.value = credential.token
        }
        // Reset access token if we're expired:
        _accessToken.collect {
          val knownToken = it
          if (knownToken != null) {
            println("We saw a new token! Let's see where this goes...")
            launch {
              println("In ${knownToken.expiresIn}s we'll attempt to fix this...")
              delay(knownToken.expiresIn.seconds.toJavaDuration())
              println("Attempting a refresh!")
              if (_accessToken.value === knownToken) {
                println("Tokens are the same")
                val credential = CredentialBootstrap.defaultCredential()
                val refreshResult = credential.refreshToken()
                println("Refreshed: $refreshResult")
                _accessToken.value = credential.getAccessTokenIfValid()?.let { credential.token }
              } else {
                println("Did something change?")
              }
            }
          }
        }
      }
    }
    return _credentialDataSource!!
  }

  @Singleton
  @Provides
  fun provideToken(@ApplicationContext context: Context): StateFlow<Token?> {
    getCredentialDataSource(context)
    return this.accessToken
  }

  @OptIn(InternalAuthFoundationApi::class)
  suspend fun login(@ActivityContext context: Context) {
    getCredentialDataSource(context)
    val credential = CredentialBootstrap.defaultCredential()
    credential.refreshToken()
    val token = credential.getAccessTokenIfValid()
    if (token != null) {
      _accessToken.value = credential.token
    } else {
      val flow = client.createAuthorizationCodeFlow()
      val redirectUrl = OAuthRequest.WEAR_REDIRECT_URL_PREFIX + "edu.rit.csh.devin"
      val flowContext = flow.start(redirectUrl).getOrThrow()
      val request =
        OAuthRequest.Builder(context).setAuthProviderUrl(flowContext.url.toString().toUri()).build()
      val responseUrl = suspendCoroutine { cont ->
        fun errorCodeToString(errorCode: Int): Exception {
          return Exception(
            when (errorCode) {
              RemoteAuthClient.ERROR_UNSUPPORTED -> "Auth not supported"
              RemoteAuthClient.ERROR_PHONE_UNAVAILABLE -> "Phone unavailable"
              else -> "Unknown error: $errorCode"
            }
          )
        }
        RemoteAuthClient.create(context).sendAuthorizationRequest(request,
          Executors.newSingleThreadExecutor(),
          object : RemoteAuthClient.Callback() {
            override fun onAuthorizationResponse(request: OAuthRequest, response: OAuthResponse) {
              response.responseUrl?.let {
                cont.resume(it)
              } ?: run {
                cont.resumeWithException(errorCodeToString(response.errorCode))
              }
            }

            override fun onAuthorizationError(request: OAuthRequest, errorCode: Int) {
              cont.resumeWithException(errorCodeToString(errorCode))
            }
          })
      }


      when (val result =
        flow.resume((redirectUrl + "?" + responseUrl.query).toUri(), flowContext)) {
        is OidcClientResult.Error -> {
          println(result.exception)
          println("Login machine broke: ${result.exception}")
          // Timber.e(result.exception, "Failed to login.")
          // TODO: Display an error to the user.
          throw result.exception
        }

        is OidcClientResult.Success -> {
          println("Got login response! $result ${result.result}")
          credential.storeToken(token = result.result)
          _accessToken.value = result.result
          // The credential instance now has a token! You can use the `Credential` to make calls to OAuth endpoints, or to sign requests!
        }
      }
    }
  }
}

@Composable
fun WearApp() {
  FlaskTheme {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colors.background),
      contentAlignment = Alignment.Center
    ) {
      DrinkList()
    }
  }
}

@HiltViewModel
class DrinkViewModel @Inject constructor(
  val token: StateFlow<Token?>
) : ViewModel() {
  private var _items = MutableStateFlow(null as List<DrinkMachineReturn>?)
  val items = _items.asStateFlow()

  private var _drinkCredits = MutableStateFlow(null as Long?)
  val drinkCredits = _drinkCredits.asStateFlow()

  private val client = OkHttpClient.Builder().addInterceptor { chain ->
    val tokenValue = token.value
    if (tokenValue == null) {
      chain.proceed(chain.request())
    } else {
      chain.proceed(
        chain.request().newBuilder().header("Authorization", "Bearer ${tokenValue.accessToken}")
          .build()
      )
    }
  }.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)).build()
  private val retrofit = Retrofit.Builder().baseUrl("https://drink.csh.rit.edu/")
    .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create())).client(client).build()
  private val api = retrofit.create(Api::class.java)

  @OptIn(ExperimentalEncodingApi::class)
  suspend fun refresh() {
    coroutineScope {
      listOf(launch {
        val drinks = api.getDrinks()
        _items.value = drinks.machines
//        drinks.machines.flatMap { machine ->
//          machine.slots.map { slot ->
//            SlotBundle(ThinDrinkMachine.valueOf(machine.name), slot)
//          }
//        }
      }, launch {
        val payloadStr =
          Base64.UrlSafe.decode(token.value!!.accessToken.split(".")[1]).decodeToString()
        val payload = Gson().fromJson(payloadStr, UserPayload::class.java)
        val credits = api.getCredits(payload.preferredUsername)
        _drinkCredits.value = credits.user.drinkBalance
      }).joinAll()
    }
  }

  private var _dropping = MutableStateFlow(false)
  val dropping = _dropping.asStateFlow()

  suspend fun drop(machineName: String, slotNumber: Int, @ActivityContext context: Context) {
    _dropping.value = true
    val dropResult = try {
      api.drop(DropPayload(machineName, slotNumber))
    } catch (err: Exception) {
      Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
      return
    } finally {
      _dropping.value = false
    }
    _drinkCredits.value = dropResult.drinkBalance
    Toast.makeText(context, "Drink dropped! Enjoy!", Toast.LENGTH_SHORT).show()
  }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun DrinkList(
  drinkViewModel: DrinkViewModel = hiltViewModel(),
  gatekeeperViewModel: GatekeeperViewModel = hiltViewModel(),
  authViewModel: AuthViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val drinkItems by drinkViewModel.items.collectAsState()
  var refreshError: Exception? by remember { mutableStateOf(null) }
  val listState = rememberScalingLazyListState()
  val authToken by authViewModel.accessToken.collectAsState()
  Scaffold(modifier = Modifier.fillMaxSize(), positionIndicator = authToken?.let {
    {
      PositionIndicator(
        scalingLazyListState = listState,
      )
    }
  }, timeText = {
    TimeText()
  }) {
    WithAuth {
      LaunchedEffect("DrinkList -> DrinkViewModel::refresh") {
        listOf(launch {
          refreshError = null
          try {
            drinkViewModel.refresh()
          } catch (err: Exception) {
            refreshError = err
          }
        }, launch {
          gatekeeperViewModel.provision()
        }).joinAll()
      }
      drinkItems?.let { drinkItems ->
        ScalingLazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .rotaryWithScroll(listState),
          autoCentering = AutoCenteringParams(itemIndex = 0),
          state = listState
        ) {
          drinkItems.forEach { machine ->
            val thinMachine = ThinDrinkMachine.valueOf(machine.name)
            item {
              Row(
                modifier = Modifier
                  .padding(horizontal = 8.dp, vertical = 12.dp)
                  .height(IntrinsicSize.Max)
              ) {
                Icon(
                  imageVector = thinMachine.icon,
                  contentDescription = null,
                  modifier = Modifier
                    .padding(end = 8.dp)
                    .fillMaxHeight()
                    .aspectRatio(1F)
                )
                Text(
                  text = machine.displayName,
                  style = MaterialTheme.typography.title3,
                  // modifier = Modifier.padding(vertical = 6.dp)
                )
              }
            }
            items(machine.slots.filter { it.buyable }) { slot ->
              Button(
                onClick = {
                  coroutineScope.launch {
                    drinkViewModel.drop(machine.name, slot.number, context)
                  }
                }, modifier = Modifier.fillMaxWidth()
              ) {
                // slot.item.price
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                  Text(slot.item.name, textAlign = TextAlign.Center)
                  Text(
                    "${slot.item.price} Credits",
                    style = MaterialTheme.typography.caption1,
                    textAlign = TextAlign.Center
                  )
                }
              }
            }
          }
        }
      } ?: run {
        Column(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          refreshError?.let {
            it.printStackTrace()
            Text("Error: ${it.message ?: "Unknown"}")
          } ?: run {
            CircularProgressIndicator()
          }
        }
      }
    }
  }
}

@Composable
fun WithAuth(
  authViewModel: AuthViewModel = hiltViewModel(), content: @Composable (token: String) -> Unit
) {
  val context = LocalContext.current
  val accessToken by authViewModel.accessToken.collectAsState()
  LaunchedEffect("WithAuth -> AuthViewModel::initToken") {
    authViewModel.initToken(context)
  }
  accessToken?.let {
    content(it.accessToken)
  } ?: run {
    var exception: Exception? by remember { mutableStateOf(null) }
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (exception != null) {
        Text(
          "Failed to Login",
          style = MaterialTheme.typography.title1,
          modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
          exception!!.message ?: "Unknown Error",
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(onClick = {
          exception = null
        }) {
          Text("Retry")
        }
      } else {
        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
          coroutineScope.launch {
            try {
              authViewModel.login(context)
            } catch (err: Exception) {
              exception = err
            }
          }
        }
        CircularProgressIndicator()
        Text(
          "Logging you in...", modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center
        )
      }
    }
  }
}
