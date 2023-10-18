package com.ekedai.merchant.networking;

import android.content.Context;
import android.content.SharedPreferences;

import com.ekedai.merchant.App;
import com.ekedai.merchant.networking.apis.AuthApi;
import com.ekedai.merchant.networking.apis.DeliveryApi;
import com.ekedai.merchant.networking.apis.FirebaseApi;
import com.ekedai.merchant.networking.apis.OrderApi;
import com.ekedai.merchant.networking.apis.ProductApi;
import com.ekedai.merchant.networking.apis.StaffApi;
import com.ekedai.merchant.networking.apis.StoreApi;
import com.ekedai.merchant.utils.SharedPrefsKey;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceGenerator {

    public static FirebaseApi createFirebaseService() {
        return new Retrofit.Builder()
                .client(new OkHttpClient())
                .baseUrl(FirebaseApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FirebaseApi.class);
    }

    public static DeliveryApi createDeliveryService() {
        return createRetrofitInstance(App.DELIVERY_SERVICE_URL).create(DeliveryApi.class);
    }

    public static AuthApi createUserService() {
        return createRetrofitInstance(App.USER_CLIENT_SERVICE_URL).create(AuthApi.class);
    }

    public static OrderApi createOrderService() {
        return createRetrofitInstance(App.ORDER_SERVICE_URL).create(OrderApi.class);
    }

    public static ProductApi createProductService() {
        return createRetrofitInstance(App.PRODUCT_SERVICE_URL).create(ProductApi.class);
    }

    public static StoreApi createStoreService() {
        return createRetrofitInstance(App.PRODUCT_SERVICE_URL).create(StoreApi.class);
    }

    public static StaffApi createStaffService() {
        return createRetrofitInstance(App.USER_SERVICE_URL).create(StaffApi.class);
    }

    private static Retrofit createRetrofitInstance(String serviceUrl) {
        SharedPreferences sharedPrefs
                = App.getAppContext().getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new RequestInterceptor(sharedPrefs))
                .build();

        String baseURL = sharedPrefs.getString(SharedPrefsKey.BASE_URL, App.BASE_URL_PRODUCTION);

        return new Retrofit.Builder()
                .client(httpClient)
                .baseUrl(baseURL + serviceUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }
}
