package com.ekedai.merchant.ui.voucher;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.ekedai.merchant.App;
import com.ekedai.merchant.R;
import com.ekedai.merchant.databinding.ActivityVoucherDetailsBinding;
import com.ekedai.merchant.models.voucher.VoucherDetails;
import com.ekedai.merchant.models.voucher.VoucherQrCodeDetails;
import com.ekedai.merchant.networking.ServiceGenerator;
import com.ekedai.merchant.networking.apis.ProductApi;
import com.ekedai.merchant.utils.SharedPrefsKey;
import com.ekedai.merchant.utils.Utilities;
import com.google.android.material.navigation.NavigationView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoucherDetailsActivity extends AppCompatActivity implements VoucherSuccessDialog.OnDialogDismissListener {

    public static String VOUCHER_DETAILS_KEY = "VOUCHER_DETAILS";

    private ActivityVoucherDetailsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoucherDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initToolbar();

        if (!getIntent().hasExtra(VOUCHER_DETAILS_KEY)) {
            finish();
        }

        VoucherQrCodeDetails voucherDetails = (VoucherQrCodeDetails) getIntent().getExtras().getSerializable(VOUCHER_DETAILS_KEY);
        Glide.with(this).load(voucherDetails.productImageUrl).into(binding.productImage);
        binding.voucherCodeText.setText(getString(R.string.voucher_code_template, voucherDetails.voucherCode));
        binding.validityPeriodText.setText(getString(R.string.valid_until_template, voucherDetails.date));
        binding.productNameText.setText(voucherDetails.productName);
        binding.productQuantityText.setText(getString(R.string.quantity_template, "1"));
        binding.productPriceText.setText(getString(
                R.string.price_template,
                Utilities.getCurrencySymbol(null, this),
                Utilities.formatPrice(voucherDetails.productPrice)
        ));

        String[] storeIds = getSharedPreferences(App.SESSION, Context.MODE_PRIVATE)
                .getString(SharedPrefsKey.STORE_ID_LIST, "")
                .split(" ");

        ProductApi productService = ServiceGenerator.createProductService();
        binding.redeemButton.setOnClickListener(btn -> {
            btn.setEnabled(false);
            binding.progressBar.setVisibility(View.VISIBLE);

            productService.redeemVoucher(
                    voucherDetails.voucherCode,
                    voucherDetails.phoneNumber,
                    voucherDetails.storeId
            ).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    binding.progressBar.setVisibility(View.INVISIBLE);
                    if (response.isSuccessful()) {
                        new VoucherSuccessDialog(VoucherDetailsActivity.this).show(getSupportFragmentManager(), VoucherSuccessDialog.TAG);
                    } else if (response.code() == 409) {
                        Toast.makeText(VoucherDetailsActivity.this,
                                "Voucher has been redeemed already.",
                                Toast.LENGTH_SHORT).show();
                        binding.alreadyRedeemedErrorText.setVisibility(View.VISIBLE);
                    } else {
                        btn.setEnabled(true);
                        if (response.errorBody() != null){
                            Utilities.handleUnknownError(VoucherDetailsActivity.this, response.errorBody());
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    binding.progressBar.setVisibility(View.INVISIBLE);
                    btn.setEnabled(true);
                    Toast.makeText(VoucherDetailsActivity.this,
                            getString(R.string.no_internet),
                            Toast.LENGTH_SHORT).show();
                }
            });
        });

        boolean isNotExpired = false;
        try {
            SimpleDateFormat dateParser = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date today = new Date();
            Date parsedDate = dateParser.parse(voucherDetails.date);
            isNotExpired = parsedDate != null && today.compareTo(parsedDate) < 0;
        } catch (ParseException e) {
            Log.e("VoucherDetailsActivity", "Failed to parse date. " + e.getLocalizedMessage());
        }

        boolean isOwnVoucher = false;
        for (String storeId : storeIds) {
            if (storeId.equals(voucherDetails.storeId)) {
                isOwnVoucher = true;
                break;
            }
        }

        binding.redeemButton.setEnabled(isOwnVoucher && isNotExpired);
        binding.expiredErrorText.setVisibility(isNotExpired ? View.GONE : View.VISIBLE);
        binding.notOwnStoreVoucherErrorText.setVisibility(isOwnVoucher ? View.GONE : View.VISIBLE);
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);

//        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
//        NavigationView navigationView = drawerLayout.findViewById(R.id.nav_view);
//        navigationView.getMenu().findItem(R.id.nav_vouchers).setChecked(true);

        binding.toolbar.appBarHome.setImageDrawable(AppCompatResources.getDrawable(this,
                R.drawable.ic_arrow_back_black_24dp));
        binding.toolbar.appBarHome.setOnClickListener(view -> super.onBackPressed());

        binding.toolbar.appBarTitle.setText(getString(R.string.voucher_redemption));
    }

    @Override
    public void onDialogDismissed() {
        finish();
    }
}