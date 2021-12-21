package com.symplified.order;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.messaging.FirebaseMessaging;
import com.symplified.order.databinding.ActivityOrdersBinding;
import com.symplified.order.services.AlertService;
import com.symplified.order.ui.tabs.SectionsPagerAdapter;

public class OrdersActivity extends AppCompatActivity {

    private ActivityOrdersBinding binding;
    private Toolbar toolbar;
    private ViewPager mViewPager;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(sharedPreferences.getBoolean("isStaging", false))
            setTheme(R.style.Theme_SymplifiedOrderUpdate_Test);

        initToolbar(sharedPreferences);

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(0);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);
        tabs.setTabMode(TabLayout.MODE_AUTO);

        mViewPager = viewPager;

        stopService(new Intent(this, AlertService.class));
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
                getSupportFragmentManager().getFragments().get(0).onResume();
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
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//
//                            }
//                        }).start();
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

//        ImageView storeLogo = toolbar.findViewById(R.id.app_bar_logo);
//        String logourl = sharedPreferences.getString("logoUrl", null);
//        Log.e("TAG", "onCreate: logourl is : "+logourl, new Error() );

//        Bitmap s = (Bitmap) getIntent().getParcelableExtra("logo");
//        Log.e("TAG", "logoBitmap: " + s, new Error() );
//        Log.e("TAG", "has logo: " +sharedPreferences.getString("logoImage", null), new Error());
//        String encodedImage = sharedPreferences.getString("logoImage", null);
//        if(getIntent().hasExtra("logo") || encodedImage != null){
//            ImageUtil.decodeAndSetImage(storeLogo, encodedImage);
//        }
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