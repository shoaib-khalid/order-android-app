package com.symplified.order;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.symplified.order.adapters.StoreAdapter;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.models.HttpResponse;
import com.symplified.order.models.Store.Store;
import com.symplified.order.models.Store.StoreResponse;
import com.symplified.order.models.asset.Asset;
import com.symplified.order.services.DownloadImageTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChooseStore extends AppCompatActivity {

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_store);
        RecyclerView recyclerView = findViewById(R.id.store_recycler);
        TextView chooseStore = findViewById(R.id.choose_store);
        TextView noStore = findViewById(R.id.no_store);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);

        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                setResult(4, new Intent().putExtra("finish", 1));
//                Intent intent = new Intent(getApplicationContext(), ChooseStore.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(intent);
//                finish();
            }
        });

        ImageView logout = toolbar.findViewById(R.id.app_bar_logout);
        SharedPreferences finalSharedPreferences = sharedPreferences;
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Login.class);
                finalSharedPreferences.edit().clear().apply();
                startActivity(intent);
                finish();
            }
        });

        ImageView storeLogo = toolbar.findViewById(R.id.app_bar_logo);
        Retrofit retrofitLogo = new Retrofit.Builder().baseUrl(App.PRODUCT_SERVICE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        StoreApi storeApiSerivice = retrofitLogo.create(StoreApi.class);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");
        storeLogo.setBackgroundResource(R.drawable.header);

//        Call<ResponseBody> responseLogo = storeApiSerivice.getStoreLogo(headers, sharedPreferences.getString("storeId", "McD"));
//
//        responseLogo.clone().enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                if(response.isSuccessful()){
//                    try {
//                        Asset.AssetResponse responseBody = new Gson().fromJson(response.body().string(), Asset.AssetResponse.class);
//                        new DownloadImageTask(storeLogo).execute(responseBody.data.logoUrl);
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//
//            }
//        });




        Retrofit retrofit = new Retrofit.Builder().baseUrl(App.PRODUCT_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        StoreApi storeApiService = retrofit.create(StoreApi.class);
        sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        String clientId = sharedPreferences.getString("ownerId", null);

        if(null == clientId)
        {
            Toast.makeText(this, "Client id is null", Toast.LENGTH_SHORT).show();
        }

//        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Call<StoreResponse> storeResponse = storeApiService.getStores(headers, clientId);

        storeResponse.clone().enqueue(new Callback<StoreResponse>() {
            @Override
            public void onResponse(Call<StoreResponse> call, Response<StoreResponse> response) {
                if(response.isSuccessful())
                {
                    Log.e("TAG", "onResponse: "+ response.body().data.content, new Error() );
                    if(response.body().data.content.isEmpty())
                    {
                        chooseStore.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.GONE);
                        noStore.setVisibility(View.VISIBLE);
                    }
                    else{
                            StoreAdapter storeAdapter = new StoreAdapter(response.body().data.content);
                            recyclerView.setLayoutManager(new LinearLayoutManager(ChooseStore.this));
                            recyclerView.setAdapter(storeAdapter);
                    }
                }
            }

            @Override
            public void onFailure(Call<StoreResponse> call, Throwable t) {
                Log.e("TAG", "onFailure: ", t.getCause());
            }
        });


//        StoreAdapter storeAdapter = new StoreAdapter(storeList.stores);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        recyclerView.setAdapter(storeAdapter);
    }
}