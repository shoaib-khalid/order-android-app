package com.symplified.easydukan.networking.apis;

import com.symplified.easydukan.models.HttpResponse;
import com.symplified.easydukan.models.asset.Asset;
import com.symplified.easydukan.models.store.StoreResponse;
import com.symplified.easydukan.models.store.StoreStatusResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StoreApi {

    @GET("stores")
    Call<StoreResponse> getStores(@Query("clientId") String clientId);

    @GET("stores/{storeId}/assets")
    Call<ResponseBody> getStoreLogo(@Path("storeId") String storeId);

    @GET("stores/asset/{clientId}")
    Call<Asset.AssetListResponse> getAllAssets(@Path("clientId") String clientId);

    @GET("stores/{storeId}")
    Call<StoreResponse.SingleStoreResponse> getStoreById(@Path("storeId") String storeId);

    @PUT("stores/{storeId}/timings/snooze")
    Call<HttpResponse> updateStoreStatus(@Path("storeId") String storeId,
                                         @Query("isSnooze") boolean isSnooze,
                                         @Query("snoozeDuration") int snoozeDuration);

    @GET("stores/{storeId}/timings/snooze")
    Call<StoreStatusResponse> getStoreStatusById(@Path("storeId") String storeId);
}
