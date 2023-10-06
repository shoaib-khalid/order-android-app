package com.ekedai.merchant.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.ekedai.merchant.App;
import com.ekedai.merchant.BuildConfig;
import com.ekedai.merchant.R;
import com.ekedai.merchant.enums.NavIntentStaff;
import com.ekedai.merchant.enums.NavIntentStore;
import com.ekedai.merchant.models.store.Store;
import com.ekedai.merchant.models.store.StoreResponse;
import com.ekedai.merchant.networking.ServiceGenerator;
import com.ekedai.merchant.ui.orders.OrdersActivity;
import com.ekedai.merchant.ui.products.ProductsActivity;
import com.ekedai.merchant.ui.staff.StaffActivity;
import com.ekedai.merchant.ui.stores.StoresActivity;
import com.ekedai.merchant.ui.voucher.VoucherActivity;
import com.ekedai.merchant.utils.SharedPrefsKey;
import com.ekedai.merchant.utils.Utilities;
import com.google.android.material.navigation.NavigationView;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NavbarActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private String storeId;
    private DrawerLayout drawerLayout;
    private ImageView storeLogo;
    private TextView storeName, storeEmail;
    private NavigationView navigationView;
    public FrameLayout frameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utilities.verifyLoginStatus(this);
    }

    @Override
    public void setContentView(View view) {
        drawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_navbar, null);

        frameLayout = drawerLayout.findViewById(R.id.navbar_framelayout);
        frameLayout.addView(view);
        super.setContentView(drawerLayout);

        navigationView = findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION, MODE_PRIVATE);

        storeId = sharedPreferences.getString("storeId", null);

        navigationView = drawerLayout.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);
        navigationView.bringToFront();
        navigationView.setVerticalScrollBarEnabled(true);

        setUpNavbarData(sharedPreferences, header);
    }

    public void setUpNavbarData(SharedPreferences sharedPreferences, View header) {
        storeLogo = header.findViewById(R.id.nav_store_logo);
        storeName = header.findViewById(R.id.nav_store_name);
        storeEmail = header.findViewById(R.id.nav_store_email);
        ((TextView) header.findViewById(R.id.nav_app_version))
                .setText(getString(
                        R.string.version_indicator,
                        Integer.toString(Calendar.getInstance().get(Calendar.YEAR)),
                        BuildConfig.VERSION_NAME
                ));

        ServiceGenerator.createStoreService(this).getStoreById(storeId)
                .enqueue(new Callback<StoreResponse.SingleStoreResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<StoreResponse.SingleStoreResponse> call,
                                           @NonNull Response<StoreResponse.SingleStoreResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            for (Store.StoreAsset asset : response.body().data.storeAssets) {
                                if (asset.assetType.equals("LogoUrl")) {
                                    Glide.with(getApplicationContext()).load(asset.assetUrl).into(storeLogo);
                                }
                            }
                            storeName.setText(response.body().data.name);
                            storeEmail.setText(response.body().data.email);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<StoreResponse.SingleStoreResponse> call,
                                          @NonNull Throwable t) {
                    }
                });

        TextView logout = header.findViewById(R.id.nav_logout);

        if (sharedPreferences.getBoolean(SharedPrefsKey.IS_STAGING, false)) {
            logout.setVisibility(View.VISIBLE);
        }

        logout.setOnClickListener(view -> Utilities.logout(this));

        navigationView.setNavigationItemSelectedListener(item -> {

            drawerLayout.closeDrawer(GravityCompat.START);

            if (item.isChecked()) {
                return false;
            }

            int id = item.getItemId();
            Intent intent = null;
            if (id == R.id.nav_orders) {
                intent = new Intent(getApplicationContext(), OrdersActivity.class);
            } else if (id == R.id.nav_vouchers) {
                intent = new Intent(getApplicationContext(), VoucherActivity.class);
            } else if (id == R.id.nav_products) {
                intent = new Intent(getApplicationContext(), ProductsActivity.class);
            } else if (id == R.id.nav_stores) {
                intent = new Intent(getApplicationContext(), StoresActivity.class);
                intent.putExtra("action", NavIntentStore.SET_STORE_TIMING);
            } else if (id == R.id.nav_qr_code) {
                intent = new Intent(getApplicationContext(), StoresActivity.class);
                intent.putExtra("action", NavIntentStore.DISPLAY_QR_CODE);
            } else if (id == R.id.nav_daily_sales) {
                intent = new Intent(getApplicationContext(), StaffActivity.class);
                intent.putExtra("action", NavIntentStaff.VIEW_DAILY_SALES);
            } else if (id == R.id.nav_manage_staff) {
                intent = new Intent(getApplicationContext(), StaffActivity.class);
                intent.putExtra("action", NavIntentStaff.MANAGE_STAFF);
            }
//            else if (id == R.id.nav_system_config) {
//                intent = new Intent(getApplicationContext(), SettingsActivity.class);
//            }

            if (intent != null) {
                startActivity(intent);
            }

            return false;
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
