package com.symplified.order.ui.orders;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.symplified.order.R;
import com.symplified.order.databinding.ActivityTrackOrderBinding;
import com.symplified.order.models.order.OrderDeliveryDetailsResponse;
import com.symplified.order.ui.NavbarActivity;

public class TrackOrderActivity extends NavbarActivity {

    private Toolbar toolbar;
    private ActivityTrackOrderBinding binding;
    private DrawerLayout drawerLayout;
    private WebView webView;
    private TextView deliveryProvider, driver, contact;
    private ImageView call;
    public Dialog progressDialog;
    private OrderDeliveryDetailsResponse.OrderDeliveryDetailsData riderDetails;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTrackOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initToolbar();

        initViews();

        Bundle data = getIntent().getExtras();
        riderDetails = (OrderDeliveryDetailsResponse.OrderDeliveryDetailsData) data.getSerializable("riderDetails");

        setDriverDetails();

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        progressDialog.show();

        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (riderDetails.trackingUrl != null)
                    view.loadUrl(riderDetails.trackingUrl);
                progressDialog.dismiss();
                return true;
            }

            public void onPageFinished(WebView view, String url) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(TrackOrderActivity.this, R.string.request_failure, Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                finish();
            }
        });

        if (riderDetails.trackingUrl != null)
            webView.loadUrl(riderDetails.trackingUrl);
        else {
            progressDialog.dismiss();
        }
    }

    private void initToolbar() {

        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(getDrawable(R.drawable.ic_arrow_back_black_24dp));
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TrackOrderActivity.super.onBackPressed();
            }
        });

        TextView title = toolbar.findViewById(R.id.app_bar_title);
        title.setText("Track Order");
    }

    private void initViews() {
        progressDialog = new Dialog(this);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);

        webView = findViewById(R.id.track_order_webview);

        deliveryProvider = findViewById(R.id.delivery_by_value);
        driver = findViewById(R.id.driver_value);
        contact = findViewById(R.id.contact_value);
        call = findViewById(R.id.address_icon_phone);
    }

    public void setDriverDetails() {
        if (riderDetails.provider.name != null) {
            deliveryProvider.setText(riderDetails.provider.name);
        }
        if (riderDetails.name != null) {
            driver.setText(riderDetails.name);
        }
        if (riderDetails.phoneNumber != null) {
            contact.setText(riderDetails.phoneNumber);
            call.setVisibility(View.VISIBLE);
        }

        call.setOnClickListener(view -> {
            Intent callDriver = new Intent(Intent.ACTION_DIAL);
            callDriver.setData(Uri.parse("tel:" + riderDetails.phoneNumber));
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
                startActivity(callDriver);
            else
                Toast.makeText(this, R.string.call_error_message, Toast.LENGTH_SHORT).show();
        });
    }
}