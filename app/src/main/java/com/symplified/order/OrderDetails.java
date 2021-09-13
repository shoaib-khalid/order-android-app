package com.symplified.order;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.symplified.order.adapters.ItemsAdapter;
import com.symplified.order.models.Item;
import com.symplified.order.models.order.Order;
import com.symplified.order.services.DateParser;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderDetails extends AppCompatActivity {
    private RecyclerView recyclerView;

    private TextView dateValue, invoiceValue, addressValue, cityValue, stateValue, postcodeValue, nameValue, noteValue, subtotalValue, serviceChargesValue, deliveryChargesValue;
    private Button process, print;

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

        process = findViewById(R.id.btn_process);
        print = findViewById(R.id.btn_print);

        Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new DateParser()).create();

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
        List<Item> items = new ArrayList<>();
        for(int i=0 ;i<5; i++)
            items.add(new Item("Burger", "Extra Cheese", "3", "22.50"));
        ItemsAdapter itemsAdapter = new ItemsAdapter(items);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(itemsAdapter);

    }
}