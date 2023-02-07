package com.symplified.order.ui.orders;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.messaging.FirebaseMessaging;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.databinding.ActivityOrdersBinding;
import com.symplified.order.models.store.StoreResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.networking.apis.StoreApi;
import com.symplified.order.services.AlertService;
import com.symplified.order.services.OrderNotificationService;
import com.symplified.order.ui.LoginActivity;
import com.symplified.order.ui.NavbarActivity;
import com.symplified.order.ui.orders.tabs.SectionsPagerAdapter;
import com.symplified.order.utils.ChannelId;
import com.symplified.order.utils.SharedPrefsKey;
import com.symplified.order.utils.Utility;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrdersActivity extends NavbarActivity {

    private Toolbar toolbar;
    private ViewPager mViewPager;
    private DrawerLayout drawerLayout;
    private ActivityOrdersBinding binding;
    private SectionsPagerAdapter sectionsPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Utility.verifyLoginStatus(this);

        drawerLayout = findViewById(R.id.drawer_layout);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initToolbar();

        SharedPreferences sharedPrefs = getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);

        List<String> tabTitles = new ArrayList<>();
        tabTitles.add(getString(R.string.new_orders));
        tabTitles.add(getString(R.string.ongoing_orders));
        tabTitles.add(getString(R.string.past_orders));
        if (sharedPrefs.getBoolean(SharedPrefsKey.IS_ORDER_CONSOLIDATION_ENABLED, false)) {
            tabTitles.add(getString(R.string.unpaid_orders));
        }

        sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager(), tabTitles);
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(4);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);
        mViewPager = viewPager;

        if (!sharedPrefs.getBoolean(SharedPrefsKey.IS_ORDER_CONSOLIDATION_ENABLED, false)) {
            if (Utility.isConnectedToInternet(this)) {
                queryStoresForConsolidateOption();
            } else {
                ConnectivityManager connMan =
                        getSystemService(ConnectivityManager.class);
                NetworkRequest networkRequest = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                        .build();
                connMan.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        queryStoresForConsolidateOption();
                    }
                });
                Toast.makeText(this, "Unable to sync stores with server. Please connect to the internet.", Toast.LENGTH_SHORT).show();
            }
        }

        stopService(new Intent(this, AlertService.class));

        boolean isSubscribedToNotifications = sharedPrefs.getBoolean(SharedPrefsKey.IS_SUBSCRIBED_TO_NOTIFICATIONS, false);
        if (!isSubscribedToNotifications) {
            verifyFirebaseConnection();
        }
        OrderNotificationService.enableOrderNotifications();
    }

    private void initToolbar() {
        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_baseline_menu_24));
        home.setOnClickListener(view -> drawerLayout.openDrawer(GravityCompat.START));

        TextView title = toolbar.findViewById(R.id.app_bar_title);
        title.setText("Your Orders");
        NavigationView navigationView = drawerLayout.findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(0).setChecked(true);
    }

    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem() != 0) {
            mViewPager.setCurrentItem(0);
        } else {
            finishAffinity();
        }
    }

    /**
     * Check if firebase server is reachable when internet is available and logout if unreachable.
     */
    private void verifyFirebaseConnection() {
        ConnectivityManager connMan =
                getSystemService(ConnectivityManager.class);
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();
        connMan.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                ServiceGenerator.createFirebaseService().ping().clone().enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            SharedPreferences sharedPreferences = getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);
                            String storeIdList = sharedPreferences.getString(SharedPrefsKey.STORE_ID_LIST, null);
                            if (storeIdList != null) {
                                for (String storeId : storeIdList.split(" ")) {
                                    FirebaseMessaging.getInstance().subscribeToTopic(storeId)
                                            .addOnSuccessListener(unused -> {
                                                sharedPreferences.edit()
                                                        .putBoolean(SharedPrefsKey.IS_SUBSCRIBED_TO_NOTIFICATIONS, true)
                                                        .apply();
                                            }).addOnFailureListener(e -> logoutWithFirebaseErrorNotification());
                                }
                            }
                        } else {
                            logoutWithFirebaseErrorNotification();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        logoutWithFirebaseErrorNotification();
                    }
                });
            }
        });
    }

    boolean isOrderConsolidationEnabled = false;
    private void queryStoresForConsolidateOption() {

        StoreApi storeApiService = ServiceGenerator.createStoreService(getApplicationContext());
        SharedPreferences sharedPrefs = getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);
        isOrderConsolidationEnabled = sharedPrefs.getBoolean(SharedPrefsKey.IS_ORDER_CONSOLIDATION_ENABLED, false);

        for (String storeId : sharedPrefs.getString(SharedPrefsKey.STORE_ID_LIST, "")
                .split(" ")) {
            storeApiService.getStoreById(storeId).clone().enqueue(new Callback<StoreResponse.SingleStoreResponse>() {
                @Override
                public void onResponse(
                        @NonNull Call<StoreResponse.SingleStoreResponse> call,
                        @NonNull Response<StoreResponse.SingleStoreResponse> response
                ) {

                    if (response.isSuccessful()
                            && response.body() != null
                            && response.body().data.dineInConsolidatedOrder
                            && !isOrderConsolidationEnabled) {
                        isOrderConsolidationEnabled = true;
                        sharedPrefs.edit()
                                .putBoolean(SharedPrefsKey.IS_ORDER_CONSOLIDATION_ENABLED, true)
                                .apply();
                        sectionsPagerAdapter.showUnpaidTab();
                        sectionsPagerAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<StoreResponse.SingleStoreResponse> call, @NonNull Throwable t) {}
            });
        }
    }

    private void logoutWithFirebaseErrorNotification() {
        Utility.notify(
                getApplicationContext(),
                getString(R.string.notif_firebase_error_title),
                getString(R.string.notif_firebase_error_text),
                getString(R.string.notif_firebase_error_text_full),
                ChannelId.ERRORS,
                ChannelId.ERRORS_NOTIF_ID,
                LoginActivity.class
        );

        Utility.logout(this);
    }
}