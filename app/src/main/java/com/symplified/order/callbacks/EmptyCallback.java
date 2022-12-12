package com.symplified.order.callbacks;

import androidx.annotation.NonNull;

import com.symplified.order.models.HttpResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmptyCallback implements Callback<Void> {
    @Override
    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {}

    @Override
    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
}
