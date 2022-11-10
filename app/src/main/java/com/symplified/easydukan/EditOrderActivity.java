package com.symplified.easydukan;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.symplified.easydukan.adapters.EditItemAdapter;
import com.symplified.easydukan.apis.OrderApi;
import com.symplified.easydukan.databinding.ActivityEditOrderBinding;
import com.symplified.easydukan.models.HttpResponse;
import com.symplified.easydukan.models.item.Item;
import com.symplified.easydukan.models.item.ItemsResponse;
import com.symplified.easydukan.models.item.UpdatedItem;
import com.symplified.easydukan.models.order.Order;
import com.symplified.easydukan.models.order.OrderDetailsResponse;
import com.symplified.easydukan.networking.ServiceGenerator;
import com.symplified.easydukan.utils.Key;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditOrderActivity extends NavbarActivity {

    private Toolbar toolbar;
    private ActivityEditOrderBinding binding;
    private DrawerLayout drawerLayout;

    public final String TAG = EditOrderActivity.class.getName();
    private RecyclerView recyclerView;
    private List<Item> items;
    private EditItemAdapter adapter;
    private Dialog progressDialog;
    private Order order = null;
    private Button update;
    private DecimalFormat formatter;

    Map<String, String> headers;
    OrderApi orderApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);

        progressDialog = new Dialog(this);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);
        formatter = new DecimalFormat("#,###.00");

        binding = ActivityEditOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = findViewById(R.id.drawer_layout);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initToolbar(sharedPreferences);

        initViews();

        Intent intent = getIntent();
        if (intent.hasExtra("order")) {
            Bundle data = getIntent().getExtras();
            order = (Order) data.getSerializable("order");
        }

        headers = new HashMap<>();
        orderApiService = ServiceGenerator.createOrderService();

        if (order != null) {
            getOrderItems(order);
        }
    }

    private void initToolbar(SharedPreferences sharedPreferences) {

        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(getDrawable(R.drawable.ic_arrow_back_black_24dp));
        home.setOnClickListener(view -> EditOrderActivity.super.onBackPressed());

        TextView title = toolbar.findViewById(R.id.app_bar_title);
        title.setText("Edit Order");
    }

    private void initViews() {
        recyclerView = findViewById(R.id.edit_order_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        update = findViewById(R.id.update_btn);
    }

    private void getOrderItems(Order order) {
        Call<ItemsResponse> itemResponseCall = orderApiService.getItemsForOrder(order.id);

        progressDialog.show();

        itemResponseCall.clone().enqueue(new Callback<ItemsResponse>() {
            @Override
            public void onResponse(Call<ItemsResponse> call, Response<ItemsResponse> response) {
                if (response.isSuccessful()) {
                    items = response.body().data.content;
                    adapter = new EditItemAdapter(items, getApplicationContext(), order);
                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();

                    update.setOnClickListener(v -> updateOrderItems());
                    update.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.request_failure, Toast.LENGTH_SHORT).show();
                    finish();
                }
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<ItemsResponse> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Failed to retrieve items. " + R.string.no_internet, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onFailureItems: ", t);
                finish();
                progressDialog.dismiss();
            }
        });
    }

    public void updateOrderItems() {
        Log.d("edit-order", order.id);
        List<UpdatedItem> updatedItems = adapter.getUpdatedItems();
        Log.d("edit-order", "Updated Items");
        for (UpdatedItem item : updatedItems) {
            Log.d("edit-order", item.id + " " + item.quantity);
        }

        if (adapter.updatedItemsList.size() == 0) {
            Toast.makeText(this, "No changes to update", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_alert_dialog);
        dialog.setCancelable(false);
        ImageView imageView = dialog.findViewById(R.id.alert_icon);
        TextView title = dialog.findViewById(R.id.alert_title);
        TextView message = dialog.findViewById(R.id.alert_message);
        dialog.findViewById(R.id.btn_positive).setVisibility(View.VISIBLE);
        dialog.findViewById(R.id.btn_negative).setVisibility(View.VISIBLE);
        title.setText(R.string.update_order);
        message.setText(R.string.update_order_warning);
        imageView.setImageDrawable(getDrawable(R.drawable.ic_baseline_warning_24));

        dialog.findViewById(R.id.btn_positive).setOnClickListener(view -> {
            dialog.dismiss();
            Call<HttpResponse> updateItemsCall = orderApiService.reviseOrderItem(headers, order.id, adapter.getUpdatedItems());
            progressDialog.show();

            updateItemsCall.clone().enqueue(new Callback<HttpResponse>() {
                @Override
                public void onResponse(Call<HttpResponse> call, Response<HttpResponse> response) {
                    Log.i("updatedItemListTAG", "onResponse: " + call.request());
                    progressDialog.dismiss();
                    if (response.isSuccessful()) {
                        getOrderByInvoiceId(order.invoiceId);
                    } else {
                        progressDialog.dismiss();
                        Log.e(TAG, "onResponse: " + response);
                        try {
                            Toast.makeText(getApplicationContext(), response.body().message,
                                    Toast.LENGTH_SHORT).show();
                        } catch (NullPointerException e) {
                            Toast.makeText(getApplicationContext(),
                                    R.string.request_failure, Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<HttpResponse> call, Throwable t) {
                    Toast.makeText(getApplicationContext(), R.string.request_failure, Toast.LENGTH_SHORT).show();
                    Log.e("TAG", "onFailure: ", t);
                    progressDialog.dismiss();
                }
            });
        });
        dialog.findViewById(R.id.btn_negative).setOnClickListener(view -> {
            dialog.dismiss();
        });
        dialog.show();
    }

    public void getOrderByInvoiceId(String invoiceId) {
        SharedPreferences sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        String clientId = sharedPreferences.getString("ownerId", "");

        Call<OrderDetailsResponse> orderRequest = orderApiService.getNewOrdersByClientIdAndInvoiceId(clientId, invoiceId);
        orderRequest.clone().enqueue(new Callback<OrderDetailsResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderDetailsResponse> call,
                                   @NonNull Response<OrderDetailsResponse> response) {

                if (response.isSuccessful() && response.body().data.content.size() > 0) {
                    Order.OrderDetails updatedOrderDetails = response.body().data.content.get(0);
                    setActivityResult(updatedOrderDetails);

                    if (updatedOrderDetails.order.orderRefund.size() > 0) {
                        showInformationDialog(updatedOrderDetails.order.orderRefund.get(0).refundAmount);
                    } else {
                        progressDialog.dismiss();
                        closeActivityWithSuccessMessage();
                    }
                } else {
                    progressDialog.dismiss();
                    closeActivityWithSuccessMessage();
                }
            }

            @Override
            public void onFailure(Call<OrderDetailsResponse> call, Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "onFailure on orderRequest. " + t.getLocalizedMessage());
                closeActivityWithSuccessMessage();
            }
        });
    }

    public void showInformationDialog(Double refundAmount) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_alert_dialog);
        dialog.setCancelable(false);

        ImageView imageView = dialog.findViewById(R.id.alert_icon);
        TextView title = dialog.findViewById(R.id.alert_title);
        TextView message = dialog.findViewById(R.id.alert_message);

        dialog.findViewById(R.id.btn_neutral).setVisibility(View.VISIBLE);
        title.setText(R.string.order_updated);
        String messageText = getResources().getString(R.string.order_updated_message) + formatter.format(refundAmount);
        message.setText(messageText);
        imageView.setImageDrawable(getDrawable(R.drawable.ic_success));
        dialog.findViewById(R.id.btn_neutral).setOnClickListener(view -> {
            dialog.dismiss();
            closeActivityWithSuccessMessage();
        });
        progressDialog.dismiss();
        dialog.show();
    }

    private void setActivityResult(Order.OrderDetails updatedOrderDetails) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(Key.ORDER_DETAILS, updatedOrderDetails);
        setResult(Activity.RESULT_OK, resultIntent);
    }

    private void closeActivityWithSuccessMessage() {
        Toast.makeText(this, "Order successfully updated", Toast.LENGTH_SHORT).show();
        finish();
    }
}