package com.symplified.order.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.symplified.order.App;
import com.symplified.order.Login;
import com.symplified.order.OrderDetails;
import com.symplified.order.R;
import com.symplified.order.handlers.LogoHandler;
import com.symplified.order.models.OrderDetailsModel;
import com.symplified.order.models.order.Order;
import com.symplified.order.utils.ImageUtil;

import java.util.Arrays;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
//    public List<OrderDetailsModel> items;

    public List<Order> orders;
    public String section;
    public boolean isPickup;
    public Context context;

    public OrderAdapter(List<Order> orders, String section, Context context){
//        List<OrderDetailsModel> items,
//        this.items = items;
        this.orders = orders;
        this.section = section;
        this.context = context;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name,phone, amount,invoice;
        private final ImageView pickup;
        private final Button process;
        private final ImageView storeLogo;
        private final TextView storeLogoText;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            name = (TextView) view.findViewById(R.id.order_row_name_value);
            phone = (TextView) view.findViewById(R.id.order_row_phone_value);
            amount = (TextView) view.findViewById(R.id.order_amount_value);
            invoice = (TextView) view.findViewById(R.id.card_invoice_value);
            storeLogo = (ImageView) view.findViewById(R.id.storeLogoOrder);
            pickup = view.findViewById(R.id.order_pickup_icon);
            process = view.findViewById(R.id.card_btn_process);
            storeLogoText = (TextView) view.findViewById(R.id.storeLogoOrderText);
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

        SharedPreferences sharedPreferences = context.getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        String storeIdList = sharedPreferences.getString("storeIdList", null);

            String encodedStoreLogo = sharedPreferences.getString("logoImage-"+orders.get(holder.getAdapterPosition()).storeId, null);

            if(storeIdList.split(" ").length-1 > 1)
            {

                if(sharedPreferences.contains("logoImage-"+orders.get(holder.getAdapterPosition()).storeId)
                        && encodedStoreLogo != null)
                {
                    ImageUtil.decodeAndSetImage(holder.storeLogo, encodedStoreLogo);
                }
                else{
                    holder.storeLogo.setVisibility(View.GONE);
                    holder.storeLogoText.setVisibility(View.VISIBLE);
                    String storeName = sharedPreferences.getString(orders.get(holder.getAdapterPosition()).storeId+"-name", null);
                    holder.storeLogoText.setText(storeName);
                }

            }

        Log.e("TAG", "onBindViewHolder: "+ sharedPreferences.getString(orders.get(holder.getAdapterPosition()).storeId+"-name", null), new Error());


        holder.name.setText(orders.get(position).orderShipmentDetail.receiverName);
        holder.phone.setText(orders.get(position).orderShipmentDetail.phoneNumber);
        holder.amount.setText(Double.toString(orders.get(position).total));
        holder.invoice.setText(orders.get(position).invoiceId);


        if(!orders.get(position).orderShipmentDetail.storePickup) {
            holder.pickup.setBackgroundResource(R.drawable.ic_highlight_off_black_24dp);
            isPickup = false;
        }
        else {
            holder.pickup.setBackgroundResource(R.drawable.ic_check_circle_black_24dp);
            isPickup = true;
        }

        holder.process.setText("Details");

        holder.process.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent (holder.itemView.getContext(), OrderDetails.class);
                intent.putExtra("selectedOrder",orders.get(position));
                intent.putExtra("section", section);
                intent.putExtra("pickup", isPickup);
                ((Activity) context).startActivityForResult(intent, 4);
//                ((Activity)view.getContext()).finish();
            }
        });

    }


    @Override
    public int getItemCount() {
        return orders.size();
    }
}

