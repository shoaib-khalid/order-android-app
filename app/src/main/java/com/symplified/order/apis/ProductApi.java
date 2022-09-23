package com.symplified.order.apis;

import com.symplified.order.models.asset.StoreProductAsset;
import com.symplified.order.models.product.Product;
import com.symplified.order.models.product.ProductEditRequest;
import com.symplified.order.models.product.ProductListResponse;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProductApi {

    @GET("stores/{storeId}/products?status=ACTIVE,INACTIVE,OUTOFSTOCK&pageSize=1000000")
    Call<ProductListResponse> getProducts(@Path("storeId") String storeId);

    @PUT("stores/{storeId}/products/{productId}")
    Call<ResponseBody> updateProduct(@Path("storeId") String storeId, @Path("productId") String productId, @Body ProductEditRequest body);

    @GET("stores/{storeId}/products/{productId}/assets")
    Call<StoreProductAsset.StoreProductAssetListResponse> getStoreProductAssets(
            @Path("storeId") String storeId, @Path("productId") String productId);
}
