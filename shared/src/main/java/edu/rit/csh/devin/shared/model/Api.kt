package edu.rit.csh.devin.shared.model

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface Api {
  @GET("drinks")
  suspend fun getDrinks(): DrinkListReturn

  @POST("drinks/drop")
  suspend fun drop(@Body payload: DropPayload): DropReturn

  @GET("/users/credits")
  suspend fun getCredits(@Query("uid") uid: String): CreditCountReturn
}