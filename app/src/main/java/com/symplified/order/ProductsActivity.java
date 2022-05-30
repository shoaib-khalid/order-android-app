package com.symplified.order;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.messaging.FirebaseMessaging;
import com.symplified.order.adapters.ProductAdapter;
import com.symplified.order.apis.ProductApi;
import com.symplified.order.models.product.Product;
import com.symplified.order.models.product.ProductResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProductsActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private RecyclerView productsrecyclerView;
    List<Product> products = new ArrayList<>();
    ProductAdapter productAdapter;
    private static final String TAG = "ProductsActivity";
    private SharedPreferences sharedPreferences;
    private String storeId;
    private String BASE_URL;
    private Dialog progressDialog;
    private FloatingActionButton addFloatingActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceStatus) {
        super.onCreate(savedInstanceStatus);
        setContentView(R.layout.activity_products);
        toolbar = findViewById(R.id.toolbar);
        productsrecyclerView = findViewById(R.id.productsRecyclerView);
        addFloatingActionButton = findViewById(R.id.add_product_btn);

        sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        storeId = sharedPreferences.getString("storeId", null);
        BASE_URL = sharedPreferences.getString("base_url", null);

        progressDialog = new Dialog(this);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);

        initToolbar(sharedPreferences);
        setData();
        getProductsList();

        addFloatingActionButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, EditProductActivity.class);
            startActivity(intent);
        });

        productsrecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    private void initToolbar(SharedPreferences sharedPreferences) {
        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(getDrawable(R.drawable.ic_home_black_24dp));
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                setResult(4, new Intent().putExtra("finish", 1));
//                Intent intent = new Intent(getApplicationContext(), ChooseStore.class);
//                FirebaseMessaging.getInstance().unsubscribeFromTopic(sharedPreferences.getString("storeId", null));
//                sharedPreferences.edit().remove("storeId").apply();
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(intent);
//                finish();
                finish();
            }
        });

        ImageView logout = toolbar.findViewById(R.id.app_bar_logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                String storeIdList = sharedPreferences.getString("storeIdList", null);
                if (storeIdList != null) {
                    for (String storeId : storeIdList.split(" ")) {
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(storeId);
                    }
                }
                sharedPreferences.edit().clear().apply();
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        ImageView settings = toolbar.findViewById(R.id.app_bar_settings);
        settings.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
        });

        ImageView products = toolbar.findViewById(R.id.app_bar_products);
        products.setOnClickListener(view -> {
            Toast.makeText(this, "Opened!", Toast.LENGTH_SHORT).show();
        });
    }

    private void getProductsList() {

        Map<String, String> headers = new HashMap<>();

        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient())
                .baseUrl(BASE_URL + App.PRODUCT_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ProductApi api = retrofit.create(ProductApi.class);
        Call<ProductResponse> responseCall = api.getProducts(headers, storeId);

        progressDialog.show();

        responseCall.clone().enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                if (response.isSuccessful()) {
                    products.addAll(response.body().data.content);
                    productAdapter.notifyDataSetChanged();
                    progressDialog.dismiss();
                    addFloatingActionButton.setVisibility(View.VISIBLE);
                }
                progressDialog.dismiss();
                addFloatingActionButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: ",t );
                progressDialog.dismiss();
                addFloatingActionButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setData() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        productsrecyclerView.setLayoutManager(linearLayoutManager);
        productAdapter = new ProductAdapter(this, products);
        productsrecyclerView.setAdapter(productAdapter);
    }
}