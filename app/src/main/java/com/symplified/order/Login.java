package com.symplified.order;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.gson.Gson;
import com.symplified.order.apis.LoginApi;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.firebase.FirebaseHelper;
import com.symplified.order.handlers.LogoHandler;
import com.symplified.order.models.HttpResponse;
import com.symplified.order.models.Store.Store;
import com.symplified.order.models.Store.StoreResponse;
import com.symplified.order.models.asset.Asset;
import com.symplified.order.models.login.LoginData;
import com.symplified.order.models.login.LoginRequest;
import com.symplified.order.models.login.LoginResponse;
import com.symplified.order.services.AlertService;
import com.symplified.order.services.DownloadImageTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Login extends AppCompatActivity{

    private Button login;
    private TextInputLayout email;
    private TextInputLayout password;
    private SharedPreferences sharedPreferences;
    private ImageView header;
    private Dialog progressDialog;
    private FirebaseRemoteConfig mRemoteConfig;
    private String testUser,testPass;
    private String BASE_URL;
    public static boolean isLogoImportComplete = false;

    private List<Store> stores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_SymplifiedOrderUpdate);
        mRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0).build();

        mRemoteConfig.setConfigSettingsAsync(configSettings);

        stores = new ArrayList<>();


//        Map<String, Object> defaults = new HashMap<>();
//        defaults.put("base_url", App.BASE_URL);
//        defaults.put("test_user", "qa_user");
//        defaults.put("test_pass", "qa@kalsym");

        mRemoteConfig.setDefaultsAsync(R.xml.defaults);

        mRemoteConfig.fetch(0);
        mRemoteConfig.activate();
        BASE_URL = mRemoteConfig.getString("base_url");

        if(BASE_URL.equals("")){
            BASE_URL = App.BASE_URL;
        }

        Log.e("TAG", "BASE_URL : "+ BASE_URL.length(), new Error() );

        testUser = mRemoteConfig.getString("test_user");
        testPass = mRemoteConfig.getString("test_pass");

        if(testUser.equals("") || testPass.equals(""))
        {
            testUser = "qa_user";
            testPass = "qa@kalsym";
        }

        Log.e("TAG", "test credentials  : user : "+ testUser+" : password : "+testPass, new Error() );

        sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        setContentView(R.layout.activity_login);
//        getSupportActionBar().hide();
        progressDialog = new Dialog(this);
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);

//        sharedPreferences.edit().putString("base_url", BASE_URL).apply();


        login = findViewById(R.id.btn_login);
        email = findViewById(R.id.tv_email);
        password = findViewById(R.id.tv_password);
        header = findViewById(R.id.iv_header);

//        header.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if(App.BASE_URL.contains("symplified.it")
//                        && App.ORDER_SERVICE_URL.contains("symplified.it")
//                        && App.PRODUCT_SERVICE_URL.contains("symplified.it")){
//                    Toast.makeText(Login.this, "Staging Version", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });


        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(email.getEditText().getText().toString().equals(testUser) && password.getEditText().getText().toString().equals(testPass))
                {
                    BASE_URL = "https://api.symplified.it/";
                    sharedPreferences.edit().putBoolean("isStaging", true).apply();
                    sharedPreferences.edit().putString("base_url", BASE_URL).apply();
                    Log.e("TAG", "BASE_URL : "+ BASE_URL, new Error() );
                    Toast.makeText(getApplicationContext(), "Switched to staging", Toast.LENGTH_SHORT).show();
                }
                progressDialog.show();


                Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).client(new OkHttpClient()).baseUrl(BASE_URL+App.USER_SERVICE_URL)
                        .addConverterFactory(GsonConverterFactory.create()).build();

                LoginApi loginApiService = retrofit.create(LoginApi.class);

                Call<LoginResponse> loginResponse = loginApiService.login("application/json",
                        new LoginRequest(email.getEditText().getText().toString(), password.getEditText().getText().toString()));

                loginResponse.clone().enqueue(new Callback<LoginResponse>() {

                    String loginMessage = "";
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {


                        if(response.isSuccessful())
                        {
                            LoginData res = response.body().data;
                            Log.d("TAG", "Login Response : "+ response.body().data.toString());
                            loginMessage = "Logged In Successfully !";
//                            Toast.makeText(getApplicationContext(), "Logged In Successfully !", Toast.LENGTH_SHORT).show();
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            Log.e("ALLVALUES", "onResponse: "+ sharedPreferences.getAll(), new Error() );
                            if(!sharedPreferences.contains("isLoggedIn") || sharedPreferences.getInt("isLoggedIn", -1) == 0)
                            {
                                editor.putString("email", res.session.username);
                                editor.putString("accessToken", res.session.accessToken);
                                editor.putString("refreshToken", res.session.refreshToken);
                                editor.putString("ownerId", res.session.ownerId);
                                editor.putString("expiry", res.session.expiry.toGMTString());
                                editor.putInt("isLoggedIn", 1);
                                editor.apply();
                                sharedPreferences.edit().putString("base_url", BASE_URL).apply();
                                getStoresAndRegister(sharedPreferences);
                                Log.e("getAllStore", "onResponse: " + stores, new Error() );
//                                Toast.makeText(getApplicationContext(), ""+stores.size(), Toast.LENGTH_SHORT).show();


                            }
//                            Toast.makeText(getApplicationContext(), "ownerID : "+sharedPreferences.getString("ownerId", null), Toast.LENGTH_SHORT).show();

                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.hide();
                                    Toast.makeText(getApplicationContext(), loginMessage, Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), Orders.class);
                                    startActivity(intent);
                                }
                            }, 5000);

