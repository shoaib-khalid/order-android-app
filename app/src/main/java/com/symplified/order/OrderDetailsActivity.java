package com.symplified.order;

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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.Gson;
import com.symplified.order.adapters.ItemAdapter;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.databinding.ActivityOrderDetailsBinding;
import com.symplified.order.enums.Status;
import com.symplified.order.models.item.ItemResponse;
import com.symplified.order.models.order.Order;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OrderDetailsActivity extends NavbarActivity {
    private RecyclerView recyclerView;

    private TextView customerName, invoice, date, orderTotal, orderTotal2, phoneNumber, address, subTotal, deliveryCharges, orderStatus, deliveryDiscount, discount, serviceCharges;
//    private TextView deliveryProvider, driverName, driverContactNumber, trackingLink, headerOrginalQty, customerPhoneNumber, orderDeliveryDetailLabel, orderDeliveryDetailValue;
    private Button acceptButton, cancelButton, statusButton, trackButton, callButton, navigateButton;
    private LinearLayout linearLayout;
//    private ImageView call, addressIcon;
    private RelativeLayout rlDiscount, rlServiceCharges, rlDeliveryDiscount;
    private String section;
    private Toolbar toolbar;
    private Dialog progressDialog;
    public static String TAG = "ProcessOrder";
    private String BASE_URL;
    private CircularProgressIndicator progressIndicator;
//    private RelativeLayout deliveryDetailsView;
//    private View deliveryDetailsDivider;
    private String nextStatus;
//    private boolean hasDeliveryDetails;
    private boolean isEdited;
    private ItemAdapter itemAdapter;
    private ActivityOrderDetailsBinding binding;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);

        //change theme for staging mode
//        if (sharedPreferences.getBoolean("isStaging", false))
//            setTheme(R.style.Theme_SymplifiedOrderUpdate_Test);
        binding = ActivityOrderDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle data = getIntent().getExtras();
        Order order = (Order) data.getSerializable("selectedOrder");
        section = null;
        section = getIntent().getStringExtra("section");
        initToolbar(order);

        //initialize all views
        initViews();

        BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);
        nextStatus = "";

        //get details of selected order from previous activity

//        itemAdapter.order = order;

        Log.e(TAG, "onCreateORDERID: " + order.id);

        //set Cancel Order buttton click listener
        cancelButton.setOnClickListener(view -> onCancelOrderButtonClick(order));

        acceptButton.setOnClickListener(view -> getOrderStatusDetails(order));

        statusButton.setOnClickListener(view -> {getOrderStatusDetails(order);});

        if (section != null) {
            switch (section) {
                case "new":
                    acceptButton.setVisibility(View.VISIBLE);
                    cancelButton.setVisibility(View.VISIBLE);
                    break;
                case "ongoing":
                    statusButton.setVisibility(View.VISIBLE);
                    switch (order.completionStatus) {
                        case "BEING_PREPARED":
                            statusButton.setText("Rready for pickup");
                            break;
                        case "AWAITING_PICKUP":
                            statusButton.setText("Dispatched");
                            break;
                        case "BEING_DELIVERED":
                            if (!(order.orderShipmentDetail.trackingUrl == null || order.orderShipmentDetail.trackingUrl.equals(""))) {
                                trackButton.setVisibility(View.VISIBLE);
                            }
                            statusButton.setText("Delivered");
                            break;
                    }
                    break;
                case "past":
                    linearLayout.setVisibility(View.VISIBLE);
                    switch (order.completionStatus) {
                        case "DELIVERED_TO_CUSTOMER":
                            orderStatus.setText("Order Delivered");
                            orderStatus.setTextColor(ContextCompat.getColor(this, R.color.sf_accept_button));
                            break;
                        case "CANCELED_BY_MERCHANT":
                        case "CANCELED_BY_CUSTOMER":
                            orderStatus.setText("Order Cancelled");
                            orderStatus.setTextColor(ContextCompat.getColor(this, R.color.sf_cancel_button));
                            break;
                    }
                    break;
            }
        }

        //initialize and setup app bar
