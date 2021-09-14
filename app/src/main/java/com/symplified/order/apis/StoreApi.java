package com.symplified.order.apis;

import com.symplified.order.models.HttpResponse;
import com.symplified.order.models.Store.StoreResponse;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StoreApi {

    @GET("stores")
    Call<StoreResponse> getStores(@HeaderMap Map<String, String> headers, @Query("clientId") String clientId);


    @GET("stores/{storeId}/assets")
    Call<ResponseBody> getStoreLogo(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId);

}
