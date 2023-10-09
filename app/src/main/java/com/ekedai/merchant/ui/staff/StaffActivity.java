package com.ekedai.merchant.ui.staff;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.ekedai.merchant.App;
import com.ekedai.merchant.R;
import com.ekedai.merchant.databinding.ActivityStaffBinding;
import com.ekedai.merchant.enums.NavIntentStaff;
import com.ekedai.merchant.ui.NavbarActivity;
import com.ekedai.merchant.utils.SharedPrefsKey;
import com.google.android.material.navigation.NavigationView;

public class StaffActivity extends NavbarActivity {

    private Toolbar toolbar;

    private ActivityStaffBinding binding;
    private NavIntentStaff action;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStaffBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = findViewById(R.id.drawer_layout);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        action = (NavIntentStaff) getIntent().getExtras().getSerializable("action");

        initToolbar();

        SharedPreferences sharedPrefs =
                getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);

        Bundle bundle = new Bundle();
        bundle.putString(
                SharedPrefsKey.CLIENT_ID,
                sharedPrefs.getString(SharedPrefsKey.CLIENT_ID, "")
        );
        bundle.putString(
                SharedPrefsKey.STORE_ID_LIST,
                sharedPrefs.getString(SharedPrefsKey.STORE_ID_LIST, "")
        );
        bundle.putSerializable("action", action);
        getSupportFragmentManager()
                .beginTransaction()
                .add(
                        R.id.staff_fragment_container,
                        action == NavIntentStaff.MANAGE_STAFF
                                ? StaffManagementFragment.class
                                : ShiftManagementFragment.class,
                        bundle
                )
                .commit();
    }

    private void initToolbar() {

        NavigationView navigationView = drawerLayout.findViewById(R.id.nav_view);
        if (action == NavIntentStaff.VIEW_DAILY_SALES) {
            navigationView.getMenu().findItem(R.id.nav_daily_sales).setChecked(true);
        } else {
            navigationView.getMenu().findItem(R.id.nav_manage_staff).setChecked(true);
        }

        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(AppCompatResources
                .getDrawable(this, R.drawable.ic_arrow_back_black_24dp));
        home.setOnClickListener(view -> super.onBackPressed());

        ((TextView) findViewById(R.id.app_bar_title)).setText(
                action == NavIntentStaff.MANAGE_STAFF
                        ? "Manage Staff" : "Staff Daily Sales");
    }
}