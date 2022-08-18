package com.symplified.order;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.symplified.order.apis.CategoryApi;
import com.symplified.order.apis.ProductApi;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.databinding.ActivityEditProductBinding;
import com.symplified.order.models.Store.Store;
import com.symplified.order.models.Store.StoreResponse;
import com.symplified.order.models.category.Category;
import com.symplified.order.models.category.CategoryResponse;
import com.symplified.order.models.product.Product;
import com.symplified.order.services.DownloadImageTask;
import com.symplified.order.utils.Utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EditProductActivity extends NavbarActivity {

    private Button updateButton;
    private Toolbar toolbar;
    private Product product = null;
    private TextInputLayout productName, productDescription, statusMenu;
    private ImageView productImage;
//    private AutoCompleteTextView categoryTextView;
//    private TextInputLayout categoryMenu;
    private AutoCompleteTextView statusTextView;
    private final int REQ_CODE = 100;

//    private List<Category> categories;
    private String BASE_URL;
    private String storeId;
    private SharedPreferences sharedPreferences;
//    private List<String> categoryNames;
//    private ArrayAdapter<String> categoryAdapter;

//    private Uri uri = null;
    private List<String> statusList;
    private String accesToken;
    private ArrayAdapter<String> statusAdapter;

    private static Dialog progressDialog;

    private ActivityEditProductBinding binding;

    private ActivityResultLauncher<Intent> selectImageActivityResultLauncher;

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        requestStorageAccessPermission();

        sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        BASE_URL = sharedPreferences.getString("base_url", null);
        accesToken = sharedPreferences.getString("accessToken", null);
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

        Intent intent = getIntent();
//        if (intent.hasExtra("product")) {
        Bundle data = getIntent().getExtras();
        product = (Product) data.getSerializable("product");
        storeId = product.storeId;
//        }

//        if (isEdit) {
        getProductDetails();
//        }

//        categories = new ArrayList<>();
//        categoryNames = new ArrayList<>();
//        categoryAdapter = new ArrayAdapter<>(this, R.layout.drop_down_item, categoryNames);
//        categoryTextView.setAdapter(categoryAdapter);
//        setCategory();

        statusList = new ArrayList<>();
        statusAdapter = new ArrayAdapter<>(this, R.layout.drop_down_item, statusList);
        statusTextView.setAdapter(statusAdapter);
        setStatus();

//        categoryTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                product.categoryId = categories.get(i).id;
//            }
//        });

        updateButton.setOnClickListener(view -> {
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

//        initImageSelector();
    }

    public void initToolbar() {

        TextView title = toolbar.findViewById(R.id.app_bar_title);
        title.setText(R.string.edit_product);
        ImageView home = toolbar.findViewById(R.id.app_bar_home);

        home.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back_black_24dp));
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditProductActivity.super.onBackPressed();
                finish();
            }
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

        if (productName.getEditText().getText().equals("") ||
                productDescription.getEditText().getText().equals("")
        ) {
            Toast.makeText(this, "Please Fill all Fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer "+ accesToken);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient())
                .baseUrl(BASE_URL + App.PRODUCT_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ProductApi api = retrofit.create(ProductApi.class);


//        if (!isEdit) {
//            Product.ProductInventory inventory = product.new ProductInventory();
//            inventory.price = Double.parseDouble(productPrice.getEditText().getText().toString());
//            inventory.quantity = Integer.parseInt(productQuantity.getEditText().getText().toString());
//            inventory.sku = productSKU.getEditText().getText().toString();
//            product.name = productName.getEditText().getText().toString();
//            product.description = productDescription.getEditText().getText().toString();
//            product.seoName = product.name.toLowerCase(Locale.ROOT).replace("/ /g", "-").replace("/[-]+/g", "-").replace("/[^\b-]+/g", "");
//            product.seoUrl = "https://" + store.domain + "/product/" + product.seoName;
//            product.minQuantityForAlarm = -1;
//            product.packingSize = "S";
//            product.isPackage = false;
//            product.storeId = storeId;
//            product.allowOutOfStockPurchases = ((store.verticalCode.equals("FnB") || store.verticalCode.equals("FnB_PK")) && !product.status.equals("OUTOFSTOCK")) ? true : false;
//
//            Call<ResponseBody> responseCall = api.postProduct(headers, storeId, product);
//
//            progressDialog.show();
//            responseCall.clone().enqueue(new Callback<ResponseBody>() {
//                @Override
//                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                    if (response.code() == 201) {
//
//                        Product temp = null;
//                        try {
//                            temp = new Gson().fromJson(response.body().string(), Product.SingleProductResponse.class).data;
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        inventory.itemCode = temp.id + "aa";
//
//                        if (uri != null) {
//                            uploadProductImage(uri, api, headers, temp);
//                        }
//
//                        Call<ResponseBody> updateInvntoryCall = api.postProductInventory(headers, storeId, temp.id, inventory);
//
//                        updateInvntoryCall.clone().enqueue(new Callback<ResponseBody>() {
//                            @Override
//                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                                if (response.isSuccessful()) {
//                                    progressDialog.dismiss();
//                                    Intent intent = new Intent(getApplicationContext(), ProductsActivity.class);
//                                    startActivity(intent);
//                                    Toast.makeText(getApplicationContext(), "Product Added Successfully", Toast.LENGTH_SHORT).show();
//                                } else {
//                                    progressDialog.dismiss();
//                                    Toast.makeText(getApplicationContext(), "Failed to Add Product", Toast.LENGTH_SHORT).show();
//                                    logError("Unsuccessful when creating product Inventory: " + response.code() + ", " + response.message());
//                                }
//                            }
//
//                            @Override
//                            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                                Toast.makeText(getApplicationContext(), "Failed to Add Product", Toast.LENGTH_SHORT).show();
//                                progressDialog.dismiss();
//                                logError("onFailure when creating product Inventory: " + t.getLocalizedMessage());
//                                t.printStackTrace();
//                            }
//                        });
//                    } else {
//                        Toast.makeText(getApplicationContext(), "Failed to Add Product", Toast.LENGTH_SHORT).show();
//                        progressDialog.dismiss();
//                        logError("Unsuccessful when creating product: " + response.code() + ", " + response.message());
//                    }
//                }
//
//                @Override
//                public void onFailure(Call<ResponseBody> call, Throwable t) {
//                    Toast.makeText(getApplicationContext(), "Failed to Add Product", Toast.LENGTH_SHORT).show();
//                    progressDialog.dismiss();
//                    logError("onFailure when adding product: " + t.getLocalizedMessage());
//                    t.printStackTrace();
//                }
//            });
//        } else {
        try {
            product.name = productName.getEditText().getText().toString();

            SpannableStringBuilder spannableString = (SpannableStringBuilder) productDescription.getEditText().getText();

            product.description = Html.toHtml(spannableString);

//                if (uri != null) {
//                    uploadProductImage(uri, api, headers, product);
//                }
            Call<ResponseBody> responseCall = api.updateProduct(headers, storeId, product.id, product);

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


//    public void setCategory() {
//        Map<String, String> headers = new HashMap<>();
//        headers.put("Authorization", "Bearer " + sharedPreferences.getString("accessToken", "accessToken"));
//
//        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient())
//                .baseUrl(BASE_URL + App.PRODUCT_SERVICE_URL)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        CategoryApi api = retrofit.create(CategoryApi.class);
//
//        Call<CategoryResponse> responseCall = api.getCategories(headers, storeId);
//
//        progressDialog.show();
//
//        responseCall.clone().enqueue(new Callback<CategoryResponse>() {
//            @Override
//            public void onResponse(Call<CategoryResponse> call, Response<CategoryResponse> response) {
//                if (response.isSuccessful()) {
//                    categories.addAll(response.body().data.content);
//                    for (Category category : categories) {
//                        categoryNames.add(category.name);
//                    }
//                    if (isEdit) {
//                        for (int i = 0; i < categories.size(); i++) {
//                            if (categories.get(i).id.equals(product.categoryId)) {
//                                categoryTextView.setText(categoryNames.get(i), false);
//                            }
//                        }
//                    }
//                    categoryAdapter.notifyDataSetChanged();
//                }
//                progressDialog.dismiss();
//            }
//
//            @Override
//            public void onFailure(Call<CategoryResponse> call, Throwable t) {
//                progressDialog.dismiss();
//
//            }
//        });
//
//    }

    public void setStatus() {

        statusList.add("Active");
        statusList.add("Inactive");
        statusList.add("Out of Stock");
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
                break;
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
        try {
            Bitmap bitmap = new DownloadImageTask().execute(product.thumbnailUrl).get();
            if (bitmap != null) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
                String encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
                if (encodedImage != null) {
                    Utility.decodeAndSetImage(productImage, encodedImage);
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

//    public String getPath(Uri uri) {
//        String[] proj = {MediaStore.Images.Media.DATA};
//        Cursor returnCursor =
//                getContentResolver().query(uri, proj, null, null, null);
//        returnCursor.moveToFirst();
//        int nameIndex = returnCursor.getColumnIndex(MediaStore.Images.Media.DATA);
//        String result = returnCursor.getString(nameIndex);
//        returnCursor.close();
//        return result;
//    }

//    private void requestStorageAccessPermission() {
//        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
//    }

//    private void uploadProductImage(Uri imageUri, ProductApi api, Map<String, String> headers, Product prod) {
//        File file = new File(getPath(imageUri));
//        RequestBody requestFile =
//                RequestBody.create(file, MediaType.parse("multipart/form-data"));
//        MultipartBody.Part body =
//                MultipartBody.Part.createFormData("file", file.getName(), requestFile);
//        Call<ResponseBody> updateThumbnailCall = api.updateThumbnail(headers, storeId, prod.id, body);
//        updateThumbnailCall.enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                if (response.isSuccessful()) {
//                    Toast.makeText(getApplicationContext(), "Product Image Uploaded Successfully", Toast.LENGTH_SHORT).show();
//                } else {
//                    Log.e("edit-product-activity", "Error while uploading photo: " + response);
//                    Toast.makeText(getApplicationContext(), "Error while uploading photo. " + response.message(), Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                Log.e("edit-product-activity", "onFailure while uploading Photo: " + t.getLocalizedMessage());
//                t.printStackTrace();
//            }
//        });
//    }

    private void logError(String errorMessage) {
        Log.e("edit-product-activity", errorMessage);
    }
}

