package com.ekedai.merchant.networking.apis;

import com.ekedai.merchant.models.category.CategoryResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CategoryApi {
    @GET("store-categories?pageSize=100")
    Call<CategoryResponse> getCategories(@Query("storeId") String storeId);
}
