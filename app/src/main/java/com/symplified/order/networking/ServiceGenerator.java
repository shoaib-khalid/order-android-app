package com.symplified.order.networking;

import android.content.Context;
import android.content.SharedPreferences;

import com.symplified.order.App;
import com.symplified.order.apis.CategoryApi;
import com.symplified.order.apis.DeliveryApi;
import com.symplified.order.apis.FirebaseApi;
import com.symplified.order.apis.LoginApi;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.apis.ProductApi;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.utils.Key;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
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

    public static LoginApi createUserService(Context context) {
        return createRetrofitInstance(context, App.USER_SERVICE_URL).create(LoginApi.class);
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

    private static Retrofit createRetrofitInstance(Context context, String serviceUrl) {
        SharedPreferences sharedPrefs
                = context.getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new CustomInterceptor(sharedPrefs))
                .build();

        String baseURL = sharedPrefs.getString(Key.BASE_URL, App.BASE_URL_PRODUCTION);

        return new Retrofit.Builder()
                .client(httpClient)
                .baseUrl(baseURL + serviceUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
