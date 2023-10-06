package com.ekedai.merchant.ui.voucher;

import android.os.Bundle;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.ekedai.merchant.R;
import com.ekedai.merchant.databinding.ActivityVoucherBinding;
import com.ekedai.merchant.ui.NavbarActivity;
import com.google.android.material.navigation.NavigationView;

public class VoucherActivity extends NavbarActivity {

    ActivityVoucherBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityVoucherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initToolbar();

        getSupportFragmentManager()
                .beginTransaction()
                .add(
                        R.id.voucher_fragment_container,
                        VoucherFragment.class,
                        new Bundle()
                )
                .commit();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = drawerLayout.findViewById(R.id.nav_view);
        navigationView.getMenu().findItem(R.id.nav_vouchers).setChecked(true);

        binding.toolbar.appBarHome.setImageDrawable(AppCompatResources.getDrawable(this,
                R.drawable.ic_arrow_back_black_24dp));
        binding.toolbar.appBarHome.setOnClickListener(view -> super.onBackPressed());

        binding.toolbar.appBarTitle.setText("Voucher Redemption");
    }
}