package com.ekedai.merchant.ui.products;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.ekedai.merchant.R;
import com.ekedai.merchant.databinding.ActivityEditProductBinding;
import com.ekedai.merchant.enums.ProductStatus;
import com.ekedai.merchant.models.product.Product;
import com.ekedai.merchant.models.product.ProductEditResponse;
import com.ekedai.merchant.models.product.UpdatedProduct;
import com.ekedai.merchant.networking.ServiceGenerator;
import com.ekedai.merchant.networking.apis.ProductApi;
import com.ekedai.merchant.ui.NavbarActivity;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProductActivity extends NavbarActivity {

    public static final String UPDATED_PRODUCT_ID = "UPDATED_PRODUCT_ID";
    public static final String UPDATED_PRODUCT = "UPDATED_PRODUCT";
    public static String PRODUCT = "product";

    private ActivityEditProductBinding binding;
    private Button updateButton;
    private Toolbar toolbar;
    private Product product = null;
    private TextInputLayout productName;
    private TextInputLayout productDescription;
    private ImageView productImage;

    private String storeId;

    private final ProductStatus[] productStatuses = ProductStatus.values();
    private ArrayAdapter<ProductStatus> statusAdapter;

    private static Dialog progressDialog;

    private ProductApi productApiService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
        product = (Product) data.getSerializable(PRODUCT);
        storeId = product.storeId;

        updateButton.setOnClickListener(view -> updateProduct());

        StatusAdapter adapter = new StatusAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                productStatuses
        );
        binding.productStatusSpinner.setAdapter(adapter);
        binding.productStatusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                product.status = adapter.getItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        getProductDetails();

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
    }


    public void updateProduct() {
        if ("".equals(productName.getEditText() != null ? productName.getEditText().getText().toString() : "")) {
            Toast.makeText(this, "Please Fill all Fields", Toast.LENGTH_SHORT).show();
            return;
        }

        product.name = productName.getEditText().getText().toString();

        progressDialog.show();
        productApiService.updateProduct(storeId, product.id, new UpdatedProduct(product))
                .clone().enqueue(new Callback<ProductEditResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductEditResponse> call,
                                   @NonNull Response<ProductEditResponse> response) {
                if (response.isSuccessful()) {
                    progressDialog.dismiss();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(UPDATED_PRODUCT_ID, product.id);
                    resultIntent.putExtra(UPDATED_PRODUCT, response.body().data);
                    setResult(Activity.RESULT_OK, resultIntent);
                    Toast.makeText(getApplicationContext(), "Product Updated Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.request_failure, Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductEditResponse> call, @NonNull Throwable t) {
                Toast.makeText(getApplicationContext(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        });
    }
    private void getProductDetails() {

        if (product.name != null && productName.getEditText() != null) {
            productName.getEditText().setText(product.name);
        }
        if (product.description != null && productDescription.getEditText() != null) {
            productDescription.getEditText().setText(Html.fromHtml(product.description));
        }
        if (product.status != null) {
//            binding.productStatus.setText(product.status.text);
            int indexOfStatus = 0;
            for (int i = 0; i < productStatuses.length; i++) {
                if (product.status == productStatuses[i]) {
                    indexOfStatus = i;
                    break;
                }
            }
            binding.productStatusSpinner.setSelection(indexOfStatus, true);
        }
        if (product.thumbnailUrl != null) {
            Glide.with(this).load(product.thumbnailUrl).into(productImage);
        }
    }

    private static class StatusAdapter extends ArrayAdapter<ProductStatus> {
        private final ProductStatus[] statuses;

        public StatusAdapter(
                Context context,
                int textViewResourceId,
                ProductStatus[] statuses
        ) {
            super(context, textViewResourceId, statuses);
            this.statuses = statuses;
        }

        @Override
        public int getCount() {
            return statuses.length;
        }

        @Nullable
        @Override
        public ProductStatus getItem(int position) {
            return statuses[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView label = (TextView) super.getView(position, convertView, parent);
            label.setText(statuses[position].text);
            return label;
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView label = (TextView) super.getDropDownView(position, convertView, parent);
            label.setText(statuses[position].text);
            return label;
        }
    }
}

