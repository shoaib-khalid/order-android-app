package com.symplified.order.adapters;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.symplified.order.OrderDetails;
import com.symplified.order.R;
import com.symplified.order.models.OrderDetailsModel;
import com.symplified.order.models.order.Order;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
//    public List<OrderDetailsModel> items;

    public List<Order> orders;

    public OrderAdapter(List<Order> orders){
//        List<OrderDetailsModel> items,
//        this.items = items;
        this.orders = orders;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name,phone, amount,invoice;
        private final ImageView pickup;
        private final Button process;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            name = (TextView) view.findViewById(R.id.order_row_name_value);
            phone = (TextView) view.findViewById(R.id.order_row_phone_value);
//            qty = (TextView) view.findViewById(R.id.order_quantity_value);
            amount = (TextView) view.findViewById(R.id.order_amount_value);
            invoice = (TextView) view.findViewById(R.id.card_invoice_value);
            pickup = view.findViewById(R.id.order_pickup_icon);
            process = view.findViewById(R.id.card_btn_process);
        }


    }


    @NonNull
    @Override
    public OrderAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.order_row, parent, false);
//        Log.e("TAG", "onCreateViewHolder: size = "+getItemCount(),new Error() );
//        listItem.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent (parent.getContext(), OrderDetails.class);
//                view.getContext().startActivity(intent);
//            }
//        });
        return new OrderAdapter.ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {

//        holder.name.setText(items.get(position).name);
//        holder.phone.setText(items.get(position).phone);
//        holder.qty.setText(items.get(position).quantity);
//        holder.amount.setText(items.get(position).amount);
//        holder.invoice.setText(items.get(position).invoice);

        holder.name.setText(orders.get(position).orderShipmentDetail.receiverName);
        holder.phone.setText(orders.get(position).orderShipmentDetail.phoneNumber);
//        holder.qty.setText("3");
        holder.amount.setText(Double.toString(orders.get(position).total));
        holder.invoice.setText(orders.get(position).invoiceId);

        if(!orders.get(position).orderShipmentDetail.storePickup)
            holder.pickup.setBackgroundResource(R.drawable.ic_highlight_off_black_24dp);
        else
            holder.pickup.setBackgroundResource(R.drawable.ic_check_circle_black_24dp);



        holder.process.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent (holder.itemView.getContext(), OrderDetails.class);
                intent.putExtra("selectedOrder",orders.get(position));
                view.getContext().startActivity(intent);
            }
        });

//        holder.itemView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent (holder.itemView.getContext(), OrderDetails.class);
//                intent.putExtra("selectedOrder",orders.get(position));
//                view.getContext().startActivity(intent);
//            }
//        });

    }


    @Override
    public int getItemCount() {
        return orders.size();
    }
}

