package com.symplified.easydukan.networking.apis;

import com.symplified.easydukan.models.category.CategoryResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Query;

public interface CategoryApi {

    @GET("store-categories?page=0&pageSize=20&sortByCol=name&sortingOrder=ASC&name=")
    Call<CategoryResponse> getCategories(@HeaderMap Map<String, String> headers, @Query("storeId") String storeId);

}
