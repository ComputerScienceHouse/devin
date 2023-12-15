package edu.rit.csh.devin.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

data class DrinkItem(
  @Keep @SerializedName("id") val id: Number,
  @Keep @SerializedName("name") val name: String,
  @Keep @SerializedName("price") val price: Long
) {
  constructor() : this(0, "", 0)
}