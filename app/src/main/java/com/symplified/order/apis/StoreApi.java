package com.symplified.order.apis;

import com.symplified.order.models.HttpResponse;
import com.symplified.order.models.Store.StoreResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StoreApi {

    @GET("stores")
    Call<StoreResponse> getStores(@HeaderMap Map<String, String> headers, @Query("clientId") String clientId);

}
