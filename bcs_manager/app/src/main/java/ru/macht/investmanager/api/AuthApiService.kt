package ru.macht.investmanager.api

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApiService {
    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("trade-api-keycloak/realms/tradeapi/protocol/openid-connect/token")
    fun refreshToken(
        @Field("client_id") clientId: String = "trade-api-read",
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String
    ): Call<AccessTokenResponse>
}
