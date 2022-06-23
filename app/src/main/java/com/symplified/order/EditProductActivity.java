package com.symplified.order;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputLayout;
import com.symplified.order.apis.CategoryApi;
import com.symplified.order.apis.ProductApi;
import com.symplified.order.databinding.ActivityEditProductBinding;
import com.symplified.order.models.category.Category;
import com.symplified.order.models.category.CategoryResponse;
import com.symplified.order.models.product.Product;
import com.symplified.order.services.DownloadImageTask;
import com.symplified.order.utils.Utility;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EditProductActivity extends NavbarActivity {

    private Button prodDetailsUpdateBtn, prodDetailsCancelBtn;
    private Toolbar toolbar;
    private Product product = null;
    private TextInputLayout prod_details_name, prod_details_price, prod_details_desc, prod_details_SKU, prod_details_Quantity;
    private ImageView prod_details_img;
    private AutoCompleteTextView categoryTextView;
    private TextInputLayout categoryMenu;
    private AutoCompleteTextView statusTextView;
    private TextInputLayout statusMenu;

    private List<Category> categories;
    private String BASE_URL;
    private String storeId;
    private SharedPreferences sharedPreferences;
    private List<String> categoryNames;
    private ArrayAdapter<String> categoryAdapter;

    private List<String> statusList;
    private ArrayAdapter<String> statusAdapter;

    private String status;

    private static Dialog progressDialog;

    private ActivityEditProductBinding binding;

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        BASE_URL = sharedPreferences.getString("base_url", null);
        storeId = sharedPreferences.getString("storeId", null);
        progressDialog = new Dialog(this);


        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initToolbar();

        initViews();

        Intent intent = getIntent();
        if (intent.hasExtra("product")) {
            Bundle data = getIntent().getExtras();
            product = (Product) data.getSerializable("product");
        }

        categories = new ArrayList<>();
        categoryNames = new ArrayList<>();
        categoryAdapter = new ArrayAdapter<>(this, R.layout.drop_down_item, categoryNames);
        categoryTextView.setAdapter(categoryAdapter);
        fetchCategories();

        statusList = new ArrayList();
        statusAdapter = new ArrayAdapter<>(this, R.layout.drop_down_item, statusList);
        statusTextView.setAdapter(statusAdapter);
        setStatus();

        if (product != null) {
            getProductDetails();
        }

        categoryTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                product.categoryId = categories.get(i).id;
            }
        });

        prodDetailsUpdateBtn.setOnClickListener(view -> {
            updateProduct();
        });

        statusTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String selected = adapterView.getItemAtPosition(i).toString();
                switch (selected) {
                    case "Active":
                        product.status = "ACTIVE";
                        break;
                    case "Inactive":
                        product.status = "INACTIVE";
                        break;
                    case "Out of Stock":
                        product.status = "OUTOFSTOCK";
                        break;
                }
            }
        });

        prodDetailsCancelBtn.setOnClickListener(view -> {
            Intent intent1 = new Intent(this, ProductsActivity.class);
            startActivity(intent1);
        });

    }

    public void initToolbar() {

        TextView title = toolbar.findViewById(R.id.app_bar_title);
        title.setText("Edit Prdocut");
        ImageView home = toolbar.findViewById(R.id.app_bar_home);

        home.setImageDrawable(getDrawable(R.drawable.ic_arrow_back_black_24dp));
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditProductActivity.super.onBackPressed();
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
    }

    public void initViews() {

        prodDetailsUpdateBtn = findViewById(R.id.prodDetailsUpdateBtn);
        prodDetailsCancelBtn = findViewById(R.id.prodDetailsCancelBtn);
        prod_details_img = findViewById(R.id.prodDetails_Img);
        prod_details_name = findViewById(R.id.prodDetailsName);
        prod_details_price = findViewById(R.id.prodDetails_Price);
        prod_details_desc = findViewById(R.id.productDetails_Desc);
        prod_details_SKU = findViewById(R.id.productDetails_SKU);
        prod_details_Quantity = findViewById(R.id.productDetails_Quantity);
        categoryMenu = findViewById(R.id.categoryMenu);
        categoryTextView = findViewById(R.id.categoryTextView);
        statusMenu = findViewById(R.id.statusMenu);
        statusTextView = findViewById(R.id.statusTextView);
    }

    public void updateProduct() {

        product.name = prod_details_name.getEditText().getText().toString();
        product.productInventories.get(0).price = Double.parseDouble(prod_details_price.getEditText().getText().toString());
        product.description = prod_details_desc.getEditText().getText().toString();
        product.productInventories.get(0).quantity = Integer.parseInt(prod_details_Quantity.getEditText().getText().toString());
        product.productInventories.get(0).sku = prod_details_SKU.getEditText().getText().toString();

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient())
                .baseUrl(BASE_URL + App.PRODUCT_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ProductApi api = retrofit.create(ProductApi.class);

        Call<ResponseBody> responseCall = api.updateProduct(headers, storeId, product.id, product);

        progressDialog.show();
        responseCall.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Call<ResponseBody> updateInvntoryCall = api.updateProductInventory(headers, storeId, product.id, product.productInventories.get(0).itemCode, product.productInventories.get(0));

                    updateInvntoryCall.clone().enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                progressDialog.dismiss();
                                Intent intent = new Intent(getApplicationContext(), ProductsActivity.class);
                                startActivity(intent);
                                Toast.makeText(getApplicationContext(), "Product Updated Successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e("ResponseError: ", response.toString());
                                progressDialog.dismiss();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            progressDialog.dismiss();
                        }
                    });

                } else {
                    Log.e("ERROR: ", response.toString());
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressDialog.dismiss();
            }
        });
    }


    public void fetchCategories() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient())
                .baseUrl(BASE_URL + App.PRODUCT_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        CategoryApi api = retrofit.create(CategoryApi.class);

        Call<CategoryResponse> responseCall = api.getCategories(headers, storeId);

        progressDialog.show();

        responseCall.clone().enqueue(new Callback<CategoryResponse>() {
            @Override
            public void onResponse(Call<CategoryResponse> call, Response<CategoryResponse> response) {
                if (response.isSuccessful()) {
                    categories.addAll(response.body().data.content);
                    for (Category category : categories) {
                        categoryNames.add(category.name);
                    }
                    if (product != null) {
                        for (int i = 0; i < categories.size(); i++) {
                            if (categories.get(i).id.equals(product.categoryId)) {
                                categoryTextView.setText(categoryNames.get(i), false);
                            }
                        }
                    }
                    categoryAdapter.notifyDataSetChanged();
                }
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<CategoryResponse> call, Throwable t) {
                progressDialog.dismiss();

            }
        });

    }

    public void setStatus() {

        statusList.add("Active");
        statusList.add("Inactive");
        statusList.add("Out of Stock");
        if (product != null) {
            String status = product.status;
            switch (status) {
                case "ACTIVE":
                    statusTextView.setText(statusList.get(0), false);
                    break;
                case "INACTIVE":
                    statusTextView.setText(statusList.get(1), false);
                    break;
                case "OUTOFSTOCK":
                    statusTextView.setText(statusList.get(2), false);

            }
        }
        statusAdapter.notifyDataSetChanged();
    }

    private void getProductDetails() {

        prod_details_name.getEditText().setText(product.name);
        prod_details_price.getEditText().setText(Double.toString(product.productInventories.get(0).price));
        prod_details_desc.getEditText().setText(Html.fromHtml(product.description));
        prod_details_Quantity.getEditText().setText(Integer.toString(product.productInventories.get(0).quantity));
        prod_details_SKU.getEditText().setText(product.productInventories.get(0).sku);
        try {
            Bitmap bitmap = new DownloadImageTask().execute(product.thumbnailUrl).get();
            if (bitmap != null) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
                String encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
                if (encodedImage != null) {
                    Utility.decodeAndSetImage(prod_details_img, encodedImage);
                }
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
