package com.ekedai.merchant.networking.apis;

import com.ekedai.merchant.models.asset.StoreProductAsset;
import com.ekedai.merchant.models.product.ProductEditResponse;
import com.ekedai.merchant.models.product.UpdatedProduct;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ProductApi {

    @PUT("stores/{storeId}/products/{productId}")
    Call<ProductEditResponse> updateProduct(
            @Path("storeId") String storeId,
            @Path("productId") String productId,
            @Body UpdatedProduct body
    );

    @GET("stores/{storeId}/products/{productId}/assets")
    Call<StoreProductAsset.StoreProductAssetListResponse> getStoreProductAssets(
            @Path("storeId") String storeId,
            @Path("productId") String productId
    );
}
