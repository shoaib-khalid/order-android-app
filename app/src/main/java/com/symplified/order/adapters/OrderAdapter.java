package com.symplified.order.adapters;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.TrackOrderActivity;
import com.symplified.order.apis.DeliveryApi;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.enums.OrderStatus;
import com.symplified.order.enums.ServiceType;
import com.symplified.order.interfaces.OrderManager;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.item.ItemsResponse;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderDeliveryDetailsResponse;
import com.symplified.order.models.order.OrderUpdateResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.utils.Utility;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    public List<Order.OrderDetails> orders;
    public String section;
    public Context context;
    public final String TAG = "order-adapter";
    public Dialog dialog;
    public DecimalFormat formatter;

    private final OrderApi orderApiService;
    private final DeliveryApi deliveryApiService;
    private final OrderManager orderManager;

    public OrderAdapter(List<Order.OrderDetails> orders, String section, Context context, OrderManager orderManager) {
        this.orders = orders;
        this.section = section;
        this.context = context;
        this.orderManager = orderManager;

        orderApiService = ServiceGenerator.createOrderService();
        deliveryApiService = ServiceGenerator.createDeliveryService();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name, invoice, date, total, total2, status, phoneNumber, address,
                subTotal, deliveryCharges, deliveryDiscount, discount, voucherDiscount,
                storeVoucherDiscount, serviceCharges;
        private final MaterialButton editButton, cancelButton, acceptButton, statusButton,
                trackButton, callButton;
        private final ImageButton printButton;
        private final CardView cardView;
        private final TextView invoiceLabel, dateLabel, totalLabel, statusLabel, typeLabel, orderType,
                currStatusLabel, currStatus, customerNotes, riderName, riderContact;
        private final LinearLayout newLayout, ongoingLayout;
        private final RelativeLayout currStatusLayout, typeLayout, rlDiscount, rlVoucherDiscount,
                rlStoreVoucherDiscount, rlServiceCharges, rlDeliveryDiscount, rlCustomerNote,
                rlRiderDetails, rlAddress, rlContact;
        private final ConstraintLayout clOrderProgressBar;
        private final View divider3, divider7, divider8, divider9;
        private final ImageView riderCallIcon;

        private final RecyclerView recyclerView;
        private final ProgressBar itemsProgressBar;
        private final TextView itemsErrorTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.order_row_name_value);
            invoice = itemView.findViewById(R.id.card_invoice_value);
            date = itemView.findViewById(R.id.order_date_value);
            total = itemView.findViewById(R.id.order_total_value);
            status = itemView.findViewById(R.id.order_status_value);
            typeLabel = itemView.findViewById(R.id.order_type);
            orderType = itemView.findViewById(R.id.order_type_value);
            customerNotes = itemView.findViewById(R.id.customer_note_value);

            phoneNumber = itemView.findViewById(R.id.contact_value);
            address = itemView.findViewById(R.id.address_value);
            subTotal = itemView.findViewById(R.id.subtotal_value);
            deliveryCharges = itemView.findViewById(R.id.delivery_by_value);
            discount = itemView.findViewById(R.id.discount_value);
            voucherDiscount = itemView.findViewById(R.id.voucher_discount_value);
            storeVoucherDiscount = itemView.findViewById(R.id.store_voucher_discount_value);
            deliveryDiscount = itemView.findViewById(R.id.billing_delivery_charges_discount_value);
            serviceCharges = itemView.findViewById(R.id.service_charges_value);
            total2 = itemView.findViewById(R.id.order_total_value_);
            callButton = itemView.findViewById(R.id.btn_call);
            printButton = itemView.findViewById(R.id.btn_print_order);
            rlDiscount = itemView.findViewById(R.id.rl_discount);
            rlVoucherDiscount = itemView.findViewById(R.id.rl_voucher_discount);
            rlStoreVoucherDiscount = itemView.findViewById(R.id.rl_store_voucher_discount);
            rlDeliveryDiscount = itemView.findViewById(R.id.rl_delivery_discount);
            rlServiceCharges = itemView.findViewById(R.id.rl_service_charges);
            rlAddress = itemView.findViewById(R.id.rl_address);
            rlContact = itemView.findViewById(R.id.rl_contact);

            riderName = itemView.findViewById(R.id.driver_value);
            riderContact = itemView.findViewById(R.id.driver_contact_value);

            riderCallIcon = itemView.findViewById(R.id.address_icon_phone);

            currStatusLabel = itemView.findViewById(R.id.order_curr_status);
            currStatus = itemView.findViewById(R.id.order_curr_status_value);
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
            clOrderProgressBar = itemView.findViewById(R.id.order_progress_bar_layout);

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

        Order.OrderDetails orderDetails = orders.get(position);
        Order order = orderDetails.order;

        formatter = Utility.getMonetaryAmountFormat();

        String currency = Utility.getCurrencySymbol(order);

        holder.name.setText(order.orderShipmentDetail.receiverName);

        if (order.created != null) {
            TimeZone storeTimeZone = order.store != null && order.store.regionCountry != null && order.store.regionCountry.timezone != null
                    ? TimeZone.getTimeZone(order.store.regionCountry.timezone)
                    : TimeZone.getDefault();
            holder.date.setText(Utility.convertUtcTimeToLocalTimezone(order.created, storeTimeZone));
        }

        holder.invoice.setText(order.invoiceId);
        holder.total.setText(currency + " " + formatter.format(order.total));

        StringBuilder fullAddress = new StringBuilder();
        if (!Utility.isBlank(order.orderShipmentDetail.address)) {
            fullAddress.append(order.orderShipmentDetail.address).append(", ");
        }
        if (!Utility.isBlank(order.orderShipmentDetail.city)) {
            fullAddress.append(order.orderShipmentDetail.city).append(", ");
        }
        if (!Utility.isBlank(order.orderShipmentDetail.state)) {
            fullAddress.append(order.orderShipmentDetail.state).append(", ");
        }
        if (!Utility.isBlank(order.orderShipmentDetail.zipcode)) {
            fullAddress.append(order.orderShipmentDetail.zipcode);
        }
        if (fullAddress.length() > 0) {
            holder.address.setText(String.valueOf(fullAddress));
            holder.rlAddress.setVisibility(View.VISIBLE);
        }

        if (!Utility.isBlank(order.orderShipmentDetail.phoneNumber)) {
            holder.phoneNumber.setText(order.orderShipmentDetail.phoneNumber);
            holder.rlContact.setVisibility(View.VISIBLE);
        }

        holder.subTotal.setText(currency + " " + formatter.format(order.subTotal));

        if (order.appliedDiscount != null && order.appliedDiscount > 0) {
            holder.discount.setText("- " + currency + " " + formatter.format(order.appliedDiscount));
            holder.rlDiscount.setVisibility(View.VISIBLE);
        }

        if (order.voucherDiscount != null && order.voucherDiscount > 0) {
            holder.voucherDiscount.setText("- " + currency + " " + formatter.format(order.voucherDiscount));
            holder.rlVoucherDiscount.setVisibility(View.VISIBLE);
        }

        if (order.storeVoucherDiscount != null && order.storeVoucherDiscount > 0) {
            holder.storeVoucherDiscount.setText("- " + currency + " " + formatter.format(order.storeVoucherDiscount));
            holder.rlStoreVoucherDiscount.setVisibility(View.VISIBLE);
        }

        holder.deliveryCharges.setText(order.deliveryCharges != null
                ? currency + " " + formatter.format(order.deliveryCharges)
                : currency + " " + "0.00");
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

        holder.callButton.setOnClickListener(view -> startCallActivity(order.orderShipmentDetail.phoneNumber));
        holder.printButton.setOnClickListener(view -> {
            ItemAdapter adapter = (ItemAdapter) holder.recyclerView.getAdapter();
            List<Item> items = adapter != null ? adapter.getItems() : new ArrayList<>();
            printReceipt(order, items);
        });
        
        holder.orderType.setText(order.serviceType == ServiceType.DINEIN
                ? "Dine In"
                : order.orderShipmentDetail.storePickup ? "Store Pickup" : "Delivery");

        order.customerNotes = order.customerNotes != null ? order.customerNotes : "";

        switch (order.customerNotes.toUpperCase()) {
            case "TAKEAWAY":
                order.customerNotes = "Take Away";
                break;
            case "SELFCOLLECT":
                order.customerNotes = "Self Collect";
                break;
        }

        if (!"".equals(order.customerNotes)) {
            holder.customerNotes.setText(order.customerNotes);
            holder.rlCustomerNote.setVisibility(View.VISIBLE);
            holder.divider3.setVisibility(View.VISIBLE);
        }

        switch (section) {
            case "new":
                if (order.isRevised) {
                    holder.editButton.setTextColor(context.getResources().getColor(R.color.dark_grey));
                    holder.editButton.setStrokeColor(ColorStateList.valueOf(context.getResources().getColor(R.color.dark_grey)));
                }
                holder.editButton.setVisibility(View.VISIBLE);
                holder.acceptButton.setText(orderDetails.nextActionText);
                holder.cancelButton.setVisibility(order.serviceType == ServiceType.DINEIN ? View.GONE : View.VISIBLE);
                holder.currStatusLayout.setVisibility(View.GONE);
                holder.divider8.setVisibility(View.GONE);
                holder.newLayout.setVisibility(View.VISIBLE);
                break;
            case "ongoing":
                if (orderDetails.nextActionText != null) {
                    holder.ongoingLayout.setVisibility(View.VISIBLE);
                    holder.statusLabel.setVisibility(View.VISIBLE);
                    holder.statusLabel.setText("Update Status: ");
                    holder.statusButton.setText(orderDetails.nextActionText);
                    holder.statusButton.setVisibility(View.VISIBLE);
                }

                switch (order.completionStatus) {
                    case BEING_PREPARED:
                        holder.currStatus.setText("Preparing");
                        holder.trackButton.setVisibility(View.GONE);
                        break;
                    case AWAITING_PICKUP:
                        holder.currStatus.setText("Awaiting Pickup");
                        if (!order.orderShipmentDetail.storePickup
                                && order.orderShipmentDetail.deliveryPeriodDetails != null) {
                            holder.trackButton.setVisibility(View.VISIBLE);
                        }
                        break;
                    case BEING_DELIVERED:
                        holder.currStatus.setText("Out for Delivery");
                        if (!order.orderShipmentDetail.storePickup
                                && order.orderShipmentDetail.deliveryPeriodDetails != null) {
                            holder.trackButton.setVisibility(View.VISIBLE);
                        }
                        break;
                }
                break;
            case "past":
                holder.ongoingLayout.setVisibility(View.VISIBLE);
                holder.typeLayout.setVisibility(View.GONE);
                holder.currStatusLayout.setVisibility(View.GONE);
                holder.status.setVisibility(View.VISIBLE);
                holder.statusLabel.setVisibility(View.VISIBLE);
                if (order.completionStatus == OrderStatus.DELIVERED_TO_CUSTOMER) {
                    holder.status.setText("Order Delivered");
                    holder.status.setTextColor(ContextCompat.getColor(context, R.color.sf_accept_button));
                } else if (order.completionStatus == OrderStatus.CANCELED_BY_MERCHANT
                        || order.completionStatus == OrderStatus.CANCELED_BY_CUSTOMER) {
                    holder.status.setText("Order Cancelled");
                    holder.status.setTextColor(ContextCompat.getColor(context, R.color.sf_cancel_button));
                }
                holder.divider7.setVisibility(View.GONE);
                holder.divider8.setVisibility(View.GONE);

                if (!order.orderShipmentDetail.storePickup && order.orderShipmentDetail.deliveryPeriodDetails != null) {
                    getRiderDetails(holder, order, 2);
                }
                break;
        }

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        holder.recyclerView.setLayoutManager(linearLayoutManager);
        getOrderItemsForView(order, holder);

        holder.cancelButton.setOnClickListener(view -> onCancelOrderButtonClick(order, holder));
        holder.acceptButton.setOnClickListener(view -> updateOrderStatus(orderDetails, holder));
        holder.statusButton.setOnClickListener(view -> updateOrderStatus(orderDetails, holder));
        holder.editButton.setOnClickListener(view -> onEditButtonClicked(order));
        holder.trackButton.setOnClickListener(view -> getRiderDetails(holder, order, 1));

        if (App.isPrinterConnected()) {
            holder.printButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    private void getOrderItemsForView(Order order, ViewHolder holder) {

        Call<ItemsResponse> itemResponseCall = orderApiService.getItemsForOrder(order.id);

        itemResponseCall.clone().enqueue(new Callback<ItemsResponse>() {
            @Override
            public void onResponse(@NonNull Call<ItemsResponse> call, @NonNull Response<ItemsResponse> response) {
                if (response.isSuccessful()) {
                    List<Item> orderItems = response.body().data.content;
                    ItemAdapter itemsAdapter = new ItemAdapter(orderItems, order, context);
                    holder.recyclerView.setAdapter(itemsAdapter);
                    itemsAdapter.notifyDataSetChanged();
                } else {
                    holder.itemsErrorTextView.setVisibility(View.VISIBLE);
                }
                holder.itemsProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<ItemsResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "onFailureItems: ", t);
                holder.itemsProgressBar.setVisibility(View.GONE);
                holder.itemsErrorTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void getOrderItemsForPrint(Order order) {
        Call<ItemsResponse> itemResponseCall = orderApiService.getItemsForOrder(order.id);

        itemResponseCall.clone().enqueue(new Callback<ItemsResponse>() {
            @Override
            public void onResponse(Call<ItemsResponse> call, Response<ItemsResponse> response) {
                if (response.isSuccessful()) {
                    printReceipt(order, response.body().data.content);
                }
            }

            @Override
            public void onFailure(Call<ItemsResponse> call, Throwable t) {
                Log.e(TAG, "onFailureItems: ", t);
            }
        });
    }

    public void onCancelOrderButtonClick(Order order, ViewHolder holder) {

        Map<String, String> headers = new HashMap<>();

        dialog = new Dialog(context);
        dialog.setContentView(R.layout.custom_alert_dialog);
        dialog.setCancelable(false);
        dialog.findViewById(R.id.btn_positive).setVisibility(View.VISIBLE);
        dialog.findViewById(R.id.btn_negative).setVisibility(View.VISIBLE);
        dialog.findViewById(R.id.btn_positive).setOnClickListener(view -> {
            dialog.dismiss();

            Order.OrderDetails removedOrder = orders.remove(holder.getAdapterPosition());
            notifyItemRemoved(holder.getAdapterPosition());
            notifyItemRangeChanged(holder.getAdapterPosition(), orders.size());

            Call<OrderUpdateResponse> processOrder = orderApiService
                    .updateOrderStatus(new Order.OrderUpdate(order.id, OrderStatus.CANCELED_BY_MERCHANT), order.id);
            processOrder.clone().enqueue(new Callback<OrderUpdateResponse>() {
                @Override
                public void onResponse(Call<OrderUpdateResponse> call, Response<OrderUpdateResponse> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(context, "Order Cancelled", Toast.LENGTH_SHORT).show();
                    } else if (response.code() != 406) {
                        Toast.makeText(context, R.string.request_failure, Toast.LENGTH_SHORT).show();
                        reAddOrder(holder.getAdapterPosition(), removedOrder);
                    }
                }

                @Override
                public void onFailure(Call<OrderUpdateResponse> call, Throwable t) {
                    Toast.makeText(context, R.string.no_internet, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "onFailure: ", t);

                    reAddOrder(holder.getAdapterPosition(), removedOrder);
                }
            });
        });
        dialog.findViewById(R.id.btn_negative).setOnClickListener(view -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateOrderStatus(Order.OrderDetails currentOrderDetails, ViewHolder holder) {

        startLoading(holder);

        Call<OrderUpdateResponse> processOrder = orderApiService
                .updateOrderStatus(new Order.OrderUpdate(currentOrderDetails.order.id,
                                currentOrderDetails.nextCompletionStatus),
                        currentOrderDetails.order.id);

        processOrder.clone().enqueue(new Callback<OrderUpdateResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderUpdateResponse> call,
                                   @NonNull Response<OrderUpdateResponse> response) {
                int indexOfOrder = orders.indexOf(currentOrderDetails);
                if (response.isSuccessful()) {
                    Order.OrderDetails updatedOrder = new Order.OrderDetails(response.body().data);

                    String statusUpdateToastText = "Status Updated";
                    if (Utility.isOrderOngoing(updatedOrder.currentCompletionStatus)) {
                        if (Utility.isOrderOngoing(currentOrderDetails.currentCompletionStatus)
                                && indexOfOrder != -1) {
                            orders.set(indexOfOrder, updatedOrder);
                            notifyItemChanged(indexOfOrder);
                            stopLoading(holder, updatedOrder.currentCompletionStatus);
                        } else {
                            orderManager.addOrderToOngoingTab(updatedOrder);
                            statusUpdateToastText = "Order moved to ongoing tab";
                        }
                    }

                    if (Utility.isOrderNew(currentOrderDetails.currentCompletionStatus)
                            || Utility.isOrderCompleted(updatedOrder.currentCompletionStatus)) {
                        if (Utility.isOrderCompleted(updatedOrder.currentCompletionStatus)) {
                            orderManager.addOrderToHistoryTab(updatedOrder);
                            statusUpdateToastText = "Order moved to history tab";
                        }

                        if (indexOfOrder != -1) {
                            orders.remove(indexOfOrder);
                            notifyItemRemoved(indexOfOrder);
                        }

                        if (Utility.isOrderNew(currentOrderDetails.currentCompletionStatus)) {
                            getOrderItemsForPrint(currentOrderDetails.order);
                        }
                    }

                    Toast.makeText(context, statusUpdateToastText, Toast.LENGTH_SHORT).show();
                } else if (response.code() == 406) {
                    Toast.makeText(context, "Error: Order already processed.", Toast.LENGTH_SHORT).show();
                    orders.remove(indexOfOrder);
                    notifyItemRemoved(indexOfOrder);
                } else {
                    Log.e(TAG, response.raw().toString());
                    Log.e(TAG, "Error body: " + response.errorBody());
                    Toast.makeText(context, R.string.request_failure, Toast.LENGTH_SHORT).show();
                    stopLoading(holder, currentOrderDetails.order.completionStatus);
                }
            }

            @Override
            public void onFailure(Call<OrderUpdateResponse> call, Throwable t) {
                Toast.makeText(context, R.string.no_internet, Toast.LENGTH_SHORT).show();
                stopLoading(holder, currentOrderDetails.order.completionStatus);
                Log.e(TAG, "onFailure: ", t);
            }
        });

    }

    private void onEditButtonClicked(Order order) {
        if (order.isRevised) {
            Toast.makeText(context, "Order already updated.", Toast.LENGTH_SHORT).show();
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
//                Intent intent = new Intent(context, EditOrderActivity.class);
//                intent.putExtra("order", order);
//                context.startActivity(intent);
                orderManager.editOrder(order);
            });
            dialog.findViewById(R.id.btn_negative).setOnClickListener(view -> {
                dialog.dismiss();
            });
            dialog.show();
        }
    }

    private void reAddOrder(int position, Order.OrderDetails removedOrder) {
        try {
            orders.add(position, removedOrder);
        } catch (Exception e) {
            orders.add(removedOrder);
            position = orders.size() - 1;
        }
        notifyItemInserted(position);
    }

    private void getRiderDetails(ViewHolder holder, Order order, int tag) {

        Map<String, String> headers = new HashMap<>();

        Call<OrderDeliveryDetailsResponse> riderDetails = deliveryApiService.getOrderDeliveryDetailsById(headers, order.id);

        riderDetails.clone().enqueue(new Callback<OrderDeliveryDetailsResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderDeliveryDetailsResponse> call,
                                   @NonNull Response<OrderDeliveryDetailsResponse> response) {
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
                                holder.riderCallIcon.setOnClickListener(view -> startCallActivity(data.phoneNumber));
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
            public void onFailure(@NonNull Call<OrderDeliveryDetailsResponse> call, @NonNull Throwable t) {
                Toast.makeText(context, R.string.no_internet, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCallActivity(String phoneNumber) {
        try {
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                Intent callDriver = new Intent(Intent.ACTION_DIAL);
                callDriver.setData(Uri.parse("tel:" + phoneNumber));
                context.startActivity(callDriver);
            } else {
                Toast.makeText(context, R.string.call_error_message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, R.string.call_error_message, Toast.LENGTH_SHORT).show();
        }
    }

    private void startLoading(ViewHolder holder) {
        holder.clOrderProgressBar.setVisibility(View.VISIBLE);
        holder.newLayout.setVisibility(View.GONE);
        holder.ongoingLayout.setVisibility(View.GONE);
        holder.editButton.setVisibility(View.GONE);
    }

    private void stopLoading(ViewHolder holder, OrderStatus completionStatus) {
        holder.clOrderProgressBar.setVisibility(View.GONE);
        if (Utility.isOrderNew(completionStatus)) {
            holder.newLayout.setVisibility(View.VISIBLE);
            holder.editButton.setVisibility(View.VISIBLE);
        } else if (Utility.isOrderOngoing(completionStatus)) {
            holder.ongoingLayout.setVisibility(View.VISIBLE);
        }
    }

    private void printReceipt(Order order, List<Item> items) {
        try {
            App.getPrinter().printReceipt(order, items);
        } catch (Exception e) {
            Log.e("order-adapter", "Failed to print order. " + e.getLocalizedMessage());
            e.printStackTrace();
            Toast.makeText(context, "Failed to print order.", Toast.LENGTH_SHORT).show();
        }
    }
}
