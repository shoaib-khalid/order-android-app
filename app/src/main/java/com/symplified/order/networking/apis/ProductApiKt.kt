package com.symplified.order.networking.apis

import com.symplified.order.models.asset.StoreProductAsset.StoreProductAssetListResponse
import com.symplified.order.models.product.ProductEditRequest
import com.symplified.order.models.product.ProductListResponse
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface ProductApiKt {

    @GET("stores/{storeId}/products?status=ACTIVE,INACTIVE,OUTOFSTOCK")
    fun getProducts(@Path("storeId") storeId: String): Response<ProductListResponse>

    @PUT("stores/{storeId}/products/{productId}")
    fun updateProduct(
        @Path("storeId") storeId: String,
        @Path("productId") productId: String,
        @Body body: ProductEditRequest
    ): Response<ResponseBody>

    @GET("stores/{storeId}/products/{productId}/assets")
    fun getStoreProductAssets(
        @Path("storeId") storeId: String,
        @Path("productId") productId: String
    ): Response<StoreProductAssetListResponse>
}