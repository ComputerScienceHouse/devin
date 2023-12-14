package edu.rit.csh.devin

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okta.authfoundation.client.OidcClient
import com.okta.authfoundation.client.OidcClientResult
import com.okta.authfoundation.client.OidcConfiguration
import com.okta.authfoundation.credential.CredentialDataSource
import com.okta.authfoundation.credential.CredentialDataSource.Companion.createCredentialDataSource
import com.okta.authfoundationbootstrap.CredentialBootstrap
import com.okta.webauthenticationui.WebAuthenticationClient.Companion.createWebAuthenticationClient
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.rit.csh.devin.ui.theme.FlaskTheme
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel
import com.okta.authfoundation.credential.Token
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.time.delay
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

enum class ThinDrinkMachine(val displayName: String, val icon: ImageVector) {
  bigdrink("Big Drink", Icons.Filled.SportsBar),
  snack("Snack", Icons.Filled.RamenDining),
  littledrink("Little Drink", Icons.Filled.EmojiFoodBeverage),
  bepis("Bepis", Icons.Filled.Coffee),
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      FlaskTheme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          Scaffold(bottomBar = {
            DrinkBottomBar()
          },
            topBar = {
              DrinkTopBar()
            }
            ) { paddingValues ->
            DrinkList(
              modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
            )
          }
        }
      }
    }
  }
}

@Module
@InstallIn(SingletonComponent::class)
class AuthViewModel @Inject constructor() : ViewModel() {
  var accessToken: StateFlow<Token?> = _accessToken.asStateFlow()

  private val client: OidcClient
  init {
    val oidcConfiguration = OidcConfiguration(
      clientId = "devin2",
      defaultScope = "openid email groups profile drink_balance",
    )
    client = OidcClient.createFromDiscoveryUrl(
      oidcConfiguration,
      "https://sso.csh.rit.edu/auth/realms/csh/.well-known/openid-configuration".toHttpUrl()
    )
  }

  companion object {
    var _credentialDataSource: CredentialDataSource? = null
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
      GlobalScope.launch {
        val credential = CredentialBootstrap.defaultCredential()
        //credential.refreshToken()
        val accessToken = credential.getAccessTokenIfValid()
        accessToken?.let {
          _accessToken.value = credential.token
        }
        // Reset access token if we're expired:
        _accessToken.collect {
          val knownToken = it
          if (knownToken != null) {
            launch {
              delay(knownToken.expiresIn.seconds.toJavaDuration())
              if (_accessToken.value == knownToken) {
                _accessToken.value = null
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

  fun login(@ActivityContext context: Context) {
    viewModelScope.launch {
      getCredentialDataSource(context)
      val credential = CredentialBootstrap.defaultCredential()
      credential.refreshToken()
      val token = credential.getAccessTokenIfValid()
      if (token != null) {
        _accessToken.value = credential.token
      } else {
        when (val result =
          client.createWebAuthenticationClient().login(context, "edu.rit.csh.devin://oauth2redirect")) {
          is OidcClientResult.Error -> {
            println("Login machine broke: ${result.exception}")
            // Timber.e(result.exception, "Failed to login.")
            // TODO: Display an error to the user.
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
}

@Composable
fun WithAuth(
  authViewModel: AuthViewModel = hiltViewModel(),
  content: @Composable (token: String) -> Unit
) {
  val context = LocalContext.current
  val accessToken by authViewModel.accessToken.collectAsState()
  LaunchedEffect("WithAuth -> AuthViewModel::initToken") {
    authViewModel.initToken(context)
  }
  accessToken?.let {
    content(it.accessToken)
  } ?: run {
    Button(onClick = {
      authViewModel.login(context)
    }) {
      Text("Login")
    }
    LaunchedEffect(Unit) {
      authViewModel.login(context)
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(
    text = "Hello $name!",
    modifier = modifier
  )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  FlaskTheme {
    Greeting("Android")
  }
}