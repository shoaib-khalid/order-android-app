package com.symplified.order;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.symplified.order.databinding.ActivitySettingsBinding;
import com.symplified.order.fragments.settings.StoreSelectionFragment;

public class SettingsActivity extends NavbarActivity {

    private Toolbar toolbar;
    private ActivitySettingsBinding binding;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        if (sharedPreferences.getBoolean("isStaging", false))
            setTheme(R.style.Theme_SymplifiedOrderUpdate_Test);

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = findViewById(R.id.drawer_layout);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initToolbar(sharedPreferences);

        getSupportFragmentManager().beginTransaction().add(R.id.settings_frame_layout, new StoreSelectionFragment()).commit();

    }

    private void initToolbar(SharedPreferences sharedPreferences) {

        NavigationView navigationView = drawerLayout.findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(1).setChecked(true);

        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(getDrawable(R.drawable.ic_arrow_back_black_24dp));
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsActivity.super.onBackPressed();
//                setResult(4, new Intent().putExtra("finish", 1));
//                Intent intent = new Intent(getApplicationContext(), ChooseStore.class);
//                FirebaseMessaging.getInstance().unsubscribeFromTopic(sharedPreferences.getString("storeId", null));
//                sharedPreferences.edit().remove("storeId").apply();
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(intent);
//                finish();
//                finish();
            }
        });

        TextView title = toolbar.findViewById(R.id.app_bar_title);
        title.setText("Choose a Store");
//        ImageView logout = toolbar.findViewById(R.id.app_bar_logout);
//        logout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
//                String storeIdList = sharedPreferences.getString("storeIdList", null);
//                if (storeIdList != null) {
//                    for (String storeId : storeIdList.split(" ")) {
//                        FirebaseMessaging.getInstance().unsubscribeFromTopic(storeId);
//                    }
//                }
//                sharedPreferences.edit().clear().apply();
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(intent);
//                finish();
//            }
//        });
//
//        ImageView settings = toolbar.findViewById(R.id.app_bar_settings);
//        settings.setOnClickListener(view -> {
//            Toast.makeText(this, "Select a store !", Toast.LENGTH_SHORT).show();
//        });
//
//        ImageView products = toolbar.findViewById(R.id.app_bar_products);
//        products.setOnClickListener(view -> {
//            Intent intent = new Intent(this, ProductsActivity.class);
//            startActivity(intent);
//        });
    }
}