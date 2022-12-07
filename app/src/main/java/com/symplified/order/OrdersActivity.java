package com.symplified.order;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.symplified.order.databinding.ActivityOrdersBinding;
import com.symplified.order.services.AlertService;
import com.symplified.order.ui.tabs.SectionsPagerAdapter;

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
}