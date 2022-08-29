package com.symplified.order.adapters;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.Gson;
import com.symplified.order.App;
import com.symplified.order.EditOrderActivity;
import com.symplified.order.R;
import com.symplified.order.TrackOrderActivity;
import com.symplified.order.apis.DeliveryApi;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.enums.Status;
import com.symplified.order.models.item.ItemResponse;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderDeliveryDetailsResponse;
import com.symplified.order.networking.ServiceGenerator;

import java.io.IOException;
import java.text.DecimalFormat;
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

    public List<Order.OrderDetailsResponse> orders;
    public String section;
    public Context context;
    public final String TAG = OrderAdapter.class.getName();
    public Dialog progressDialog, dialog;
    public String nextStatus;
    public DecimalFormat formatter;

    private OrderApi orderApiService;
    private DeliveryApi deliveryApiService;

    public OrderAdapter(List<Order.OrderDetailsResponse> orders, String section, Context context) {
        this.orders = orders;
        this.section = section;
        this.context = context;

        progressDialog = new Dialog((Activity) context);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);

        orderApiService = ServiceGenerator.createOrderService();
        deliveryApiService = ServiceGenerator.createDeliveryService();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name, invoice, date, total, total2, status, phoneNumber, address, subTotal,
                deliveryCharges, deliveryDiscount, discount, serviceCharges;
        private final MaterialButton editButton, cancelButton, acceptButton, statusButton, trackButton,  callButton;
        private final CardView cardView;
        private final TextView invoiceLabel, dateLabel, totalLabel, statusLabel, typeLabel, type, currStatusLabel, currStatus, customerNotes, riderName, riderContact;
        private final LinearLayout newLayout, ongoingLayout;
        private final RelativeLayout currStatusLayout, typeLayout, rlDiscount, rlServiceCharges, rlDeliveryDiscount, rlCustomerNote, rlRiderDetails;
        private final View divider3, divider7, divider8, divider9;
        private final ImageView riderCallIcon;

        private final RecyclerView recyclerView;
        private final ProgressBar itemsProgressBar;
        private final TextView itemsErrorTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.order_row_name_value);
            invoice = (TextView) itemView.findViewById(R.id.card_invoice_value);
            date = (TextView) itemView.findViewById(R.id.order_date_value);
            total = (TextView) itemView.findViewById(R.id.order_total_value);
            status = (TextView) itemView.findViewById(R.id.order_status_value);
            typeLabel = (TextView) itemView.findViewById(R.id.order_type);
            type = (TextView) itemView.findViewById(R.id.order_type_value);
            customerNotes = itemView.findViewById(R.id.customer_note_value);

            phoneNumber = itemView.findViewById(R.id.contact_value);
            address = itemView.findViewById(R.id.address_value);
            subTotal = itemView.findViewById(R.id.subtotal_value);
            deliveryCharges = itemView.findViewById(R.id.delivery_by_value);
            discount = itemView.findViewById(R.id.discount_value);
            deliveryDiscount = itemView.findViewById(R.id.billing_delivery_charges_discount_value);
            serviceCharges = itemView.findViewById(R.id.service_charges_value);
            total2 = itemView.findViewById(R.id.order_total_value_);
            callButton = itemView.findViewById(R.id.btn_call);
            rlDiscount = itemView.findViewById(R.id.rl_discount);
            rlDeliveryDiscount = itemView.findViewById(R.id.rl_delivery_discount);
            rlServiceCharges = itemView.findViewById(R.id.rl_service_charges);

            riderName = itemView.findViewById(R.id.driver_value);
            riderContact = itemView.findViewById(R.id.driver_contact_value);

            riderCallIcon = itemView.findViewById(R.id.address_icon_phone);

            currStatusLabel = (TextView) itemView.findViewById(R.id.order_curr_status);
            currStatus = (TextView) itemView.findViewById(R.id.order_curr_status_value);
            recyclerView = itemView.findViewById(R.id.order_items_recycler);

            editButton = itemView.findViewById(R.id.btn_edit_order);
            cancelButton = itemView.findViewById(R.id.btn_order_cancel);
            acceptButton = itemView.findViewById(R.id.btn_order_accept);
            statusButton = itemView.findViewById(R.id.btn_order_status);
            trackButton = itemView.findViewById(R.id.btn_track_order);

            cardView = itemView.findViewById(R.id.order_card_parent);

            invoiceLabel = itemView.findViewById(R.id.order_invoice);
            dateLabel = itemView.findViewById(R.id.order_date);
            totalLabel = itemView.findViewById(R.id.order_total);
            statusLabel = itemView.findViewById(R.id.update_status);

            typeLayout = itemView.findViewById(R.id.layout_order_type_row);
            currStatusLayout = itemView.findViewById(R.id.layout_order_status_row);
            newLayout = itemView.findViewById(R.id.layout_new);
            ongoingLayout = itemView.findViewById(R.id.layout_ongoing);
            rlCustomerNote = itemView.findViewById(R.id.rl_customer_note);
            rlRiderDetails = itemView.findViewById(R.id.rl_rider_details);

            divider3 = itemView.findViewById(R.id.divider_card3);
            divider7 = itemView.findViewById(R.id.divider_card7);
            divider8 = itemView.findViewById(R.id.divider_card8);
            divider9 = itemView.findViewById(R.id.divider_card9);

            itemsProgressBar = itemView.findViewById(R.id.order_items_progress_bar);
            itemsErrorTextView = itemView.findViewById(R.id.order_items_fail_textview);

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

        Order.OrderDetailsResponse orderDetails = orders.get(position);
        Order order = orderDetails.order;

        formatter = new DecimalFormat("#,###0.00");

        SharedPreferences sharedPreferences = context.getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        String storeIdList = sharedPreferences.getString("storeIdList", null);
        String BASE_URL = sharedPreferences.getString("base_url", null);
        String currency = sharedPreferences.getString("currency", null);

        holder.name.setText(order.orderShipmentDetail.receiverName);
        holder.invoice.setText(order.invoiceId);
        holder.total.setText(currency + " " + formatter.format(order.total));

        holder.phoneNumber.setText(order.orderShipmentDetail.phoneNumber);
        String fullAddress = order.orderShipmentDetail.address+", "+order.orderShipmentDetail.city+", "+order.orderShipmentDetail.state+" "+order.orderShipmentDetail.zipcode;
        holder.address.setText(fullAddress);
        holder.subTotal.setText(currency + " " + formatter.format(order.subTotal));
        if (order.appliedDiscount == null || order.appliedDiscount == 0.00) {
            holder.rlDiscount.setVisibility(View.GONE);
        } else {
            holder.discount.setText("- " + currency + " " + formatter.format(order.appliedDiscount));
        }
        holder.deliveryCharges.setText(order.deliveryCharges != null ? currency + " " + formatter.format(order.deliveryCharges) : currency + " " + "0.00");
        holder.total2.setText(currency + " " + formatter.format(order.total));
        if (order.deliveryDiscount == null || order.deliveryDiscount == 0.00) {
            holder.rlDeliveryDiscount.setVisibility(View.GONE);
        } else {
            holder.deliveryDiscount.setText("- " + currency + " " + formatter.format(order.deliveryDiscount));
        }
        if (order.storeServiceCharges == null || order.storeServiceCharges == 0.00) {
            holder.rlServiceCharges.setVisibility(View.GONE);
        } else {
            holder.serviceCharges.setText(currency + " " + formatter.format(order.storeServiceCharges));
        }
        holder.callButton.setOnClickListener(view -> {
            Intent callDriver = new Intent(Intent.ACTION_DIAL);
            callDriver.setData(Uri.parse("tel:" + order.orderShipmentDetail.phoneNumber));
            context.startActivity(callDriver);
        });

        holder.date.setText(order.created);

        if (order.customerNotes != null && !order.customerNotes.equals("")) {
            holder.rlCustomerNote.setVisibility(View.VISIBLE);
            holder.divider3.setVisibility(View.VISIBLE);
            holder.customerNotes.setText(order.customerNotes);
        }

        String orderStatus = order.completionStatus;
        if (section.equals("new")) {
            if (order.isRevised) {
                holder.editButton.setTextColor(context.getResources().getColor(R.color.dark_grey));
                holder.editButton.setStrokeColor(ColorStateList.valueOf(context.getResources().getColor(R.color.dark_grey)));
            }
            holder.editButton.setVisibility(View.VISIBLE);
            holder.newLayout.setVisibility(View.VISIBLE);
            holder.acceptButton.setText(orderDetails.nextActionText);
            if (order.orderShipmentDetail.storePickup) {
                holder.type.setText("Self-Pickup");
            } else {
                holder.type.setText("Delivery");
            }
            holder.currStatusLayout.setVisibility(View.GONE);
            holder.divider8.setVisibility(View.GONE);

        } else if (section.equals("ongoing")) {
            if (orderDetails.nextActionText != null) {
                holder.ongoingLayout.setVisibility(View.VISIBLE);
                holder.statusLabel.setVisibility(View.VISIBLE);
                holder.statusLabel.setText("Update Status: ");
                holder.statusButton.setText(orderDetails.nextActionText);
                holder.statusButton.setVisibility(View.VISIBLE);
            }

            if (order.orderShipmentDetail.storePickup) {
                holder.type.setText("Self-Pickup");
            } else {
                holder.type.setText("Delivery");
            }

            switch (orderStatus) {
                case "BEING_PREPARED":
                    holder.currStatus.setText("Preparing");
                    holder.trackButton.setVisibility(View.GONE);
                    break;
                case "AWAITING_PICKUP":
                    holder.currStatus.setText("Awaiting Pickup");
                    if (!order.orderShipmentDetail.storePickup && order.orderShipmentDetail.deliveryPeriodDetails != null)
                        holder.trackButton.setVisibility(View.VISIBLE);
                    break;
                case "BEING_DELIVERED":
                    holder.currStatus.setText("Out for Delivery");
                    if (!order.orderShipmentDetail.storePickup && order.orderShipmentDetail.deliveryPeriodDetails != null)
//                    if (order.orderShipmentDetail.trackingUrl != null)
                        holder.trackButton.setVisibility(View.VISIBLE);
                    break;
            }
        } else if (section.equals("past")) {
            holder.ongoingLayout.setVisibility(View.VISIBLE);
            holder.typeLayout.setVisibility(View.GONE);
            holder.currStatusLayout.setVisibility(View.GONE);
            holder.status.setVisibility(View.VISIBLE);
            holder.statusLabel.setVisibility(View.VISIBLE);
            if (orderStatus.equals("DELIVERED_TO_CUSTOMER")) {
                holder.status.setText("Order Delivered");
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.sf_accept_button));
            } else if (orderStatus.equals("CANCELED_BY_MERCHANT") || orderStatus.equals("CANCELED_BY_CUSTOMER")) {
                holder.status.setText("Order Cancelled");
                holder.status.setTextColor(ContextCompat.getColor(context, R.color.sf_cancel_button));
            }
            holder.divider7.setVisibility(View.GONE);
            holder.divider8.setVisibility(View.GONE);

            if (!order.orderShipmentDetail.storePickup && order.orderShipmentDetail.deliveryPeriodDetails != null) {
                getRiderDetails(holder, order, 2);
            }
        }

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);

        holder.recyclerView.setLayoutManager(linearLayoutManager);

        getOrderItems(order, BASE_URL, holder);

        holder.cancelButton.setOnClickListener(view -> {onCancelOrderButtonClick(order, BASE_URL, holder);});

        holder.acceptButton.setOnClickListener(view -> {
            updateOrderStatus(orderDetails, BASE_URL, position);
        });

        holder.statusButton.setOnClickListener(view -> {
            updateOrderStatus(orderDetails, BASE_URL, position);
        });

        holder.editButton.setOnClickListener(view -> {
            onEditButtonClicked(order);
        });

        holder.trackButton.setOnClickListener(view -> {
            getRiderDetails(holder, order, 1);
        });

