package com.symplified.order;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.messaging.FirebaseMessaging;
import com.symplified.order.databinding.ActivityOrdersBinding;
import com.symplified.order.services.AlertService;
import com.symplified.order.ui.tabs.SectionsPagerAdapter;

public class OrdersActivity extends NavbarActivity {

    private ActivityOrdersBinding binding;
    private Toolbar toolbar;
    private ViewPager mViewPager;
    private DrawerLayout drawerLayout;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = findViewById(R.id.drawer_layout);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        if(sharedPreferences.getBoolean("isStaging", false))
//            setTheme(R.style.Theme_SymplifiedOrderUpdate_Test);

        initToolbar(sharedPreferences);

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(0);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);

        mViewPager = viewPager;

        stopService(new Intent(this, AlertService.class));
    }

    private void initToolbar(SharedPreferences sharedPreferences) {
        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(getDrawable(R.drawable.ic_baseline_menu_24));
        home.setOnClickListener(view -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });
        TextView title = toolbar.findViewById(R.id.app_bar_title);
        title.setText("Your Orders");
        NavigationView navigationView = drawerLayout.findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(0).setChecked(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
            if(resultCode == RESULT_OK){
                this.finishActivity(4);
                this.finish();
            }
    }

    @Override
    public void onBackPressed() {
        if(mViewPager.getCurrentItem() != 0){
            mViewPager.setCurrentItem(0);
        }else{
            super.onBackPressed();
        }
    }
}