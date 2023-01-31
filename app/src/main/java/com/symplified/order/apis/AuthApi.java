package com.symplified.order.apis;

import com.symplified.order.models.client.ClientResponse;
import com.symplified.order.models.error.ErrorRequest;
import com.symplified.order.models.login.LoginRequest;
import com.symplified.order.models.login.LoginResponse;
import com.symplified.order.models.ping.PingRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface AuthApi {

    @Headers("Content-Type: application/json")
    @POST("authenticate")
    Call<LoginResponse> authenticate(@Body LoginRequest loginRequest);

    @Headers("Content-Type: application/plain")
    @POST("session/refresh")
    Call<LoginResponse> refreshAccessToken(@Body String refreshToken);

    @PUT("{clientId}/pingresponse/{transactionId}")
    Call<Void> ping(
            @Path("clientId") String clientId,
            @Path("transactionId") String transactionId,
            @Body PingRequest pingRequest
    );

    @Headers("Content-Type: application/json")
    @POST("logerror")
    Call<Void> logError(@Body ErrorRequest errorRequest);

    @GET("{clientId}")
    Call<ClientResponse> getClientById(@Path("clientId") String clientId);
}