//        holder.detailsButton.setOnClickListener(view -> {
//            Intent intent = new Intent(context, OrderDetailsActivity.class);
//            intent.putExtra("selectedOrder", orders.get(position));
//            intent.putExtra("section", section);
//            context.startActivity(intent);
//        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    private void getOrderItems(Order order, String BASE_URL, ViewHolder holder) {

        Map<String, String> headers = new HashMap<>();
//        headers.put("Authorization", "Bearer Bearer accessToken");
//
//        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.ORDER_SERVICE_URL)
//                .addConverterFactory(GsonConverterFactory.create()).build();
//
//        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Call<ItemResponse> itemResponseCall = orderApiService.getItemsForOrder(headers, order.id);

//        progressDialog.show();

        itemResponseCall.clone().enqueue(new Callback<ItemResponse>() {
            @Override
            public void onResponse(Call<ItemResponse> call, Response<ItemResponse> response) {
                if (response.isSuccessful()) {
                    ItemAdapter itemsAdapter = new ItemAdapter(response.body().data.content, order, context);
                    holder.recyclerView.setAdapter(itemsAdapter);
                    itemsAdapter.notifyDataSetChanged();
                } else {
                    holder.itemsErrorTextView.setVisibility(View.VISIBLE);
                }
                holder.itemsProgressBar.setVisibility(View.GONE);
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<ItemResponse> call, Throwable t) {
                Log.e(TAG, "onFailureItems: ", t);
                progressDialog.dismiss();
                holder.itemsProgressBar.setVisibility(View.GONE);
                holder.itemsErrorTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void onCancelOrderButtonClick(Order order, String BASE_URL, ViewHolder holder) {
        //add headers required for api calls
        Map<String, String> headers = new HashMap<>();
//        headers.put("Authorization", "Bearer Bearer accessToken");
//
//        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.ORDER_SERVICE_URL)
//                .addConverterFactory(GsonConverterFactory.create()).build();
//
//        OrderApi orderApiService = retrofit.create(OrderApi.class);

        dialog = new Dialog(context);
        dialog.setContentView(R.layout.custom_alert_dialog);
        dialog.setCancelable(false);
        dialog.findViewById(R.id.btn_positive).setVisibility(View.VISIBLE);
        dialog.findViewById(R.id.btn_negative).setVisibility(View.VISIBLE);
        dialog.findViewById(R.id.btn_positive).setOnClickListener(view -> {
            dialog.dismiss();

            int position = holder.getAdapterPosition();
            Order.OrderDetailsResponse removedOrder = orders.remove(position);
            notifyItemRemoved(position);

            Call<ResponseBody> processOrder = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.CANCELED_BY_MERCHANT), order.id);
//                    progressDialog.show();
                    processOrder.clone().enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            progressDialog.dismiss();
                            if (response.isSuccessful()) {
                                Toast.makeText(context, "Order Cancelled", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, R.string.request_failure, Toast.LENGTH_SHORT).show();
                                reAddOrder(position, removedOrder);
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(context, R.string.no_internet, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "onFailure: ", t);
                            progressDialog.dismiss();

                            reAddOrder(position, removedOrder);
                        }
                    });
        });
        dialog.findViewById(R.id.btn_negative).setOnClickListener(view -> {
            dialog.dismiss();
        });

        dialog.show();

    }

    private void updateOrderStatus(Order.OrderDetailsResponse orderDetails, String BASE_URL, int position) {

        //add headers required for api calls
        Map<String, String> headers = new HashMap<>();
//        headers.put("Authorization", "Bearer Bearer accessToken");
//
//        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.ORDER_SERVICE_URL)
//                .addConverterFactory(GsonConverterFactory.create()).build();
//
//        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Call<ResponseBody> processOrder = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(orderDetails.order.id, Status.fromString(orderDetails.nextCompletionStatus)), orderDetails.order.id);

        progressDialog.show();
        processOrder.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
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
                    Log.e(TAG, response.toString());
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
            Toast.makeText(context, "Order already updated.", Toast.LENGTH_SHORT).show();
            return;
        } else {
            dialog = new Dialog(context);
            dialog.setContentView(R.layout.custom_alert_dialog);
            dialog.setCancelable(false);
            ImageView imageView = dialog.findViewById(R.id.alert_icon);
            TextView title = dialog.findViewById(R.id.alert_title);
            TextView message = dialog.findViewById(R.id.alert_message);
            dialog.findViewById(R.id.btn_positive).setVisibility(View.VISIBLE);
            dialog.findViewById(R.id.btn_negative).setVisibility(View.VISIBLE);
            title.setText(R.string.edit_order);
            message.setText(R.string.edit_order_warning);
            imageView.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_warning_24));
            dialog.findViewById(R.id.btn_positive).setOnClickListener(view -> {
                dialog.dismiss();
                Intent intent = new Intent(context, EditOrderActivity.class);
                intent.putExtra("order", order);
                context.startActivity(intent);
            });
            dialog.findViewById(R.id.btn_negative).setOnClickListener(view -> {
                dialog.dismiss();
            });
            dialog.show();
        }
    }

    private void reAddOrder(int position, Order.OrderDetailsResponse removedOrder) {
        try {
            orders.add(position, removedOrder);
            notifyItemInserted(position);
        } catch (Exception e) {
            orders.add(removedOrder);
            notifyItemInserted(orders.size() - 1);
        }
    }

    private boolean isOrderNew(Order.OrderDetailsResponse order) {
        return order.currentCompletionStatus.equals(Status.PAYMENT_CONFIRMED.toString())
                || order.currentCompletionStatus.equals(Status.RECEIVED_AT_STORE.toString());
    }

    private boolean isOrderOngoing(Order.OrderDetailsResponse order) {
        return !isOrderNew(order) && order.nextCompletionStatus != null;
    }

    private void getRiderDetails(ViewHolder holder, Order order, int tag) {

        Map<String, String> headers = new HashMap<>();

        Call<OrderDeliveryDetailsResponse> riderDetails = deliveryApiService.getOrderDeliveryDetailsById(headers, order.id);

        riderDetails.clone().enqueue(new Callback<OrderDeliveryDetailsResponse>() {
            @Override
            public void onResponse(Call<OrderDeliveryDetailsResponse> call, Response<OrderDeliveryDetailsResponse> response) {
                if (response.isSuccessful()) {
                    if (tag == 1) {
                        Intent intent = new Intent(context, TrackOrderActivity.class);
                        intent.putExtra("riderDetails", response.body().data);
                        context.startActivity(intent);
                    } else if (tag == 2) {
                        OrderDeliveryDetailsResponse.OrderDeliveryDetailsData data = response.body().data;
                        if (data.name != null || data.phoneNumber != null) {
                            holder.rlRiderDetails.setVisibility(View.VISIBLE);
                            holder.divider9.setVisibility(View.VISIBLE);
                            if (data.name != null)
                                holder.riderName.setText(data.name);
                            if (data.phoneNumber != null) {
                                holder.riderContact.setText(data.phoneNumber);
                                holder.riderCallIcon.setVisibility(View.VISIBLE);
                                holder.riderCallIcon.setOnClickListener(view -> {
                                    Intent callDriver = new Intent(Intent.ACTION_DIAL);
                                    callDriver.setData(Uri.parse("tel:" + data.phoneNumber));
                                    context.startActivity(callDriver);
                                });
                            }
                        } else {
                            holder.rlRiderDetails.setVisibility(View.GONE);
                            holder.divider9.setVisibility(View.GONE);
                        }
                    }
                } else {
                    if (tag == 1)
                        Toast.makeText(context, R.string.request_failure, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "ERROR: " + response.toString());
                }
            }

            @Override
            public void onFailure(Call<OrderDeliveryDetailsResponse> call, Throwable t) {
                Toast.makeText(context, R.string.no_internet, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
