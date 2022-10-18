package com.symplified.order.apis;

import com.symplified.order.models.HttpResponse;
import com.symplified.order.models.error.ErrorRequest;
import com.symplified.order.models.login.LoginData;
import com.symplified.order.models.login.LoginRequest;
import com.symplified.order.models.login.LoginResponse;

import org.json.JSONObject;

import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.HeaderMap;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface LoginApi {

    @Headers("Content-Type: application/json")
    @POST("authenticate")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @Headers("Content-Type: application/plain")
    @POST("session/refresh")
    Call<LoginResponse> refreshAccessToken(@Body String refreshToken);

    @PUT("{clientId}/pingresponse/{transactionId}")
    Call<HttpResponse> ping(@Path("clientId") String clientId,
                            @Path("transactionId") String transactionId);

    @Headers("Content-Type: application/json")
    @POST("logerror")
    Call<HttpResponse> logError(@Body ErrorRequest errorRequest);
}
