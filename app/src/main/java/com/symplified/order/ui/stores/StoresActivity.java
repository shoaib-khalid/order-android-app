package com.symplified.order.ui.stores;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.databinding.ActivitySettingsBinding;
import com.symplified.order.ui.NavbarActivity;
import com.symplified.order.ui.orders.OrdersActivity;
import com.symplified.order.utils.Utility;

public class StoresActivity extends NavbarActivity {

    private Toolbar toolbar;
    private ActivitySettingsBinding binding;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Utility.verifyLoginStatus(this);

        drawerLayout = findViewById(R.id.drawer_layout);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initToolbar();

        String clientId = getSharedPreferences(App.SESSION, MODE_PRIVATE)
                .getString("ownerId", null);

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.settings_frame_layout, new StoreSelectionFragment(clientId))
                .commit();
    }

    private void initToolbar() {

        NavigationView navigationView = drawerLayout.findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(1).setChecked(true);

        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(getDrawable(R.drawable.ic_arrow_back_black_24dp));
        home.setOnClickListener(view -> StoresActivity.super.onBackPressed());

        TextView title = toolbar.findViewById(R.id.app_bar_title);
        title.setText("Stores");
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, OrdersActivity.class);
        startActivity(intent);
        finish();
    }
}