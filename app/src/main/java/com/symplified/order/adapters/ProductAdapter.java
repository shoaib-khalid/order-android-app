package com.symplified.order.adapters;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
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
    private static final String TAG = "ProductAdapter";
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
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);

        holder.productName.setText(products.get(position).name);
        holder.productPrice.setText(currency+" "+Double.toString(products.get(position).productInventories.get(0).price));
        String status = products.get(position).status;

        switch (status) {
            case "INACTIVE":
                holder.productStatus.setText("Inactive");
                holder.productStatus.setTextColor(ContextCompat.getColor(context, R.color.sf_cancel_button));
                holder.statusIcon.setColorFilter(context.getResources().getColor(R.color.sf_cancel_button));
                break;
            case "OUTOFSTOCK":
                holder.productStatus.setText("Out of Stock");
                holder.productStatus.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                holder.statusIcon.setColorFilter(context.getResources().getColor(R.color.dark_grey));
                break;
        }


        holder.edit.setOnClickListener(view -> {
            Intent intent = new Intent(context, EditProductActivity.class);
            intent.putExtra("product", products.get(position));
            context.startActivity(intent);
        });

        try {
            Bitmap bitmap = new DownloadImageTask().execute(products.get(position).thumbnailUrl).get();
            if (bitmap != null) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
                String encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
                if (encodedImage != null) {
                    Utility.decodeAndSetImage(holder.productImage, encodedImage);
                }
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return products == null ? 0 : products.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice, productStatus;
        ImageView productImage, statusIcon, edit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            productImage = itemView.findViewById(R.id.product_image);
            edit = itemView.findViewById(R.id.product_edit);
            productStatus = itemView.findViewById(R.id.product_status);
            statusIcon = itemView.findViewById(R.id.ic_product_status);
        }
    }
}