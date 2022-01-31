package com.symplified.order.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.transition.MaterialContainerTransform;
import com.symplified.order.App;
import com.symplified.order.OrderDetailsActivity;
import com.symplified.order.R;
import com.symplified.order.apis.DeliveryApi;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderDeliveryDetailsResponse;
import com.symplified.order.services.AlertService;
import com.symplified.order.utils.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
//    public List<OrderDetailsModel> items;

    public List<Order> orders;
    public String section;
    public boolean isPickup;
    public Context context;
    private final String TAG = OrderAdapter.class.getName();
//    private OrderDeliveryDetailsResponse.OrderDeliveryDetailsData deliveryDetails;
    private Dialog progressDialog;
//    private boolean hasDeliveryDetails;
    private Map<String, Boolean> hasDeliveryDetailsMap;

    public OrderAdapter(List<Order> orders, String section, Context context){
//        List<OrderDetailsModel> items,
//        this.items = items;
        this.orders = orders;
        this.section = section;
        this.context = context;

        hasDeliveryDetailsMap = new HashMap<>();
        progressDialog = new Dialog((Activity) context);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name,phone, amount,invoice;
        private final ImageView pickup;
        private final Button process;
        private final ImageView storeLogo;
        private final TextView storeLogoText;
        private final ConstraintLayout driverDetails;
        private final View driverDetailsDivider;
        private final TextView driverName;
        private final TextView driverPhoneNumber;
        private final MaterialCardView cardLayout;
        private final TextView driverNameLable, driverPhoneLable, nameLable,phoneLable, invoiceLable, amountLable, pickupLable;

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
            driverDetails = view.findViewById(R.id.driver_info_card);
            driverDetailsDivider = view.findViewById(R.id.divider_card2);
            driverName = view.findViewById(R.id.driver_value_card);
            driverPhoneNumber = view.findViewById(R.id.driver_contact_value_card);
            cardLayout = view.findViewById(R.id.order_card_parent);
            driverNameLable = view.findViewById(R.id.driver_label_card);
            driverPhoneLable = view.findViewById(R.id.driver_contact_label_card);
            nameLable = view.findViewById(R.id.order_row_name);
            phoneLable = view.findViewById(R.id.order_row_phone);
            invoiceLable = view.findViewById(R.id.card_invoice);
            amountLable = view.findViewById(R.id.order_amount);
            pickupLable = view.findViewById(R.id.order_pickup);
        }

        public void changeTextColorWhite(){

            name.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            phone.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            amount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            invoice.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            storeLogoText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            driverName.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            driverPhoneNumber.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            driverNameLable.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            driverPhoneLable.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            nameLable.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            phoneLable.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            invoiceLable.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            amountLable.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            pickupLable.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
            pickup.setImageTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.white));

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
//        OrderDeliveryDetailsResponse.OrderDeliveryDetailsData deliveryDetails;

            String encodedStoreLogo = sharedPreferences.getString("logoImage-"+orders.get(holder.getAdapterPosition()).storeId, null);

            if(storeIdList != null && storeIdList.split(" ").length > 0)
            {

                if(sharedPreferences.contains("logoImage-"+orders.get(holder.getAdapterPosition()).storeId)
                        && encodedStoreLogo != null)
                {
                    Utility.decodeAndSetImage(holder.storeLogo, encodedStoreLogo);
                }
                else{
                    holder.storeLogo.setVisibility(View.GONE);
                    holder.storeLogoText.setVisibility(View.VISIBLE);
                    String storeName = sharedPreferences.getString(orders.get(holder.getAdapterPosition()).storeId+"-name", null);
                    holder.storeLogoText.setText(storeName);
                }

            }

