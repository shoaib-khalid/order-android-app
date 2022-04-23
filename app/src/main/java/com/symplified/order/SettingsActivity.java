package com.symplified.order;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;
import com.symplified.order.fragments.settings.StoreSelectionFragment;

public class SettingsActivity extends AppCompatActivity {

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        if(sharedPreferences.getBoolean("isStaging", false))
            setTheme(R.style.Theme_SymplifiedOrderUpdate_Test);
        setContentView(R.layout.activity_settings);

        toolbar = findViewById(R.id.toolbar);

        initToolbar(sharedPreferences);
        setSupportActionBar(toolbar);

        getSupportFragmentManager().beginTransaction().add(R.id.settings_frame_layout, new StoreSelectionFragment()).commit();

    }

    private void initToolbar(SharedPreferences sharedPreferences) {
        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(getDrawable(R.drawable.ic_home_black_24dp));
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                setResult(4, new Intent().putExtra("finish", 1));
//                Intent intent = new Intent(getApplicationContext(), ChooseStore.class);
//                FirebaseMessaging.getInstance().unsubscribeFromTopic(sharedPreferences.getString("storeId", null));
//                sharedPreferences.edit().remove("storeId").apply();
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(intent);
//                finish();
                finish();
            }
        });

        ImageView logout = toolbar.findViewById(R.id.app_bar_logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                String storeIdList = sharedPreferences.getString("storeIdList", null);
                if(storeIdList != null )
                {
                    for(String storeId : storeIdList.split(" ")){
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(storeId);
                    }
                }
                sharedPreferences.edit().clear().apply();
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        ImageView settings = toolbar.findViewById(R.id.app_bar_settings);
        settings.setOnClickListener(view -> {
            Toast.makeText(this, "Select a store !", Toast.LENGTH_SHORT).show();
        });
    }
}