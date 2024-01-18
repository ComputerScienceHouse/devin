package edu.rit.csh.devin.shared.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

data class DrinkMachineReturn(
  @Keep @SerializedName("display_name") val displayName: String,
  @Keep @SerializedName("id") val id: Int,
  @Keep @SerializedName("is_online") val isOnline: Boolean,
  @Keep @SerializedName("name") val name: String,
  @Keep @SerializedName("slots") val slots: List<DrinkSlot>
) {
  @Keep
  constructor() : this("", 0, false, "", listOf())
}