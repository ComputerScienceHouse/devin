package edu.rit.csh.devin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.google.gson.GsonBuilder
import com.okta.authfoundation.credential.Token
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import javax.inject.Inject
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.google.gson.Gson
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import retrofit2.http.Query
import java.util.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrinkList(drinkViewModel: DrinkViewModel = hiltViewModel(), gatekeeperViewModel: GatekeeperViewModel = hiltViewModel(), modifier: Modifier) {
  val selectedMachine by drinkViewModel.selectedMachine.collectAsState()
  val drinkItems by drinkViewModel.items.collectAsState()
  WithAuth {
    // Get drinks
    LaunchedEffect("DrinkList -> DrinkViewModel::refresh") {
      drinkViewModel.refresh()
      gatekeeperViewModel.provision()
    }
    val items = drinkItems?.filter {
      selectedMachine == null || selectedMachine == it.machine
    }?.filter {
      it.buyable
    }
    val pullRefreshState = rememberPullToRefreshState()
    if (items != null) {

      if (pullRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
          drinkViewModel.refresh()
          pullRefreshState.endRefresh()
        }
      }
      Box(modifier = modifier.nestedScroll(connection = pullRefreshState.nestedScrollConnection)) {
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          item {
            Spacer(modifier = Modifier)
          }
          items(items) { slot ->
            DrinkRow(slot)
          }
          item {
            Spacer(modifier = Modifier)
          }
        }
        PullToRefreshContainer(
          state = pullRefreshState,
          modifier = Modifier.align(alignment = Alignment.TopCenter)
        )
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
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = slot.machine.icon,
        contentDescription = slot.machine.displayName,
        modifier = Modifier
          .requiredSize(48.dp)
          .padding(horizontal = 8.dp),
      )
      Column(modifier = Modifier.fillMaxHeight()) {
        Text(
          slot.slot.item.name,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(top = 8.dp)
        )
        val dollarUnit by drinkViewModel.dollarUnit.collectAsState()
        val creditCount by drinkViewModel.drinkCredits.collectAsState()
        TextButton(
          enabled = creditCount?.let { it >= slot.slot.item.price } ?: true,
          contentPadding = PaddingValues(0.dp),
          onClick = {
            drinkViewModel.togglePriceUnit()
          }
        ) {
          val price = slot.slot.item.price
          Text(
            formatPrice(dollarUnit, price),
            style = MaterialTheme.typography.bodyLarge
          )
        }
      }
      Spacer(modifier = Modifier.weight(1.0f))
      Button(
        onClick = {
          coroutineScope.launch {
            drinkViewModel.drop(slot.machine.name, slot.slot.number)
          }
        },
        modifier = Modifier.padding(horizontal = 8.dp)
      ) {
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

data class DrinkItem(
  val id: Number,
  val name: String,
  val price: Long
)

class SlotBundle(
  val machine: ThinDrinkMachine,
  val slot: DrinkSlot,
) {
  val buyable: Boolean
    get() = slot.active && (slot.count == null || slot.count > 0) && !slot.empty
}

class DrinkSlot(
  val active: Boolean,
  val count: Int?,
  val empty: Boolean,
  val item: DrinkItem,
  val machine: Int,
  val number: Int
)

@HiltViewModel
class DrinkViewModel @Inject constructor(
  val token: StateFlow<Token?>
) : ViewModel() {
  var selectedMachine = MutableStateFlow(null as ThinDrinkMachine?)
  var _items = MutableStateFlow(null as List<SlotBundle>?)
  val items = _items.asStateFlow()

  var _drinkCredits = MutableStateFlow(null as Long?)
  val drinkCredits = _drinkCredits.asStateFlow()

  val client = OkHttpClient.Builder()
    .addInterceptor { chain ->
      val tokenValue = token.value
      if (tokenValue == null) {
        chain.proceed(chain.request())
      } else {
        chain.proceed(
          chain.request().newBuilder()
            .header("Authorization", "Bearer ${tokenValue.accessToken}")
            .build()
        )
      }
    }
    .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
    .build()
  val retrofit = Retrofit.Builder()
    .baseUrl("https://drink.csh.rit.edu/")
    .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
    .client(client)
    .build()
  val api = retrofit.create(Api::class.java)

  @OptIn(ExperimentalEncodingApi::class)
  suspend fun refresh() {
    coroutineScope {
      listOf(
        launch {
          val drinks = api.getDrinks()
          _items.value = drinks.machines.flatMap { machine ->
            machine.slots.map { slot ->
              SlotBundle(ThinDrinkMachine.valueOf(machine.name), slot)
            }
          }
        },
        launch {
          val payloadStr =
            kotlin.io.encoding.Base64.UrlSafe.decode(token.value!!.accessToken.split(".")[1])
              .decodeToString()
          val payload = Gson().fromJson(payloadStr, UserPayload::class.java)
          val credits = api.getCredits(payload.preferred_username)
          _drinkCredits.value = credits.user.drinkBalance
        }).joinAll()
    }
  }

  suspend fun drop(machineName: String, slotNumber: Int) {
    val dropResult = api.drop(DropPayload(machineName, slotNumber))
    _drinkCredits.value = dropResult.drinkBalance
  }

  var _dollarUnit = MutableStateFlow(false)
  val dollarUnit = _dollarUnit.asStateFlow()

  fun togglePriceUnit() {
    _dollarUnit.value = !_dollarUnit.value
  }
}

data class UserPayload(
  val preferred_username: String
)

data class DrinkMachineReturn(
  val display_name: String,
  val id: Int,
  val is_online: Boolean,
  val name: String,
  val slots: List<DrinkSlot>
)

data class DrinkListReturn(
  val machines: List<DrinkMachineReturn>,
)

data class DropReturn(
  val drinkBalance: Long
)

data class DropPayload(
  val machine: String,
  val slot: Int
)

interface Api {
  @GET("drinks")
  suspend fun getDrinks(): DrinkListReturn

  @POST("drinks/drop")
  suspend fun drop(@Body payload: DropPayload): DropReturn

  @GET("/users/credits")
  suspend fun getCredits(@Query("uid") uid: String): CreditCountReturn
}

data class CreditCountReturn(
  val user: DropReturn
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrinkTopBar(drinkViewModel: DrinkViewModel = hiltViewModel()) {
  TopAppBar(
    title = {
      Text("Flask")
    },
    actions = {
      val drinkCredits by drinkViewModel.drinkCredits.collectAsState()
      val dollarUnit by drinkViewModel.dollarUnit.collectAsState()
      Text(drinkCredits?.let { formatPrice(dollarUnit, it) } ?: "Loading Credits")
    }
  )
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
      NavigationBarItem(
        selected = selectedMachine == it,
        onClick = {
          drinkViewModel.selectedMachine.value = it
        },
        label = {
          Text(displayName)
        },
        icon = {
          Icon(imageVector = icon, contentDescription = displayName)
        }
      )
    }
  })
}