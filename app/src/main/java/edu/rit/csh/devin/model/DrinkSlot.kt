package edu.rit.csh.devin.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import edu.rit.csh.devin.model.DrinkItem

data class DrinkSlot(
  @Keep @SerializedName("active") val active: Boolean,
  @Keep @SerializedName("count") val count: Int?,
  @Keep @SerializedName("empty") val empty: Boolean,
  @Keep @SerializedName("item") val item: DrinkItem,
  @Keep @SerializedName("machine") val machine: Int,
  @Keep @SerializedName("number") val number: Int
) {
  @Keep
  constructor() : this(false, null, false, DrinkItem(), 0, 0)
}