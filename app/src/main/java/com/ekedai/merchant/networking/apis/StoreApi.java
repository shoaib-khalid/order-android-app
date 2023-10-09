package com.ekedai.merchant.networking.apis;

import com.ekedai.merchant.models.HttpResponse;
import com.ekedai.merchant.models.asset.Asset;
import com.ekedai.merchant.models.category.CategoryResponse;
import com.ekedai.merchant.models.product.ProductListResponse;
import com.ekedai.merchant.models.store.StoreResponse;
import com.ekedai.merchant.models.store.StoreStatusResponse;

import io.reactivex.Observable;
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
    Call<HttpResponse> updateStoreStatus(
            @Path("storeId") String storeId,
            @Query("isSnooze") boolean isSnooze,
            @Query("snoozeDuration") int snoozeDuration
    );

    @GET("stores/{storeId}/timings/snooze")
    Call<StoreStatusResponse> getStoreStatusById(@Path("storeId") String storeId);

    @GET("store-categories?pageSize=100")
    Observable<CategoryResponse> getCategoriesByStoreId(@Query("storeId") String storeId);

    @GET("stores/{storeId}/products?status=ACTIVE,INACTIVE,OUTOFSTOCK")
    Observable<ProductListResponse> getProducts(
            @Path("storeId") String storeId,
            @Query("categoryId") String categoryId,
            @Query("name") String name,
            @Query("page") int pageNo
    );
}