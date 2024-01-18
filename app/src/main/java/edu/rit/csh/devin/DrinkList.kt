package edu.rit.csh.devin

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.okta.authfoundation.credential.Token
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.rit.csh.devin.shared.model.Api
import edu.rit.csh.devin.shared.model.DrinkMachineReturn
import edu.rit.csh.devin.shared.model.DrinkSlot
import edu.rit.csh.devin.shared.model.DropPayload
import edu.rit.csh.devin.shared.model.UserPayload
import edu.rit.csh.devin.shared.GatekeeperViewModel
import edu.rit.csh.devin.shared.model.ThinDrinkMachine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DrinkList(
  drinkViewModel: DrinkViewModel = hiltViewModel(),
  gatekeeperViewModel: GatekeeperViewModel = hiltViewModel(),
  modifier: Modifier
) {
  val selectedMachine by drinkViewModel.selectedMachine.collectAsState()
  val drinkItems by drinkViewModel.items.collectAsState()
  var refreshError: Exception? by remember { mutableStateOf(null) }
  WithAuth {
    // Get drinks
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
    val items = drinkItems?.filter {
      selectedMachine == null || selectedMachine!!.name == it.name
    }
    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
      LaunchedEffect(Unit) {
        refreshError = null
        try {
          drinkViewModel.refresh()
        } catch (err: Exception) {
          refreshError = err
        }
        pullRefreshState.endRefresh()
      }
    }
    if (refreshError != null) {
      Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          "Failed to Load",
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
          refreshError!!.message ?: "Unknown Error",
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(onClick = {
          pullRefreshState.startRefresh()
        }) {
          Text("Retry")
        }
      }
    } else if (items != null) {
      Box(modifier = modifier.nestedScroll(connection = pullRefreshState.nestedScrollConnection)) {
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
          modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          item {
            Spacer(modifier = Modifier)
          }
          items.forEach { machine ->
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
                  style = MaterialTheme.typography.headlineLarge,
                  // modifier = Modifier.padding(vertical = 6.dp)
                )
              }
            }
            items(machine.slots.filter {
              it.buyable
            }) { slot ->
              DrinkRow(SlotBundle(thinMachine, slot))
            }
          }
          item {
            Spacer(modifier = Modifier)
          }
        }
        PullToRefreshContainer(
          state = pullRefreshState, modifier = Modifier.align(alignment = Alignment.TopCenter)
        )
      }
    } else {
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }
  }
}

@Composable
fun DrinkRow(slot: SlotBundle, drinkViewModel: DrinkViewModel = hiltViewModel()) {
  val coroutineScope = rememberCoroutineScope()
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
//      Icon(
//        imageVector = slot.machine.icon,
//        contentDescription = slot.machine.displayName,
//        modifier = Modifier
//          .requiredSize(48.dp)
//          .padding(horizontal = 8.dp),
//      )
      Column(modifier = Modifier
        .fillMaxHeight()
        .padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          slot.slot.item.name,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 8.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        val dollarUnit by drinkViewModel.dollarUnit.collectAsState()
        val price = slot.slot.item.price
        Box(modifier = Modifier
          .clip(ButtonDefaults.textShape)
          .clickable {
            drinkViewModel.togglePriceUnit()
          }) {
          Text(
            formatPrice(dollarUnit, price),
            style = MaterialTheme.typography.bodyMedium.copy(color = ButtonDefaults.textButtonColors().contentColor),
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }
      Spacer(modifier = Modifier.weight(1.0f))
      val creditCount by drinkViewModel.drinkCredits.collectAsState()
      val dropping by drinkViewModel.dropping.collectAsState()
      Button(onClick = {
        coroutineScope.launch {
          drinkViewModel.drop(slot.machine.name, slot.slot.number)
        }
      },
        enabled = !dropping && (creditCount?.let { it >= slot.slot.item.price } ?: true),
        modifier = Modifier.padding(horizontal = 8.dp)) {
        Text("Drop")
      }
    }
  }
}

fun formatPrice(dollarUnit: Boolean, price: Long): String {
  return if (dollarUnit) {
    val dollars = "%.2f".format(price.toDouble() / 100)
    "\$$dollars"
  } else {
    "$price Credits"
  }
}

data class SlotBundle(
  val machine: ThinDrinkMachine,
  val slot: DrinkSlot,
)

@HiltViewModel
class DrinkViewModel @Inject constructor(
  val token: StateFlow<Token?>, val snackbarHostState: SnackbarHostStateWrapper
) : ViewModel() {
  var selectedMachine = MutableStateFlow(null as ThinDrinkMachine?)
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
          kotlin.io.encoding.Base64.UrlSafe.decode(token.value!!.accessToken.split(".")[1])
            .decodeToString()
        val payload = Gson().fromJson(payloadStr, UserPayload::class.java)
        val credits = api.getCredits(payload.preferredUsername)
        _drinkCredits.value = credits.user.drinkBalance
      }).joinAll()
    }
  }

  private var _dropping = MutableStateFlow(false)
  val dropping = _dropping.asStateFlow()

  suspend fun drop(machineName: String, slotNumber: Int) {
    val job = viewModelScope.launch {
      snackbarHostState.snackbarHostState.showSnackbar(
        message = "Dropping...", duration = SnackbarDuration.Indefinite
      )
    }
    _dropping.value = true
    val dropResult = try {
      api.drop(DropPayload(machineName, slotNumber))
    } catch (err: Exception) {
      viewModelScope.launch {
        snackbarHostState.snackbarHostState.showSnackbar(message = "Drop failed: ${err.message}")
      }
      return
    } finally {
      _dropping.value = false
      job.cancel()
    }
    _drinkCredits.value = dropResult.drinkBalance
    viewModelScope.launch {
      snackbarHostState.snackbarHostState.showSnackbar(message = "Drink dropped! Enjoy!")
    }
  }

  var _dollarUnit = MutableStateFlow(false)
  val dollarUnit = _dollarUnit.asStateFlow()

  fun togglePriceUnit() {
    _dollarUnit.value = !_dollarUnit.value
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrinkTopBar(drinkViewModel: DrinkViewModel = hiltViewModel()) {
  TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      titleContentColor = MaterialTheme.colorScheme.primary,
    ),
    title = {
      Text("Flask")
    },
    actions = {
      val drinkCredits by drinkViewModel.drinkCredits.collectAsState()
      val dollarUnit by drinkViewModel.dollarUnit.collectAsState()
      drinkCredits?.let {
        Text(
          formatPrice(dollarUnit, it),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(end = 12.dp)
        )
      }
    })
}

@Composable
fun DrinkBottomBar(
  drinkViewModel: DrinkViewModel = hiltViewModel(),
) {
  val selectedMachine by drinkViewModel.selectedMachine.collectAsState()
  BottomAppBar(actions = {
    val machines = listOf(null as ThinDrinkMachine?) + ThinDrinkMachine.values()
    machines.forEach {
      val icon = it?.icon ?: Icons.Filled.Star
      val displayName = it?.displayName ?: "All"
      NavigationBarItem(selected = selectedMachine == it, onClick = {
        drinkViewModel.selectedMachine.value = it
      }, label = {
        Text(displayName)
      }, icon = {
        Icon(imageVector = icon, contentDescription = null)
      })
    }
  })
}