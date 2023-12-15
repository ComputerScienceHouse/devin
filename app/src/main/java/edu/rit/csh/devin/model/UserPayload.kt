package edu.rit.csh.devin.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

data class UserPayload(
  @Keep @SerializedName("preferred_username") val preferredUsername: String
) {
  @Keep
  constructor() : this("")
}