package edu.rit.csh.devin.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

data class DropReturn(
  @Keep @SerializedName("drinkBalance") val drinkBalance: Long = 0
) {
  @Keep
  constructor() : this(0)
}