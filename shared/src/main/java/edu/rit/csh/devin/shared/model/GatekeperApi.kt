package edu.rit.csh.devin.shared.model

import retrofit2.http.GET

interface GatekeperApi {
  @GET("mobile/provision")
  suspend fun provision(): AIDPack
}