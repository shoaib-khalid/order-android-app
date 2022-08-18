package com.symplified.order;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.ActivityResult;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.symplified.order.apis.LoginApi;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.firebase.FirebaseHelper;
import com.symplified.order.handlers.LogoHandler;
import com.symplified.order.models.Store.Store;
import com.symplified.order.models.Store.StoreResponse;
import com.symplified.order.models.login.LoginData;
import com.symplified.order.models.login.LoginRequest;
import com.symplified.order.models.login.LoginResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {

    private static final int UPDATE_REQUEST_CODE = 112;
    private static final String TAG = LoginActivity.class.getName();
    private Button login, btnSwitchToProduction;
    private TextInputLayout email;
    private TextInputLayout password;
    private SharedPreferences sharedPreferences;
    private ImageView header;
    private Dialog progressDialog;
    private FirebaseRemoteConfig mRemoteConfig;
    private String testUser, testPass;
    private String BASE_URL;
    private List<Store> stores;
    private TextView welcomeText;
    private ProgressBar progressBar;
    private ConstraintLayout mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_SymplifiedOrderUpdate);

        callInAppUpdate();

        configureRemoteConfig();

        sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        setContentView(R.layout.activity_login);
        initViews();

        login.setOnClickListener(view -> {
            onLoginButtonClick();
        });

        btnSwitchToProduction.setOnClickListener(view -> {
            switchToProductionMode();
        });

        if (sharedPreferences.getBoolean("isStaging", false)) {
            switchToStagingMode();
        }
    }

    private void switchToProductionMode() {
        BASE_URL = App.BASE_URL;
        sharedPreferences.edit().putBoolean("isStaging", false).apply();
        sharedPreferences.edit().putString("base_url", BASE_URL).apply();
        Toast.makeText(getApplicationContext(), "Switched to production", Toast.LENGTH_SHORT).show();
        welcomeText.setText(R.string.welcome_message);
        btnSwitchToProduction.setVisibility(View.GONE);
    }

    private void switchToStagingMode() {
        BASE_URL = App.BASE_URL_STAGING;
        sharedPreferences.edit().putBoolean("isStaging", true).apply();
        sharedPreferences.edit().putString("base_url", BASE_URL).apply();
        Toast.makeText(getApplicationContext(), "Switched to staging", Toast.LENGTH_SHORT).show();
        MaterialTextView welcomeText = findViewById(R.id.welcome);
        welcomeText.setText(R.string.staging_mode);
        btnSwitchToProduction.setVisibility(View.VISIBLE);
    }


    /**
     * method to initialize all the views in this activity
     */
    private void initViews() {
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
        btnSwitchToProduction = findViewById(R.id.btn_production);
        welcomeText = findViewById(R.id.welcome);

        progressBar = findViewById(R.id.progress_bar);
        mainLayout = findViewById(R.id.main_layout);
    }

    /**
     * method to configure and setup Firebase Remote Config
     */
    private void configureRemoteConfig() {
        mRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings
                .Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        mRemoteConfig.setConfigSettingsAsync(configSettings);
        mRemoteConfig.setDefaultsAsync(R.xml.defaults);

        mRemoteConfig.fetch(0);
        mRemoteConfig.activate();

        stores = new ArrayList<>();
        BASE_URL = mRemoteConfig.getString("base_url");

        if (BASE_URL.equals("")) {
            BASE_URL = App.BASE_URL;
        }

        Log.i("TAG", "BASE_URL : " + BASE_URL.length());

        testUser = mRemoteConfig.getString("test_user");
        testPass = mRemoteConfig.getString("test_pass");

        if (testUser.equals("") || testPass.equals("")) {
            testUser = "qa_user";
            testPass = "qa@kalsym";
        }

        Log.i("TAG", "test credentials  : user : " + testUser + " : password : " + testPass);

    }


    /**
     * onClick method for Login Button
     */
    private void onLoginButtonClick() {
        if (email.getEditText().getText().toString().equals(testUser)
                && password.getEditText().getText().toString().equals(testPass)) {
            switchToStagingMode();
            email.getEditText().getText().clear();
            password.getEditText().getText().clear();
            email.getEditText().requestFocus();
        } else {
//            progressDialog.show();
            startLoading();

            Retrofit retrofit = new Retrofit.Builder()
                    .client(new OkHttpClient())
                    .baseUrl(BASE_URL + App.USER_SERVICE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            LoginApi loginApiService = retrofit.create(LoginApi.class);

            Call<LoginResponse> loginResponse = loginApiService.login("application/json",
                    new LoginRequest(email.getEditText().getText().toString(), password.getEditText().getText().toString()));

            loginResponse.clone().enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful()) {
                        LoginData res = response.body().data;
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        if (!sharedPreferences.contains("isLoggedIn") || sharedPreferences.getInt("isLoggedIn", -1) == 0) {
                            editor.putString("email", res.session.username);
                            editor.putString("accessToken", res.session.accessToken);
                            editor.putString("refreshToken", res.session.refreshToken);
                            editor.putString("ownerId", res.session.ownerId);
                            editor.putString("expiry", res.session.expiry.toGMTString());
                            editor.putInt("isLoggedIn", 1);
                            editor.putInt("versionCode", BuildConfig.VERSION_CODE);
                            editor.apply();
                            sharedPreferences.edit().putString("base_url", BASE_URL).apply();
                            getStoresAndRegister(sharedPreferences);
                        }
                    } else {
                        handleError();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Log.e("TAG", "onFailure: ", t.getCause());
                    Toast.makeText(getApplicationContext(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    stopLoading();
                }

            });
        }
    }

    /**
     * method to store information to sharedPreferences for user session management
     *
     * @param context
     * @param items
     * @param sharedPreferences
     */
    private void setStoreData(Context context, List<Store> items, SharedPreferences sharedPreferences) {

        final SharedPreferences.Editor editor = sharedPreferences.edit();
        StringBuilder timeZoneList = new StringBuilder();
        StringBuilder storeIdList = new StringBuilder();
        for (Store store : items) {
            timeZoneList.append(store.regionCountry.timezone).append(" ");
            storeIdList.append(store.id).append(" ");
            sharedPreferences.edit().putString(store.id + "-name", store.name).apply();
        }
        editor.putString("currency", items.get(0).regionCountry.currencySymbol);
        editor.putString("storeId", storeIdList.toString().split(" ")[0]).apply();
        editor.putString("timezone", timeZoneList.toString()).apply();
        editor.putString("storeIdList", storeIdList.toString()).apply();
        editor.putInt("hasLogos", 0).apply();

        progressDialog.dismiss();
        Intent intent = new Intent(getApplicationContext(), OrdersActivity.class);
        startActivity(intent);
    }

    /**
     * method to subscribe the user to all the stores that belong to user, to receive new order notifications
     *
     * @param stores
     * @param context
     */
    private void subscribeStores(List<Store> stores, Context context) {
        Log.i("TAG", "subscribeStores: " + stores);

        for (Store store : stores) {
            FirebaseHelper.initializeFirebase(store.id, context);
        }
    }

    /**
     * method to download and store Store logos Asynchronously to sharedPreferences to avoid frequent downloads.
     *
     * @param stores
     * @param context
     * @param clientId
     */
    public void downloadAndSaveLogos(String[] stores, Context context, String clientId) {
        LogoHandler logoHandler = new LogoHandler(stores, context, new Handler(), clientId);
        Thread thread = new Thread(logoHandler);
        thread.setName("Logo Fetcher Thread");
        thread.start();
    }

    /**
     * method to make the api call to get all the stores of user from backend
     *
     * @param sharedPreferences
     */
    private void getStoresAndRegister(SharedPreferences sharedPreferences) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.PRODUCT_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        StoreApi storeApiService = retrofit.create(StoreApi.class);
        String clientId = sharedPreferences.getString("ownerId", null);

        Call<StoreResponse> storeResponse = storeApiService.getStores(headers, clientId);
