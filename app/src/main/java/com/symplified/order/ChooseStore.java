package com.symplified.order;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.symplified.order.adapters.StoreAdapter;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.firebase.FirebaseHelper;
import com.symplified.order.models.Store.Store;
import com.symplified.order.models.Store.StoreResponse;
import com.symplified.order.models.asset.Asset;
import com.symplified.order.services.DownloadImageTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChooseStore extends AppCompatActivity {

    private String BASE_URL;
    private Toolbar toolbar;
    private Dialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_store);
        RecyclerView recyclerView = findViewById(R.id.store_recycler);
        TextView chooseStore = findViewById(R.id.choose_store);
        TextView noStore = findViewById(R.id.no_store);

        progressDialog = new Dialog(this, R.style.Theme_SymplifiedOrderUpdate);
        progressDialog.setContentView(R.layout.progress_dialog);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        sharedPreferences.edit().remove("logoImage").apply();
        if (sharedPreferences.getBoolean("isStaging", false))
            setTheme(R.style.Theme_SymplifiedOrderUpdate_Test);
        BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);
        ImageView home = toolbar.findViewById(R.id.app_bar_home);

        ImageView logout = toolbar.findViewById(R.id.app_bar_logout);
        final SharedPreferences finalSharedPreferences = sharedPreferences;
        logout.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            FirebaseMessaging.getInstance().unsubscribeFromTopic(finalSharedPreferences.getString("storeId", null));
            finalSharedPreferences.edit().clear().apply();
            startActivity(intent);
            finish();
        });

        ImageView storeLogo = toolbar.findViewById(R.id.app_bar_logo);
        Retrofit retrofitLogo = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.PRODUCT_SERVICE_URL).addConverterFactory(GsonConverterFactory.create()).build();

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");
        storeLogo.setBackgroundResource(R.drawable.header);


        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.PRODUCT_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        StoreApi storeApiService = retrofit.create(StoreApi.class);
        sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        String clientId = sharedPreferences.getString("ownerId", null);

        if (null == clientId) {
            Log.d("Client-ID", "onCreate: client id is null");
//            Toast.makeText(this, "Client id is null", Toast.LENGTH_SHORT).show();
        }
        headers.put("Authorization", "Bearer Bearer accessToken");

        Call<StoreResponse> storeResponse = storeApiService.getStores(headers, clientId);
//        progressDialog.show();
        /*
        storeResponse.clone().enqueue(new Callback<StoreResponse>() {
            @Override
            public void onResponse(Call<StoreResponse> call, Response<StoreResponse> response) {
                if (response.isSuccessful()) {
                    Log.e("TAG", "onResponse: " + response.body().data.content, new Error());
                    if (response.body().data.content.isEmpty()) {
                        chooseStore.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.GONE);
                        noStore.setVisibility(View.VISIBLE);
                    } else {
                        if (response.body().data.content.size() == 1) {
//                            progressDialog.hide();
                            setStoreData(getApplicationContext(), response.body().data.content);
                        } else {
                            StoreAdapter storeAdapter = new StoreAdapter(response.body().data.content, getApplicationContext(), progressDialog);
                            recyclerView.setLayoutManager(new LinearLayoutManager(ChooseStore.this));
                            recyclerView.setAdapter(storeAdapter);
                            progressDialog.hide();
                        }
                    }
                }
//                progressDialog.hide();
            }

            @Override
            public void onFailure(Call<StoreResponse> call, Throwable t) {
                Log.e("TAG", "onFailure: ", t.getCause());
                progressDialog.hide();
            }
        }); */
    }

    public void setStoreData(Context context, List<Store> stores) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("timezone", stores.get(0).regionCountry.timezone).apply();
        editor.putString("storeId", stores.get(0).id).apply();

        String BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);
        Retrofit retrofitLogo = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.PRODUCT_SERVICE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        StoreApi storeApiSerivice = retrofitLogo.create(StoreApi.class);

        Log.e("TAG", "onEnterLogoUrl: " + sharedPreferences.getAll(), new Error());
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Call<ResponseBody> responseLogo = storeApiSerivice.getStoreLogo(headers, sharedPreferences.getString("storeId", "McD"));
        Intent intent = new Intent(context, Orders.class);

        responseLogo.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    Asset.AssetResponse responseBody = new Gson().fromJson(response.body().string(), Asset.AssetResponse.class);

                    if (responseBody.data != null) {
                        Bitmap bitmap = new DownloadImageTask().execute(responseBody.data.logoUrl).get();
                        if (bitmap != null) {
                            Log.e("TAG", "bitmapLogo: " + bitmap, new Error());
//                                String bitmap64 = ImageUtil.encodeTobase64(bitmap);
//                                Log.e("TAG", "bitmap: "+ bitmap64, new Error() );
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
//                                intent.putExtra("logo", byteArrayOutputStream.toByteArray());
                            String encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
                            editor.putString("logoImage", encodedImage);
                            editor.apply();
                        }
                    }

                    FirebaseHelper.initializeFirebase(stores.get(0).id, context);
                    Log.e("TAG", "preferences: " + sharedPreferences.getAll(), new Error());
//                            Toast.makeText(view.getContext(), "Store id : " + (items.get(holder.getAdapterPosition()).id), Toast.LENGTH_SHORT).show();
//                    progressDialog.hide();
                    startActivity(intent);
                    finish();


                } catch (IOException | ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressDialog.hide();

            }
        });


    }
}