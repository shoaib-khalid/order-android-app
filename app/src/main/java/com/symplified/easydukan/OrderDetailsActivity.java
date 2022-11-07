package com.symplified.easydukan;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.symplified.easydukan.adapters.ItemsAdapter;
import com.symplified.easydukan.apis.DeliveryApi;
import com.symplified.easydukan.apis.OrderApi;
import com.symplified.easydukan.enums.Status;
import com.symplified.easydukan.models.item.Item;
import com.symplified.easydukan.models.item.ItemResponse;
import com.symplified.easydukan.models.order.Order;
import com.symplified.easydukan.models.order.OrderDeliveryDetailsResponse;
import com.symplified.easydukan.utils.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
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
    private TextView deliveryProvider, driverName, driverContactNumber, trackingLink, headerOrginalQty, customerPhoneNumber, orderDeliveryDetailLabel, orderDeliveryDetailValue;
    private Button process, print, cancelOrder, editOrder;
    private ImageView pickup, storeLogo, phoneIcon, phoneIconCustomer;
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
    private boolean isEdited;
    private Order order;
    private List<Item> items = new ArrayList<>();
    private ItemsAdapter itemsAdapter;

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
        order = (Order) data.getSerializable("selectedOrder");

        itemsAdapter.order = order;

        editOrder.setOnClickListener(view -> {
            if(!order.isRevised){
                editOrderItem(order);
            }else{
                Toast.makeText(OrderDetailsActivity.this, "Order already revised !", Toast.LENGTH_SHORT).show();
//                editOrder.setEnabled(false);
//                editOrder.setVisibility(View.GONE);
            }
        });

        Log.e(TAG, "onCreateORDERID: " + order.id);

        //set Cancel Order buttton click listener
        cancelOrder.setOnClickListener(view -> onCancelOrderButtonClick(order));

        if(section != null && section.equals("new")){
            cancelOrder.setVisibility(View.VISIBLE);
            if(!order.isRevised){
                editOrder.setVisibility(View.VISIBLE);
            }
        }

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

        progressDialog.show();
        itemResponseCall.clone().enqueue(new Callback<ItemResponse>() {
            @Override
            public void onResponse(Call<ItemResponse> call, Response<ItemResponse> response) {

                if (response.isSuccessful()) {
                    if (order.isRevised) {
                        headerOrginalQty.setVisibility(View.VISIBLE);
                    }
                    items = response.body().data.content;
                    itemsAdapter.setItems(items);
                    recyclerView.setAdapter(itemsAdapter);
                    itemsAdapter.notifyDataSetChanged();
                    progressDialog.dismiss();
                    Log.i("get-orders", "Order items set");
                }
            }

            @Override
            public void onFailure(Call<ItemResponse> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Failed to retrieve items", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onFailureItems: ", t);
                progressDialog.dismiss();
            }
        });
    }

    private void editOrderItem(Order order) {
        if (isEdited) {
            if(!order.isRevised){
                new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog__Center)
                        .setTitle("Update Order")
                        .setMessage("You have made changes to a confirmed order.\nThis action cannot be undone.\nDo you want to continue?")
                        .setNegativeButton("No", null)
                        .setPositiveButton("Yes", (dialogInterface, i) -> {
                            itemsAdapter.updateOrderItems(order, BASE_URL, progressDialog);
                            editOrder.setClickable(false);
                            editOrder.setEnabled(false);
                        }).show();
            }
        } else {
            new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog__Center)
                    .setTitle("Edit Order")
                    .setMessage("You are about to change a confirmed order.\nYou can only perform this once.\nAny extra money will be refunded to customer.")
                    .setNegativeButton("No", null)
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        isEdited = true;
//                        Toast.makeText(this, "edit enabled", Toast.LENGTH_SHORT).show();
                        itemsAdapter.editable = true;
                        itemsAdapter.notifyDataSetChanged();
                        headerOrginalQty.setVisibility(View.VISIBLE);
                        editOrder.setText(R.string.update_order);
                    }).show();
        }
    }

    private void updateOrderStatus(Order order) {

        //add headers required for api calls
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL+App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Call<ResponseBody> processOrder = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.fromString(nextStatus)), order.id);
        Log.e(TAG, "updateOrderStatus: nextStatus = " + nextStatus );
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
                        Toast.makeText(OrderDetailsActivity.this, "Something went wrong, Please retry !", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                        e.printStackTrace();
                    }
                }
                else{
                    Toast.makeText(OrderDetailsActivity.this, "Something went wrong, Please retry !", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    Log.e(TAG, "Unsuccessful onResponse: " + response.raw());
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
        Log.e(TAG, "displayOrderDetails: storeId = " + order.storeId );
        int  indexOfStore = Arrays.asList(storeIdList.split(" ")).indexOf(order.storeId);
        Log.e(TAG, "displayOrderDetails: index = " + indexOfStore + " stores = " + Arrays.asList(storeIdList.split(" ")));
        if(indexOfStore == -1){
            indexOfStore = 0;
        }
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
        customerPhoneNumber.setText(order.orderShipmentDetail.phoneNumber);
        noteValue.setText(order.customerNotes);
        subtotalValue.setText(Double.toString(order.subTotal));

        if(!order.orderShipmentDetail.storePickup && order.orderShipmentDetail.deliveryPeriodDetails != null){
            orderDeliveryDetailLabel.setVisibility(View.VISIBLE);
            orderDeliveryDetailValue.setVisibility(View.VISIBLE);
            orderDeliveryDetailValue.setText(order.orderShipmentDetail.deliveryPeriodDetails.name);
        }

        if(order.appliedDiscount != null){
            discount.setText(Double.toString(order.appliedDiscount));
        }else{
            discount.setText("0.0");
        }
        serviceChargesValue.setText(Double.toString(order.storeServiceCharges));

        if(order.deliveryCharges != null ){
            deliveryChargesValue.setText(Double.toString(order.deliveryCharges));
        }else {
            deliveryChargesValue.setText("0.0");
        }
        deliveryDiscount.setText(Double.toString(order.deliveryDiscount));
        billingTotal.setText(Double.toString(order.total));

        phoneIconCustomer.setOnClickListener(view -> {
            Intent callDriver = new Intent(Intent.ACTION_DIAL);
            callDriver.setData(Uri.parse("tel:" + order.orderShipmentDetail.phoneNumber));
            startActivity(callDriver);
        });

        if((section.equals("sent") || section.equals("pickup")) && hasDeliveryDetails ){
            setDriverDeliveryDetails(order, sharedPreferences);
        }

        if(order.orderShipmentDetail.storePickup) {
            pickup.setBackgroundResource(R.drawable.ic_check_circle_black_24dp);
        } else {
            pickup.setBackgroundResource(R.drawable.ic_highlight_off_black_24dp);
        }

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

        if (storeIdList.split(" ").length > 0) {
            if(encodedImage != null) {
                Utility.decodeAndSetImage(storeLogo, encodedImage);
            } else {
                storeLogo.setVisibility(View.GONE);
                storeLogoText.setVisibility(View.VISIBLE);
                storeLogoText.setText(sharedPreferences.getString(order.storeId+"-name", null));
            }
        } else {
            storeLogo.setVisibility(View.GONE);
            storeLogoText.setVisibility(View.GONE);
        }
        home.setOnClickListener(view -> {
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
        customerPhoneNumber = findViewById(R.id.address_phone_value);
        process = findViewById(R.id.btn_process);
        print = findViewById(R.id.btn_print);
        storeLogo = findViewById(R.id.storeLogoDetails);
        process = findViewById(R.id.btn_process);
        process.setVisibility(View.GONE);
        deliveryProvider = findViewById(R.id.delivery_by_value);
        driverName = findViewById(R.id.driver_value);
        driverContactNumber = findViewById(R.id.contact_value);
        phoneIcon = findViewById(R.id.address_icon_phone);
        phoneIconCustomer = findViewById(R.id.address_icon_phone_customer);
        trackingLink = findViewById(R.id.tracking_value);
        storeLogoText = findViewById(R.id.storeLogoDetailsText);
        deliveryDetailsView = findViewById(R.id.delivery_details);
        deliveryDetailsDivider = findViewById(R.id.divide3);
        cancelOrder = findViewById(R.id.btn_cancel_order);
        headerOrginalQty = findViewById(R.id.header_org_qty);

        orderDeliveryDetailLabel = findViewById(R.id.delivery_time_label);
        orderDeliveryDetailValue = findViewById(R.id.delivery_time_value);

        editOrder = findViewById(R.id.btn_edit_order);
        editOrder.setVisibility(View.GONE);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        isEdited = false;
        itemsAdapter = new ItemsAdapter();
        itemsAdapter.editable = false;
        itemsAdapter.context = this;
        itemsAdapter.sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);

        //setup progress indicator
        progressDialog = new Dialog(this);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);

        if (App.isPrinterConnected()) {
            print.setVisibility(View.VISIBLE);
        }
        print.setOnClickListener(view -> printReceipt());
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

                    phoneIcon.setOnClickListener(view -> {
                        Intent callDriver = new Intent(Intent.ACTION_DIAL);
                        callDriver.setData(Uri.parse("tel:" + response.body().data.phoneNumber));
                        startActivity(callDriver);
                    });

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

    public void onCancelOrderButtonClick(Order order){
        //add headers required for api calls
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL+App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Dialog dialog = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog__Center)
                .setTitle("Cancel Order")
                .setMessage("Do you really want to cancel this order ?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    Call<ResponseBody> processOrder = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.CANCELED_BY_MERCHANT), order.id);
                    progressDialog.show();
                    processOrder.clone().enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            progressDialog.dismiss();
                            if(response.isSuccessful()){
                                Toast.makeText(getApplicationContext(), "Order Cancelled", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(getApplicationContext(), "Check your internet connection !", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "onFailure: ", t);
                        }
                    });
                })
                .create();
        TextView title = dialog.findViewById(android.R.id.title);
        TextView message = dialog.findViewById(android.R.id.message);
        if(title != null && message != null){
            title.setTypeface(Typeface.DEFAULT_BOLD);
            message.setTextSize(14);
            message.setTypeface(Typeface.DEFAULT_BOLD);
        }
        dialog.show();
    }

    private void printReceipt() {
        try {
            App.getPrinter().printReceipt(order, items);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to print receipt", Toast.LENGTH_SHORT).show();
        }
    }
}