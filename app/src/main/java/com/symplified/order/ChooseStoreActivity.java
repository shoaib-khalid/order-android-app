package com.symplified.order;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.networking.ServiceGenerator;

import java.util.HashMap;
import java.util.Map;

//not used anymore
public class ChooseStoreActivity extends AppCompatActivity {

    private String BASE_URL;
    private Toolbar toolbar;
    private Dialog progressDialog;
    private Map<String, String> headers;
    StoreApi storeApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_store);

        progressDialog = new Dialog(this, R.style.Theme_SymplifiedOrderUpdate);
        progressDialog.setContentView(R.layout.progress_dialog);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        sharedPreferences.edit().remove("logoImage").apply();
        BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);

        headers = new HashMap<>();

        storeApiService = ServiceGenerator.createStoreService();

        sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        String clientId = sharedPreferences.getString("ownerId", null);

        if (null == clientId) {
            Log.d("Client-ID", "onCreate: client id is null");
        }
    }
}