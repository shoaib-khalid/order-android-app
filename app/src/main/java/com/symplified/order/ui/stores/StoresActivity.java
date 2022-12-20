package com.symplified.order.ui.stores;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.databinding.ActivitySettingsBinding;
import com.symplified.order.enums.NavIntentStore;
import com.symplified.order.ui.NavbarActivity;
import com.symplified.order.ui.orders.OrdersActivity;
import com.symplified.order.utils.Key;
import com.symplified.order.utils.Utility;

public class StoresActivity extends NavbarActivity
        implements StoreSelectionFragment.StoreFragmentListener {

    private Toolbar toolbar;
    private ActivitySettingsBinding binding;
    private DrawerLayout drawerLayout;

    private NavIntentStore action;

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
                .getString(Key.CLIENT_ID, null);

        Bundle bundle = new Bundle();
        bundle.putString(Key.CLIENT_ID, clientId);
        action = (NavIntentStore) getIntent().getExtras().getSerializable("action");
        if (action == NavIntentStore.DISPLAY_QR_CODE) {
            bundle.putSerializable("action", NavIntentStore.DISPLAY_QR_CODE);
        } else if (action == NavIntentStore.SET_STORE_TIMING) {
            bundle.putSerializable("action", NavIntentStore.SET_STORE_TIMING);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.settings_fragment_container, StoreSelectionFragment.class, bundle)
                .commit();
    }

    private void initToolbar() {

        NavigationView navigationView = drawerLayout.findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(1).setChecked(true);

        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(getDrawable(R.drawable.ic_arrow_back_black_24dp));
        home.setOnClickListener(view -> StoresActivity.super.onBackPressed());

        TextView title = toolbar.findViewById(R.id.app_bar_title);
        title.setText("Stores With QR Code");
    }

    @Override
    public void onStoreSelected(Fragment fragment, String storeId) {
        if (action == NavIntentStore.DISPLAY_QR_CODE) {
            Bundle bundle = new Bundle();
            bundle.putString("storeId", storeId);

            getSupportFragmentManager()
                    .beginTransaction()
                    .hide(fragment)
                    .addToBackStack(null)
                    .add(R.id.settings_fragment_container, QrCodeFragment.class, bundle)
                    .commit();
            hideSystemUi();
        }
    }

    @Override
    public void onStoreListReopened() {
        showSystemUi();
    }

    private void hideSystemUi() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        toolbar.setVisibility(View.GONE);
    }

    private void showSystemUi() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        new WindowInsetsControllerCompat(window, binding.getRoot()).show(WindowInsetsCompat.Type.systemBars());
        toolbar.setVisibility(View.VISIBLE);
    }
}