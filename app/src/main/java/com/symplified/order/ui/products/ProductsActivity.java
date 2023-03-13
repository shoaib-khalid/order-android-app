package com.symplified.order.ui.products;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.navigation.NavigationView;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.adapters.ProductAdapter;
import com.symplified.order.databinding.ActivityProductsBinding;
import com.symplified.order.models.product.Product;
import com.symplified.order.models.product.ProductListResponse;
import com.symplified.order.models.store.Store;
import com.symplified.order.models.store.StoreResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.networking.apis.ProductApi;
import com.symplified.order.networking.apis.StoreApi;
import com.symplified.order.ui.NavbarActivity;
import com.symplified.order.utils.SharedPrefsKey;
import com.symplified.order.utils.Utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductsActivity extends NavbarActivity {
    private Toolbar toolbar;
    private ProductAdapter productAdapter;
    private static final String TAG = "ProductsActivity";
    private String storeIdList;
    private DrawerLayout drawerLayout;
    private ProductApi productApiService;
    private StoreApi storeApiService;
    private List<Store> stores = new ArrayList<>();
    private ActivityProductsBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceStatus) {
        super.onCreate(savedInstanceStatus);

        binding = ActivityProductsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Utility.verifyLoginStatus(this);

        drawerLayout = findViewById(R.id.drawer_layout);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initToolbar();

        productAdapter = new ProductAdapter(this);
        binding.recyclerView.setAdapter(productAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        binding.textBoxSearch.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                productAdapter.filter(s.toString());
            }
        });

        storeIdList = getSharedPreferences(App.SESSION, MODE_PRIVATE)
                .getString(SharedPrefsKey.STORE_ID_LIST, null);

        binding.refreshLayout.setOnRefreshListener(this::getProductsList);

        productApiService = ServiceGenerator.createProductService(getApplicationContext());
        storeApiService = ServiceGenerator.createStoreService(getApplicationContext());

        fetchStoresAndProducts();
    }

    private void initToolbar() {
        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back_black_24dp));
        home.setOnClickListener(view -> super.onBackPressed());

        TextView title = toolbar.findViewById(R.id.app_bar_title);
        title.setText("All Products");

        NavigationView navigationView = drawerLayout.findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(2).setChecked(true);
    }

    private void fetchStoresAndProducts() {
        startLoading();
        productAdapter.clear();

        String clientId = getSharedPreferences(App.SESSION, MODE_PRIVATE)
                .getString(SharedPrefsKey.CLIENT_ID, "");
        storeApiService.getStores(clientId)
                .clone()
                .enqueue(new Callback<StoreResponse>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<StoreResponse> call,
                            @NonNull Response<StoreResponse> response
                    ) {
                        if (response.isSuccessful() && response.body() != null) {
                            stores = response.body().data.content;
                            productAdapter.setStores(stores);
                            getProductsList();
                        } else {
                            Toast.makeText(ProductsActivity.this, "An error occurred. Swipe down to retry", Toast.LENGTH_SHORT).show();
                            stopLoading();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<StoreResponse> call, @NonNull Throwable t) {
                        Toast.makeText(ProductsActivity.this, "An error occurred. Swipe down to retry", Toast.LENGTH_SHORT).show();
                        stopLoading();
                    }
                });
    }

    @SuppressLint("CheckResult")
    private void getProductsList() {
        List<Observable<ProductListResponse>> requests = new ArrayList<>();

        for (Store store : stores) {
            requests.add(productApiService.getProducts(store.id));
        }

        Observable<List<ProductListResponse>> observableResult
                = Observable.zip(requests, objects -> {
                    List<ProductListResponse> responses = new ArrayList<>();
                    for (Object o : objects) {
                        responses.add((ProductListResponse) o);
                    }
                    return responses;
        }).subscribeOn(Schedulers.newThread());
        observableResult.observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new Observer<List<ProductListResponse>>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onNext(List<ProductListResponse> productListResponses) {
                        List<Product> productsList = new ArrayList<>();
                        for (ProductListResponse response : productListResponses) {
                            if (response.status == 200) {
                                productsList.addAll(response.data.content);
                            }
                        }

                        Collections.sort(productsList, (p1, p2) -> p1.name.compareTo(p2.name));

                        productAdapter.setProducts(productsList);
                        EditText searchEditText = binding.textBoxSearch.getEditText();
                        String searchText = searchEditText != null
                                ? searchEditText.getText().toString() : "";
                        productAdapter.filter(searchText);
                    }

                    @Override
                    public void onError(Throwable e) {
                        stopLoading();
                        Toast.makeText(
                                ProductsActivity.this,
                                "An error occurred. Please swipe down to retry.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onComplete() { stopLoading(); }
                });
    }

    private void startLoading() {
        binding.refreshLayout.setRefreshing(true);
        binding.refreshLayout.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    private void stopLoading() {
        binding.refreshLayout.setRefreshing(false);
        binding.refreshLayout.setVisibility(View.VISIBLE);
        binding.progressBar.setVisibility(View.GONE);
    }
}