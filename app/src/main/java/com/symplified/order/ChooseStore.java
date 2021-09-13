package com.symplified.order;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.symplified.order.adapters.StoreAdapter;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.models.HttpResponse;
import com.symplified.order.models.Store.Store;
import com.symplified.order.models.Store.StoreResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChooseStore extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_store);
        RecyclerView recyclerView = findViewById(R.id.store_recycler);
        TextView chooseStore = findViewById(R.id.choose_store);
        TextView noStore = findViewById(R.id.no_store);




        Retrofit retrofit = new Retrofit.Builder().baseUrl(App.PRODUCT_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        StoreApi storeApiService = retrofit.create(StoreApi.class);
        SharedPreferences sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        String clientId = sharedPreferences.getString("ownerId", null);

        if(null == clientId)
        {
            Toast.makeText(this, "Client id is null", Toast.LENGTH_SHORT).show();
        }

        Map<String, String> headers = new HashMap<>();
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