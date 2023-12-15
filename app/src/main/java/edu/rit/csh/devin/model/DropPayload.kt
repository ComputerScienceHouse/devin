package edu.rit.csh.devin.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

data class DropPayload(
  @Keep @SerializedName("machine") val machine: String,
  @Keep @SerializedName("slot") val slot: Int
) {
  @Keep
  constructor() : this("", 0)
}