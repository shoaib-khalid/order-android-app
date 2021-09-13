package com.symplified.order.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.symplified.order.App;
import com.symplified.order.Orders;
import com.symplified.order.R;
import com.symplified.order.firebase.FirebaseHelper;
import com.symplified.order.models.Store.Store;

import java.util.List;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.ViewHolder> {
    public List<Store> items;

    public StoreAdapter(List<Store> items){
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            name = (TextView) view.findViewById(R.id.store_name);
        }


    }


    @NonNull
    @Override
    public StoreAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.store_list_item, parent, false);
//        Log.e("TAG", "onCreateViewHolder: size = "+getItemCount(),new Error() );
//        listItem.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//            }
//        });
        return new StoreAdapter.ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.name.setText(items.get(position).name);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPreferences = holder.itemView.getContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
                sharedPreferences.edit().putString("storeId", items.get(holder.getAdapterPosition()).id).apply();
                FirebaseHelper.initializeFirebase(items.get(holder.getAdapterPosition()).id,view.getContext());
                Intent intent = new Intent (holder.itemView.getContext(), Orders.class);
                Log.e("TAG", "preferences: "+sharedPreferences.getAll(),new Error() );
                Toast.makeText(view.getContext(), "Store id : "+ (items.get(holder.getAdapterPosition()).id), Toast.LENGTH_SHORT).show();
                view.getContext().startActivity(intent);
                ((Activity) holder.itemView.getContext()).finish();
            }
        });
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    public boolean isEmpty(){return items.isEmpty();}
}