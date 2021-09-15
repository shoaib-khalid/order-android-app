package com.symplified.order;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.symplified.order.adapters.ItemsAdapter;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.enums.Status;
import com.symplified.order.models.asset.Asset;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.item.ItemResponse;
import com.symplified.order.models.order.Order;
import com.symplified.order.services.DateParser;
import com.symplified.order.services.DownloadImageTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OrderDetails extends AppCompatActivity {
    private RecyclerView recyclerView;

    private TextView dateValue, invoiceValue, addressValue, cityValue, stateValue, postcodeValue, nameValue, noteValue, subtotalValue, serviceChargesValue, deliveryChargesValue,billingTotal;
    private Button process, print;
    private ImageView pickup;
    private String section;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        Bundle data = getIntent().getExtras();
        Order order = (Order) data.getSerializable("selectedOrder");
        section = null;

        section = getIntent().getStringExtra("section");

        dateValue = findViewById(R.id.invoice_tv_date_value);
        addressValue = findViewById(R.id.address_shipment_value);
        invoiceValue = findViewById(R.id.invoice_tv_invNumber_value);
        cityValue = findViewById(R.id.address_city_value);
        stateValue = findViewById(R.id.address_state_value);
        postcodeValue = findViewById(R.id.address_postcode_value);
        nameValue = findViewById(R.id.address_name_value);
        noteValue = findViewById(R.id.address_note_value);
        subtotalValue = findViewById(R.id.billing_subtotal_value);
        serviceChargesValue = findViewById(R.id.billing_service_charges_value);
        deliveryChargesValue = findViewById(R.id.billing_delivery_charges_value);
        billingTotal = findViewById(R.id.billing_total_value);
        pickup = findViewById(R.id.address_is_pickup);
        process = findViewById(R.id.btn_process);
        print = findViewById(R.id.btn_print);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);

        ImageView home = toolbar.findViewById(R.id.app_bar_home);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(4, new Intent().putExtra("finish", 1));
                Intent intent = new Intent(getApplicationContext(), ChooseStore.class);
                sharedPreferences.edit().remove("storeId").apply();
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
//                finish();
            }
        });

        ImageView logout = toolbar.findViewById(R.id.app_bar_logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Login.class);
                sharedPreferences.edit().clear().apply();
                startActivity(intent);
