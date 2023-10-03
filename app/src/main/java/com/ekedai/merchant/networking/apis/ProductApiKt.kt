package com.ekedai.merchant.networking.apis

import com.ekedai.merchant.models.asset.StoreProductAsset
import com.ekedai.merchant.models.product.ProductListResponse
import com.ekedai.merchant.models.product.UpdatedProduct
import okhttp3.ResponseBody
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
        @Body body: UpdatedProduct
    ): Response<ResponseBody>

    @GET("stores/{storeId}/products/{productId}/assets")
    fun getStoreProductAssets(
        @Path("storeId") storeId: String,
        @Path("productId") productId: String
    ): Response<StoreProductAsset.StoreProductAssetListResponse>
}