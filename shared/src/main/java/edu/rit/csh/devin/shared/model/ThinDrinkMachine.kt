package edu.rit.csh.devin.shared.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.EmojiFoodBeverage
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.ui.graphics.vector.ImageVector

enum class ThinDrinkMachine(val displayName: String, val icon: ImageVector) {
  bigdrink("Big Drink", Icons.Filled.SportsBar),
  snack(
    "Snack",
    Icons.Filled.RamenDining
  ),
  littledrink("Little Drink", Icons.Filled.EmojiFoodBeverage),
  bepis("Bepis", Icons.Filled.Coffee),
}