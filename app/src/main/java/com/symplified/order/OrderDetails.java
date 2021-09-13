package com.symplified.order;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.symplified.order.adapters.ItemsAdapter;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.Store.StoreResponse;
import com.symplified.order.models.item.ItemResponse;
import com.symplified.order.models.order.Order;
import com.symplified.order.services.DateParser;

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

    private TextView dateValue, invoiceValue, addressValue, cityValue, stateValue, postcodeValue, nameValue, noteValue, subtotalValue, serviceChargesValue, deliveryChargesValue;
    private Button process, print;
    private ImageView pickup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);
        Bundle data = getIntent().getExtras();
        Order order = (Order) data.getSerializable("selectedOrder");


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
        pickup = findViewById(R.id.address_is_pickup);

        process = findViewById(R.id.btn_process);
        print = findViewById(R.id.btn_print);

        Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new DateParser()).create();

        if(order.orderShipmentDetail.storePickup)
            pickup.setBackgroundResource(R.drawable.ic_check_circle_black_24dp);
        else
            pickup.setBackgroundResource(R.drawable.ic_highlight_off_black_24dp);


        dateValue.setText(gson.toJson(order.created).toString());
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


        recyclerView = findViewById(R.id.order_items);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Retrofit retrofit = new Retrofit.Builder().baseUrl(App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        OrderApi storeApiService = retrofit.create(OrderApi.class);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Call<ItemResponse> itemResponseCall = storeApiService.getItemsForOrder(headers, order.id);

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





//        recyclerView = findViewById(R.id.order_items);
////        ItemsAdapter itemsAdapter = new ItemsAdapter(items);
////        itemsAdapter.notifyDataSetChanged();
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        recyclerView.setAdapter(itemsAdapter);

    }
}