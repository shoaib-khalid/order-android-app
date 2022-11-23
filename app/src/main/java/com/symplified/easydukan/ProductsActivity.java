package com.symplified.easydukan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.navigation.NavigationView;
import com.symplified.easydukan.adapters.ProductAdapter;
import com.symplified.easydukan.apis.ProductApi;
import com.symplified.easydukan.databinding.ActivityProductsBinding;
import com.symplified.easydukan.models.product.Product;
import com.symplified.easydukan.models.product.ProductListResponse;
import com.symplified.easydukan.networking.ServiceGenerator;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductsActivity extends NavbarActivity {
    private Toolbar toolbar;
    List<Product> products = new ArrayList<>();
    ProductAdapter productAdapter;
    private static final String TAG = "ProductsActivity";
    private String storeIdList;
    private DrawerLayout drawerLayout;
    private SwipeRefreshLayout refreshLayout;
    private ProgressBar progressBar;
    private ProductApi productApiService;

    @Override
    protected void onCreate(Bundle savedInstanceStatus) {
        super.onCreate(savedInstanceStatus);

        ActivityProductsBinding binding = ActivityProductsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = findViewById(R.id.drawer_layout);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initToolbar(sharedPreferences);

        RecyclerView recyclerView = findViewById(R.id.products_recyclerview);

        productAdapter = new ProductAdapter(this, products);
        recyclerView.setAdapter(productAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        storeIdList = sharedPreferences.getString("storeIdList", null);

        progressBar = findViewById(R.id.product_progress_bar);
        refreshLayout = findViewById(R.id.layout_products_refresh);
        refreshLayout.setOnRefreshListener(this::getProductsList);

        productApiService = ServiceGenerator.createProductService();

        getProductsList();
    }

    private void initToolbar(SharedPreferences sharedPreferences) {
        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(getDrawable(R.drawable.ic_arrow_back_black_24dp));
        home.setOnClickListener(view -> onBackPressed());

        TextView title = toolbar.findViewById(R.id.app_bar_title);
        title.setText("All Products");

        NavigationView navigationView = drawerLayout.findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(2).setChecked(true);
    }

    private void getProductsList() {
        startLoading();
        products.clear();

        for (String storeId: storeIdList.split(" ")) {

            Call<ProductListResponse> responseCall = productApiService.getProducts(storeId);

            responseCall.clone().enqueue(new Callback<ProductListResponse>() {
                @Override
                public void onResponse(@NonNull Call<ProductListResponse> call,
                                       @NonNull Response<ProductListResponse> response) {
                    if (response.isSuccessful()) {
                        products.addAll(response.body().data.content);
                        productAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(ProductsActivity.this, "An Error Occurred. Swipe down to retry", Toast.LENGTH_SHORT).show();
                    }
                    stopLoading();
                }

                @Override
                public void onFailure(@NonNull Call<ProductListResponse> call,
                                      @NonNull Throwable t) {
                    Log.e(TAG, "onFailure: ", t);
                    Toast.makeText(ProductsActivity.this, R.string.no_internet, Toast.LENGTH_SHORT).show();
                    stopLoading();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, OrdersActivity.class);
        startActivity(intent);
    }

    private void startLoading() {
        refreshLayout.setRefreshing(true);
        refreshLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void stopLoading() {
        refreshLayout.setRefreshing(false);
        progressBar.setVisibility(View.GONE);
        refreshLayout.setVisibility(View.VISIBLE);
    }
}