//        Log.e("TAG", "onBindViewHolder: "+ sharedPreferences.getString(orders.get(holder.getAdapterPosition()).storeId+"-name", null), new Error());


        holder.name.setText(orders.get(position).orderShipmentDetail.receiverName);
        holder.phone.setText(orders.get(position).orderShipmentDetail.phoneNumber);
        holder.amount.setText(Double.toString(orders.get(position).total));
        holder.invoice.setText(orders.get(position).invoiceId);
        if(section.equals("sent") || section.equals("pickup")){
            setDriverDeliveryDetails(orders.get(position), sharedPreferences, holder);
        }


        if(!orders.get(position).orderShipmentDetail.storePickup) {
            holder.pickup.setBackgroundResource(R.drawable.ic_highlight_off_black_24dp);
            isPickup = false;
        }
        else {
            holder.pickup.setBackgroundResource(R.drawable.ic_check_circle_black_24dp);
            isPickup = true;
        }

        holder.process.setText("Details");

        holder.process.setOnClickListener(view -> {
            Intent intent = new Intent (holder.itemView.getContext(), OrderDetailsActivity.class);
            intent.putExtra("selectedOrder",orders.get(position));
            intent.putExtra("section", section);
            intent.putExtra("pickup", isPickup);
            intent.putExtra("hasDeliveryDetails", hasDeliveryDetailsMap.get(orders.get(position).id));
//                intent.putExtra("deliveryDetails", deliveryDetails);
            context.stopService(new Intent(context, AlertService.class));
            ((Activity) context).startActivityForResult(intent, 4);
//                ((Activity)view.getContext()).finish();
        });

    }

    private void setDriverDeliveryDetails(Order order, SharedPreferences sharedPreferences, ViewHolder holder) {

        String BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient())
                .baseUrl(BASE_URL+App.DELIVERY_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        DeliveryApi deliveryApiService = retrofit.create(DeliveryApi.class);

        //12dc5195-5f03-42fd-94f0-f147dc4ced55
        Call<OrderDeliveryDetailsResponse> deliveryDetailsResponseCall = deliveryApiService.getOrderDeliveryDetailsById(headers, order.id);

        progressDialog.show();

        deliveryDetailsResponseCall.clone().enqueue(new Callback<OrderDeliveryDetailsResponse>() {
            @Override
            public void onResponse(Call<OrderDeliveryDetailsResponse> call, Response<OrderDeliveryDetailsResponse> response) {
                if(response.isSuccessful()){
                    if(section.equals("pickup")){
                        Log.e(TAG, "onResponse: onPickupTab" );
                        Log.e(TAG, "onResponse: order-id = "+order.id );
                    }
                    Log.i(TAG, "onResponse123: "+ response.body().toString());
                    OrderDeliveryDetailsResponse.OrderDeliveryDetailsData data = response.body().data;

                    if(data.riderStatus != null && data.riderStatus.equals("CANCELED")){
                        holder.cardLayout.setCardBackgroundColor(ContextCompat.getColor(context, R.color.red));
                        holder.changeTextColorWhite();
                    }

                    holder.driverDetails.setVisibility(View.VISIBLE);
                    holder.driverDetailsDivider.setVisibility(View.VISIBLE);

                    if(data.name != null && data.phoneNumber != null){

                        Log.i(TAG, "onResponse123: " + order.invoiceId + " id : "+order.id);
                        String name = response.body().data.name ;
                        if(name != null && name.split(" ").length > 1){
                            name = name.trim().split(" ")[0] + " ...";
                        }
                        holder.driverName.setText(name);
                        holder.driverPhoneNumber.setText(response.body().data.phoneNumber);
//                        hasDeliveryDetails = true;
                        hasDeliveryDetailsMap.put(order.id, true);

//                        OrderDeliveryDetailsResponse.Provider provider = new OrderDeliveryDetailsResponse.Provider(response.body().data.provider.id,
//                                response.body().data.provider.name, response.body().data.provider.providerImage);
//
//                        deliveryDetails = new OrderDeliveryDetailsResponse
//                                .OrderDeliveryDetailsData(response.body().data.name,
//                                response.body().data.phoneNumber, response.body().data.plateNumber,
//                                response.body().data.trackingUrl, response.body().data.orderNumber,
//                                provider);
                    }
                    else {
                        Log.e(TAG, "Response Unsuccessful" + " onResponse: "+response.body());
                        if(data.riderStatus != null && data.riderStatus.equals("CANCELED")){
                            holder.driverPhoneLable.setVisibility(View.GONE);
                            holder.driverNameLable.setVisibility(View.GONE);
                            holder.driverName.setText("Status : Problem with delivery. No driver available !");
                        }
                        else{
                            holder.driverDetails.setVisibility(View.GONE);
                            holder.driverDetailsDivider.setVisibility(View.GONE);
                        }
//                        hasDeliveryDetails = false;
                        hasDeliveryDetailsMap.put(order.id, false);
                    }

                }
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<OrderDeliveryDetailsResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: ",t );
                progressDialog.dismiss();
            }
        });


    }


    @Override
    public int getItemCount() {
        return orders.size();
    }
}

