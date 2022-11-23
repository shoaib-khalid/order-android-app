package com.symplified.easydukan;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.symplified.easydukan.apis.ProductApi;
import com.symplified.easydukan.databinding.ActivityEditProductBinding;
import com.symplified.easydukan.models.product.Product;
import com.symplified.easydukan.models.product.ProductEditRequest;
import com.symplified.easydukan.networking.ServiceGenerator;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProductActivity extends NavbarActivity {

    private Button updateButton;
    private Toolbar toolbar;
    private Product product = null;
    private TextInputLayout productName, productDescription, statusMenu;
    private ImageView productImage;
    private AutoCompleteTextView statusTextView;
    private final int REQ_CODE = 100;

    private String storeId;

    private List<String> statusList;
    private ArrayAdapter<String> statusAdapter;

    private static Dialog progressDialog;

    private ActivityEditProductBinding binding;

    private ActivityResultLauncher<Intent> selectImageActivityResultLauncher;

    private ProductApi productApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        progressDialog = new Dialog(this);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initToolbar();

        initViews();

        Bundle data = getIntent().getExtras();
        product = (Product) data.getSerializable("product");
        storeId = product.storeId;

        getProductDetails();

        statusList = new ArrayList<>();
        statusAdapter = new ArrayAdapter<>(this, R.layout.drop_down_item, statusList);
        statusTextView.setAdapter(statusAdapter);
        setStatus();

        updateButton.setOnClickListener(view -> {
            updateProduct();
        });

        statusTextView.setOnItemClickListener((adapterView, view, i, l) -> {
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
        });

        productApiService = ServiceGenerator.createProductService();
    }

    public void initToolbar() {

        TextView title = toolbar.findViewById(R.id.app_bar_title);
        title.setText(R.string.edit_product);
        ImageView home = toolbar.findViewById(R.id.app_bar_home);

        home.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back_black_24dp));
        home.setOnClickListener(view -> {
            EditProductActivity.super.onBackPressed();
            finish();
        });
    }

    public void initViews() {
        updateButton = findViewById(R.id.update_button);
        productImage = findViewById(R.id.product_image);
        productName = findViewById(R.id.product_name);
        productDescription = findViewById(R.id.product_description);
        statusMenu = findViewById(R.id.product_status);
        statusTextView = findViewById(R.id.statusTextView);
    }


    public void updateProduct() {
        if (productName.getEditText().getText().equals("")
                || productDescription.getEditText().getText().equals("")) {
            Toast.makeText(this, "Please Fill all Fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            product.name = productName.getEditText().getText().toString();

            ProductEditRequest requestBody = new ProductEditRequest(product);
            Call<ResponseBody> responseCall = productApiService.updateProduct(storeId, product.id, requestBody);

            progressDialog.show();
            responseCall.clone().enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        progressDialog.dismiss();
                        Intent intent = new Intent(getApplicationContext(), ProductsActivity.class);
                        startActivity(intent);
                        Toast.makeText(getApplicationContext(), "Product Updated Successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.request_failure, Toast.LENGTH_SHORT).show();
                        Log.e("edit-product-activity", "ERROR: " + response.toString());
                        progressDialog.dismiss();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(getApplicationContext(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            });
        } catch (Exception e) {
            Log.e("edit-product-activity", "Error while editing product" + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }


    public void setStatus() {
        statusList.add("Active");
        statusList.add("Inactive");
//        statusList.add("Out of Stock");
        String status = product.status;
        switch (status) {
            case "ACTIVE":
                statusTextView.setText(statusList.get(0), false);
                break;
            case "INACTIVE":
                statusTextView.setText(statusList.get(1), false);
                break;
//            case "OUTOFSTOCK":
//                statusTextView.setText(statusList.get(2), false);
//                break;
        }
        statusAdapter.notifyDataSetChanged();
    }

    private void getProductDetails() {

        if (product.name != null) {
            productName.getEditText().setText(product.name);
        }
        if (product.description != null) {
            productDescription.getEditText().setText(Html.fromHtml(product.description));
        }
        if (product.thumbnailUrl != null) {
            Glide.with(this).load(product.thumbnailUrl).into(productImage);
        }
    }
}

