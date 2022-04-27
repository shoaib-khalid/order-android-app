package com.symplified.order.apis;

import com.symplified.order.models.category.CategoryResponse;
import com.symplified.order.models.product.ProductResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProductApi {

    @GET("store-categories")
    Call<CategoryResponse> getCategories(@HeaderMap Map<String, String> headers, @Query("storeId") String storeId);

    @GET("stores/{storeId}/products")
    Call<ProductResponse> getProducts(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId);
}
