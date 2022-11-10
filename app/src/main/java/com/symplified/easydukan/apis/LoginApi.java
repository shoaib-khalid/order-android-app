package com.symplified.easydukan.apis;

import com.symplified.easydukan.models.HttpResponse;
import com.symplified.easydukan.models.error.ErrorRequest;
import com.symplified.easydukan.models.login.LoginRequest;
import com.symplified.easydukan.models.login.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
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
