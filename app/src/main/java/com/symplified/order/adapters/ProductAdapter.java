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

import com.bumptech.glide.Glide;
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
import java.text.DecimalFormat;
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
    private static final String TAG = "ProductAdapter";
    private DecimalFormat formatter;

    public ProductAdapter(Context context, List<Product> products) {
        this.context = context;
        this.products = products;
    }

    @NonNull
    @Override
    public ProductAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.product_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        String currency = sharedPreferences.getString("currency", null);

        formatter = new DecimalFormat("#,###.00");

        holder.productName.setText(products.get(position).name);
        holder.productPrice.setText(currency+" "+ formatter.format(products.get(position).productInventories.get(0).price));
        String status = products.get(position).status;

        switch (status) {
            case "ACTIVE":
                holder.productStatus.setText("Active");
                holder.productStatus.setTextColor(ContextCompat.getColor(context, R.color.sf_primary));
                holder.statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.sf_primary));
                break;
            case "INACTIVE":
                holder.productStatus.setText("Inactive");
                holder.productStatus.setTextColor(ContextCompat.getColor(context, R.color.sf_cancel_button));
                holder.statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.sf_cancel_button));
                break;
            case "OUTOFSTOCK":
                holder.productStatus.setText("Out of Stock");
                holder.productStatus.setTextColor(ContextCompat.getColor(context, R.color.dark_grey));
                holder.statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.dark_grey));
                break;
        }


        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, EditProductActivity.class);
            intent.putExtra("product", products.get(position));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });

        if (products.get(position).thumbnailUrl != null)
            Glide.with(context).load(products.get(position).thumbnailUrl).into(holder.productImage);

//        try {
//            Bitmap bitmap = new DownloadImageTask().execute(products.get(position).thumbnailUrl).get();
//            if (bitmap != null) {
//                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
//                String encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
//                if (encodedImage != null) {
//                    Utility.decodeAndSetImage(holder.productImage, encodedImage);
//                }
//            }
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView productName, productPrice, productStatus;
        private final ImageView productImage, statusIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            productImage = itemView.findViewById(R.id.product_image);
            productStatus = itemView.findViewById(R.id.product_status);
            statusIcon = itemView.findViewById(R.id.ic_product_status);
        }
    }
}