//        String storeIdList = sharedPreferences.getString("storeIdList", null);
//        initAppBar(sharedPreferences, order, storeIdList);

        //get list of items in order
        getOrderItems(order);

        //get current order status details of the order
//        getOrderStatusDetails(order);
        Log.i(TAG, "onCreate: " + order.toString());

        //display all order details to relevant fields
        displayOrderDetails(sharedPreferences, order);
    }

    public void initToolbar(Order order) {
        TextView title = toolbar.findViewById(R.id.app_bar_title);
        switch (section) {
            case "new":
                title.setText("New Order Details");
                break;
            case "ongoing":
                title.setText("Ongoing Order Details");
                break;
            case "past":
                switch (order.completionStatus) {
                    case "DELIVERED_TO_CUSTOMER":
                        title.setText("Completed Order Details");
                        break;
                    case "CANCELED_BY_MERCHANT":
                    case "CANCELED_BY_CUSTOMER":
                        title.setText("Cancelled Order Details");
                        break;
                }
                break;
        }
        ImageView home = toolbar.findViewById(R.id.app_bar_home);

        home.setImageDrawable(getDrawable(R.drawable.ic_arrow_back_black_24dp));
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OrderDetailsActivity.super.onBackPressed();
                finish();
            }
        });
    }

    private void getOrderStatusDetails(Order order) {

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
                                    updateOrderStatus(order);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(OrderDetailsActivity.this, R.string.request_failure, Toast.LENGTH_SHORT).show();
                }
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(OrderDetailsActivity.this, R.string.no_internet, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onFailure: ", t);
            }
        });

    }

    private void getOrderItems(Order order) {
        //add headers required for api calls
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
                    Log.e("TAG", "onResponse: " + order.id, new Error());
//                    editOrder.setVisibility(View.VISIBLE);
                    itemAdapter = new ItemAdapter(response.body().data.content, order, getApplicationContext());
                    recyclerView.setAdapter(itemAdapter);
                    itemAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(OrderDetailsActivity.this, R.string.request_failure, Toast.LENGTH_SHORT).show();
                }
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<ItemResponse> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Failed to retrieve items. " + R.string.no_internet, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onFailureItems: ", t);
                progressDialog.dismiss();
            }
        });
    }

    private void updateOrderStatus(Order order) {

        //add headers required for api calls
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Call<ResponseBody> processOrder = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.fromString(nextStatus)), order.id);

        onProcessButtonClick(processOrder);

    }

    private void onProcessButtonClick(Call<ResponseBody> processOrder) {
        progressDialog.show();
        processOrder.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
//                    progressDialog.dismiss();
                    try {
                        Log.i(TAG, "onResponse: " + response.raw().toString());
                        Order.UpdatedOrder currentOrder = new Gson().fromJson(response.body().string(), Order.UpdatedOrder.class);
                        Toast.makeText(getApplicationContext(), "Status Updated", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), OrdersActivity.class);
                        startActivity(intent);
                        finish();
                    } catch (IOException e) {
                        progressDialog.dismiss();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(OrderDetailsActivity.this, R.string.request_failure, Toast.LENGTH_SHORT).show();
                }
                progressDialog.dismiss();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(OrderDetailsActivity.this, R.string.no_internet, Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                Log.e(TAG, "onFailure: ", t);
            }
        });

    }

    private void displayOrderDetails(SharedPreferences sharedPreferences, Order order) {

        String currency = sharedPreferences.getString("currency", null);
        nextStatus = "";

        customerName.setText(order.orderShipmentDetail.receiverName);
        phoneNumber.setText(order.orderShipmentDetail.phoneNumber);
        String fullAddress = order.orderShipmentDetail.address+", "+order.orderShipmentDetail.city+", "+order.orderShipmentDetail.state+" "+order.orderShipmentDetail.zipcode;
        address.setText(fullAddress);
        invoice.setText(order.invoiceId);
        date.setText(order.created);
        orderTotal.setText(currency + " " + Double.toString(order.total));


        subTotal.setText(currency + " " + Double.toString(order.subTotal));
        if (order.appliedDiscount == null || order.appliedDiscount == 0.0) {
            rlDiscount.setVisibility(View.GONE);
        } else {
            discount.setText("- " + currency + " " + Double.toString(order.appliedDiscount));
        }
        deliveryCharges.setText(order.deliveryCharges != null ? currency + " " + Double.toString(order.deliveryCharges) : currency + " " + "0.0");
        orderTotal2.setText(currency + " " +Double.toString(order.total));
        if (order.deliveryDiscount == null || order.deliveryDiscount == 0.0) {
            rlDeliveryDiscount.setVisibility(View.GONE);
        } else {
            deliveryDiscount.setText("- " + currency + " " + Double.toString(order.deliveryDiscount));
        }
        if (order.storeServiceCharges == null || order.storeServiceCharges == 0.0) {
            rlServiceCharges.setVisibility(View.GONE);
        } else {
            serviceCharges.setText(currency + " " + Double.toString(order.storeServiceCharges));
        }
        callButton.setOnClickListener(view -> {
            Intent callDriver = new Intent(Intent.ACTION_DIAL);
            callDriver.setData(Uri.parse("tel:" + order.orderShipmentDetail.phoneNumber));
            startActivity(callDriver);
        });

//        @SuppressLint("SimpleDateFormat") SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        String timeZones = sharedPreferences.getString("timezone", null);
//        Log.e(TAG, "displayOrderDetails: storeId = " + order.storeId);
//        int indexOfStore = Arrays.asList(storeIdList.split(" ")).indexOf(order.storeId);
//        Log.e(TAG, "displayOrderDetails: index = " + indexOfStore + " stores = " + Arrays.asList(storeIdList.split(" ")));
//        if (indexOfStore == -1) {
//            indexOfStore = 0;
//        }
//        String currentTimezone = Arrays.asList(timeZones.split(" ")).get(indexOfStore);
//        TimeZone timezone = TimeZone.getTimeZone(currentTimezone);
//        Calendar calendar = new GregorianCalendar();
//        try {
//            calendar.setTime(dtf.parse(order.created));
//            calendar.add(Calendar.HOUR_OF_DAY, (timezone.getRawOffset() / 3600000));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
//        dateValue.setText(formatter.format(calendar.getTime()));
//        addressValue.setText(order.orderShipmentDetail.address);
//        invoiceValue.setText(order.invoiceId);
//        cityValue.setText(order.orderShipmentDetail.city);
//        stateValue.setText(order.orderShipmentDetail.state);
//        postcodeValue.setText(order.orderShipmentDetail.zipcode);
//        nameValue.setText(order.orderShipmentDetail.receiverName);
//        customerPhoneNumber.setText(order.orderShipmentDetail.phoneNumber);
//        noteValue.setText(order.customerNotes);
//        subtotalValue.setText(Double.toString(order.subTotal));
//
//        if (!order.orderShipmentDetail.storePickup && order.orderShipmentDetail.deliveryPeriodDetails != null) {
//            orderDeliveryDetailLabel.setVisibility(View.VISIBLE);
//            orderDeliveryDetailValue.setVisibility(View.VISIBLE);
//            orderDeliveryDetailValue.setText(order.orderShipmentDetail.deliveryPeriodDetails.name);
//        }
//
//        discount.setText(order.appliedDiscount != null ? Double.toString(order.appliedDiscount) : "0.0");
//        if (order.storeServiceCharges == 0.0) {
//            serviceChargesValue.setVisibility(View.GONE);
//            findViewById(R.id.billing_service_charges).setVisibility(View.GONE);
//        } else {
//            serviceChargesValue.setText(Double.toString(order.storeServiceCharges));
//        }
//
//        deliveryChargesValue.setText(order.deliveryCharges != null ? Double.toString(order.deliveryCharges) : "0.0");
//        deliveryDiscount.setText(order.deliveryDiscount != null ? Double.toString(order.deliveryDiscount) : "0.0");
//        billingTotal.setText(Double.toString(order.total));
//
//        phoneIconCustomer.setOnClickListener(view -> {
//            Intent callDriver = new Intent(Intent.ACTION_DIAL);
//            callDriver.setData(Uri.parse("tel:" + order.orderShipmentDetail.phoneNumber));
//            startActivity(callDriver);
//        });
//
////        && deliveryDetails != null
//        if ((section.equals("sent") || section.equals("pickup")) && hasDeliveryDetails) {
//            setDriverDeliveryDetails(order, sharedPreferences);
//        }
//
//        if (order.orderShipmentDetail.storePickup)
//            pickup.setBackgroundResource(R.drawable.ic_check_circle_black_24dp);
//        else
//            pickup.setBackgroundResource(R.drawable.ic_highlight_off_black_24dp);
//
        recyclerView = findViewById(R.id.order_items_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        DividerItemDecoration mDividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
//                DividerItemDecoration.VERTICAL);
//        recyclerView.addItemDecoration(mDividerItemDecoration);

    }

        private void initViews() {
        customerName = findViewById(R.id.order_row_name_value);
        invoice = findViewById(R.id.card_invoice_value);
        date = findViewById(R.id.order_date_value);
        orderTotal = findViewById(R.id.order_total_value);
        address = findViewById(R.id.address_value);
        subTotal = findViewById(R.id.subtotal_value);
        deliveryCharges = findViewById(R.id.delivery_by_value);
        discount = findViewById(R.id.discount_value);
        phoneNumber = findViewById(R.id.contact_value);
        address = findViewById(R.id.address_value);
        deliveryDiscount = findViewById(R.id.billing_delivery_charges_discount_value);
        serviceCharges = findViewById(R.id.service_charges_value);
        orderTotal2 = findViewById(R.id.order_total_value_);
        linearLayout = findViewById(R.id.layout_status);
        orderStatus = findViewById(R.id.order_status_value);
        cancelButton = findViewById(R.id.btn_cancel);
        acceptButton = findViewById(R.id.btn_accept);
        statusButton = findViewById(R.id.btn_status);
        trackButton = findViewById(R.id.btn_track_order);
        callButton = findViewById(R.id.btn_call);
        navigateButton = findViewById(R.id.btn_navigate);

        rlDiscount = findViewById(R.id.rl_discount);
        rlDeliveryDiscount = findViewById(R.id.rl_delivery_discount);
        rlServiceCharges = findViewById(R.id.rl_service_charges);

//        dateValue = findViewById(R.id.invoice_tv_date_value);
//        addressValue = findViewById(R.id.address_shipment_value);
//        invoiceValue = findViewById(R.id.invoice_tv_invNumber_value);
//        cityValue = findViewById(R.id.address_city_value);
//        stateValue = findViewById(R.id.address_state_value);
//        postcodeValue = findViewById(R.id.address_postcode_value);
//        nameValue = findViewById(R.id.address_name_value);
//        noteValue = findViewById(R.id.address_note_value);
//        subtotalValue = findViewById(R.id.billing_subtotal_value);
//        discount = findViewById(R.id.billing_discount_value);
//        serviceChargesValue = findViewById(R.id.billing_service_charges_value);
//        deliveryChargesValue = findViewById(R.id.billing_delivery_charges_value);
//        deliveryDiscount = findViewById(R.id.billing_delivery_charges_discount_value);
//        billingTotal = findViewById(R.id.billing_total_value);
//        pickup = findViewById(R.id.address_is_pickup);
//        customerPhoneNumber = findViewById(R.id.address_phone_value);
//        process = findViewById(R.id.btn_process);
//        print = findViewById(R.id.btn_print);
//        storeLogo = findViewById(R.id.storeLogoDetails);
//        print.setVisibility(View.GONE);
//        process = findViewById(R.id.btn_process);
//        process.setVisibility(View.GONE);
//        deliveryProvider = findViewById(R.id.delivery_by_value);
//        driverName = findViewById(R.id.driver_value);
//        driverContactNumber = findViewById(R.id.contact_value);
//        phoneIcon = findViewById(R.id.address_icon_phone);
//        phoneIconCustomer = findViewById(R.id.address_icon_phone_customer);
//        trackingLink = findViewById(R.id.tracking_value);
//        storeLogoText = findViewById(R.id.storeLogoDetailsText);
//        deliveryDetailsView = findViewById(R.id.delivery_details);
//        deliveryDetailsDivider = findViewById(R.id.divide3);
//        cancelOrder = findViewById(R.id.btn_cancel_order);
//        headerOrginalQty = findViewById(R.id.header_org_qty);

//        orderDeliveryDetailLabel = findViewById(R.id.delivery_time_label);
//        orderDeliveryDetailValue = findViewById(R.id.delivery_time_value);

//        editOrder = findViewById(R.id.btn_edit_order);
//        editOrder.setVisibility(View.GONE);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        isEdited = false;
        itemAdapter = new ItemAdapter();
//        itemsAdapter.editable = false;
        itemAdapter.context = this;
//        itemAdapter.sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);

        //setup progress indicator
        progressDialog = new Dialog(this);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);
    }

