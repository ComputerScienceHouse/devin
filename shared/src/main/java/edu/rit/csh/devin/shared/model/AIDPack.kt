package edu.rit.csh.devin.shared.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

data class AIDPack(
  @Keep @SerializedName("doorsId") val doorsId: String,
  @Keep @SerializedName("drinkId") val drinkId: String,
  @Keep @SerializedName("memberProjectsId") val memberProjectsId: String
) {
  @Keep
  constructor() : this("", "", "")
}