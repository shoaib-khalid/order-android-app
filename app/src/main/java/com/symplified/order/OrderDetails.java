package com.symplified.order;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;

import com.symplified.order.adapters.ItemsAdapter;
import com.symplified.order.models.Item;

import java.util.ArrayList;
import java.util.List;

public class OrderDetails extends AppCompatActivity {
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);
        recyclerView = findViewById(R.id.order_items);
        List<Item> items = new ArrayList<>();
        for(int i=0 ;i<5; i++)
            items.add(new Item("Burger", "Extra Cheese", "3", "22.50"));
        ItemsAdapter itemsAdapter = new ItemsAdapter(items);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(itemsAdapter);

    }
}