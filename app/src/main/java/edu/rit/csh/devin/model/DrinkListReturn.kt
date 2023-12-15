package edu.rit.csh.devin.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

data class DrinkListReturn(
  @Keep @SerializedName("machines") val machines: List<DrinkMachineReturn>,
) {
  @Keep
  constructor() : this(listOf())
}