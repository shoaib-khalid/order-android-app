package com.symplified.order.ui.orders.tabs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.symplified.order.R;
import com.symplified.order.adapters.ItemAdapter;
import com.symplified.order.models.qrorders.ConsolidatedOrder;
import com.symplified.order.ui.orders.ConsolidateOrderActivity;

import java.util.ArrayList;
import java.util.List;

public class TablesAdapter extends RecyclerView.Adapter<TablesAdapter.ViewHolder> {

    private final OnTableClickListener clickListener;
    private final List<ConsolidatedOrder> orders = new ArrayList<>();

    public interface OnTableClickListener {
        void onTableClicked(ConsolidatedOrder order);
    }

    public TablesAdapter(OnTableClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tableNoTextView;

        public ViewHolder(View view) {
            super(view);

            tableNoTextView = view.findViewById(R.id.text_view);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.grid_table, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tableNoTextView.setText(orders.get(position).tableNo);
        holder.itemView.setOnClickListener(v ->
                clickListener.onTableClicked(orders.get(holder.getAdapterPosition())));
    }

    @Override
    public int getItemCount() { return orders.size(); }

    public void setOrders(List<ConsolidatedOrder> orders) {
        clear();
        this.orders.addAll(orders);
        notifyItemRangeInserted(0, orders.size());
    }

    public void updatedOrder(ConsolidatedOrder updatedOrder) {
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).invoiceNo.equals(updatedOrder.invoiceNo)) {
                orders.set(i, updatedOrder);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void removeOrder(ConsolidatedOrder orderToRemove) {
        int indexToRemove = -1;
        for (int i = 0; i < orders.size(); i++) {
            if (orderToRemove.invoiceNo.equals(orders.get(i).invoiceNo)) {
                indexToRemove = i;
                break;
            }
        }
        if (indexToRemove != -1) {
            orders.remove(indexToRemove);
            notifyItemRemoved(indexToRemove);
        }
    }

    public void clear() {
        int originalSize = orders.size();
        orders.clear();
        notifyItemRangeRemoved(0, originalSize);
    }
}
