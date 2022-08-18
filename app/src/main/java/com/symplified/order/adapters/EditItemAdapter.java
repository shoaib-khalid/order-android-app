package com.symplified.order.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.gson.Gson;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.apis.ProductApi;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.item.UpdatedItem;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.product.Product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EditItemAdapter extends RecyclerView.Adapter<EditItemAdapter.ViewHolder> {

    public List<Item> items;
    public Context context;

    public List<UpdatedItem> updatedItemsList;
    public Order order;

    public String TAG = EditItemAdapter.class.getName();

    public EditItemAdapter() {
    }

    public EditItemAdapter(List<Item> items, Context context, Order order) {
        this.items = items;
        this.context = context;
        this.order = order;

        updatedItemsList = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            items.forEach(item -> {
                item.newQuantity = item.quantity;
            });
        } else {
            for (Item item : items) {
                item.newQuantity = item.quantity;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView itemImage, delete, decrement, increment;
        private final TextView name, price, qty, specialInstructions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            itemImage = itemView.findViewById(R.id.item_image);
            delete = itemView.findViewById(R.id.delete_icon);
            decrement = itemView.findViewById(R.id.item_decrement);
            increment = itemView.findViewById(R.id.item_increment);

            name = itemView.findViewById(R.id.item_name);
            price = itemView.findViewById(R.id.item_price);
            qty = itemView.findViewById(R.id.item_quantity);
            specialInstructions = itemView.findViewById(R.id.item_special_instructions);

        }
    }

    @NonNull
    @Override
    public EditItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.edit_order_item_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EditItemAdapter.ViewHolder holder, int position) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        String currency = sharedPreferences.getString("currency", null);
        String BASE_URL = sharedPreferences.getString("base_url", null);
        String storeId = sharedPreferences.getString("storeId", null);

        holder.name.setText(items.get(position).productName);
        holder.price.setText(currency + " " + String.format("%.2f", items.get(position).price));
        holder.qty.setText(Integer.toString(items.get(position).newQuantity));

        if (items.get(position).specialInstruction != null && !items.get(position).specialInstruction.equals("")) {
            holder.specialInstructions.setVisibility(View.VISIBLE);
            holder.specialInstructions.setText(items.get(position).specialInstruction);
        }

        if (holder.itemImage.getDrawable() == null) {
            getProductImage(items.get(position), BASE_URL, storeId, holder);
        }

        UpdatedItem updatedItem = new UpdatedItem();

        updatedItem.id = items.get(position).id;
        updatedItem.itemCode = items.get(position).itemCode;
        updatedItem.quantity = items.get(position).newQuantity;

        if (!updatedItemsList.contains(updatedItem) && items.get(position).newQuantity != items.get(position).quantity) {
            updatedItemsList.add(updatedItem);
        }

        holder.decrement.setOnClickListener(view -> {
            if (items.get(position).newQuantity > 0) {
                items.get(position).newQuantity--;
                notifyDataSetChanged();
            }
        });

        holder.increment.setOnClickListener(view -> {
            if (items.get(position).newQuantity < items.get(position).quantity) {
                items.get(position).newQuantity++;
                notifyDataSetChanged();
            }
        });

        holder.delete.setOnClickListener(view -> {
            updatedItem.quantity = 0;
            updatedItemsList.add(updatedItem);
            items.remove(position);
            Toast.makeText(context, "Item Removed", Toast.LENGTH_SHORT).show();
            notifyItemRemoved(position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void getProductImage(Item item, String BASE_URL, String storeId, ViewHolder holder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient())
                .baseUrl(BASE_URL + App.PRODUCT_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ProductApi productApi = retrofit.create(ProductApi.class);

        Call<ResponseBody> responseBodyCall = productApi.getProductById(headers, storeId, item.productId);

        responseBodyCall.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        Product.SingleProductResponse product = new Gson().fromJson(response.body().string(), Product.SingleProductResponse.class);
                        if (product.data.thumbnailUrl != null)
                            Glide.with(context)
                                .load(product.data.thumbnailUrl).into(holder.itemImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e(TAG, "Error Downloading Image");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "onFailure", t);
            }
        });
    }

    public List<UpdatedItem> getUpdatedItems() {
        return updatedItemsList;
    }
}