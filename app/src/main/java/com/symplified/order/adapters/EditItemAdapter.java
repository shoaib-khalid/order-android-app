package com.symplified.order.adapters;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.models.asset.StoreProductAsset;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.item.UpdatedItem;
import com.symplified.order.models.order.Order;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.networking.apis.ProductApi;
import com.symplified.order.utils.SharedPrefsKey;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditItemAdapter extends RecyclerView.Adapter<EditItemAdapter.ViewHolder> {

    public List<Item> items;
    public Context context;

    public List<UpdatedItem> updatedItemsList;
    public Order order;

    public String TAG = EditItemAdapter.class.getName();
    public DecimalFormat formatter;

    public EditItemAdapter() {
    }

    public EditItemAdapter(List<Item> items, Context context, Order order) {
        this.items = items;
        this.context = context;
        this.order = order;

        updatedItemsList = new ArrayList<>();


            for (Item item : items) {
                item.newQuantity = item.quantity;
            }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView itemImage, delete, decrement, increment;
        private final TextView name, price, qty, specialInstructions;
        private final RelativeLayout layoutSpecialInstructions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            itemImage = itemView.findViewById(R.id.item_image);
            delete = itemView.findViewById(R.id.delete_icon);
            decrement = itemView.findViewById(R.id.item_decrement);
            increment = itemView.findViewById(R.id.item_increment);

            name = itemView.findViewById(R.id.item_name);
            price = itemView.findViewById(R.id.item_price_label);
            qty = itemView.findViewById(R.id.item_quantity);
            specialInstructions = itemView.findViewById(R.id.item_special_instructions_value);
            layoutSpecialInstructions = itemView.findViewById(R.id.rl_special_instructions);

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

        SharedPreferences sharedPreferences = context.getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);
        String currency = sharedPreferences.getString(SharedPrefsKey.CURRENCY_SYMBOL, null);

        formatter = new DecimalFormat("#,###0.00");

        holder.name.setText(items.get(position).productName);
        holder.price.setText(currency + " " + formatter.format(items.get(position).price));
        holder.qty.setText(Integer.toString(items.get(position).newQuantity));

        if (items.get(position).specialInstruction != null && !items.get(position).specialInstruction.equals("")) {
            holder.layoutSpecialInstructions.setVisibility(View.VISIBLE);
            holder.specialInstructions.setText(items.get(position).specialInstruction);
        }

        if (holder.itemImage.getDrawable() == null) {
            getProductImageFromAssets(items.get(position), holder);
        }

        UpdatedItem updatedItem = new UpdatedItem();

        updatedItem.id = items.get(position).id;
        updatedItem.itemCode = items.get(position).itemCode;
        updatedItem.quantity = items.get(position).newQuantity;

        if (!updatedItemsList.contains(updatedItem) && items.get(position).newQuantity != items.get(position).quantity) {
            updatedItemsList.add(updatedItem);
        }

        holder.decrement.setOnClickListener(view -> {
            Item item = items.get(holder.getAdapterPosition());
            if (item.newQuantity > 0) {
                item.newQuantity--;
                item.price = item.productPrice * item.newQuantity;
                notifyDataSetChanged();
            }
        });

        holder.increment.setOnClickListener(view -> {
            Item item = items.get(holder.getAdapterPosition());

            if (item.newQuantity < item.quantity) {
                item.newQuantity++;
                item.price = item.productPrice * item.newQuantity;
                notifyDataSetChanged();
            }
        });

        holder.delete.setOnClickListener(view -> {
            int currentPosition = holder.getAdapterPosition();
            updatedItem.quantity = 0;
            updatedItemsList.add(updatedItem);
            items.remove(currentPosition);
            Toast.makeText(context, "Item Removed", Toast.LENGTH_SHORT).show();
            notifyItemRemoved(currentPosition);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void getProductImageFromAssets(Item item, ViewHolder holder) {
        ProductApi productApiService = ServiceGenerator.createProductService(context);
        productApiService.getStoreProductAssets(order.storeId, item.productId)
                .clone().enqueue(new Callback<StoreProductAsset.StoreProductAssetListResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<StoreProductAsset.StoreProductAssetListResponse> call,
                                           @NonNull Response<StoreProductAsset.StoreProductAssetListResponse> response) {
                        if (response.isSuccessful()) {
                            boolean foundAsset = false;
                            List<StoreProductAsset> assets = response.body().data;
                            for (StoreProductAsset asset : assets) {
                                if (item.itemCode.equals(asset.itemCode)
                                        && asset.url != null) {
                                    loadItemImageFromUrl(asset.url, holder);
                                    foundAsset = true;
                                }
                            }
                            if (!foundAsset && assets.size() >= 1) {
                                loadItemImageFromUrl(assets.get(0).url, holder);
                            }
                        } else {
                            Toast.makeText(context, "Failed to load item image", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<StoreProductAsset.StoreProductAssetListResponse> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(context, "Failed to load item image", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public List<UpdatedItem> getUpdatedItems() {
        return updatedItemsList;
    }

    private void loadItemImageFromUrl(String url, ViewHolder holder) {
        Glide.with(context)
                .load(url).into(holder.itemImage);
    }
}