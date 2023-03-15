package com.symplified.order.ui.staff;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.databinding.ActivityStaffBinding;
import com.symplified.order.enums.NavIntentStaff;
import com.symplified.order.ui.NavbarActivity;
import com.symplified.order.utils.SharedPrefsKey;
import com.symplified.order.utils.Utility;

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

        Utility.verifyLoginStatus(this);

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
        navigationView
                .getMenu()
                .getItem(4)
                .getSubMenu()
                .getItem(action == NavIntentStaff.VIEW_DAILY_SALES ? 0 : 1)
                .setChecked(true);

        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(AppCompatResources
                .getDrawable(this, R.drawable.ic_arrow_back_black_24dp));
        home.setOnClickListener(view -> super.onBackPressed());

        ((TextView) findViewById(R.id.app_bar_title)).setText(
                action == NavIntentStaff.MANAGE_STAFF
                        ? "Manage Staff" : "Staff Daily Sales");
    }
}