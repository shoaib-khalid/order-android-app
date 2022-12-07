package com.symplified.order.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.symplified.order.App;
import com.symplified.order.EditProductActivity;
import com.symplified.order.R;
import com.symplified.order.models.product.Product;

import java.text.DecimalFormat;
import java.util.List;

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

        formatter = new DecimalFormat("#,###0.00");

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

            Glide.with(context).load(products.get(position).thumbnailUrl).into(holder.productImage);

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