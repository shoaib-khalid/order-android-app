package com.symplified.order;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.firebase.FirebaseHelper;
import com.symplified.order.models.Store.Store;
import com.symplified.order.models.Store.StoreResponse;
import com.symplified.order.models.asset.Asset;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.services.DownloadImageTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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