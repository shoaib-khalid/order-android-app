package com.symplified.order.callbacks;

import androidx.annotation.NonNull;

import com.symplified.order.models.HttpResponse;

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
