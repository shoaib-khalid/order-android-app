package com.symplified.order;

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

import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.symplified.order.adapters.EditItemAdapter;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.databinding.ActivityEditOrderBinding;
import com.symplified.order.models.HttpResponse;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.item.ItemResponse;
import com.symplified.order.models.item.UpdatedItem;
import com.symplified.order.models.order.Order;
import com.symplified.order.networking.ServiceGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EditOrderActivity extends NavbarActivity {

    private Toolbar toolbar;
    private ActivityEditOrderBinding binding;
    private DrawerLayout drawerLayout;

    public final String TAG = EditOrderActivity.class.getName();
    private RecyclerView recyclerView;
    private List<Item> items;
    private EditItemAdapter adapter;
    private String BASE_URL;
    private Dialog progressDialog, dialog;
    private Order order = null;
    private Button update, negative, positive;

    Map<String, String> headers;
    OrderApi orderApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);

        BASE_URL = sharedPreferences.getString("base_url", null);

        progressDialog = new Dialog(this);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);

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
//        headers.put("Authorization", "Bearer Bearer accessToken");
//        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.ORDER_SERVICE_URL)
//                .addConverterFactory(GsonConverterFactory.create()).build();
//        orderApiService = retrofit.create(OrderApi.class);
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
        Call<ItemResponse> itemResponseCall = orderApiService.getItemsForOrder(headers, order.id);

        progressDialog.show();

        itemResponseCall.clone().enqueue(new Callback<ItemResponse>() {
            @Override
            public void onResponse(Call<ItemResponse> call, Response<ItemResponse> response) {
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
            public void onFailure(Call<ItemResponse> call, Throwable t) {
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

        if(adapter.updatedItemsList.size() == 0){
            Toast.makeText(this, "No changes to update", Toast.LENGTH_SHORT).show();
            return;
        }

        dialog = new Dialog(this);
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
                        getOrderById(order.id);
//                        Toast.makeText(getApplicationContext(), "Order Updated Successfully", Toast.LENGTH_SHORT).show();
//                        finish();
                    } else {
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

    public void getOrderById(String id) {

        Call<Order.OrderByIdResponse> orderByIdResponseCall = orderApiService.getOrderById(headers, id);

        orderByIdResponseCall.clone().enqueue(new Callback<Order.OrderByIdResponse>() {
            @Override
            public void onResponse(Call<Order.OrderByIdResponse> call, Response<Order.OrderByIdResponse> response) {
                if (response.isSuccessful()) {
                    Order order = response.body().data;
                    dialog = new Dialog(getApplicationContext());
                    dialog.setContentView(R.layout.custom_alert_dialog);
                    dialog.setCancelable(false);
                    ImageView imageView = dialog.findViewById(R.id.alert_icon);
                    TextView title = dialog.findViewById(R.id.alert_title);
                    TextView message = dialog.findViewById(R.id.alert_message);
                    dialog.findViewById(R.id.btn_neutral).setVisibility(View.VISIBLE);
                    title.setText(R.string.order_updated);
                    message.setText(R.string.order_updated_message + Double.toString(order.orderRefund.get(0).refundAmount));
                    imageView.setImageDrawable(getDrawable(R.drawable.ic_success));
                    dialog.findViewById(R.id.btn_neutral).setOnClickListener(view -> {
                        finish();
                    });
                    dialog.show();
                }
            }

            @Override
            public void onFailure(Call<Order.OrderByIdResponse> call, Throwable t) {
            }
        });

    }
}