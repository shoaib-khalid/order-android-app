package com.symplified.order.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.item.ItemAddOn;
import com.symplified.order.models.item.SubItem;
import com.symplified.order.models.item.UpdatedItem;
import com.symplified.order.models.order.Order;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    public List<Item> items;
    public Order order;
    public List<UpdatedItem> updatedItemsList;
    public Context context;
    public DecimalFormat formatter;
    private static String TAG = ItemAdapter.class.getName();

    public ItemAdapter(List<Item> items, Order order, Context context){
        this.items = items;
        this.order = order;
        this.updatedItemsList = new ArrayList<>();
        this.context = context;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView name, qty, price, specialInstructions;
        private final RelativeLayout subItemLayout;
        private final RecyclerView subItemsRecyclerView;
        private final RelativeLayout layoutSpecialInstructions;

        public ViewHolder(View view) {
            super(view);

            name = view.findViewById(R.id.header_item_name);
            qty = view.findViewById(R.id.header_qty);
            price = view.findViewById(R.id.header_price);
            specialInstructions = view.findViewById(R.id.header_instruction_value);
            subItemsRecyclerView = view.findViewById(R.id.subItemRecyclerView);
            subItemsRecyclerView.setLayoutManager(new LinearLayoutManager(subItemsRecyclerView.getContext(),
                    RecyclerView.HORIZONTAL, false));
            subItemLayout = view.findViewById(R.id.subItems);
            layoutSpecialInstructions = view.findViewById(R.id.rl_special_instructions);
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

        Item orderItem = items.get(position);

        SharedPreferences sharedPreferences = context.getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);
        String currency = sharedPreferences.getString("currency", null);

        formatter = new DecimalFormat("#,###0.00");
        holder.name.setText(orderItem.productName);

        List<String> subItems = new ArrayList<>();
        SubItemsAdapter adapter = new SubItemsAdapter();
        for (SubItem subItem: orderItem.orderSubItem) {
            subItems.add(subItem.productName);
        }

        if (orderItem.productVariant != null) {
            subItems.addAll(Arrays.asList(orderItem.productVariant.split(",")));
        }

        for (ItemAddOn itemAddOn : orderItem.orderItemAddOn) {
            subItems.add(itemAddOn.productAddOn.addOnTemplateItem.name);
        }

        adapter.items = subItems;
        holder.subItemsRecyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        holder.subItemLayout.setVisibility(View.VISIBLE);

        holder.qty.setText(Integer.toString(orderItem.quantity));
        holder.price.setText(currency+ " " + formatter.format(orderItem.price));

        if (orderItem.specialInstruction != null && !orderItem.specialInstruction.equals("")) {
            holder.layoutSpecialInstructions.setVisibility(View.VISIBLE);
            holder.specialInstructions.setText(orderItem.specialInstruction);
        }

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public List<Item> getItems() { return items; }
}
