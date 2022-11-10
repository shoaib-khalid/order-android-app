package com.symplified.easydukan;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

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
import com.symplified.easydukan.apis.LoginApi;
import com.symplified.easydukan.apis.StoreApi;
import com.symplified.easydukan.firebase.FirebaseHelper;
import com.symplified.easydukan.models.Store.Store;
import com.symplified.easydukan.models.Store.StoreResponse;
import com.symplified.easydukan.models.login.LoginData;
import com.symplified.easydukan.models.login.LoginRequest;
import com.symplified.easydukan.models.login.LoginResponse;
import com.symplified.easydukan.networking.ServiceGenerator;
import com.symplified.easydukan.utils.Key;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final int UPDATE_REQUEST_CODE = 112;
    private static final String TAG = LoginActivity.class.getName();
    private Button login, btnSwitchToProduction;
    private TextInputLayout email;
    private TextInputLayout password;
    private SharedPreferences sharedPreferences;
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
        setContentView(R.layout.activity_login);
        initViews();

        sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);

        callInAppUpdate();

        configureRemoteConfig();

        if (sharedPreferences.getBoolean("isStaging", false)) {
            switchToStagingMode();
        }

        login.setOnClickListener(view -> onLoginButtonClick());
        btnSwitchToProduction.setOnClickListener(view -> switchToProductionMode());
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

        login = findViewById(R.id.btn_login);
        btnSwitchToProduction = findViewById(R.id.btn_production);
        email = findViewById(R.id.tv_email);
        password = findViewById(R.id.tv_password);
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

        if (BASE_URL.equals("")) {
            BASE_URL = App.BASE_URL;
        }

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
            startLoading();

            LoginApi loginApiService = ServiceGenerator.createUserService();
            Call<LoginResponse> loginResponse = loginApiService
                    .login(new LoginRequest(email.getEditText().getText().toString(),
                            password.getEditText().getText().toString()));

            loginResponse.clone().enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful()) {
                        LoginData res = response.body().data;
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("email", res.session.username);
                        editor.putString("accessToken", res.session.accessToken);
                        editor.putString("refreshToken", res.session.refreshToken);
                        editor.putString("ownerId", res.session.ownerId);
                        editor.putString("expiry", res.session.expiry.toGMTString());
                        editor.putBoolean(Key.IS_LOGGED_IN, true);
                        editor.putInt("versionCode", BuildConfig.VERSION_CODE);
                        editor.putString("base_url", BASE_URL);
                        editor.apply();
                        getStoresAndRegister(sharedPreferences);
                    } else {
                        handleError(response.raw());
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Log.e("TAG", "onFailure: ", t.getCause());
                    Toast.makeText(getApplicationContext(), R.string.no_internet, Toast.LENGTH_SHORT).show();
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
    private void setStoreDataAndProceed(Context context, List<Store> items, SharedPreferences sharedPreferences) {

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
            FirebaseHelper.initializeFirebase(store.id);
        }
    }

    /**
     * method to make the api call to get all the stores of user from backend
     *
     * @param sharedPreferences
     */
    private void getStoresAndRegister(SharedPreferences sharedPreferences) {

        String clientId = sharedPreferences.getString("ownerId", null);

        StoreApi storeApiService = ServiceGenerator.createStoreService();
        Call<StoreResponse> storeResponse = storeApiService.getStores(clientId);
//        progressDialog.show();
        storeResponse.clone().enqueue(new Callback<StoreResponse>() {

            @Override
            public void onResponse(Call<StoreResponse> call, Response<StoreResponse> response) {
                if (response.isSuccessful()) {
                    stores = response.body().data.content;
                    subscribeStores(stores, getApplicationContext());
                    setStoreDataAndProceed(getApplicationContext(), stores, sharedPreferences);
                } else {
                    handleError(response.raw());
                }
            }

            @Override
            public void onFailure(Call<StoreResponse> call, Throwable t) {
                Log.e("TAG", "onFailure: ", t.getCause());
                Toast.makeText(getApplicationContext(), t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
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
        if (sharedPreferences.getBoolean(Key.IS_LOGGED_IN, false)
                && sharedPreferences.contains("storeIdList")) {
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
        if (sharedPreferences.getBoolean(Key.IS_LOGGED_IN, false)) {
            this.finishAffinity();
        } else {
            super.onBackPressed();
        }
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
            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
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

    private void handleError(okhttp3.Response rawResponse) {
        Toast.makeText(this, R.string.request_failure, Toast.LENGTH_SHORT).show();
        email.getEditText().setText("");
        password.getEditText().setText("");
        stopLoading();
        email.getEditText().requestFocus();
        Log.e("login-activity", rawResponse.toString());
    }
}