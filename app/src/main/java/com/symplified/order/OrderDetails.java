package com.symplified.order;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.symplified.order.adapters.ItemsAdapter;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.enums.Status;
import com.symplified.order.models.item.ItemResponse;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderDeliveryDetailsResponse;
import com.symplified.order.utils.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OrderDetails extends AppCompatActivity {
    private RecyclerView recyclerView;

    private TextView storeLogoText, dateValue, invoiceValue, addressValue, cityValue, stateValue, postcodeValue, nameValue, noteValue, subtotalValue, serviceChargesValue, deliveryChargesValue,billingTotal, discount, deliveryDiscount;
    private TextView deliveryProvider, driverName, driverContactNumber, trackingLink;
    private Button process, print;
    private ImageView pickup, storeLogo;
    private String section;
    private Toolbar toolbar;
    private Dialog progressDialog;
    public static String TAG = "ProcessOrder";
    private String BASE_URL;
    private CircularProgressIndicator progressIndicator;
    private RelativeLayout deliveryDetailsView;
    private View deliveryDetailsDivider;
    private String nextStatus;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);

        //change theme for staging mode
        if(sharedPreferences.getBoolean("isStaging", false))
            setTheme(R.style.Theme_SymplifiedOrderUpdate_Test);

        setContentView(R.layout.activity_order_details);
        setResult(RESULT_CANCELED, new Intent().putExtra("finish", 0));

        section = null;
        section = getIntent().getStringExtra("section");

        //initialize all views
        initViews();
        nextStatus = "";

        //get details of selected order from previous activity
        Bundle data = getIntent().getExtras();
        Order order = (Order) data.getSerializable("selectedOrder");

        //get Delivery Driver details from previous activity
        OrderDeliveryDetailsResponse.OrderDeliveryDetailsData driverDetails;
        driverDetails = (OrderDeliveryDetailsResponse.OrderDeliveryDetailsData) data.getSerializable("deliveryDetails");

        //get base url for api calls
        BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);

        //initialize and setup app bar
        String storeIdList = sharedPreferences.getString("storeIdList", null);
        initAppBar(sharedPreferences, order, storeIdList);

        getOrderItems(order);

        getOrderStatusDetails(order);

        Log.i(TAG, "onCreate: "+order.toString());

