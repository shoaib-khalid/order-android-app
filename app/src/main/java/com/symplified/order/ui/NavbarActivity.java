package com.symplified.order.ui;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.symplified.order.App;
import com.symplified.order.BuildConfig;
import com.symplified.order.R;
import com.symplified.order.enums.NavIntentStaff;
import com.symplified.order.enums.NavIntentStore;
import com.symplified.order.models.store.Store;
import com.symplified.order.models.store.StoreResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.ui.orders.OrdersActivity;
import com.symplified.order.ui.products.ProductsActivity;
import com.symplified.order.ui.settings.SettingsActivity;
import com.symplified.order.ui.staff.StaffActivity;
import com.symplified.order.ui.stores.StoresActivity;
import com.symplified.order.utils.SharedPrefsKey;
import com.symplified.order.utils.Utility;

import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NavbarActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private String storeId;
    private DrawerLayout drawerLayout;
    private ImageView storeLogo;
    private TextView storeName, storeEmail;
    private NavigationView navigationView;
    public FrameLayout frameLayout;

    private BroadcastReceiver btReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utility.verifyLoginStatus(this);

        syncPairedBtDevices();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        try {
//            unregisterReceiver(btReceiver);
//        } catch (Exception ignored) {
//        }
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
                .setText(getString(R.string.version_indicator, BuildConfig.VERSION_NAME));

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

        logout.setOnClickListener(view -> Utility.logout(this));

        navigationView.setNavigationItemSelectedListener(item -> {

            drawerLayout.closeDrawer(GravityCompat.START);
            int id = item.getItemId();
            Intent intent;
            switch (id) {
                case R.id.nav_orders:
                    if (!item.isChecked()) {
                        intent = new Intent(getApplicationContext(), OrdersActivity.class);
                        startActivity(intent);
                    }
                    break;
                case R.id.nav_products:
                    if (!item.isChecked()) {
                        intent = new Intent(getApplicationContext(), ProductsActivity.class);
                        startActivity(intent);
                    }
                    break;
                case R.id.nav_stores:
                    if (!item.isChecked()) {
                        intent = new Intent(getApplicationContext(), StoresActivity.class);
                        intent.putExtra("action", NavIntentStore.SET_STORE_TIMING);
                        startActivity(intent);
                    }
                    break;
                case R.id.nav_qr_code:
                    if (!item.isChecked()) {
                        intent = new Intent(getApplicationContext(), StoresActivity.class);
                        intent.putExtra("action", NavIntentStore.DISPLAY_QR_CODE);
                        startActivity(intent);
                    }
                    break;
                case R.id.nav_daily_sales:
                    if (!item.isChecked()) {
                        intent = new Intent(getApplicationContext(), StaffActivity.class);
                        intent.putExtra("action", NavIntentStaff.VIEW_DAILY_SALES);
                        startActivity(intent);
                    }
                    break;
                case R.id.nav_manage_staff:
                    if (!item.isChecked()) {
                        intent = new Intent(getApplicationContext(), StaffActivity.class);
                        intent.putExtra("action", NavIntentStaff.MANAGE_STAFF);
                        startActivity(intent);
                    }
                    break;
                case R.id.nav_system_config:
                    if (!item.isChecked()) {
                        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                    }
                    break;
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


    private void syncPairedBtDevices() {
        new Thread() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        && ContextCompat.checkSelfPermission(
                        getApplicationContext(), BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(
                            NavbarActivity.this,
                            new String[]{BLUETOOTH_CONNECT},
                            App.PERMISSION_REQUEST_CODE
                    );
                    return;
                }

                BluetoothAdapter adapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
                Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
                Log.d("navbar-activity", "Syncing paired bt devices. Size: " + pairedDevices.size());

                if (pairedDevices != null) {
                    for (BluetoothDevice btDevice : pairedDevices) {
                        App.addBtPrinter(btDevice, getApplicationContext());
                    }
                }
            }
        }.start();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == App.PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            syncPairedBtDevices();
        }
    }
}
