package com.symplified.order.ui.products;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.symplified.order.App;
import com.symplified.order.R;
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
import java.util.List;
import java.util.function.Function;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductsActivity extends NavbarActivity implements ProductAdapter.OnProductClickListener {
    private Toolbar toolbar;
    private ProductAdapter productAdapter;
    private static final String TAG = "ProductsActivity";
    private DrawerLayout drawerLayout;
    private ProductApi productApiService;
    private StoreApi storeApiService;
    private List<Store> stores = new ArrayList<>();
    private final List<Product> products = new ArrayList<>();
    private ActivityProductsBinding binding;
    private int pageNo = 0;
    private boolean isLoading = false;
    private boolean isAnyProductLeft = true;
    private final String ALL_STORES = "All";
    private Store selectedStore;
    private String currencySymbol;

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

        currencySymbol = getSharedPreferences(App.SESSION, Context.MODE_PRIVATE)
                .getString(SharedPrefsKey.CURRENCY_SYMBOL, "RM");
        initProductList();

        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)
                        && newState == RecyclerView.SCROLL_STATE_IDLE
                        && !isLoading
                        && isAnyProductLeft) {
                    fetchProducts();
                }
            }
        });

        binding.textBoxSearch.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.refreshLayout.setOnRefreshListener(() -> {
            resetAll();
            fetchStoresAndProducts();
        });

        productApiService = ServiceGenerator.createProductService(getApplicationContext());
        storeApiService = ServiceGenerator.createStoreService(getApplicationContext());

        if (savedInstanceStatus == null) {
            fetchStoresAndProducts();
        }

    }

    private void initToolbar() {
        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back_black_24dp));
        home.setOnClickListener(view -> super.onBackPressed());

        TextView title = toolbar.findViewById(R.id.app_bar_title);
        title.setText("Products");

        NavigationView navigationView = drawerLayout.findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(2).setChecked(true);
    }

    private void fetchStoresAndProducts() {
        startLoading();

        String clientId = getSharedPreferences(App.SESSION, MODE_PRIVATE)
                .getString(SharedPrefsKey.CLIENT_ID, "");

        if (!stores.isEmpty()) {
            fetchProducts();
            return;
        }
        storeApiService.getStores(clientId).clone().enqueue(new Callback<StoreResponse>() {
            @Override
            public void onResponse(
                    @NonNull Call<StoreResponse> call,
                    @NonNull Response<StoreResponse> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    stores = response.body().data.content;
                    String[] storeNames = new String[stores.size() + 1];
                    storeNames[0] = ALL_STORES;
                    for (int i = 0; i < stores.size(); i++) {
                        storeNames[i + 1] = stores.get(i).name;
                    }
                    binding.storeSelectButton.setOnClickListener(v -> {
                        new MaterialAlertDialogBuilder(ProductsActivity.this)
                                .setTitle("Store Products to Show")
                                .setItems(storeNames, (dialog, which) -> {
                                    // If the same store has been selected as the onebefore
                                    if ((selectedStore == null && storeNames[which].equals(ALL_STORES))
                                            || (selectedStore != null && storeNames[which].equals(selectedStore.name))) {
                                        return;
                                    }

                                    resetAll();
                                    if (storeNames[which].equals(ALL_STORES)) {
                                        selectedStore = null;
                                    } else {
                                        for (Store store : stores) {
                                            if (store.name.equals(storeNames[which])) {
                                                selectedStore = store;
                                                break;
                                            }
                                        }
                                    }
                                    binding.storeSelectButton.setText(selectedStore != null ? selectedStore.name : ALL_STORES);
                                    fetchProducts();
                                })
                                .show();

                    });
                    fetchProducts();
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
    private void fetchProducts() {
        startLoading();
        List<Observable<ProductListResponse>> requests = new ArrayList<>();

        if (selectedStore == null) {
            for (Store store : stores) {
                requests.add(productApiService.getProducts(store.id, pageNo));
            }
        } else {
            requests.add(productApiService.getProducts(selectedStore.id, pageNo));
        }

        Observable<List<ProductListResponse>> observableResult
                = Observable.zip(requests, objects -> {
            List<ProductListResponse> responses = new ArrayList<>();
            for (Object o : objects) {
                responses.add((ProductListResponse) o);
            }
            return responses;
        }).subscribeOn(Schedulers.newThread());
        observableResult.observeOn(AndroidSchedulers.mainThread()).subscribeWith(new Observer<List<ProductListResponse>>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(List<ProductListResponse> productListResponses) {
                boolean isLastPage = true;
                for (ProductListResponse response : productListResponses) {
                    if (response.status == 200) {
                        if (!response.data.last) {
                            isLastPage = false;
                        }
                        for (Product product : response.data.content) {
                            for (Store store : stores) {
                                if (store.id.equals(product.storeId)) {
                                    product.store = store;
                                    break;
                                }
                            }
                        }
                        products.addAll(response.data.content);
                    }
                }
                isAnyProductLeft = !isLastPage;

                productAdapter.submitList(products);
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
            public void onComplete() {
                pageNo++;
                if (products.isEmpty()) {
                    binding.emptyProductsText.setVisibility(View.VISIBLE);
                }
                stopLoading();
            }
        });
    }

    private void startLoading() {
        isLoading = true;
        binding.emptyProductsText.setVisibility(View.GONE);
        if (products.isEmpty()) {
//            binding.mainLayout.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        if (!products.isEmpty()) {
            binding.bottomProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void stopLoading() {
        isLoading = false;
        binding.refreshLayout.setRefreshing(false);
//        binding.refreshLayout.setVisibility(View.VISIBLE);
        binding.mainLayout.setVisibility(View.VISIBLE);
        binding.progressBar.setVisibility(View.GONE);
        binding.bottomProgressBar.setVisibility(View.GONE);
    }

    private void resetAll() {
        pageNo = 0;
        products.clear();
        isAnyProductLeft = false;
        initProductList();
    }

    private void initProductList() {
        productAdapter = new ProductAdapter(currencySymbol, this);
        binding.recyclerView.setAdapter(productAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void clearSelectedStore() {
        selectedStore = null;
        binding.storeSelectButton.setText(ALL_STORES);
    }

    @Override
    public void onProductClicked(Product p) {
        Intent intent = new Intent(this, EditProductActivity.class);
        intent.putExtra(EditProductActivity.PRODUCT, p);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}