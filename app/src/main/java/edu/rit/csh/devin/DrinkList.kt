package edu.rit.csh.devin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.Navigation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@Composable
fun DrinkList(drinkViewModel: DrinkViewModel = hiltViewModel()) {
  val selectedMachine by drinkViewModel.selectedMachine.collectAsState()
  WithAuth {
    // Get drinks
    Text("Drink list: $selectedMachine")
  }
}

@HiltViewModel
class DrinkViewModel @Inject constructor() : ViewModel() {
  var selectedMachine = MutableStateFlow(null as ThinDrinkMachine?)
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