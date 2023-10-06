package com.ekedai.merchant.ui.orders;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ekedai.merchant.App;
import com.ekedai.merchant.R;
import com.ekedai.merchant.databinding.ActivityConsolidateOrderBinding;
import com.ekedai.merchant.models.qrorders.ConsolidatedOrder;
import com.ekedai.merchant.networking.ServiceGenerator;
import com.ekedai.merchant.networking.apis.OrderApi;
import com.ekedai.merchant.ui.NavbarActivity;
import com.ekedai.merchant.utils.SharedPrefsKey;
import com.ekedai.merchant.utils.Utilities;

import java.text.DecimalFormat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConsolidateOrderActivity extends NavbarActivity implements ConfirmProcessOrderDialog.OnConfirmProcessOrderListener {

    public static String CONSOLIDATED_ORDER_KEY = "order";

    private final DecimalFormat formatter = Utilities.getMonetaryAmountFormat();
    private ActivityConsolidateOrderBinding binding;
    private OrderApi orderApiService;
    private ConsolidatedOrder consolidatedOrder;
    private String currencySymbol = "RM";

    private Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityConsolidateOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initToolbar();

        orderApiService = ServiceGenerator.createOrderService(getApplicationContext());

        Intent intent = getIntent();
        if (!intent.hasExtra(CONSOLIDATED_ORDER_KEY)) {
            finish();
        }
        Bundle data = getIntent().getExtras();
        consolidatedOrder = (ConsolidatedOrder) data.getSerializable(CONSOLIDATED_ORDER_KEY);

        binding.itemsList.setLayoutManager(new LinearLayoutManager(this));
        binding.itemsList.setAdapter(new ItemAdapter(consolidatedOrder.orderItemWithDetails));

        binding.heading.setText(getString(R.string.table_no, consolidatedOrder.tableNo));
        binding.invoiceIdText.setText(getString(R.string.invoice_no, consolidatedOrder.invoiceNo));
        binding.dateTextView.setText(consolidatedOrder.orderTimeConverted);

        currencySymbol = getSharedPreferences(App.SESSION, Context.MODE_PRIVATE)
                .getString(SharedPrefsKey.CURRENCY_SYMBOL, "RM");

        if (consolidatedOrder.isPaid && consolidatedOrder.changeDue != null) {
            binding.changeDueLabel.setVisibility(View.VISIBLE);
            binding.changeDueTextView.setText(getString(R.string.monetary_amount,
                    currencySymbol, formatter.format(consolidatedOrder.changeDue)));
            binding.changeDueTextView.setVisibility(View.VISIBLE);
        }

        binding.subtotalText.setText(getString(R.string.monetary_amount, currencySymbol, formatter.format(consolidatedOrder.subTotal)));
        binding.serviceChargesText.setText(getString(R.string.monetary_amount, currencySymbol, formatter.format(consolidatedOrder.serviceCharges)));
        binding.appliedDiscountText.setText(getString(R.string.inverse_monetary_amount, currencySymbol, formatter.format(consolidatedOrder.appliedDiscount)));
        binding.totalSalesTextView.setText(getString(R.string.monetary_amount, currencySymbol, formatter.format(consolidatedOrder.totalOrderAmount)));

        setPaymentButtonsEnabled(!consolidatedOrder.isPaid);
    }

    private void initToolbar() {
    }

    private void showConfirmationDialog() {
        new ConfirmProcessOrderDialog(currencySymbol, consolidatedOrder, this)
                .show(getSupportFragmentManager(), ConfirmProcessOrderDialog.TAG);
    }

    @Override
    public void onProcessConfirmed(ConsolidatedOrder updatedOrder) {
        consolidatedOrder = updatedOrder;
        binding.changeDueLabel.setVisibility(View.VISIBLE);
        binding.changeDueTextView.setText(getString(R.string.monetary_amount,
                currencySymbol, formatter.format(updatedOrder.changeDue)));
        binding.changeDueTextView.setVisibility(View.VISIBLE);
        processOrder();
    }

    private void processOrder() {
        setLoading(true);

        Context context = this;
        orderApiService.consolidateOrder(consolidatedOrder)
                .clone()
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            consolidatedOrder.isPaid = true;
                            setPaymentButtonsEnabled(false);
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra(CONSOLIDATED_ORDER_KEY, consolidatedOrder);
                            setResult(Activity.RESULT_OK, resultIntent);

                            Toast.makeText(context, "Table No. " + consolidatedOrder.tableNo + " paid.", Toast.LENGTH_SHORT).show();
                            if (App.isPrinterConnected()) {
                                try {
                                    App.getPrinter().printConsolidatedOrderReceipt(consolidatedOrder, currencySymbol);
                                } catch (Exception e) {
                                    Toast.makeText(context, "Failed to print order.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            finish();
                        } else {
                            Toast.makeText(context, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        setLoading(false);
                        Toast.makeText(context, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        binding.paymentButtonCash.setEnabled(!isLoading);
        binding.paymentButtonDuitnow.setEnabled(!isLoading);
        binding.paymentButtonDuitnow.setEnabled(!isLoading);
        binding.progressLayout.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void setPaymentButtonsEnabled(boolean isEnabled) {
        binding.paymentButtonCash.setVisibility(isEnabled ? View.VISIBLE : View.INVISIBLE);
        binding.paymentButtonDuitnow.setVisibility(isEnabled ? View.VISIBLE : View.INVISIBLE);
        binding.paymentButtonOther.setVisibility(isEnabled ? View.VISIBLE : View.INVISIBLE);

        if (isEnabled) {
            binding.paymentButtonCash.setOnClickListener(v -> showConfirmationDialog());
            binding.paymentButtonDuitnow.setOnClickListener(v -> processOrder());
            binding.paymentButtonOther.setOnClickListener(v -> processOrder());
        }
    }
}