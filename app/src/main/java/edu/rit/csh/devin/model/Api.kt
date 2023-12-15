package edu.rit.csh.devin.model

import edu.rit.csh.devin.model.CreditCountReturn
import edu.rit.csh.devin.model.DrinkListReturn
import edu.rit.csh.devin.model.DropPayload
import edu.rit.csh.devin.model.DropReturn
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