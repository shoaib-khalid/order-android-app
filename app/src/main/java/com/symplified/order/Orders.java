package com.symplified.order;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.databinding.ActivityOrdersBinding;
import com.symplified.order.models.asset.Asset;
import com.symplified.order.services.AlertService;
import com.symplified.order.services.DownloadImageTask;
import com.symplified.order.ui.main.SectionsPagerAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Orders extends AppCompatActivity {

    private ActivityOrdersBinding binding;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(2);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);
        stopService(new Intent(this, AlertService.class));
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);

        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(4, new Intent().putExtra("finish", 1));
                Intent intent = new Intent(getApplicationContext(), ChooseStore.class);
                FirebaseMessaging.getInstance().unsubscribeFromTopic(sharedPreferences.getString("storeId", null));
                sharedPreferences.edit().remove("storeId").apply();
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        ImageView logout = toolbar.findViewById(R.id.app_bar_logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Login.class);
                sharedPreferences.edit().clear().apply();
                startActivity(intent);
                finish();
            }
        });

        ImageView storeLogo = toolbar.findViewById(R.id.app_bar_logo);
        Retrofit retrofitLogo = new Retrofit.Builder().baseUrl(App.PRODUCT_SERVICE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        StoreApi storeApiSerivice = retrofitLogo.create(StoreApi.class);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Call<ResponseBody> responseLogo = storeApiSerivice.getStoreLogo(headers, sharedPreferences.getString("storeId", "McD"));

        responseLogo.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.isSuccessful()){
                    try {
                        Asset.AssetResponse responseBody = new Gson().fromJson(response.body().string(), Asset.AssetResponse.class);
                        new DownloadImageTask(storeLogo).execute(responseBody.data.logoUrl);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(getApplicationContext(), "onActivityResult Called", Toast.LENGTH_SHORT).show();
            if(data != null && data.getIntExtra("finish",0) == 1){
                this.finishActivity(4);
//                this.finish();
            }
    }

}