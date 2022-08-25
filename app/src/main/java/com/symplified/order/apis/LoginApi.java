package com.symplified.order.apis;

import com.symplified.order.models.HttpResponse;
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

    @POST("authenticate")
    Call<LoginResponse> login(@Header("Content-Type") String contentType, @Body LoginRequest loginRequest);

    @Headers("Content-Type: application/plain")
    @POST("session/refresh")
    Call<LoginResponse> refreshAccessToken(@Body String refreshToken);

    @PUT("{clientId}/pingResponse/{transactionId}")
    Call<HttpResponse> ping(@Path("clientId") String clientId,
                            @Path("transactionId") String transactionId);
}
