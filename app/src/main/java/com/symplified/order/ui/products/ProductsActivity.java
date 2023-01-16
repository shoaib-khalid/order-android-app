package com.symplified.order.ui.products;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.symplified.order.apis.ProductApi;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.databinding.ActivityProductsBinding;
import com.symplified.order.models.product.Product;
import com.symplified.order.models.product.ProductListResponse;
import com.symplified.order.models.store.Store;
import com.symplified.order.models.store.StoreResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.ui.NavbarActivity;
import com.symplified.order.utils.SharedPrefsKey;
import com.symplified.order.utils.Utility;

import java.util.ArrayList;
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
    private SwipeRefreshLayout refreshLayout;
    private ProgressBar progressBar;
    private ProductApi productApiService;
    private StoreApi storeApiService;
    private List<Store> stores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceStatus) {
        super.onCreate(savedInstanceStatus);

        ActivityProductsBinding binding = ActivityProductsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Utility.verifyLoginStatus(this);

        drawerLayout = findViewById(R.id.drawer_layout);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initToolbar();

        RecyclerView recyclerView = findViewById(R.id.products_recyclerview);

        productAdapter = new ProductAdapter(this);
        recyclerView.setAdapter(productAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        storeIdList = getSharedPreferences(App.SESSION, MODE_PRIVATE)
                .getString(SharedPrefsKey.STORE_ID_LIST, null);

        progressBar = findViewById(R.id.product_progress_bar);
        refreshLayout = findViewById(R.id.layout_products_refresh);
        refreshLayout.setOnRefreshListener(this::getProductsList);

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
                        productAdapter.setProducts(productsList);
                    }

                    @Override
                    public void onError(Throwable e) {
                        stopLoading();
                        Toast.makeText(ProductsActivity.this, "An error occurred. Please swipe down to retry.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() { stopLoading(); }
                });
    }

//    private void getProductsList() {
//        for (String storeId: storeIdList.split(" ")) {
//
//            Call<ProductListResponse> responseCall = productApiService.getProducts(storeId);
//
//            responseCall.clone().enqueue(new Callback<ProductListResponse>() {
//                @Override
//                public void onResponse(@NonNull Call<ProductListResponse> call,
//                                       @NonNull Response<ProductListResponse> response) {
//                    if (response.isSuccessful() && response.body() != null) {
//                        products.addAll(response.body().data.content);
//                        productAdapter.notifyDataSetChanged();
//                    } else {
//                        Toast.makeText(ProductsActivity.this, "An Error Occurred. Swipe down to retry", Toast.LENGTH_SHORT).show();
//                    }
//                    stopLoading();
//                }
//
//                @Override
//                public void onFailure(@NonNull Call<ProductListResponse> call,
//                                      @NonNull Throwable t) {
//                    Toast.makeText(ProductsActivity.this, R.string.no_internet, Toast.LENGTH_SHORT).show();
//                    stopLoading();
//                }
//            });
//        }
//    }

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