package com.symplified.order.ui.stores;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.databinding.ActivityStoresBinding;
import com.symplified.order.enums.NavIntentStore;
import com.symplified.order.ui.NavbarActivity;
import com.symplified.order.utils.SharedPrefsKey;

public class StoresActivity extends NavbarActivity
        implements StoreSelectionFragment.StoreFragmentListener {

    private Toolbar toolbar;
    private ActivityStoresBinding binding;
    private DrawerLayout drawerLayout;

    private NavIntentStore action;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityStoresBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = findViewById(R.id.drawer_layout);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String clientId = getSharedPreferences(App.SESSION, MODE_PRIVATE)
                .getString(SharedPrefsKey.CLIENT_ID, "");

        action = (NavIntentStore) getIntent().getExtras().getSerializable("action");

        initToolbar();

        Bundle bundle = new Bundle();
        bundle.putString(SharedPrefsKey.CLIENT_ID, clientId);
        bundle.putSerializable("action", action);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.store_fragment_container, StoreSelectionFragment.class, bundle)
                .commit();
    }

    private void initToolbar() {

        NavigationView navigationView = drawerLayout.findViewById(R.id.nav_view);

        MenuItem item = action == NavIntentStore.SET_STORE_TIMING
                ? navigationView.getMenu().getItem(1).getSubMenu().getItem(0)
                : navigationView.getMenu().getItem(3);
        item.setChecked(true);

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
                    .add(R.id.store_fragment_container, QrCodeFragment.class, bundle)
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
        View fragmentContainer = binding.getRoot();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 30) {
            fragmentContainer.getWindowInsetsController().hide(WindowInsets.Type.systemBars());
        } else {
            fragmentContainer.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LOW_PROFILE
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            );
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();
    }

    private void showSystemUi() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 30) {
            binding.getRoot().getWindowInsetsController().show(WindowInsets.Type.systemBars());
        } else {
            binding.getRoot().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.show();
    }
}