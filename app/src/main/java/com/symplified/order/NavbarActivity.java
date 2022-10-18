package com.symplified.order;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.models.store.Store;
import com.symplified.order.models.store.StoreResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.services.DownloadImageTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NavbarActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private String storeId, BASE_URL;
    private DrawerLayout drawerLayout;
    private ImageView storeLogo;
    private TextView storeName, storeEmail, appVersion;
    private String version;
    private NavigationView navigationView;
    private static SharedPreferences sharedPreferences;
    private StoreApi storeApiService;
    public FrameLayout frameLayout;

    @Override
    public void setContentView(View view) {
        Log.d("navbar", "setContentView");
        drawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_navbar, null);

        frameLayout = drawerLayout.findViewById(R.id.navbar_framelayout);
        frameLayout.addView(view);
        super.setContentView(drawerLayout);

        navigationView = findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);

        version = BuildConfig.VERSION_NAME;

        sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);

        storeId = sharedPreferences.getString("storeId", null);

        Log.d("imran-debug-navbar", "StoreId: " + storeId);

        BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);

        navigationView = drawerLayout.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);

        storeApiService = ServiceGenerator.createStoreService();
        setUpNavbarData(sharedPreferences, header);
    }

    public void setUpNavbarData(SharedPreferences sharedPreferences, View header) {
        storeLogo = header.findViewById(R.id.nav_store_logo);
        storeName = header.findViewById(R.id.nav_store_name);
        storeEmail = header.findViewById(R.id.nav_store_email);
        appVersion = navigationView.findViewById(R.id.nav_app_version);
        appVersion.setText("Symplified 2022 | version " + version);


        Map<String, String> headers = new HashMap<>();
//        headers.put("Authorization", "Bearer accessToken");

//        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient())
//                .baseUrl(BASE_URL + App.PRODUCT_SERVICE_URL)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        StoreApi storeApi = retrofit.create(StoreApi.class);

        Call<ResponseBody> storeResponse = storeApiService.getStoreById(headers, storeId);

        storeResponse.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        StoreResponse.SingleStoreResponse responseBody
                                = new Gson().fromJson(response.body().string(), StoreResponse.SingleStoreResponse.class);
                        if (responseBody != null) {
                            for (Store.StoreAsset asset : responseBody.data.storeAssets) {
                                if (asset.assetType.equals("LogoUrl")) {
                                    try {
                                        Bitmap bitmap = new DownloadImageTask().execute(asset.assetUrl).get();
                                        if (bitmap != null) {
                                            storeLogo.setImageBitmap(bitmap);
                                        }

                                    } catch (ExecutionException e) {
                                        e.printStackTrace();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            storeName.setText(responseBody.data.name);
                            storeEmail.setText(responseBody.data.email);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
            }
        });

        TextView logout = navigationView.findViewById(R.id.nav_logout);


        if (sharedPreferences.getBoolean("isStaging", false))
            logout.setVisibility(View.VISIBLE);

        logout.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            String storeIdList = sharedPreferences.getString("storeIdList", null);
            if (storeIdList != null) {
                for (String storeId : storeIdList.split(" ")) {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(storeId);
                }
            }
            boolean isStaging = sharedPreferences.getBoolean("isStaging", false);
            sharedPreferences.edit().clear().apply();
            sharedPreferences.edit().putBoolean("isStaging", isStaging).apply();

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                drawerLayout.closeDrawer(GravityCompat.START);
                int id = item.getItemId();
                Intent intent;
                switch (id) {
                    case R.id.nav_orders:
                        if (!item.isChecked()) {
                            intent = new Intent(getApplicationContext(), OrdersActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(NavbarActivity.this, "Opened", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case R.id.nav_products:
                        if (!item.isChecked()) {
                            intent = new Intent(getApplicationContext(), ProductsActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(NavbarActivity.this, "Opened", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case R.id.nav_stores:
                        if (!item.isChecked()) {
                            intent = new Intent(getApplicationContext(), SettingsActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(NavbarActivity.this, "Opened", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                return false;
            }
        });
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
