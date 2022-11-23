package com.symplified.easydukan.networking;

import android.content.Context;
import android.content.SharedPreferences;

import com.symplified.easydukan.App;
import com.symplified.easydukan.apis.CategoryApi;
import com.symplified.easydukan.apis.DeliveryApi;
import com.symplified.easydukan.apis.LoginApi;
import com.symplified.easydukan.apis.OrderApi;
import com.symplified.easydukan.apis.ProductApi;
import com.symplified.easydukan.apis.StoreApi;
import com.symplified.easydukan.utils.Key;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceGenerator {
    public static CategoryApi createCategoryService() {
        return createRetrofitInstance(App.PRODUCT_SERVICE_URL).create(CategoryApi.class);
    }

    public static DeliveryApi createDeliveryService() {
        return createRetrofitInstance(App.DELIVERY_SERVICE_URL).create(DeliveryApi.class);
    }

    public static LoginApi createUserService() {
        return createRetrofitInstance(App.USER_SERVICE_URL).create(LoginApi.class);
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

    private static Retrofit createRetrofitInstance(String serviceUrl) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new CustomInterceptor())
                .build();

        SharedPreferences sharedPrefs = App.getAppContext()
                .getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        String baseURL = sharedPrefs.getString(Key.BASE_URL, App.BASE_URL_PRODUCTION);

        return new Retrofit.Builder()
                .client(httpClient)
                .baseUrl(baseURL + serviceUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
