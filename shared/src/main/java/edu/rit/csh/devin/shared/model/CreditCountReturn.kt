package edu.rit.csh.devin.shared.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

data class CreditCountReturn(
  @Keep @SerializedName("user") val user: DropReturn
) {
  @Keep
  constructor() : this(DropReturn())
}