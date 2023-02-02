package com.symplified.order.ui.orders;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.adapters.ItemAdapter;
import com.symplified.order.databinding.ActivityConsolidateOrderBinding;
import com.symplified.order.models.qrorders.ConsolidatedOrder;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.networking.apis.OrderApi;
import com.symplified.order.ui.NavbarActivity;
import com.symplified.order.utils.SharedPrefsKey;
import com.symplified.order.utils.Utility;

import java.text.DecimalFormat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConsolidateOrderActivity extends NavbarActivity implements ConfirmProcessOrderDialog.OnConfirmProcessOrderListener {

    public static String CONSOLIDATED_ORDER_KEY = "order";

    private ActivityConsolidateOrderBinding binding;
    private OrderApi orderApiService;
    private ConsolidatedOrder consolidatedOrder;
    private String currencySymbol = "RM";

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        DecimalFormat formatter = Utility.getMonetaryAmountFormat();

        binding.subtotalText.setText(getString(R.string.monetary_amount, currencySymbol, formatter.format(consolidatedOrder.subTotal)));
        binding.serviceChargesText.setText(getString(R.string.monetary_amount, currencySymbol, formatter.format(consolidatedOrder.serviceCharges)));
        Log.d("consolidate", "discount " + consolidatedOrder.appliedDiscount);
        binding.appliedDiscountText.setText(getString(R.string.inverse_monetary_amount, currencySymbol, formatter.format(consolidatedOrder.appliedDiscount)));
        binding.totalSalesTextView.setText(getString(R.string.monetary_amount, currencySymbol, formatter.format(consolidatedOrder.totalOrderAmount)));

        binding.paymentButtonCash.setOnClickListener(v -> showConfirmationDialog("CASH"));
        binding.paymentButtonDuitnow.setOnClickListener(v -> showConfirmationDialog("Duit Now"));
        binding.paymentButtonOther.setOnClickListener(v -> showConfirmationDialog("Other"));
    }

    private void initToolbar() {
    }

    private void showConfirmationDialog(String paymentType) {
        new ConfirmProcessOrderDialog(paymentType, this)
                .show(getSupportFragmentManager(), ConfirmProcessOrderDialog.TAG);
    }

    @Override
    public void onProcessConfirmed() {
        setLoading(true);

        Context context = this;
        orderApiService.consolidateOrder(consolidatedOrder)
                .clone()
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
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
                            setLoading(false);
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
}