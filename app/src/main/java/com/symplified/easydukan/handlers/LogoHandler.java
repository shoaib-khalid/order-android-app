package com.symplified.easydukan.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.symplified.easydukan.App;
import com.symplified.easydukan.apis.StoreApi;
import com.symplified.easydukan.models.asset.Asset;
import com.symplified.easydukan.services.DownloadImageTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LogoHandler implements Runnable{
    String[] stores;
    Context context;
    Handler handler;
    String clientId;
    public LogoHandler(String[] storeIdList, Context context, Handler handler, String clientId){
        this.stores = storeIdList;
        this.context = context;
        this.handler = handler;
        this.clientId = clientId;
    }

    @Override
    public void run() {

        SharedPreferences sharedPreferences = context.getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Retrofit retrofitLogo = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(sharedPreferences.getString("base_url", App.BASE_URL) + App.PRODUCT_SERVICE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        StoreApi storeApiService = retrofitLogo.create(StoreApi.class);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Call<ResponseBody> getAllLogosCall = storeApiService.getAllAssets(headers, clientId);

        getAllLogosCall.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.isSuccessful()){

                    try {
                        Asset.AllAssetResponse assets = new Gson().fromJson(response.body().string(), Asset.AllAssetResponse.class);

                        if (assets.data != null) {
                            new Thread(() -> {
                                try{
                                    for(Asset asset : assets.data){

                                        Bitmap bitmap = new DownloadImageTask().execute(asset.logoUrl).get();
                                        if (bitmap != null) {
                                            Log.e("TAG", "bitmapLogo: " + bitmap, new Error());
                                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                            bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
                                            String encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
                                            editor.putString("logoImage-"+asset.storeId, encodedImage);
                                            editor.apply();
                                        }

                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }).start();
                        }

                    } catch (IOException e
//                            | InterruptedException | ExecutionException e
                    ) {
                        e.printStackTrace();
                    }

                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
        sharedPreferences.edit().putInt("hasLogos", 1).apply();
    }

}
