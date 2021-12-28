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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.symplified.order.adapters.ItemsAdapter;
import com.symplified.order.apis.DeliveryApi;
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

public class OrderDetailsActivity extends AppCompatActivity {
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
    private boolean hasDeliveryDetails;

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

        if(getIntent().getExtras().containsKey("hasDeliveryDetails")){
            hasDeliveryDetails = getIntent().getBooleanExtra("hasDeliveryDetails", false);
        }

        //initialize all views
        initViews();
        nextStatus = "";

        //get details of selected order from previous activity
        Bundle data = getIntent().getExtras();
        Order order = (Order) data.getSerializable("selectedOrder");

        //get Delivery Driver details from previous activity
//        OrderDeliveryDetailsResponse.OrderDeliveryDetailsData driverDetails;
//        driverDetails = (OrderDeliveryDetailsResponse.OrderDeliveryDetailsData) data.getSerializable("deliveryDetails");

        //get base url for api calls
        BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);

        //initialize and setup app bar
        String storeIdList = sharedPreferences.getString("storeIdList", null);
        initAppBar(sharedPreferences, order, storeIdList);

        //get list of items in order
        getOrderItems(order);

        //get current order status details of the order
        getOrderStatusDetails(order);
        Log.i(TAG, "onCreate: "+order.toString());

        //display all order details to relevant fields
        displayOrderDetails(sharedPreferences, order, storeIdList);
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
                    progressDialog.dismiss();
                }


            }

            @Override
            public void onFailure(Call<ItemResponse> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Failed to retrieve items", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
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

        Call<ResponseBody> processOrder = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.fromString(nextStatus)), order.id);

        process.setOnClickListener(view -> {
            onProcessButtonClick(processOrder);
        });

    }

    private void onProcessButtonClick(Call<ResponseBody> processOrder) {
        progressDialog.show();
        processOrder.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.isSuccessful()){
//                    progressDialog.dismiss();
                    try {
                        Log.i(TAG, "onResponse: "+response.raw().toString());
                        Order.UpdatedOrder currentOrder = new Gson().fromJson(response.body().string(), Order.UpdatedOrder.class);
                        process.setText(Utility.removeUnderscores(currentOrder.data.completionStatus));
                        process.setEnabled(false);
                        process.setClickable(false);
                        Toast.makeText(getApplicationContext(), "Status Updated", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                        finish();
                    } catch (IOException e) {
                        progressDialog.dismiss();
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(OrderDetailsActivity.this, "Check your internet connection !", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                Log.e(TAG, "onFailure: ",t );
            }
        });

    }

    private void displayOrderDetails(SharedPreferences sharedPreferences,
                                     Order order, String storeIdList
//                                     OrderDeliveryDetailsResponse.OrderDeliveryDetailsData deliveryDetails
    ) {
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

//        && deliveryDetails != null
        if((section.equals("sent") || section.equals("pickup")) && hasDeliveryDetails ){
            setDriverDeliveryDetails(order, sharedPreferences);
//            deliveryDetailsView.setVisibility(View.VISIBLE);
//            deliveryDetailsDivider.setVisibility(View.VISIBLE);
//            deliveryProvider.setText(deliveryDetails.provider.name);
//            driverName.setText(deliveryDetails.name);
//            driverContactNumber.setText(deliveryDetails.phoneNumber);
//            String link = "<a color=\"#1DA1F2\" href=\""+deliveryDetails.trackingUrl+"\">Click Here</a>";
//            new SpannableString(link).setSpan(
//                    new BackgroundColorSpan( getColor(R.color.twitter_blue)), 0, link.length(),
//                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
//            trackingLink.setText(Html.fromHtml(link), TextView.BufferType.SPANNABLE);
//            trackingLink.setMovementMethod(LinkMovementMethod.getInstance());
//            Spannable spannableTrackingLink = (Spannable) trackingLink.getText();
//            spannableTrackingLink.setSpan(new ForegroundColorSpan(getColor(R.color.twitter_blue)),0,spannableTrackingLink.length(),0);
        }

        if(order.orderShipmentDetail.storePickup)
            pickup.setBackgroundResource(R.drawable.ic_check_circle_black_24dp);
        else
            pickup.setBackgroundResource(R.drawable.ic_highlight_off_black_24dp);

        recyclerView = findViewById(R.id.order_items);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration mDividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(mDividerItemDecoration);

    }

    private void initAppBar(SharedPreferences sharedPreferences, Order order, String storeIdList) {

        String encodedImage = sharedPreferences.getString("logoImage-"+order.storeId, null);
        ImageView storeLogo = findViewById(R.id.storeLogoDetails);
        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        ImageView logout = toolbar.findViewById(R.id.app_bar_logout);

        if(storeIdList.split(" ").length > 1)
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
        home.setOnClickListener(view -> {
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
        });
        logout.setOnClickListener(view -> {
            setResult(RESULT_OK, new Intent().putExtra("finish", 1));
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            String storeIdList1 = sharedPreferences.getString("storeIdList", null);
            if(storeIdList1 != null )
            {
                for(String storeId : storeIdList1.split(" ")){
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(storeId);
                }
            }
            sharedPreferences.edit().clear().apply();
            startActivity(intent);
            finish();
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
        deliveryDetailsView = findViewById(R.id.delivery_details);
        deliveryDetailsDivider = findViewById(R.id.divide3);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        //setup progress indicator
        progressDialog = new Dialog(this);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);
    }

    private void setDriverDeliveryDetails(Order order, SharedPreferences sharedPreferences) {

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
                if (response.isSuccessful()) {
                    deliveryDetailsView.setVisibility(View.VISIBLE);
                    deliveryDetailsDivider.setVisibility(View.VISIBLE);
                    deliveryProvider.setText(response.body().data.provider.name);
                    driverName.setText(response.body().data.name);
                    driverContactNumber.setText(response.body().data.phoneNumber);
                    String link = "<a color=\"#1DA1F2\" href=\""+response.body().data.trackingUrl+"\">Click Here</a>";
                    new SpannableString(link).setSpan(
                            new BackgroundColorSpan( getColor(R.color.twitter_blue)), 0, link.length(),
                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    trackingLink.setText(Html.fromHtml(link), TextView.BufferType.SPANNABLE);
                    trackingLink.setMovementMethod(LinkMovementMethod.getInstance());
                    Spannable spannableTrackingLink = (Spannable) trackingLink.getText();
                    spannableTrackingLink.setSpan(new ForegroundColorSpan(getColor(R.color.twitter_blue)),0,spannableTrackingLink.length(),0);

                }
            }

            @Override
            public void onFailure(Call<OrderDeliveryDetailsResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: ",t );
                progressDialog.dismiss();
            }
        });


    }

}