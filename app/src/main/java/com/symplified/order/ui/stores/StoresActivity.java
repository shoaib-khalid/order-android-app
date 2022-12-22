package com.symplified.order.ui.stores;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
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

        String clientId = getSharedPreferences(App.SESSION, MODE_PRIVATE)
                .getString(Key.CLIENT_ID, null);
        action = (NavIntentStore) getIntent().getExtras().getSerializable("action");

        initToolbar();

        Bundle bundle = new Bundle();
        bundle.putString(Key.CLIENT_ID, clientId);
        bundle.putSerializable("action", action);
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
        home.setOnClickListener(view -> super.onBackPressed());

        ((TextView) findViewById(R.id.app_bar_title)).setText(
                action == NavIntentStore.DISPLAY_QR_CODE
                        ? "Stores With QR Code" : "Store Timings");
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
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        toolbar.setVisibility(View.GONE);
    }

    private void showSystemUi() {
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .show(WindowInsetsCompat.Type.systemBars());
        toolbar.setVisibility(View.VISIBLE);
    }
}