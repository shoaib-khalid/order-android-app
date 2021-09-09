package com.symplified.order.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.symplified.order.OrderDetails;
import com.symplified.order.R;
import com.symplified.order.models.OrderDetailsModel;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
    public List<OrderDetailsModel> items;

    public OrderAdapter(List<OrderDetailsModel> items){
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name,phone, qty, amount,invoice;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            name = (TextView) view.findViewById(R.id.order_row_name_value);
            phone = (TextView) view.findViewById(R.id.order_row_phone_value);
            qty = (TextView) view.findViewById(R.id.order_quantity_value);
            amount = (TextView) view.findViewById(R.id.order_amount_value);
            invoice = (TextView) view.findViewById(R.id.card_invoice_value);
        }


    }


    @NonNull
    @Override
    public OrderAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.order_row, parent, false);
//        Log.e("TAG", "onCreateViewHolder: size = "+getItemCount(),new Error() );
        listItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent (parent.getContext(), OrderDetails.class);
                view.getContext().startActivity(intent);
            }
        });
        return new OrderAdapter.ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        holder.name.setText(items.get(position).name);
        holder.phone.setText(items.get(position).phone);
        holder.qty.setText(items.get(position).quantity);
        holder.amount.setText(items.get(position).amount);
        holder.invoice.setText(items.get(position).invoice);

    }


    @Override
    public int getItemCount() {
        return items.size();
    }
}

