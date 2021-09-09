package com.symplified.order;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.symplified.order.adapters.StoreAdapter;

import java.util.ArrayList;
import java.util.List;

public class ChooseStore extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_store);
        RecyclerView recyclerView = findViewById(R.id.store_recycler);
        List<String> items = new ArrayList<>();

        for(int i=0; i<10; i++)
            items.add(new String("Symplified "+(i+1)));

        StoreAdapter storeAdapter = new StoreAdapter(items);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(storeAdapter);
    }
}