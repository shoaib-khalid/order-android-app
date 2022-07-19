package com.symplified.order;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.symplified.order.adapters.EditItemAdapter;
import com.symplified.order.adapters.ItemAdapter;
import com.symplified.order.adapters.OrderAdapter;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.databinding.ActivityEditOrderBinding;
import com.symplified.order.enums.Status;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.item.ItemResponse;
import com.symplified.order.models.order.Order;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
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
    private Dialog progressDialog;
    private Order order = null;
    private Button cancel, update;

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
            Log.e("OrderEDIT:", order.toString());
        }

        if (order != null) {
            getOrderItems(order);
        }

    }

    private void initToolbar(SharedPreferences sharedPreferences) {

        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(getDrawable(R.drawable.ic_arrow_back_black_24dp));
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditOrderActivity.super.onBackPressed();
            }
        });

        TextView title = toolbar.findViewById(R.id.app_bar_title);
        title.setText("Edit Order");
    }

    private void initViews() {

        recyclerView = findViewById(R.id.edit_order_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        cancel = findViewById(R.id.cancel_btn);
        update = findViewById(R.id.update_btn);

        cancel.setOnClickListener(view -> {
            onCancelOrderButtonClick(order);
        });

        update.setOnClickListener(view -> {
            adapter.updateOrderItems(order, BASE_URL, progressDialog);
        });

    }

    private void getOrderItems(Order order) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Call<ItemResponse> itemResponseCall = orderApiService.getItemsForOrder(headers, order.id);

        progressDialog.show();

        itemResponseCall.clone().enqueue(new Callback<ItemResponse>() {
            @Override
            public void onResponse(Call<ItemResponse> call, Response<ItemResponse> response) {
                if (response.isSuccessful()) {
                    adapter = new EditItemAdapter(response.body().data.content, getApplicationContext(), order);
                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    cancel.setVisibility(View.VISIBLE);
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

    public void onCancelOrderButtonClick(Order order) {
        //add headers required for api calls
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Dialog dialog = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog__Center)
                .setTitle("Cancel Order")
                .setMessage("Do you really want to cancel this order ?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    progressDialog.show();
                    Call<ResponseBody> processOrder = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.CANCELED_BY_MERCHANT), order.id);
                    progressDialog.show();
                    processOrder.clone().enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            progressDialog.dismiss();
                            if (response.isSuccessful()) {
                                Toast.makeText(getApplicationContext(), "Order Cancelled", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                                Intent intent = new Intent(getApplicationContext(), OrdersActivity.class);
                                startActivity(intent);
                                finish();

                            } else {
                                Toast.makeText(getApplicationContext(), R.string.request_failure, Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(getApplicationContext(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "onFailure: ", t);
                            progressDialog.dismiss();
                        }
                    });
                })
                .create();
        TextView title = dialog.findViewById(android.R.id.title);
        TextView message = dialog.findViewById(android.R.id.message);
        if (title != null && message != null) {
            title.setTypeface(Typeface.DEFAULT_BOLD);
            message.setTextSize(14);
            message.setTypeface(Typeface.DEFAULT_BOLD);
        }
        dialog.show();
    }
}