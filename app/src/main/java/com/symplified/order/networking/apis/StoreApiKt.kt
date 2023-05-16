package com.symplified.order.networking.apis

import com.symplified.order.models.HttpResponse
import com.symplified.order.models.asset.Asset.AssetListResponse
import com.symplified.order.models.store.StoreResponse
import com.symplified.order.models.store.StoreResponse.SingleStoreResponse
import com.symplified.order.models.store.StoreStatusResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface StoreApiKt {

    @GET("stores")
    suspend fun getStores(@Query("clientId") clientId: String): Response<StoreResponse>

    @GET("stores/{storeId}")
    suspend fun getStoreById(@Path("storeId") storeId: String): Response<SingleStoreResponse>

    @PUT("stores/{storeId}/timings/snooze")
    suspend fun updateStoreStatus(
        @Path("storeId") storeId: String?,
        @Query("isSnooze") isSnooze: Boolean,
        @Query("snoozeDuration") snoozeDuration: Int
    ): Response<HttpResponse>

    @GET("stores/{storeId}/timings/snooze")
    suspend fun getStoreStatusById(@Path("storeId") storeId: String): Response<StoreStatusResponse>
}