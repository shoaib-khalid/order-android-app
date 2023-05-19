package com.symplified.easydukan.networking;

import android.content.Context;
import android.content.SharedPreferences;

import com.symplified.easydukan.App;
import com.symplified.easydukan.networking.apis.AuthApi;
import com.symplified.easydukan.networking.apis.CategoryApi;
import com.symplified.easydukan.networking.apis.DeliveryApi;
import com.symplified.easydukan.networking.apis.FirebaseApi;
import com.symplified.easydukan.networking.apis.OrderApi;
import com.symplified.easydukan.networking.apis.ProductApi;
import com.symplified.easydukan.networking.apis.StaffApi;
import com.symplified.easydukan.networking.apis.StoreApi;
import com.symplified.easydukan.utils.SharedPrefsKey;

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

    public static CategoryApi createCategoryService(Context context) {
        return createRetrofitInstance(context, App.PRODUCT_SERVICE_URL).create(CategoryApi.class);
    }

    public static DeliveryApi createDeliveryService(Context context) {
        return createRetrofitInstance(context, App.DELIVERY_SERVICE_URL).create(DeliveryApi.class);
    }

    public static AuthApi createUserService(Context context) {
        return createRetrofitInstance(context, App.USER_CLIENT_SERVICE_URL).create(AuthApi.class);
    }

    public static OrderApi createOrderService(Context context) {
        return createRetrofitInstance(context, App.ORDER_SERVICE_URL).create(OrderApi.class);
    }

    public static ProductApi createProductService(Context context) {
        return createRetrofitInstance(context, App.PRODUCT_SERVICE_URL).create(ProductApi.class);
    }

    public static StoreApi createStoreService(Context context) {
        return createRetrofitInstance(context, App.PRODUCT_SERVICE_URL).create(StoreApi.class);
    }

    public static StaffApi createStaffService(Context context) {
        return createRetrofitInstance(context, App.USER_SERVICE_URL).create(StaffApi.class);
    }

    private static Retrofit createRetrofitInstance(Context context, String serviceUrl) {
        SharedPreferences sharedPrefs
                = context.getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);

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