package com.symplified.easydukan.apis;

import com.symplified.easydukan.models.HttpResponse;
import com.symplified.easydukan.models.login.LoginRequest;
import com.symplified.easydukan.models.login.LoginResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface LoginApi {

    @POST("authenticate")
    Call<LoginResponse> login(@Header("Content-Type") String contentType, @Body LoginRequest loginRequest);

    @PUT("{clientId}/pingresponse/{transactionId}")
    Call<HttpResponse> ping(@HeaderMap Map<String, String> headers,
                            @Path("clientId") String clientId,
                            @Path("transactionId") String transactionId);
}
