package com.symplified.order.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.item.UpdatedItem;
import com.symplified.order.models.order.Order;

import java.util.ArrayList;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    public List<Item> items;
    public Order order;
    public List<UpdatedItem> updatedItemsList;
    public Context context;
    private static String TAG = ItemAdapter.class.getName();
    public void setItems(List<Item> items) {
        this.items = items;
    }

    public ItemAdapter(){}
    public ItemAdapter(List<Item> items, Order order, Context context){
        this.items = items;
        this.order = order;
        this.updatedItemsList = new ArrayList<>();
        this.context = context;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name, qty, price, specialInstructions;

        public ViewHolder(View view) {
            super(view);

            name = view.findViewById(R.id.header_item_name);
            qty = view.findViewById(R.id.header_qty);
            price = view.findViewById(R.id.header_price);
            specialInstructions = view.findViewById(R.id.header_instruction_value);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.order_item_row, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        String currency = sharedPreferences.getString("currency", null);

        holder.name.setText(items.get(position).productName);
        holder.qty.setText(Integer.toString(items.get(position).quantity));
        holder.price.setText(currency+ " " + Double.toString(items.get(position).price));
        if (!items.get(position).specialInstruction.equals("") && items.get(position).specialInstruction != null) {
            holder.specialInstructions.setVisibility(View.VISIBLE);
            holder.specialInstructions.setText(items.get(position).specialInstruction);
        }

    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