//                            progressDialog.hide();
//                            Intent intent = new Intent(getApplicationContext(), Orders.class);
//                            startActivity(intent);
                        }
                        else {
                            Log.d("TAG", "Login response : Not successful");
                            progressDialog.hide();
                            loginMessage = "Unsuccessful, Please try again";
//                            Toast.makeText(getApplicationContext(), "Unsuccessful, Please try again", Toast.LENGTH_SHORT).show();
                            email.getEditText().setText("");
                            password.getEditText().setText("");
                            email.getEditText().requestFocus();
                        }

                        if(!(BASE_URL.contains(".it") && loginMessage.contains("success"))){
                            Toast.makeText(getApplicationContext(), loginMessage, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        Log.e("TAG", "onFailure: ", t.getCause());
                        Toast.makeText(getApplicationContext(), "Check your internet connection !", Toast.LENGTH_SHORT).show();
                        progressDialog.hide();
                    }

                });
            }
        });

    }


    private void setStoreData(Context context, List<Store> items, SharedPreferences sharedPreferences) {

//            SharedPreferences sharedPreferences = context.getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            StringBuilder timeZoneList = new StringBuilder();
            StringBuilder storeIdList = new StringBuilder();
            for(Store store : items)
            {
                timeZoneList.append(store.regionCountry.timezone).append(" ");
                storeIdList.append(store.id).append(" ");
                sharedPreferences.edit().putString(store.id+"-name", store.name).apply();
            }
            editor.putString("storeId", storeIdList.toString().split(" ")[0]).apply();
            editor.putString("timezone", timeZoneList.toString()).apply();
            editor.putString("storeIdList", storeIdList.toString()).apply();
            editor.putInt("hasLogos", 0).apply();

        Log.e("TIMEZONELIST", "setStoreData: "+ timeZoneList, new Error() );
//                FirebaseHelper.initializeFirebase(items.get(holder.getAdapterPosition()).id, context);
            String BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);
            Retrofit retrofitLogo = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL+App.PRODUCT_SERVICE_URL).addConverterFactory(GsonConverterFactory.create()).build();
            StoreApi storeApiSerivice = retrofitLogo.create(StoreApi.class);

            Log.e("TAG", "onEnterLogoUrl: "+ sharedPreferences.getAll(), new Error());

    }

    private void subscribeStores(List<Store> stores, Context context) {
        Log.e("TAG", "subscribeStores: "+ stores, new Error() );

        for(Store store : stores)
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FirebaseHelper.initializeFirebase(store.id, context);
                }
            }).start();
        }

    }


    public void downloadAndSaveLogos(String[] stores, Context context, String clientId){
        ImageView iv = new ImageView(context);

        LogoHandler logoHandler = new LogoHandler(stores, context, new Handler(), clientId);
        Thread thread = new Thread(logoHandler);
            thread.setName("Logo Fetcher Thread");
            thread.start();

    }

    private void getStoresAndRegister(SharedPreferences sharedPreferences) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.PRODUCT_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        StoreApi storeApiService = retrofit.create(StoreApi.class);
//        sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        String clientId = sharedPreferences.getString("ownerId", null);

        Call<StoreResponse> storeResponse = storeApiService.getStores(headers, clientId);
        progressDialog.show();
        storeResponse.clone().enqueue(new Callback<StoreResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResponse(Call<StoreResponse> call, Response<StoreResponse> response) {
                if (response.isSuccessful()) {
//                    Log.e("getMYSTORES", "onResponse: " + response.body().data.content, new Error());
//                    Toast.makeText(getApplicationContext(), "List items : "+ response.body().data.content.size(), Toast.LENGTH_SHORT).show();
                    stores = response.body().data.content;
                    subscribeStores(stores, getApplicationContext());
                    setStoreData(getApplicationContext(),stores, sharedPreferences);
                    downloadAndSaveLogos(sharedPreferences.getString("storeIdList", null).split(" "), getApplicationContext(),clientId);
                    Log.e("getMYSTORES", "onResponse: " + stores, new Error());
                }
            }

            @Override
            public void onFailure(Call<StoreResponse> call, Throwable t) {
                Log.e("TAG", "onFailure: ", t.getCause());
            }
        });


    }

    @Override
    protected void onStart() {
        if(sharedPreferences.getInt("isLoggedIn",-1) == 1 && !sharedPreferences.contains("storeId")) {
            Intent intent = new Intent(getApplicationContext(), Orders.class);
            startActivity(intent);
            finish();
        }
        else if(sharedPreferences.getInt("isLoggedIn",-1) == 1 && sharedPreferences.contains("storeId")){
            Intent intent = new Intent(getApplicationContext(), Orders.class);
            startActivity(intent);
            finish();
        }

        super.onStart();
    }

    @Override
    public void onBackPressed() {
        if(sharedPreferences.getInt("isLoggedIn", -1) == 1)
        {
            this.finishAffinity();
        }
        else
            super.onBackPressed();
    }
//
//    @Override
//    public void onLogoDownloadCompleteListener() {
//        Intent intent = new Intent(getApplicationContext(), Orders.class);
//        startActivity(intent);
//    }
}