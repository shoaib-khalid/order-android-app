package com.symplified.order;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.symplified.order.adapters.ProductAdapter;
import com.symplified.order.apis.ProductApi;
import com.symplified.order.databinding.ActivityProductsBinding;
import com.symplified.order.models.product.Product;
import com.symplified.order.models.product.ProductListResponse;
import com.symplified.order.networking.ServiceGenerator;

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

public class ProductsActivity extends NavbarActivity {
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    List<Product> products = new ArrayList<>();
    ProductAdapter productAdapter;
    private static final String TAG = "ProductsActivity";
    private String storeIdList;
    private String BASE_URL;
    private Dialog progressDialog;
    private ActivityProductsBinding binding;
    private DrawerLayout drawerLayout;
    private TextView addNew;
    private SwipeRefreshLayout refreshLayout;
    private ProgressBar progressBar;
    private ProductApi productApiService;

    @Override
    protected void onCreate(Bundle savedInstanceStatus) {
        super.onCreate(savedInstanceStatus);

        binding = ActivityProductsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = findViewById(R.id.drawer_layout);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initToolbar(sharedPreferences);

        recyclerView = findViewById(R.id.products_recyclerview);

        productAdapter = new ProductAdapter(this, products);
        recyclerView.setAdapter(productAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        storeIdList = sharedPreferences.getString("storeIdList", null);
        BASE_URL = sharedPreferences.getString("base_url", null);

        progressDialog = new Dialog(this);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);

        progressBar = findViewById(R.id.product_progress_bar);
        refreshLayout = findViewById(R.id.layout_products_refresh);
        refreshLayout.setOnRefreshListener(() -> getProductsList());

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

        Map<String, String> headers = new HashMap<>();

        for (String storeId: storeIdList.split(" ")) {

            Call<ProductListResponse> responseCall = productApiService.getProducts(headers, storeId);

            responseCall.clone().enqueue(new Callback<ProductListResponse>() {
                @Override
                public void onResponse(Call<ProductListResponse> call, Response<ProductListResponse> response) {
                    if (response.isSuccessful()) {
                        products.addAll(response.body().data.content);
                        productAdapter.notifyDataSetChanged();
                        progressDialog.dismiss();

                    } else {
                        Toast.makeText(ProductsActivity.this, "An Error Occurred. Swipe down to retry", Toast.LENGTH_SHORT).show();
                    }
                    stopLoading();
                }

                @Override
                public void onFailure(Call<ProductListResponse> call, Throwable t) {
                    Log.e(TAG, "onFailure: ", t);
                    Toast.makeText(ProductsActivity.this, R.string.no_internet, Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
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