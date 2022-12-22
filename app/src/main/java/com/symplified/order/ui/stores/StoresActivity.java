package com.symplified.order.ui.stores;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.content.res.AppCompatResources;
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
        home.setImageDrawable(AppCompatResources
                .getDrawable(this, R.drawable.ic_arrow_back_black_24dp));
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
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    @Override
    public void onStoreListReopened() {
        showSystemUi();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    private void hideSystemUi() {
        View fragmentContainer = findViewById(R.id.settings_fragment_container);
        if (Build.VERSION.SDK_INT >= 30) {
            fragmentContainer.getWindowInsetsController().hide(WindowInsets.Type.systemBars());
        } else {
            fragmentContainer.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();
    }

    private void showSystemUi() {
        if (Build.VERSION.SDK_INT >= 30) {
            binding.getRoot().getWindowInsetsController().show(WindowInsets.Type.systemBars());
        } else {
            binding.getRoot().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.show();
    }
}