package com.symplified.order.ui.products;

import android.annotation.SuppressLint;
import android.app.Activity;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.symplified.order.models.category.Category;
import com.symplified.order.models.category.CategoryResponse;
import com.symplified.order.models.product.Product;
import com.symplified.order.models.product.ProductListResponse;
import com.symplified.order.models.product.UpdatedProduct;
import com.symplified.order.models.qrorders.ConsolidatedOrder;
import com.symplified.order.models.store.Store;
import com.symplified.order.models.store.StoreResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.networking.apis.ProductApi;
import com.symplified.order.networking.apis.StoreApi;
import com.symplified.order.ui.NavbarActivity;
import com.symplified.order.ui.orders.ConsolidateOrderActivity;
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

@SuppressLint("CheckResult")

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
    private final String ALL_CATEGORIES = "All";
    private Store selectedStore;
    private Category selectedCategory;
    private String currencySymbol;
    private ActivityResultLauncher<Intent> editProductActivityResultLauncher;


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
            fetchAll();
        });

        productApiService = ServiceGenerator.createProductService(getApplicationContext());
        storeApiService = ServiceGenerator.createStoreService(getApplicationContext());

        editProductActivityResultLauncher
                = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                String updatedProductId = data.getStringExtra(EditProductActivity.UPDATED_PRODUCT_ID);
                UpdatedProduct updatedProduct = (UpdatedProduct) data
                        .getSerializableExtra(EditProductActivity.UPDATED_PRODUCT);

                for (Product product : products) {
                    if (product.id.equals(updatedProductId)) {
                        product.name = updatedProduct.name;
                        product.status = updatedProduct.status;
                        productAdapter.submitList(null);
                        productAdapter.submitList(products);
                        break;
                    }
                }
            }
        });
        if (savedInstanceStatus == null) {
            fetchAll();
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

    private void fetchAll() {
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
                    fetchCategories();
                    String[] storeNames = new String[stores.size() + 1];
                    storeNames[0] = ALL_STORES;
                    for (int i = 0; i < stores.size(); i++) {
                        storeNames[i + 1] = stores.get(i).name;
                    }
                    binding.storeSelectButton.setOnClickListener(v -> new MaterialAlertDialogBuilder(ProductsActivity.this)
                            .setTitle("Store")
                            .setItems(storeNames, (dialog, which) -> {
                                // If the same store has been selected as the one before
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
                                initCategoryView();
                                fetchProducts();
                            }).show());
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

    private void fetchCategories() {
        List<Observable<CategoryResponse>> requests = new ArrayList<>();

        for (Store store : stores) {
            requests.add(storeApiService.getCategoriesByStoreId(store.id));
        }

        Observable.zip(requests, objects -> {
                    List<CategoryResponse> responses = new ArrayList<>();
                    for (Object o : objects) {
                        responses.add((CategoryResponse) o);
                    }
                    return responses;
                }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribeWith(new Observer<List<CategoryResponse>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(List<CategoryResponse> categoryResponses) {
                        for (CategoryResponse response : categoryResponses) {
                            if (response.status == 200) {
                                List<Category> categories = response.data.content;
                                if (categories.size() > 0) {
                                    String storeId = categories.get(0).storeId;
                                    for (Store store : stores) {
                                        if (store.id.equals(storeId)) {
                                            store.categories = categories;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                        initCategoryView();
                    }
                });
    }

    @SuppressLint("CheckResult")
    private void fetchProducts() {
        startLoading();
        List<Observable<ProductListResponse>> requests = new ArrayList<>();

        if (selectedStore != null) {
            requests.add(storeApiService.getProducts(
                    selectedStore.id,
                    selectedCategory != null ? selectedCategory.id : null,
                    pageNo
            ));
        } else {
            for (Store store : stores) {
                requests.add(storeApiService.getProducts(
                        store.id, null, pageNo
                ));
            }
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

    private void initCategoryView() {
        selectedCategory = null;
        binding.categorySelectButton.setText(ALL_CATEGORIES);

        if (selectedStore == null || selectedStore.categories.isEmpty()) {
            binding.categorySelectButton.setVisibility(View.GONE);
            return;
        }

        String[] categoryNames = new String[selectedStore.categories.size() + 1];
        categoryNames[0] = ALL_CATEGORIES;
        for (int i = 0; i < selectedStore.categories.size(); i++) {
            categoryNames[i + 1] = selectedStore.categories.get(i).name;
        }
        binding.categorySelectButton.setOnClickListener(v -> new MaterialAlertDialogBuilder(ProductsActivity.this)
                .setTitle("Store Category")
                .setItems(categoryNames, (dialog, which) -> {
                    // If the same category has been selected as the one before
                    if ((selectedCategory == null && categoryNames[which].equals(ALL_CATEGORIES))
                            || (selectedCategory != null && categoryNames[which].equals(selectedCategory.name))) {
                        return;
                    }

                    resetAll();
                    if (categoryNames[which].equals(ALL_CATEGORIES)) {
                        selectedStore = null;
                    } else {
                        for (Category category : selectedStore.categories) {
                            if (category.name.equals(categoryNames[which])) {
                                selectedCategory = category;
                                break;
                            }
                        }
                    }
                    binding.categorySelectButton.setText(selectedCategory != null ? selectedCategory.name : ALL_CATEGORIES);
                    fetchProducts();
                })
                .show());
        binding.categorySelectButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onProductClicked(Product p) {
//        Intent intent = new Intent(this, EditProductActivity.class);
//        intent.putExtra(EditProductActivity.PRODUCT, p);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);

        Intent intent = new Intent(this, EditProductActivity.class);
        intent.putExtra(EditProductActivity.PRODUCT, p);
        editProductActivityResultLauncher.launch(intent);
    }
}