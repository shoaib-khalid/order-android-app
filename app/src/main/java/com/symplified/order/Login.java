package com.symplified.order;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
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
import com.symplified.order.services.StoreBroadcastReceiver;
import com.symplified.order.services.StoreManagerService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
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
    private List<Store> stores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_SymplifiedOrderUpdate);
        mRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0).build();

        mRemoteConfig.setConfigSettingsAsync(configSettings);

        stores = new ArrayList<>();

        mRemoteConfig.setDefaultsAsync(R.xml.defaults);

        mRemoteConfig.fetch(0);
        mRemoteConfig.activate();
        BASE_URL = mRemoteConfig.getString("base_url");

        if(BASE_URL.equals("")){
            BASE_URL = App.BASE_URL;
        }

        Log.i("TAG", "BASE_URL : "+ BASE_URL.length());

        testUser = mRemoteConfig.getString("test_user");
        testPass = mRemoteConfig.getString("test_pass");

        if(testUser.equals("") || testPass.equals(""))
        {
            testUser = "qa_user";
            testPass = "qa@kalsym";
        }

        Log.i("TAG", "test credentials  : user : "+ testUser+" : password : "+testPass );

        sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        setContentView(R.layout.activity_login);
        initScheduler();
        progressDialog = new Dialog(this);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);

        login = findViewById(R.id.btn_login);
        email = findViewById(R.id.tv_email);
        password = findViewById(R.id.tv_password);
        header = findViewById(R.id.iv_header);

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
                            SharedPreferences.Editor editor = sharedPreferences.edit();
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
                                Log.i("getAllStore", "onResponse: " + stores );

                            }

                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.hide();
                                    Toast.makeText(getApplicationContext(), loginMessage, Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), Orders.class);
                                    startActivity(intent);
                                }
                            }, 5000);
                        }
                        else {
                            Log.d("TAG", "Login response : Not successful");
                            progressDialog.hide();
                            loginMessage = "Unsuccessful, Please try again";
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

    private void initScheduler() {

//        ComponentName componentName = new ComponentName(this, StoreManagerService.class);
////        PersistableBundle bundle = new PersistableBundle();
////        bundle.putString("storeId", "Testing Store Closure");
//        JobInfo.Builder builder = new JobInfo.Builder(1423, componentName)
//                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
//                .setPersisted(true)
//                .setPeriodic(60 * 1000);
//        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
//        int resultCode = scheduler.schedule(builder.build());
//        if(resultCode == JobScheduler.RESULT_SUCCESS){
//            Log.d("LoginActivity", "initScheduler: Job Scheduled");
//        }else
//            Log.d("LoginActivity", "initScheduler: Job could not Scheduled");

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.MINUTE, 1);

        Intent job = new Intent(getApplicationContext(), StoreBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, job, 0);
//        alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);


        Dialog dialog = new Dialog(this);
        dialog.setCancelable(false);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.store_status_dialog);

        RadioGroup radioGroup = dialog.findViewById(R.id.store_status_options);
        TimePicker timePicker = dialog.findViewById(R.id.status_timePicker);
        MaterialTimePicker materialTimePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
                .setMinute(Calendar.getInstance().get(Calendar.MINUTE))
                .setTitleText("Paused Until")
                .build();
//        RadioGroup pausedGroup = dialog.findViewById(R.id.paused_status);
        radioGroup.setOnCheckedChangeListener((radioGroup1, i) -> {
            switch (i){
                case R.id.store_status_paused:{
//                    pausedGroup.setVisibility(View.VISIBLE);
//                    timePicker.setVisibility(View.VISIBLE);
                    materialTimePicker.show(getSupportFragmentManager(), "materialTimePicker");
                    break;
                }
                default:{
//                    pausedGroup.setVisibility(View.GONE);
                    timePicker.setVisibility(View.GONE);
                    break;
                }
            }
        });

//        RadioButton radio_30_min = dialog.findViewById(R.id.paused_30m);
//        RadioButton radio_1_hr = dialog.findViewById(R.id.paused_1h);
//        RadioButton radio_24hr = dialog.findViewById(R.id.paused_24h);
//
//        radio_30_min.setOnClickListener(view -> {
//            radio_1_hr.setTextColor(Color.BLACK);
//            radio_24hr.setTextColor(Color.BLACK);
//            radio_30_min.setTextColor(Color.WHITE);
//        });
//
//        radio_1_hr.setOnClickListener(view -> {
//            radio_1_hr.setTextColor(Color.WHITE);
//            radio_24hr.setTextColor(Color.BLACK);
//            radio_30_min.setTextColor(Color.BLACK);
//        });
//
//        radio_24hr.setOnClickListener(view -> {
//            radio_1_hr.setTextColor(Color.BLACK);
//            radio_24hr.setTextColor(Color.WHITE);
//            radio_30_min.setTextColor(Color.BLACK);
//        });

        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        dialog.show();
    }


    private void setStoreData(Context context, List<Store> items, SharedPreferences sharedPreferences) {

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

        Log.i("TIMEZONELIST", "setStoreData: "+ timeZoneList);
    }

    private void subscribeStores(List<Store> stores, Context context) {
        Log.i("TAG", "subscribeStores: "+ stores);

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
        String clientId = sharedPreferences.getString("ownerId", null);

        Call<StoreResponse> storeResponse = storeApiService.getStores(headers, clientId);
        progressDialog.show();
        storeResponse.clone().enqueue(new Callback<StoreResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResponse(Call<StoreResponse> call, Response<StoreResponse> response) {
                if (response.isSuccessful()) {
                    stores = response.body().data.content;
                    subscribeStores(stores, getApplicationContext());
                    setStoreData(getApplicationContext(),stores, sharedPreferences);
                    downloadAndSaveLogos(sharedPreferences.getString("storeIdList", null).split(" "), getApplicationContext(),clientId);
                    Log.i("getMYSTORES", "onResponse: " + stores);
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
}