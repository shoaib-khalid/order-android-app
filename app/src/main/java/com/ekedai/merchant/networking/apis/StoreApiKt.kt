package com.ekedai.merchant.networking.apis

import com.ekedai.merchant.models.HttpResponse
import com.ekedai.merchant.models.store.StoreResponse
import com.ekedai.merchant.models.store.StoreStatusResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface StoreApiKt {

    @GET("stores")
    suspend fun getStores(@Query("clientId") clientId: String): Response<StoreResponse>

    @GET("stores/{storeId}")
    suspend fun getStoreById(@Path("storeId") storeId: String): Response<StoreResponse.SingleStoreResponse>

    @PUT("stores/{storeId}/timings/snooze")
    suspend fun updateStoreStatus(
        @Path("storeId") storeId: String?,
        @Query("isSnooze") isSnooze: Boolean,
        @Query("snoozeDuration") snoozeDuration: Int
    ): Response<HttpResponse>

    @GET("stores/{storeId}/timings/snooze")
    suspend fun getStoreStatusById(@Path("storeId") storeId: String): Response<StoreStatusResponse>
}