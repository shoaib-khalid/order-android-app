package com.symplified.easydukan.callbacks;

import androidx.annotation.NonNull;

import com.symplified.easydukan.models.HttpResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmptyCallback implements Callback<HttpResponse> {
    @Override
    public void onResponse(@NonNull Call<HttpResponse> call, @NonNull Response<HttpResponse> response) {
    }

    @Override
    public void onFailure(@NonNull Call<HttpResponse> call, @NonNull Throwable t) {
    }
}
