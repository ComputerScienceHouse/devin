package edu.rit.csh.devin.model

import retrofit2.Call
import retrofit2.http.GET

interface GatekeperApi {
  @GET("mobile/provision")
  suspend fun provision(): AIDPack
}