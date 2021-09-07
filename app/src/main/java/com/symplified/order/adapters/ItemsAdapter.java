package com.symplified.order.adapters;

import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.symplified.order.R;
import com.symplified.order.models.Item;

import java.util.List;

public class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ViewHolder> {
    public List<Item> items;

    public ItemsAdapter(List<Item> items){
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView item;
        private final TextView instructions;
        private final TextView qty;
        private final TextView price;
        private final ConstraintLayout constraintLayout;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            item = (TextView) view.findViewById(R.id.header_item);
            instructions = (TextView) view.findViewById(R.id.header_instruction);
            qty = (TextView) view.findViewById(R.id.header_qty);
            price = (TextView) view.findViewById(R.id.header_price);
            constraintLayout = (ConstraintLayout)  view.findViewById(R.id.order_item) ;

            item.setTypeface(Typeface.DEFAULT);
            instructions.setTypeface(Typeface.DEFAULT);
            qty.setTypeface(Typeface.DEFAULT);
            price.setTypeface(Typeface.DEFAULT);
        }


    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.order_item, parent, false);
//        Log.e("TAG", "onCreateViewHolder: size = "+getItemCount(),new Error() );
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        holder.item.setText(items.get(position).item);
        holder.instructions.setText(items.get(position).specialInstruction);
        holder.qty.setText(items.get(position).qty);
        holder.price.setText(items.get(position).price);

    }


    @Override
    public int getItemCount() {
        return items.size();
    }
}
