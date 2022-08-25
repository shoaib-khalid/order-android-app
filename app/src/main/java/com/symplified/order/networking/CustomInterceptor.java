package com.symplified.order.networking;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.symplified.order.App;
import com.symplified.order.apis.LoginApi;
import com.symplified.order.models.login.LoginResponse;
import com.symplified.order.models.login.Session;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class CustomInterceptor implements Interceptor {

    SharedPreferences sharedPrefs;
    LoginApi loginService;

    public CustomInterceptor() {
        sharedPrefs = App.getAppContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);

        String baseURL = sharedPrefs.getString("base_url", App.BASE_URL);
        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient())
                .baseUrl(baseURL + App.USER_SERVICE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        loginService = retrofit.create(LoginApi.class);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        String accessToken = sharedPrefs.getString("accessToken", "");
        Request originalRequest = chain.request();
        Request request = addTokenToRequest(originalRequest, accessToken);

        Response response = chain.proceed(request);

        if (response.code() == 401 && sharedPrefs.getBoolean("isLoggedIn", false)) {
            try {
                String refreshToken = sharedPrefs.getString("refreshToken", "");
                Call<LoginResponse> refreshRequest = loginService.refreshAccessToken(refreshToken);

                Session sessionData = refreshRequest.execute().body().data.session;

                String newAccessToken = sessionData.accessToken;
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString("accessToken", newAccessToken);
                editor.putString("refreshToken", sessionData.refreshToken);
                editor.apply();

                request = addTokenToRequest(originalRequest, newAccessToken);

                response.close();
                response = chain.proceed(request);
            } catch (Exception e) {
                Log.e(App.DEV_TAG, "Failed to refresh token. " + e.getLocalizedMessage());
            }
        }

        return response;
    }

    private static Request addTokenToRequest(Request originalRequest, String accessToken) {
        return originalRequest.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .method(originalRequest.method(), originalRequest.body())
                .build();
    }
}
