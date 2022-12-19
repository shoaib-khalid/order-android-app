package com.symplified.order.ui.orders;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.services.AlertService;
import com.symplified.order.ui.LoginActivity;
import com.symplified.order.ui.NavbarActivity;
import com.symplified.order.ui.orders.tabs.SectionsPagerAdapter;
import com.symplified.order.utils.ChannelId;
import com.symplified.order.utils.Key;
import com.symplified.order.utils.Utility;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrdersActivity extends NavbarActivity {

    private Toolbar toolbar;
    private ViewPager mViewPager;
    private DrawerLayout drawerLayout;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityOrdersBinding binding = ActivityOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Utility.verifyLoginStatus(this);

        drawerLayout = findViewById(R.id.drawer_layout);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initToolbar();

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(2);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);

        mViewPager = viewPager;

        stopService(new Intent(this, AlertService.class));

        boolean isSubscribedToNotifications = getSharedPreferences(App.SESSION, Context.MODE_PRIVATE)
                .getBoolean(Key.IS_SUBSCRIBED_TO_NOTIFICATIONS, false);
        if (!isSubscribedToNotifications) {
            verifyFirebaseConnection();
        }
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
                            String storeIdList = sharedPreferences.getString(Key.STORE_ID_LIST, null);
                            if (storeIdList != null) {
                                for (String storeId : storeIdList.split(" ")) {
                                    FirebaseMessaging.getInstance().subscribeToTopic(storeId)
                                            .addOnSuccessListener(unused -> {
                                                sharedPreferences.edit()
                                                        .putBoolean(Key.IS_SUBSCRIBED_TO_NOTIFICATIONS, true)
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