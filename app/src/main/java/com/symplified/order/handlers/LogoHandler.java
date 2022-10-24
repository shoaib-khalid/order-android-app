package com.symplified.order.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.symplified.order.App;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.models.asset.Asset;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.services.DownloadImageTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LogoHandler implements Runnable {
    String[] stores;
    Context context;
    Handler handler;
    String clientId;
    StoreApi storeApiService;

    public LogoHandler(String[] storeIdList, Context context, Handler handler, String clientId) {
        this.stores = storeIdList;
        this.context = context;
        this.handler = handler;
        this.clientId = clientId;
        storeApiService = ServiceGenerator.createStoreService();
    }

    @Override
    public void run() {

        Call<Asset.AssetListResponse> getAllLogosCall = storeApiService.getAllAssets(clientId);

        getAllLogosCall.clone().enqueue(new Callback<Asset.AssetListResponse>() {
            @Override
            public void onResponse(Call<Asset.AssetListResponse> call, Response<Asset.AssetListResponse> response) {
                Log.d("getAllLogos", "getAllLogos onResponse: " + response.raw());
                if (response.isSuccessful()) {
                    new Thread(() -> {
                        for (Asset asset : response.body().data) {
                            try {
                                Bitmap bitmap = new DownloadImageTask().execute(asset.logoUrl).get();
                                Log.i("getAllLogos", "bitmapLogo: " + bitmap);
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
                                String encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
                                context.getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("logoImage-" + asset.storeId, encodedImage)
                                        .apply();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }

            @Override
            public void onFailure(Call<Asset.AssetListResponse> call, Throwable t) {

            }
        });
    }

}
