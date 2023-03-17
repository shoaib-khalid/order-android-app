package com.symplified.order.ui.products;

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
import com.symplified.order.R;
import com.symplified.order.models.product.Product;
import com.symplified.order.models.store.Store;
import com.symplified.order.utils.SharedPrefsKey;
import com.symplified.order.utils.Utility;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private final Context context;
    private List<Store> stores = new ArrayList<>();
    private List<Product> products = new ArrayList<>();
    private List<Product> productsToShow = new ArrayList<>();
    private static final String TAG = "ProductAdapter";
    private final DecimalFormat formatter = Utility.getMonetaryAmountFormat();

    public ProductAdapter(Context context) {
        this.context = context;
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

    @NonNull
    @Override
    public ProductAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.product_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);
        String currency = sharedPreferences.getString(SharedPrefsKey.CURRENCY_SYMBOL, null);

        Product product = productsToShow.get(position);

        holder.productName.setText(product.name);

        for (Store store : stores) {
            if(product.storeId.equals(store.id)) {
                Product.ProductInventory productInventory = product.productInventories.get(0);
                String price = formatter.format(
                        store.isDineIn ? productInventory.dineInPrice : productInventory.price
                );

                holder.productPrice.setText(currency + " " + price);
            }
        }

        switch (product.status) {
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
            intent.putExtra("product", product);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });

        Glide.with(context)
                .load(product.thumbnailUrl)
                .into(holder.productImage);
    }

    @Override
    public int getItemCount() {
        return productsToShow.size();
    }

    public void setStores(List<Store> stores) {
        this.stores = stores;
    }

    public void setProducts(List<Product> products) {
        clear();
        this.products = products;
        notifyItemRangeInserted(0, products.size());
    }

    public void clear() {
        int originalSize = products.size();
        products.clear();
        notifyItemRangeRemoved(0, originalSize);
    }

    public void filter(String searchTerm) {
        searchTerm = searchTerm.toLowerCase();
        productsToShow.clear();
        if (!Utility.isBlank(searchTerm)) {
            for (Product product : products) {
                if (product.name.toLowerCase().contains(searchTerm) &&
                        !productsToShow.contains(product)) {
                    productsToShow.add(product);
                    notifyItemInserted(productsToShow.size() - 1);
                }
            }
        } else {
            productsToShow.addAll(products);
        }
        notifyDataSetChanged();
    }
}