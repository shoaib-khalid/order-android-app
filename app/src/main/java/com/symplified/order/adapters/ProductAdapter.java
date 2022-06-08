package com.symplified.order.adapters;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.symplified.order.App;
import com.symplified.order.EditProductActivity;
import com.symplified.order.R;
import com.symplified.order.apis.ProductApi;
import com.symplified.order.models.product.Product;
import com.symplified.order.services.DownloadImageTask;
import com.symplified.order.utils.Utility;

import java.io.ByteArrayOutputStream;
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

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private Context context;
    private List<Product> products;
    private static Dialog progressDialog;
    private String BASE_URL;
    private static final String TAG = "ProductsAdapter";
    private SharedPreferences sharedPreferences;
    private String storeId, currency;

    public ProductAdapter(Context context, List<Product> products) {
        this.context = context;
        this.products = products;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.product_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        sharedPreferences = context.getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        BASE_URL = sharedPreferences.getString("base_url", null);
        storeId = sharedPreferences.getString("storeId", null);
        currency = sharedPreferences.getString("currency", null);
        progressDialog = new Dialog(context);

        holder.prodTitle.setText(products.get(position).name);
        holder.prodPrice.setText(Double.toString(products.get(position).productInventories.get(0).price));
        holder.currency.setText(" " + currency);

        try {
            Bitmap bitmap = new DownloadImageTask().execute(products.get(position).thumbnailUrl).get();
            if (bitmap != null) {
                holder.prodImage.setImageBitmap(bitmap);
//                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
//                String encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
//                if (encodedImage != null) {
//                    Utility.decodeAndSetImage(holder.prodImage, encodedImage);
//                }
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        holder.edit.setOnClickListener(view -> {
            Intent intent = new Intent(context, EditProductActivity.class);
            intent.putExtra("product", products.get(position));

            context.startActivity(intent);
        });

        holder.delete.setOnClickListener(view ->  {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer Bearer accessToken");

            Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient())
                    .baseUrl(BASE_URL + App.PRODUCT_SERVICE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ProductApi api = retrofit.create(ProductApi.class);

            Dialog dialog = new MaterialAlertDialogBuilder(context)
                    .setTitle("Delete Product")
                    .setMessage("Do you really want to delete this product ?")
                    .setNegativeButton("No", null)
                    .setPositiveButton("Yes", ((dialogInterface, i) -> {
                        progressDialog.show();

                        Call<ResponseBody> responseBodyCall = api.deleteProduct(headers, storeId, products.get(holder.getAdapterPosition()).id);
                        progressDialog.show();
                        responseBodyCall.clone().enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.isSuccessful()) {
                                    products.remove(holder.getAdapterPosition());
                                    notifyDataSetChanged();
                                    progressDialog.dismiss();
                                }
                                progressDialog.dismiss();
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Log.e(TAG, "onFailure: ",t );
                                progressDialog.dismiss();
                            }
                        });

                    }))
                    .create();
            dialog.show();
        });
    }

    @Override
    public int getItemCount() {
        return products == null ? 0 : products.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView prodTitle, prodPrice, edit, delete, currency;
        ImageView prodImage;
        CardView prod_cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.prodTitle = itemView.findViewById(R.id.prodName);
            this.prodPrice = itemView.findViewById(R.id.prodPrice);
            this.prodImage = itemView.findViewById(R.id.prodImg);
            this.prod_cardView = itemView.findViewById(R.id.product_card_parent);
            edit = itemView.findViewById(R.id.product_edit);
            delete = itemView.findViewById(R.id.product_delete);
            currency = itemView.findViewById(R.id.prod_row_price);
        }
    }
}