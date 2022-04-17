package com.symplified.easydukan.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.symplified.easydukan.R;
import com.symplified.easydukan.models.item.SubItem;

import java.util.List;

public class SubItemsAdapter extends RecyclerView.Adapter<SubItemsAdapter.ViewHolder> {
    public List<SubItem> items;
    public List<String> testItems;

    public void setItems(List<SubItem> items) {
        this.items = items;
    }


    public SubItemsAdapter (){}
    public SubItemsAdapter(List<SubItem> items){
        this.items = items;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        private Chip subItemChip;


        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            subItemChip = view.findViewById(R.id.subItemName);

        }


    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.sub_item_row, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        holder.subItemChip.setText(items.get(position).productName);

    }


    @Override
    public int getItemCount() {
        return items.size();
    }
}
