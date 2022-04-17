package com.symplified.easydukan.apis;

import com.symplified.easydukan.models.login.LoginRequest;
import com.symplified.easydukan.models.login.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface LoginApi {

    @POST("authenticate")
    Call<LoginResponse> login(@Header("Content-Type") String contentType, @Body LoginRequest loginRequest);

}