//        progressDialog.show();
        storeResponse.clone().enqueue(new Callback<StoreResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResponse(Call<StoreResponse> call, Response<StoreResponse> response) {
                if (response.isSuccessful()) {
                    stores = response.body().data.content;
//                    int storeCount = stores.size();
                    subscribeStores(stores, getApplicationContext());
                    setStoreData(getApplicationContext(), stores, sharedPreferences);
                    downloadAndSaveLogos(sharedPreferences.getString("storeIdList", null).split(" "), getApplicationContext(), clientId);
                    Log.i("getMYSTORES", "onResponse: " + stores);
                } else {
                    handleError();
                }
            }

            @Override
            public void onFailure(Call<StoreResponse> call, Throwable t) {
                Log.e("TAG", "onFailure: ", t.getCause());
                Toast.makeText(getApplicationContext(), t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        });


    }

    /**
     * overridden onStart method to accomplish persistent login
     */
    @Override
    protected void onStart() {
        callInAppUpdate();
        //check if user session already exists, for persistent login
        if (sharedPreferences.getInt("isLoggedIn", -1) == 1
                && sharedPreferences.contains("storeIdList")
                && sharedPreferences.getInt("versionCode", 0) == BuildConfig.VERSION_CODE) {
            Intent intent = new Intent(getApplicationContext(), OrdersActivity.class);
            startActivity(intent);
            finish();
        } else {
            String storeIdList = sharedPreferences.getString("storeIdList", null);
            if (storeIdList != null) {
                for (String storeId : storeIdList.split(" ")) {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(storeId);
                }
            }
            boolean isStaging = sharedPreferences.getBoolean("isStaging", false);
            sharedPreferences.edit().clear().apply();
            sharedPreferences.edit().putBoolean("isStaging", isStaging).apply();
        }

        super.onStart();
    }

    /**
     * overridden onBackPressed method to cater persistent login flow
     */
    @Override
    public void onBackPressed() {
        if (sharedPreferences.getInt("isLoggedIn", -1) == 1) {
            this.finishAffinity();
        } else
            super.onBackPressed();
    }

    public void callInAppUpdate() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);
// Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
// Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                Toast.makeText(this, "Update is available", Toast.LENGTH_SHORT).show();
                // Request the update.
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            this,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                                    .setAllowAssetPackDeletion(true)
                                    .build(),
                            UPDATE_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UPDATE_REQUEST_CODE && resultCode == RESULT_OK) {
            Toast.makeText(this, "Update Started !", Toast.LENGTH_SHORT).show();
        } else if (resultCode == ActivityResult.RESULT_IN_APP_UPDATE_FAILED) {
            Log.d(TAG, "onActivityResult: " + "Update flow failed! Result code: " + resultCode);
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        callInAppUpdate();
    }

    private void startLoading() {
        progressBar.setVisibility(View.VISIBLE);
        mainLayout.setVisibility(View.GONE);
    }

    private void stopLoading() {
        progressBar.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);
    }

    private void handleError() {
        progressDialog.dismiss();
        Toast.makeText(this, R.string.request_failure, Toast.LENGTH_SHORT).show();
        email.getEditText().setText("");
        password.getEditText().setText("");
        stopLoading();
        email.getEditText().requestFocus();
    }
}