//        Log.d("GETALLVALUES", "onCreate: "+sharedPreferences.getAll().toString());

        //display all order details to relevant fields
        displayOrderDetails(sharedPreferences, order, storeIdList, driverDetails);



    }

    private void getOrderStatusDetails(Order order) {

        //add headers required for api calls
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL+App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Call<ResponseBody> orderStatusDetailsResponseCall = orderApiService.getOrderStatusDetails(headers, order.id);
        orderStatusDetailsResponseCall.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
//                    JSONObject responseJson = new Gson().fromJson(response.body().string(),JSONObject.class);
                    JSONObject responseJson = new JSONObject(response.body().string().toString());
                    Log.e(TAG, "onResponse: "+ responseJson, new Error() );
                    new Handler().post(() -> {
                        try {
                            if(!section.equals("sent")){
                                process.setVisibility(View.VISIBLE);
                                process.setText(responseJson.getJSONObject("data").getString("nextActionText"));
                                nextStatus += responseJson.getJSONObject("data").getString("nextCompletionStatus");
                                //get order items from API
                                updateOrderStatus(order);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });

    }

    private void getOrderItems(Order order){
        //add headers required for api calls
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL+App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Call<ItemResponse> itemResponseCall = orderApiService.getItemsForOrder(headers, order.id);

//        boolean isPickup = order.orderShipmentDetail.storePickup;
        ItemsAdapter itemsAdapter = new ItemsAdapter();
        progressDialog.show();
        itemResponseCall.clone().enqueue(new Callback<ItemResponse>() {
            @Override
            public void onResponse(Call<ItemResponse> call, Response<ItemResponse> response) {

                if(response.isSuccessful())
                {
                    Log.e("TAG", "onResponse: "+order.id, new Error() );
                    itemsAdapter.setItems(response.body().data.content);
                    recyclerView.setAdapter(itemsAdapter);
                    itemsAdapter.notifyDataSetChanged();
                    progressDialog.hide();
                }


            }

            @Override
            public void onFailure(Call<ItemResponse> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Failed to retrieve items", Toast.LENGTH_SHORT).show();
                progressDialog.hide();
            }
        });
    }

    private void updateOrderStatus(Order order) {

        //add headers required for api calls
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL+App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi orderApiService = retrofit.create(OrderApi.class);

//        Call<ItemResponse> itemResponseCall = orderApiService.getItemsForOrder(headers, order.id);
//
////        boolean isPickup = order.orderShipmentDetail.storePickup;
//        ItemsAdapter itemsAdapter = new ItemsAdapter();
//        progressDialog.show();
//        itemResponseCall.clone().enqueue(new Callback<ItemResponse>() {
//            @Override
//            public void onResponse(Call<ItemResponse> call, Response<ItemResponse> response) {
//
//                if(response.isSuccessful())
//                {
//                    Log.e("TAG", "onResponse: "+order.id, new Error() );
//                    itemsAdapter.setItems(response.body().data.content);
//                    recyclerView.setAdapter(itemsAdapter);
//                    itemsAdapter.notifyDataSetChanged();
//                    progressDialog.hide();
//                }
//
//
//            }
//
//            @Override
//            public void onFailure(Call<ItemResponse> call, Throwable t) {
//                Toast.makeText(getApplicationContext(), "Failed to retrieve items", Toast.LENGTH_SHORT).show();
//                progressDialog.hide();
//            }
//        });

        Call<ResponseBody> processOrder = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.fromString(nextStatus)), order.id);


//        if(section.equals("new")) {
//            Log.e(TAG, "getOrderItems: nextStatus "+ nextStatus,new Error() );
//            /*process.setOnClickListener(view -> {
//                progressDialog.show();
//                processOrder.clone().enqueue(new Callback<ResponseBody>() {
//                    @Override
//                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                        Log.i(TAG, "request body : "+ call.request().toString());
//                        if(response.isSuccessful()){
//                            try {
//                                Log.i(TAG, "request body : "+ call.request().body());
//                                Order.UpdatedOrder currentOrder = new Gson().fromJson(response.body().string(), Order.UpdatedOrder.class);
//                                Log.i(TAG, "response body : "+ currentOrder.data.toString());
//                                if(currentOrder.data.completionStatus.toString().equals(Status.BEING_PREPARED.toString()))
//                                {
////                                        process.setText("Pickup");
//                                }
//                                else {
////                                        process.setText("Failed");
//                                }
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                            process.setClickable(false);
//                            process.setEnabled(false);
//                        }
//                        else {
//                            try {
//                                Log.e("TAG", "onResponse: "+response.errorBody().string(), new Error() );
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        progressDialog.hide();
//                    }
//
//                    @Override
//                    public void onFailure(Call<ResponseBody> call, Throwable t) {
//                        Toast.makeText(getApplicationContext(), "Not Found", Toast.LENGTH_SHORT).show();
//                        progressDialog.hide();
//                    }
//                });
//            });*/
//        }
//        else if (section.equals("processed")) {
////            Call<ResponseBody> processOrder ;
////            if(!isPickup)
////                processOrder = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.AWAITING_PICKUP), order.id);
////            else
////                processOrder = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.DELIVERED_TO_CUSTOMER), order.id);
////
//
////            Log.i("PICKUPMSG", "onCreate: isPickup :"+isPickup, new Error() );
//
//            process.setText("Pickup");
//
//            process.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//
////                Toast.makeText(getApplicationContext(), "being delivered clicked", Toast.LENGTH_SHORT).show();
//                    progressDialog.show();
//                    processOrder.clone().enqueue(new Callback<ResponseBody>() {
//                        @Override
//                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//
//                            Log.i(TAG, "request body : "+ call.request().body());
//                            if(response.isSuccessful()){
//
//                                try {
////                                Log.e("TAG", "onResponse: "+response.body().string(), new Error() );
//                                    Log.i(TAG, "request body : "+ call.request().body());
//                                    Order.UpdatedOrder currentOrder = new Gson().fromJson(response.body().string(), Order.UpdatedOrder.class);
//                                    Log.i(TAG, "response body : "+ currentOrder.data.toString());
//                                    Log.i("PICKUPMSG", "onResponse: "+currentOrder.data.completionStatus.toString());
//                                    if(currentOrder.data.completionStatus.toString().equals(Status.DELIVERED_TO_CUSTOMER.toString()))
//                                        process.setText("Delivered");
//
//                                    else if(currentOrder.data.completionStatus.toString().equals(Status.AWAITING_PICKUP.toString())){
//                                        process.setText("Awaiting Pickup");
//                                    }
//                                    else
//                                        process.setText("Failed");
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                                process.setClickable(false);
//                                process.setEnabled(false);
//                            }
//                            else {
//                                try {
//                                    Log.e("TAG", "isPickup : "+isPickup+" response error body : "+response.errorBody().string(), new Error() );
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                            progressDialog.hide();
//                        }
//
//                        @Override
//                        public void onFailure(Call<ResponseBody> call, Throwable t) {
//                            Toast.makeText(getApplicationContext(), "Not Found", Toast.LENGTH_SHORT).show();
//                            progressDialog.hide();
//                        }
//                    });
//                }
//            });
//
//        }
//        else if(section.equals("sent"))
//            process.setVisibility(View.INVISIBLE);

        process.setOnClickListener(view -> {
            processOrder.clone().enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if(response.isSuccessful()){
                        try {
                            Log.i(TAG, "onResponse: "+response.raw().toString());
                            Order.UpdatedOrder currentOrder = new Gson().fromJson(response.body().string(), Order.UpdatedOrder.class);
                            process.setText(Utility.removeUnderscores(currentOrder.data.completionStatus));
                            process.setEnabled(false);
                            process.setClickable(false);
                            Toast.makeText(getApplicationContext(), "Status Updated", Toast.LENGTH_SHORT).show();
                            finish();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e(TAG, "onFailure: ",t );
                }
            });
        });

    }

    private void displayOrderDetails(SharedPreferences sharedPreferences,
                                     Order order, String storeIdList,
                                     OrderDeliveryDetailsResponse.OrderDeliveryDetailsData deliveryDetails) {

        @SuppressLint("SimpleDateFormat") SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeZones = sharedPreferences.getString("timezone", null);
        int  indexOfStore = Arrays.asList(storeIdList.split(" ")).indexOf(order.storeId);
        String currentTimezone = Arrays.asList(timeZones.split(" ")).get(indexOfStore);
        TimeZone timezone = TimeZone.getTimeZone(currentTimezone);
        Calendar calendar = new GregorianCalendar();
        try {
            calendar.setTime(dtf.parse(order.created));
            calendar.add(Calendar.HOUR_OF_DAY, (timezone.getRawOffset()/3600000));
        } catch (Exception e) {
            e.printStackTrace();
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
        dateValue.setText(formatter.format(calendar.getTime()));
        addressValue.setText(order.orderShipmentDetail.address);
        invoiceValue.setText(order.invoiceId);
        cityValue.setText(order.orderShipmentDetail.city);
        stateValue.setText(order.orderShipmentDetail.state);
        postcodeValue.setText(order.orderShipmentDetail.zipcode);
        nameValue.setText(order.orderShipmentDetail.receiverName);
        noteValue.setText(order.customerNotes);
        subtotalValue.setText(Double.toString(order.subTotal));
        discount.setText(Double.toString(order.appliedDiscount));
        serviceChargesValue.setText(Double.toString(order.storeServiceCharges));
        deliveryChargesValue.setText(Double.toString(order.deliveryCharges));
        deliveryDiscount.setText(Double.toString(order.deliveryDiscount));
        billingTotal.setText(Double.toString(order.total));

        if(section.equals("sent") && deliveryDetails != null){
            deliveryDetailsView.setVisibility(View.VISIBLE);
            deliveryDetailsDivider.setVisibility(View.VISIBLE);
            deliveryProvider.setText(deliveryDetails.provider.name);
            driverName.setText(deliveryDetails.name);
            driverContactNumber.setText(deliveryDetails.phoneNumber);
            String link = "<a color=\"#1DA1F2\" href=\""+deliveryDetails.trackingUrl+"\">Click Here</a>";
            new SpannableString(link).setSpan(
                    new BackgroundColorSpan( getColor(R.color.twitter_blue)), 0, link.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            trackingLink.setText(Html.fromHtml(link), TextView.BufferType.SPANNABLE);
            trackingLink.setMovementMethod(LinkMovementMethod.getInstance());
            Spannable spannableTrackingLink = (Spannable) trackingLink.getText();
            spannableTrackingLink.setSpan(new ForegroundColorSpan(getColor(R.color.twitter_blue)),0,spannableTrackingLink.length(),0);
        }

        if(order.orderShipmentDetail.storePickup)
            pickup.setBackgroundResource(R.drawable.ic_check_circle_black_24dp);
        else
            pickup.setBackgroundResource(R.drawable.ic_highlight_off_black_24dp);

        recyclerView = findViewById(R.id.order_items);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

    }

    private void initAppBar(SharedPreferences sharedPreferences, Order order, String storeIdList) {

        String encodedImage = sharedPreferences.getString("logoImage-"+order.storeId, null);
        ImageView storeLogo = findViewById(R.id.storeLogoDetails);
        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        ImageView logout = toolbar.findViewById(R.id.app_bar_logout);

        if(storeIdList.split(" ").length-1 > 1)
        {
            if(encodedImage != null)
                Utility.decodeAndSetImage(storeLogo, encodedImage);
            else{
                storeLogo.setVisibility(View.GONE);
                storeLogoText.setVisibility(View.VISIBLE);
                storeLogoText.setText(sharedPreferences.getString(order.storeId+"-name", null));
            }
        }
        else{
            storeLogo.setVisibility(View.GONE);
            storeLogoText.setVisibility(View.GONE);
        }
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                setResult(RESULT_OK, new Intent().putExtra("finish", 1));
                Intent intent = new Intent(getApplicationContext(), ChooseStore.class);
                FirebaseMessaging.getInstance().unsubscribeFromTopic(sharedPreferences.getString("storeId", null));
                sharedPreferences.edit().remove("storeId").apply();
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                */
                finish();
            }
        });
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_OK, new Intent().putExtra("finish", 1));
                Intent intent = new Intent(getApplicationContext(), Login.class);
                String storeIdList = sharedPreferences.getString("storeIdList", null);
                if(storeIdList != null )
                {
                    for(String storeId : storeIdList.split(" ")){
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(storeId);
                    }
                }
                sharedPreferences.edit().clear().apply();
                startActivity(intent);
                finish();
            }
        });

        ImageView settings = toolbar.findViewById(R.id.app_bar_settings);
        settings.setOnClickListener(view -> {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
        });

    }

    private void initViews() {
        dateValue = findViewById(R.id.invoice_tv_date_value);
        addressValue = findViewById(R.id.address_shipment_value);
        invoiceValue = findViewById(R.id.invoice_tv_invNumber_value);
        cityValue = findViewById(R.id.address_city_value);
        stateValue = findViewById(R.id.address_state_value);
        postcodeValue = findViewById(R.id.address_postcode_value);
        nameValue = findViewById(R.id.address_name_value);
        noteValue = findViewById(R.id.address_note_value);
        subtotalValue = findViewById(R.id.billing_subtotal_value);
        discount = findViewById(R.id.billing_discount_value);
        serviceChargesValue = findViewById(R.id.billing_service_charges_value);
        deliveryChargesValue = findViewById(R.id.billing_delivery_charges_value);
        deliveryDiscount = findViewById(R.id.billing_delivery_charges_discount_value);
        billingTotal = findViewById(R.id.billing_total_value);
        pickup = findViewById(R.id.address_is_pickup);
        process = findViewById(R.id.btn_process);
        print = findViewById(R.id.btn_print);
        storeLogo = findViewById(R.id.storeLogoDetails);
        print.setVisibility(View.GONE);
        process = findViewById(R.id.btn_process);
        process.setVisibility(View.GONE);
        deliveryProvider = findViewById(R.id.delivery_by_value);
        driverName = findViewById(R.id.driver_value);
        driverContactNumber = findViewById(R.id.contact_value);
        trackingLink = findViewById(R.id.tracking_value);
        storeLogoText = findViewById(R.id.storeLogoDetailsText);
        toolbar = findViewById(R.id.toolbar);
        deliveryDetailsView = findViewById(R.id.delivery_details);
        deliveryDetailsDivider = findViewById(R.id.divide3);
        setSupportActionBar(toolbar);


        //setup progress indicator
        progressDialog = new Dialog(this);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);
    }
}