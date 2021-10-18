package com.symplified.order.handlers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.symplified.order.App;
import com.symplified.order.Login;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.models.asset.Asset;
import com.symplified.order.services.DownloadImageTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


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
//    ImageView imageView;
    String clientId;
    public LogoHandler(String[] storeIdList, Context context, Handler handler, String clientId){
        this.stores = storeIdList;
        this.context = context;
        this.handler = handler;
//        this.imageView = imageView;
        this.clientId = clientId;
    }

    @Override
    public void run() {

        SharedPreferences sharedPreferences = context.getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Retrofit retrofitLogo = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(sharedPreferences.getString("base_url", App.BASE_URL) + App.PRODUCT_SERVICE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        StoreApi storeApiSerivice = retrofitLogo.create(StoreApi.class);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Call<ResponseBody> getAllLogosCall = storeApiSerivice.getAllAssets(headers, clientId);

        getAllLogosCall.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.isSuccessful()){

                    try {
                        Asset.AllAssetResponse assets = new Gson().fromJson(response.body().string(), Asset.AllAssetResponse.class);

                        if (assets.data != null) {

                            for(Asset asset : assets.data){

                                Bitmap bitmap = new DownloadImageTask().execute(asset.logoUrl).get();
                                if (bitmap != null) {
                                    Log.e("TAG", "bitmapLogo: " + bitmap, new Error());
                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
                                    String encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
//                                    handler.post(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            imageView.setImageBitmap(bitmap);
//                                        }
//                                    });
                                    editor.putString("logoImage-"+asset.storeId, encodedImage);
                                    editor.apply();
                                }

                            }
                        }



                    } catch (IOException | InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });

//        for(String store : stores) {
//
//            Call<ResponseBody> responseLogo = storeApiSerivice.getStoreLogo(headers, store);
//
//            responseLogo.clone().enqueue(new Callback<ResponseBody>() {
//                @Override
//                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                    if(response.isSuccessful()){
//
//                        try{
//                            Asset.AssetResponse responseBody = new Gson().fromJson(response.body().string(), Asset.AssetResponse.class);
//
//                            if (responseBody.data != null) {
//                                Bitmap bitmap = new DownloadImageTask().execute(responseBody.data.logoUrl).get();
//                                if (bitmap != null) {
//                                    Log.e("TAG", "bitmapLogo: " + bitmap, new Error());
//                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                                    bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
//                                    String encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
////                                    handler.post(new Runnable() {
////                                        @Override
////                                        public void run() {
////                                            imageView.setImageBitmap(bitmap);
////                                        }
////                                    });
//                                    editor.putString("logoImage-"+store, encodedImage);
//                                    editor.apply();
//                                }
//                            }
//                        }catch (Exception e){
//                            Log.e("LogoFetch", "onResponse: ",e );
//                        }
//
//                    }
//                }
//
//                @Override
//                public void onFailure(Call<ResponseBody> call, Throwable t) {
//                    Log.e("LogoFetch", "onResponse: ",t.getCause() );
//                }
//            });
//
//        }

        sharedPreferences.edit().putInt("hasLogos", 1).apply();


        Log.e("LogoHandlerTAG", "run: Image Download Complete. Stopping the thread", new Error());
//        callback.onLogoDownloadCompleteListener();

//        ((Activity) context).finish();
//        ((Activity) context).overridePendingTransition(0, 0);
//        context.startActivity(((Activity) context).getIntent());
//        ((Activity) context).overridePendingTransition(0, 0);

    }

}
