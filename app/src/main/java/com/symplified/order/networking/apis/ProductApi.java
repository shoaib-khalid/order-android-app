package com.symplified.order.networking.apis;

import com.symplified.order.models.asset.StoreProductAsset;
import com.symplified.order.models.product.ProductEditRequest;
import com.symplified.order.models.product.ProductListResponse;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProductApi {

    @GET("stores/{storeId}/products?status=ACTIVE,INACTIVE,OUTOFSTOCK")
    Observable<ProductListResponse> getProducts(
            @Path("storeId") String storeId,
            @Query("page") int pageNo
    );

    @PUT("stores/{storeId}/products/{productId}")
    Call<ResponseBody> updateProduct(
            @Path("storeId") String storeId,
            @Path("productId") String productId,
            @Body ProductEditRequest body
    );

    @GET("stores/{storeId}/products/{productId}/assets")
    Call<StoreProductAsset.StoreProductAssetListResponse> getStoreProductAssets(
            @Path("storeId") String storeId,
            @Path("productId") String productId
    );
}
