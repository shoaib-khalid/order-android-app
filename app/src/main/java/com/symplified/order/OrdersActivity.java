package com.symplified.order;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.messaging.FirebaseMessaging;
import com.symplified.order.databinding.ActivityOrdersBinding;
import com.symplified.order.services.AlertService;
import com.symplified.order.ui.tabs.SectionsPagerAdapter;
import com.symplified.order.utils.Key;

import java.util.ResourceBundle;

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

        drawerLayout = findViewById(R.id.drawer_layout);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initToolbar();

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(0);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);

        mViewPager = viewPager;

        stopService(new Intent(this, AlertService.class));

        subscribeToFirebase();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initToolbar() {
        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_baseline_menu_24));
        home.setOnClickListener(view -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });
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

    private void subscribeToFirebase() {
        SharedPreferences sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        String storeIdList = sharedPreferences.getString("storeIdList", null);
        for (String storeId : storeIdList.split(" ")) {
            FirebaseMessaging.getInstance().subscribeToTopic(storeId)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            logout();
                        }
                    });
        }
    }

    private void logout() {
        SharedPreferences sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        String storeIdList = sharedPreferences.getString("storeIdList", null);
        if (storeIdList != null) {
            for (String storeId : storeIdList.split(" ")) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(storeId);
            }
        }
        boolean isStaging = sharedPreferences.getBoolean(Key.IS_STAGING, false);
        String baseUrl = sharedPreferences.getString(Key.BASE_URL, App.BASE_URL_PRODUCTION);
        sharedPreferences.edit().clear().apply();
        sharedPreferences.edit()
                .putBoolean(Key.IS_STAGING, isStaging)
                .putString(Key.BASE_URL, baseUrl)
                .apply();

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}