//                finish();
            }
        });

        ImageView storeLogo = toolbar.findViewById(R.id.app_bar_logo);
        Retrofit retrofitLogo = new Retrofit.Builder().baseUrl(App.PRODUCT_SERVICE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        StoreApi storeApiSerivice = retrofitLogo.create(StoreApi.class);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Call<ResponseBody> responseLogo = storeApiSerivice.getStoreLogo(headers, sharedPreferences.getString("storeId", "McD"));

        responseLogo.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.isSuccessful()){
                    try {
                        Asset.AssetResponse responseBody = new Gson().fromJson(response.body().string(), Asset.AssetResponse.class);
                        new DownloadImageTask(storeLogo).execute(responseBody.data.logoUrl);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });


        if(order.orderShipmentDetail.storePickup)
            pickup.setBackgroundResource(R.drawable.ic_check_circle_black_24dp);
        else
            pickup.setBackgroundResource(R.drawable.ic_highlight_off_black_24dp);


        dateValue.setText(order.created);
        addressValue.setText(order.orderShipmentDetail.address);
        invoiceValue.setText(order.invoiceId);
        cityValue.setText(order.orderShipmentDetail.city);
        stateValue.setText(order.orderShipmentDetail.state);
        postcodeValue.setText(order.orderShipmentDetail.zipcode);
        nameValue.setText(order.orderShipmentDetail.receiverName);
        noteValue.setText(order.customerNotes);
        subtotalValue.setText(Double.toString(order.subTotal));
        serviceChargesValue.setText(Double.toString(order.storeServiceCharges));
        deliveryChargesValue.setText(Double.toString(order.deliveryCharges));
        billingTotal.setText(Double.toString(order.total));

        process = findViewById(R.id.btn_process);


        recyclerView = findViewById(R.id.order_items);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        Retrofit retrofit = new Retrofit.Builder().baseUrl(App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi orderApiService = retrofit.create(OrderApi.class);

        Call<ItemResponse> itemResponseCall = orderApiService.getItemsForOrder(headers, order.id);

        boolean isPickup = getIntent().getBooleanExtra("pickup",false);
        ItemsAdapter itemsAdapter = new ItemsAdapter();
        List<Item> items = new ArrayList<>();
        itemResponseCall.clone().enqueue(new Callback<ItemResponse>() {
            @Override
            public void onResponse(Call<ItemResponse> call, Response<ItemResponse> response) {

                if(response.isSuccessful())
                {
                    Log.e("TAG", "onResponse: "+order.id, new Error() );
                    itemsAdapter.setItems(response.body().data.content);
                    itemsAdapter.notifyDataSetChanged();
                    recyclerView.setAdapter(itemsAdapter);
                }


            }

            @Override
            public void onFailure(Call<ItemResponse> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Failed to retrieve items", Toast.LENGTH_SHORT).show();
            }
        });

//        Call<ResponseBody> processOrder = storeApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.BEING_PREPARED), order.id);


        if(section.equals("new")) {
            Call<ResponseBody> processOrder = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.BEING_PREPARED), order.id);
            process.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast.makeText(getApplicationContext(), "process clicked", Toast.LENGTH_SHORT).show();
                processOrder.clone().enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if(response.isSuccessful()){

                            try {
                                Order.UpdatedOrder currentOrder = new Gson().fromJson(response.body().string(), Order.UpdatedOrder.class);
                                if(currentOrder.data.completionStatus.toString().equals(Status.BEING_PREPARED.toString()))
                                    process.setText("Being Prepared");
                                else
                                    process.setText("Failed");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            try {
                                Log.e("TAG", "onResponse: "+response.errorBody().string(), new Error() );
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast.makeText(getApplicationContext(), "Not Found", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        }
        else if (section.equals("processed")) {
            Call<ResponseBody> processOrder ;
//                    = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.AWAITING_PICKUP), order.id);

            if(!isPickup)
                processOrder = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.BEING_DELIVERED), order.id);
            else
                processOrder = orderApiService.updateOrderStatus(headers, new Order.OrderUpdate(order.id, Status.DELIVERED_TO_CUSTOMER), order.id);


            Log.e("PICKUPMSG", "onCreate: isPickup :"+isPickup, new Error() );

            process.setText("Being Perpared");

            process.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast.makeText(getApplicationContext(), "being delivered clicked", Toast.LENGTH_SHORT).show();
                processOrder.clone().enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if(response.isSuccessful()){

                            try {
//                                Log.e("TAG", "onResponse: "+response.body().string(), new Error() );
                                Order.UpdatedOrder currentOrder = new Gson().fromJson(response.body().string(), Order.UpdatedOrder.class);
                                Log.e("PICKUPMSG", "onResponse: "+currentOrder.data.completionStatus.toString(),new Error() );
                                if(currentOrder.data.completionStatus.toString().equals(Status.DELIVERED_TO_CUSTOMER.toString()))
                                    process.setText("Delivered");

                                else if(currentOrder.data.completionStatus.toString().equals(Status.BEING_DELIVERED.toString())){
                                    process.setText("Being Delivered");
                                }
                                else
                                    process.setText("Failed");

//                                Log.e("TAG", "response code: "+response.code(), new Error() );
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            try {
                                Log.e("TAG", "isPickup : "+isPickup+response.errorBody().string(), new Error() );
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast.makeText(getApplicationContext(), "Not Found", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        }
        else if(section.equals("sent"))
            process.setVisibility(View.INVISIBLE);


        Log.e("TAG", "onCreate: Seciton : "+section, new Error() );

//        process.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                Toast.makeText(getApplicationContext(), "process clicked", Toast.LENGTH_SHORT).show();
//                processOrder.clone().enqueue(new Callback<ResponseBody>() {
//                    @Override
//                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                        if(response.isSuccessful()){
//
//                            try {
////                                Log.e("TAG", "onResponse: "+response.body().string(), new Error() );
//                                Order.UpdatedOrder currentOrder = new Gson().fromJson(response.body().string(), Order.UpdatedOrder.class);
//                                if(currentOrder.data.completionStatus.toString().equals(Status.BEING_PREPARED.toString()))
//                                    process.setText("Being Prepared");
//
////                                Log.e("TAG", "response code: "+response.code(), new Error() );
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        else {
//                            try {
//                                Log.e("TAG", "onResponse: "+response.errorBody().string(), new Error() );
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                    }
//
//                    @Override
//                    public void onFailure(Call<ResponseBody> call, Throwable t) {
//                        Toast.makeText(getApplicationContext(), "Not Found", Toast.LENGTH_SHORT).show();
//                    }
//                });
//            }
//        });





//        recyclerView = findViewById(R.id.order_items);
////        ItemsAdapter itemsAdapter = new ItemsAdapter(items);
////        itemsAdapter.notifyDataSetChanged();
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        recyclerView.setAdapter(itemsAdapter);

    }
}