//    private void setDriverDeliveryDetails(Order order, SharedPreferences sharedPreferences) {
//
//        String BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);
//
//        Map<String, String> headers = new HashMap<>();
//        headers.put("Authorization", "Bearer Bearer accessToken");
//
//        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient())
//                .baseUrl(BASE_URL + App.DELIVERY_SERVICE_URL)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        DeliveryApi deliveryApiService = retrofit.create(DeliveryApi.class);
//
//        //12dc5195-5f03-42fd-94f0-f147dc4ced55
//        Call<OrderDeliveryDetailsResponse> deliveryDetailsResponseCall = deliveryApiService.getOrderDeliveryDetailsById(headers, order.id);
//
//        progressDialog.show();
//
//        deliveryDetailsResponseCall.clone().enqueue(new Callback<OrderDeliveryDetailsResponse>() {
//            @Override
//            public void onResponse(Call<OrderDeliveryDetailsResponse> call, Response<OrderDeliveryDetailsResponse> response) {
//                if (response.isSuccessful()) {
//                    deliveryDetailsView.setVisibility(View.VISIBLE);
//                    deliveryDetailsDivider.setVisibility(View.VISIBLE);
//                    deliveryProvider.setText(response.body().data.provider.name);
//                    driverName.setText(response.body().data.name);
//                    driverContactNumber.setText(response.body().data.phoneNumber);
//
//                    phoneIcon.setOnClickListener(view -> {
//                        Intent callDriver = new Intent(Intent.ACTION_DIAL);
//                        callDriver.setData(Uri.parse("tel:" + response.body().data.phoneNumber));
//                        startActivity(callDriver);
//                    });
//
//                    String link = "<a color=\"#1DA1F2\" href=\"" + response.body().data.trackingUrl + "\">Click Here</a>";
//                    new SpannableString(link).setSpan(
//                            new BackgroundColorSpan(getColor(R.color.twitter_blue)), 0, link.length(),
//                            Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
//                    trackingLink.setText(Html.fromHtml(link), TextView.BufferType.SPANNABLE);
//                    trackingLink.setMovementMethod(LinkMovementMethod.getInstance());
//                    Spannable spannableTrackingLink = (Spannable) trackingLink.getText();
//                    spannableTrackingLink.setSpan(new ForegroundColorSpan(getColor(R.color.twitter_blue)), 0, spannableTrackingLink.length(), 0);
//
//                } else {
//                    Toast.makeText(OrderDetailsActivity.this, R.string.request_failure, Toast.LENGTH_SHORT).show();
//                }
//                progressDialog.dismiss();
//            }
//
//            @Override
//            public void onFailure(Call<OrderDeliveryDetailsResponse> call, Throwable t) {
//                Log.e(TAG, "onFailure: ", t);
//                Toast.makeText(OrderDetailsActivity.this, R.string.request_failure, Toast.LENGTH_SHORT).show();
//                progressDialog.dismiss();
//            }
//        });
//
//
//    }

    public void onCancelOrderButtonClick(Order order) {
        //add headers required for api calls
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.ORDER_SERVICE_URL)
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
                            if (response.isSuccessful()) {
                                Toast.makeText(getApplicationContext(), "Order Cancelled", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(OrderDetailsActivity.this, R.string.request_failure, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(getApplicationContext(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "onFailure: ", t);
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

}