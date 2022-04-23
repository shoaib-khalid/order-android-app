package com.symplified.order.adapters;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.symplified.order.App;
import com.symplified.order.OrderDetailsActivity;
import com.symplified.order.R;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.item.ItemResponse;
import com.symplified.order.models.item.UpdatedItem;
import com.symplified.order.models.order.Order;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ViewHolder> {
    public List<Item> items;
    public boolean editable;
    public Order order;
    public List<UpdatedItem> updatedItemsList;
    public SharedPreferences sharedPreferences;
    public Context context;
    private static String TAG = ItemsAdapter.class.getName();
    public void setItems(List<Item> items) {
        this.items = items;
    }

    public ItemsAdapter (){}
    public ItemsAdapter(List<Item> items, boolean editable, Order order){
        this.items = items;
        this.editable = editable;
        this.order = order;
        this.updatedItemsList = new ArrayList<>();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView item;
//        private final TextView instructions;
        private final TextView instructionsValue;
        private final TextView variant;
        private final TextView qty, orignalQty;
        private final Spinner spinner;
        private final TextView price;
        private final RelativeLayout expandableInstructions;
        private final RecyclerView subItemsRecyclerView;
        private final RelativeLayout subItemLayout;
//        private final ConstraintLayout constraintLayout;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            item = (TextView) view.findViewById(R.id.header_item);
//            instructions = (TextView) view.findViewById(R.id.header_instruction);
            qty = (TextView) view.findViewById(R.id.header_qty);
            orignalQty = view.findViewById(R.id.header_org_qty);
            price = (TextView) view.findViewById(R.id.header_price);
            spinner = view.findViewById(R.id.header_qty_editor);
//            constraintLayout = (ConstraintLayout)  view.findViewById(R.id.order_item) ;
            expandableInstructions = view.findViewById(R.id.exanded_instructions);
            instructionsValue = view.findViewById(R.id.header_instruction_value);
            variant = view.findViewById(R.id.header_variant);
            subItemsRecyclerView = (RecyclerView) view.findViewById(R.id.subItemRecyclerView);
            subItemsRecyclerView.setLayoutManager(new LinearLayoutManager(subItemsRecyclerView.getContext(), RecyclerView.HORIZONTAL, false));
            subItemLayout = view.findViewById(R.id.subItems);


            item.setTypeface(Typeface.DEFAULT);
//            instructions.setTypeface(Typeface.DEFAULT);
            qty.setTypeface(Typeface.DEFAULT);
            price.setTypeface(Typeface.DEFAULT);
            variant.setTypeface(Typeface.DEFAULT);
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

//        sharedPreferences = holder.item.getContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
//        context = holder.item.getContext();

        holder.item.setText(items.get(position).productName);


        if(items.get(position).orderSubItem != null && items.get(position).orderSubItem.size() > 0){
            SubItemsAdapter adapter = new SubItemsAdapter();
            adapter.items = items.get(position).orderSubItem;

            holder.subItemsRecyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            holder.subItemLayout.setVisibility(View.VISIBLE);
        }else {
            holder.subItemLayout.setVisibility(View.GONE);
        }

        if(items.get(position).specialInstruction == null || items.get(position).specialInstruction.equals("")){
            holder.expandableInstructions.setVisibility(View.GONE);
        }else {
            holder.expandableInstructions.setVisibility(View.VISIBLE);
            holder.instructionsValue.setText(items.get(position).specialInstruction);
        }
        if(editable){
            holder.qty.setVisibility(View.GONE);
            updatedItemsList = new ArrayList<>();

            Integer[] qtyValues = new Integer[items.get(position).quantity+1];
            for(int i = 0; i <= items.get(position).quantity; i++)
                qtyValues[i] = i;
            ArrayAdapter<Integer> arrayAdapter = new ArrayAdapter<>(
                    holder.expandableInstructions.getContext(),
                    android.R.layout.simple_spinner_item,
                    qtyValues
            );

            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            holder.spinner.setAdapter(arrayAdapter);
            holder.spinner.setSelection(items.get(position).quantity);

            UpdatedItem updatedItem = new UpdatedItem();

            holder.spinner.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                            UpdatedItem updatedItem = new UpdatedItem(
//                                    items.get(holder.getAdapterPosition()).id,
//                                    items.get(holder.getAdapterPosition()).itemCode,
//                                    (Integer) adapterView.getSelectedItem()
//                            );
                            updatedItem.id = items.get(holder.getAdapterPosition()).id;
                            updatedItem.itemCode = items.get(holder.getAdapterPosition()).itemCode;
                            updatedItem.quantity = (Integer) adapterView.getSelectedItem();

                            if(!updatedItemsList.contains(updatedItem) && (Integer) adapterView.getSelectedItem() != items.get(holder.getAdapterPosition()).quantity){
                                updatedItemsList.add(updatedItem);
//                                Toast.makeText(view.getContext(), "updated Item length : " + updatedItemsList.size(), Toast.LENGTH_SHORT).show();
                            }
                            Log.i("TAG", "onItemSelected: " + updatedItemsList);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    }
            );

//                updatedItemsList.add(
//                        new UpdatedItem(
//                                items.get(holder.getAdapterPosition()).id,
//                                items.get(holder.getAdapterPosition()).itemCode,
//                                (Integer)  holder.spinner.getSelectedItem()
//                        )
//                );


            holder.spinner.setVisibility(View.VISIBLE);
            holder.orignalQty.setVisibility(View.VISIBLE);
            holder.orignalQty.setText(Integer.toString(items.get(position).quantity));
        }else{
            holder.spinner.setVisibility(View.GONE);
            holder.qty.setVisibility(View.VISIBLE);
            holder.qty.setText(Integer.toString(items.get(position).quantity));
        }
        holder.price.setText(Double.toString(items.get(position).price));
        if(items.get(position).productVariant != null){
            holder.variant.setText(items.get(position).productVariant);
        }
        if(items.get(position).originalQuantity == null)
            items.get(position).originalQuantity = items.get(position).quantity;

//        Toast.makeText(context, "originalQTY = "+Integer.toString(items.get(position).originalQuantity), Toast.LENGTH_SHORT).show();
        if(
                order.isRevised
//                items.get(position).originalQuantity != 0
        ){
            holder.orignalQty.setVisibility(View.VISIBLE);
            holder.orignalQty.setText(Integer.toString(items.get(position).originalQuantity));
        }



    }

    public void updateOrderItems(Order order, String BASE_URL, Dialog progressDialog){
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

//        String BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL+ App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Log.i("updatedItemListTAG", "updateOrderItems: " + updatedItemsList);

        if(updatedItemsList.size() == 0){
            Toast.makeText(context, "No changes to update", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<ResponseBody> updateItemsCall = orderApiService.reviseOrderItem(headers, order.id, updatedItemsList);
        progressDialog.show();

        updateItemsCall.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.i("updatedItemListTAG", "onResponse: " + call.request().toString());
//                Toast.makeText(context, "response received", Toast.LENGTH_SHORT).show();
                if(response.isSuccessful()){
                    progressDialog.dismiss();
//                    Toast.makeText(context, "updated items", Toast.LENGTH_SHORT).show();
                    try {
                        Log.i("TAG123", "onResponse: " + order.id);
                        Log.i("TAG", "onResponse: " + response.body().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    getOrderItems(order, BASE_URL, progressDialog);
                }
                else {
                    progressDialog.dismiss();
                    Log.i("TAG", "onResponse: " + response.raw());
                    Log.e(TAG, "onResponse: " + response.message() + " " );
                    Toast.makeText(context, "Failed to update order", Toast.LENGTH_SHORT).show();
                    ((Activity) context).finish();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(context, "Failed to update Order !", Toast.LENGTH_SHORT).show();
                Log.e("TAG", "onFailure: ", t);
            }
        });

//        ItemsAdapter itemsAdapter = new ItemsAdapter();
//        progressDialog.show();
    }

    private boolean containsItem(List<UpdatedItem> list, UpdatedItem item){
        for(UpdatedItem e : list){
            if(
                    e.id.equals(item.id)
                    && e.itemCode.equals(item.itemCode)
            ){
                if(!e.quantity.equals(item.quantity)){
                    e.quantity = item.quantity;
                }
                return true;
            }
        }
        return false;
    }


    private void getOrderItems(Order order, String BASE_URL, Dialog progressDialog){
        //add headers required for api calls
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL+App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Call<ItemResponse> itemResponseCall = orderApiService.getItemsForOrder(headers, order.id);

//        ItemsAdapter itemsAdapter = new ItemsAdapter();
//        itemsAdapter.editable = false;
//        progressDialog.show();
//        itemResponseCall.clone().enqueue(new Callback<ItemResponse>() {
//            @Override
//            public void onResponse(Call<ItemResponse> call, Response<ItemResponse> response) {
//
//                if(response.isSuccessful())
//                {
//                    Log.i("TAG", "onResponse: "+order.id, new Error() );
//                    items = response.body().data.content;
//                    notifyDataSetChanged();
//                    editable = false;
//                    progressDialog.dismiss();
//                }
//
//
//            }
//
//            @Override
//            public void onFailure(Call<ItemResponse> call, Throwable t) {
//                Toast.makeText(context, "Failed to retrieve items", Toast.LENGTH_SHORT).show();
//                Log.e("TAG", "onFailureItems: ", t);
//                progressDialog.dismiss();
//            }
//        });

       Call<Order.OrderByIdResponse> getOrderByIdCall = orderApiService.getOrderById(headers, order.id);
        progressDialog.show();
        Log.e(TAG, "getOrderItems: " + getOrderByIdCall.request() );
       getOrderByIdCall.clone().enqueue(new Callback<Order.OrderByIdResponse>() {
           @Override
           public void onResponse(Call<Order.OrderByIdResponse> call, Response<Order.OrderByIdResponse> response) {
               Log.i(TAG, "onResponse: " + response.raw());
               progressDialog.dismiss();
               if(response.isSuccessful()){
                   Order orderResponse = response.body().data;
                   Log.e(TAG, "onResponse: updated order : " +  orderResponse);
                   editable = false;
                   notifyDataSetChanged();
                   new MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog__Center)
                           .setTitle("Order Updated")
                           .setMessage("The refund will be in place.\nRefund Amount : "+ orderResponse.orderRefund.get(0).refundAmount)
                           .setPositiveButton("OK", ((dialogInterface, i) -> {
                               dialogInterface.dismiss();
                               Toast.makeText(context, "Order Updated !", Toast.LENGTH_SHORT).show();
                               Intent refreshIntent = ((Activity) context).getIntent();
                               Bundle data = refreshIntent.getExtras();
                               data.putSerializable("selectedOrder", orderResponse);
                               refreshIntent.putExtras(data);
                               ((Activity) context).finish();
                               ((Activity) context).overridePendingTransition(0, 0);
                               context.startActivity(refreshIntent);
                               ((Activity) context).overridePendingTransition(0, 0);
                           })).show();
               }
           }

           @Override
           public void onFailure(Call<Order.OrderByIdResponse> call, Throwable t) {
               progressDialog.dismiss();
               Log.e(TAG, "onFailure: ", t);
           }
       });
    }



    @Override
    public int getItemCount() {
        return items.size();
    }
}
