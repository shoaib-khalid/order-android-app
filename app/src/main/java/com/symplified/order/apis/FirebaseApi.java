package com.symplified.order.apis;

import retrofit2.Call;
import retrofit2.http.GET;

public interface FirebaseApi {

    String BASE_URL = "https://firebase.google.com/";

    @GET(".")
    Call<Void> ping();
}
