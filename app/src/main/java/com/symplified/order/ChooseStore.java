package com.symplified.order;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActionBar;
import android.app.Dialog;
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

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.symplified.order.adapters.StoreAdapter;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.firebase.FirebaseHelper;
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
    private Dialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_store);
        RecyclerView recyclerView = findViewById(R.id.store_recycler);
        TextView chooseStore = findViewById(R.id.choose_store);
        TextView noStore = findViewById(R.id.no_store);

        progressDialog = new Dialog(this);
        progressDialog.setContentView(R.layout.progress_dialog);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        sharedPreferences.edit().remove("logoImage").apply();

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
        final SharedPreferences finalSharedPreferences = sharedPreferences;
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Login.class);
                FirebaseMessaging.getInstance().unsubscribeFromTopic(finalSharedPreferences.getString("storeId", null));
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





        Retrofit retrofit = new Retrofit.Builder().baseUrl(App.PRODUCT_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        StoreApi storeApiService = retrofit.create(StoreApi.class);
        sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        String clientId = sharedPreferences.getString("ownerId", null);

        if(null == clientId)
        {
            Log.d("Client-ID", "onCreate: client id is null");
//            Toast.makeText(this, "Client id is null", Toast.LENGTH_SHORT).show();
        }

//        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Call<StoreResponse> storeResponse = storeApiService.getStores(headers, clientId);
        progressDialog.show();
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
                            StoreAdapter storeAdapter = new StoreAdapter(response.body().data.content, getApplicationContext(), progressDialog);
                            recyclerView.setLayoutManager(new LinearLayoutManager(ChooseStore.this));
                            recyclerView.setAdapter(storeAdapter);
                    }
                }
                progressDialog.hide();
            }

            @Override
            public void onFailure(Call<StoreResponse> call, Throwable t) {
                Log.e("TAG", "onFailure: ", t.getCause());
                progressDialog.hide();
            }
        });


//        StoreAdapter storeAdapter = new StoreAdapter(storeList.stores);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        recyclerView.setAdapter(storeAdapter);
    }
}