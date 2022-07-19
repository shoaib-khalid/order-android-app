package com.symplified.order.adapters;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.Gson;
import com.symplified.order.App;
import com.symplified.order.EditOrderActivity;
import com.symplified.order.OrderDetailsActivity;
import com.symplified.order.R;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.enums.Status;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.item.ItemResponse;
import com.symplified.order.models.order.Order;
import com.symplified.order.utils.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    public List<Order> orders;
    public String section;
    public Context context;
    public final String TAG = OrderAdapter.class.getName();
    public Dialog progressDialog;
    public String nextStatus;

    public OrderAdapter(List<Order> orders, String section, Context context) {
        this.orders = orders;
        this.section = section;
        this.context = context;

        progressDialog = new Dialog((Activity) context);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name, invoice, date, total, status;
        private final MaterialButton editButton, detailsButton, cancelButton, acceptButton, statusButton, trackButton, dispatchedButton;
        private final CardView cardView;
        private final TextView invoiceLabel, dateLabel, totalLabel, statusLabel, typeLabel, type, currStatusLabel, currStatus;
        private final RecyclerView recyclerView;
        private final LinearLayout rightButtonsLayout, leftButtonsLayout, statusLayout, typeLayout, currStatusLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.order_row_name_value);
            invoice = (TextView) itemView.findViewById(R.id.card_invoice_value);
            date = (TextView) itemView.findViewById(R.id.order_date_value);
            total = (TextView) itemView.findViewById(R.id.order_total_value);
            status = (TextView) itemView.findViewById(R.id.order_status_value);
            typeLabel = (TextView) itemView.findViewById(R.id.order_type);
            type = (TextView) itemView.findViewById(R.id.order_type_value);
            currStatusLabel = (TextView) itemView.findViewById(R.id.order_curr_status);
            currStatus = (TextView) itemView.findViewById(R.id.order_curr_status_value);
            recyclerView = itemView.findViewById(R.id.order_items_recycler);

            editButton = itemView.findViewById(R.id.btn_edit_order);
            detailsButton = itemView.findViewById(R.id.btn_order_details);
            cancelButton = itemView.findViewById(R.id.btn_order_cancel);
            acceptButton = itemView.findViewById(R.id.btn_order_accept);
            statusButton = itemView.findViewById(R.id.btn_order_status);
            trackButton = itemView.findViewById(R.id.btn_track_order);
            dispatchedButton = itemView.findViewById(R.id.btn_order_status_dispatched);

            cardView = itemView.findViewById(R.id.order_card_parent);

            invoiceLabel = itemView.findViewById(R.id.order_invoice);
            dateLabel = itemView.findViewById(R.id.order_date);
            totalLabel = itemView.findViewById(R.id.order_total);
            statusLabel = itemView.findViewById(R.id.order_status);

            rightButtonsLayout = itemView.findViewById(R.id.layout_buttons_right);
            leftButtonsLayout = itemView.findViewById(R.id.layout_buttons_left);
            statusLayout = itemView.findViewById(R.id.layout_order_status);
            typeLayout = itemView.findViewById(R.id.layout_order_type_row);
            currStatusLayout = itemView.findViewById(R.id.layout_order_curr_status);

        }
    }

    @NonNull
    @Override
    public OrderAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.order_row, parent, false);
        return new OrderAdapter.ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderAdapter.ViewHolder holder, int position) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        String storeIdList = sharedPreferences.getString("storeIdList", null);
        String BASE_URL = sharedPreferences.getString("base_url", null);
        String currency = sharedPreferences.getString("currency", null);
        nextStatus = "";

        holder.name.setText(orders.get(position).orderShipmentDetail.receiverName);
        holder.invoice.setText(orders.get(position).invoiceId);
        holder.total.setText(currency + " " + Double.toString(orders.get(position).total));
        holder.date.setText(orders.get(position).created);

        String orderStatus = orders.get(holder.getAdapterPosition()).completionStatus;
        if (section.equals("new")) {
            holder.editButton.setVisibility(View.VISIBLE);
            holder.rightButtonsLayout.setVisibility(View.VISIBLE);
            if (orders.get(position).orderShipmentDetail.storePickup) {
                holder.type.setText("Self-Pickup");
            } else {
                holder.type.setText("Delivery");
            }
            holder.currStatusLayout.setVisibility(View.GONE);

        } else if (section.equals("ongoing")) {
            holder.statusLayout.setVisibility(View.VISIBLE);
            holder.statusLabel.setVisibility(View.VISIBLE);
            holder.statusLabel.setText("Change Status: ");
            holder.statusButton.setVisibility(View.VISIBLE);
            if (orders.get(position).orderShipmentDetail.storePickup) {
                holder.type.setText("Self-Pickup");
            } else {
                holder.type.setText("Delivery");
            }
            switch (orderStatus) {
                case "BEING_PREPARED":
                    holder.currStatus.setText("Preparing");
                    holder.statusButton.setText("Rready for pickup");
                    break;
                case "AWAITING_PICKUP":
                    holder.currStatus.setText("Ready for Pickup");
                    holder.statusButton.setText("Dispatched");
                    break;
                case "BEING_DELIVERED":
                    holder.currStatus.setText("Order Dispatched");
                    holder.trackButton.setVisibility(View.VISIBLE);
                    holder.statusButton.setText("Delivered");
                    break;
            }
        } else if (section.equals("past")) {
            holder.typeLayout.setVisibility(View.GONE);
            holder.statusLayout.setVisibility(View.VISIBLE);
            holder.status.setVisibility(View.VISIBLE);
            holder.statusLabel.setVisibility(View.VISIBLE);
            if (orderStatus.equals("DELIVERED_TO_CUSTOMER")) {
                holder.status.setText("Delivered");
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.sf_accept_button));
            } else if (orderStatus.equals("CANCELED_BY_MERCHANT") || orderStatus.equals("CANCELED_BY_CUSTOMER")) {
                holder.status.setText("Cancelled");
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.sf_cancel_button));

            }

        }

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);

        holder.recyclerView.setLayoutManager(linearLayoutManager);

        getOrderItems(orders.get(position), BASE_URL, holder, position);

        holder.cancelButton.setOnClickListener(view -> {onCancelOrderButtonClick(orders.get(position), BASE_URL, holder);});

        holder.acceptButton.setOnClickListener(view -> {
            getOrderStatusDetails(orders.get(position), BASE_URL, holder);
        });

        holder.statusButton.setOnClickListener(view -> {
            getOrderStatusDetails(orders.get(position), BASE_URL, holder);
        });

        holder.editButton.setOnClickListener(view -> {
            onEditButtonClicked(orders.get(position));
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    private void getOrderItems(Order order, String BASE_URL, ViewHolder holder, int position) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Call<ItemResponse> itemResponseCall = orderApiService.getItemsForOrder(headers, order.id);

        progressDialog.show();

        itemResponseCall.clone().enqueue(new Callback<ItemResponse>() {
            @Override
            public void onResponse(Call<ItemResponse> call, Response<ItemResponse> response) {
                if (response.isSuccessful()) {
                    ItemAdapter itemsAdapter = new ItemAdapter(response.body().data.content, orders.get(position), context);
                    holder.recyclerView.setAdapter(itemsAdapter);
                    itemsAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(context, R.string.request_failure, Toast.LENGTH_SHORT).show();
                }
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<ItemResponse> call, Throwable t) {
                Toast.makeText(context, "Failed to retrieve items. " + R.string.no_internet, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onFailureItems: ", t);
                progressDialog.dismiss();
            }
        });
    }

    public void onCancelOrderButtonClick(Order order, String BASE_URL, ViewHolder holder) {
        //add headers required for api calls
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Dialog dialog = new MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog__Center)
                .setTitle("Cancel Order")
                .setMessage("Do you really want to cancel this order ?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    progressDialog.show();
                    Call<ResponseBody> processOrder = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.CANCELED_BY_MERCHANT), order.id);
                    progressDialog.show();
                    processOrder.clone().enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            progressDialog.dismiss();
                            if (response.isSuccessful()) {
                                Toast.makeText(context, "Order Cancelled", Toast.LENGTH_SHORT).show();
                                orders.remove(holder.getAdapterPosition());
                                notifyDataSetChanged();
                                progressDialog.dismiss();

                            } else {
                                Toast.makeText(context, R.string.request_failure, Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(context, R.string.no_internet, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "onFailure: ", t);
                            progressDialog.dismiss();
                        }
                    });
                })
                .create();
        TextView title = dialog.findViewById(android.R.id.title);
        TextView message = dialog.findViewById(android.R.id.message);
        if (title != null && message != null) {
            title.setTypeface(Typeface.DEFAULT_BOLD);
            message.setTextSize(14);
            message.setTypeface(Typeface.DEFAULT_BOLD);
        }
        dialog.show();
    }

    private void getOrderStatusDetails(Order order, String BASE_URL, ViewHolder holder) {

        //add headers required for api calls
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Call<ResponseBody> orderStatusDetailsResponseCall = orderApiService.getOrderStatusDetails(headers, order.id);
        progressDialog.show();
        orderStatusDetailsResponseCall.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        JSONObject responseJson = new JSONObject(response.body().string().toString());
                        Log.e(TAG, "onResponse: " + responseJson, new Error());
                        new Handler().post(() -> {
                            try {
                                if (!section.equals("sent")) {
                                    nextStatus += responseJson.getJSONObject("data").getString("nextCompletionStatus");
                                    //get order items from API
                                    updateOrderStatus(order, BASE_URL, holder);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(context, R.string.request_failure, Toast.LENGTH_SHORT).show();
                }
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(context, R.string.no_internet, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onFailure: ", t);
            }
        });

    }

    private void updateOrderStatus(Order order, String BASE_URL, ViewHolder holder) {

        //add headers required for api calls
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Call<ResponseBody> processOrder = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.fromString(nextStatus)), order.id);

        onProcessButtonClick(processOrder, holder.getAdapterPosition());

    }

    private void onProcessButtonClick(Call<ResponseBody> processOrder, int position) {
        progressDialog.show();
        processOrder.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
//                    progressDialog.dismiss();
                    try {
                        Log.i(TAG, "onResponse: " + response.raw().toString());
                        Order.UpdatedOrder currentOrder = new Gson().fromJson(response.body().string(), Order.UpdatedOrder.class);
                        Toast.makeText(context, "Status Updated", Toast.LENGTH_SHORT).show();
                        orders.remove(position);
                        notifyDataSetChanged();
                    } catch (IOException e) {
                        progressDialog.dismiss();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(context, R.string.request_failure, Toast.LENGTH_SHORT).show();
                }
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(context, R.string.no_internet, Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                Log.e(TAG, "onFailure: ", t);
            }
        });

    }

   private void onEditButtonClicked(Order order) {
        if (order.isRevised) {
            Toast.makeText(context, "Order already edited", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(context, EditOrderActivity.class);
        intent.putExtra("order", order);
        context.startActivity(intent);
    }

}
