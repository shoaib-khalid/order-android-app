package com.symplified.order;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.symplified.order.models.login.LoginData;
import com.symplified.order.models.login.LoginRequest;
import com.symplified.order.models.login.LoginResponse;
import com.symplified.order.models.store.Store;
import com.symplified.order.models.store.StoreResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.utils.Key;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final int UPDATE_REQUEST_CODE = 112;
    private static final String TAG = LoginActivity.class.getName();
    private Button btnSwitchToProduction;
    private TextInputLayout email;
    private TextInputLayout password;
    private SharedPreferences sharedPreferences;
    private String testUser, testPass;
    private final String deliverinUser = "pak_user", deliverinPass = "pak@123";
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

        switch (sharedPreferences.getString(Key.BASE_URL, App.BASE_URL_PRODUCTION)) {
            case App.BASE_URL_STAGING:
                switchToStagingMode();
                break;
            case App.BASE_URL_DELIVERIN:
                switchToDeliverinUrl();
                break;
            default:
                switchToProductionMode();
                break;
        }
    }

    private void switchToProductionMode() {
        sharedPreferences.edit().putBoolean(Key.IS_STAGING, false)
                .putString(Key.BASE_URL, App.BASE_URL_PRODUCTION).apply();
        welcomeText.setText(R.string.welcome_message);
        btnSwitchToProduction.setVisibility(View.GONE);
    }

    private void switchToStagingMode() {
        sharedPreferences.edit().putBoolean(Key.IS_STAGING, true)
                .putString(Key.BASE_URL, App.BASE_URL_STAGING).apply();
        Toast.makeText(getApplicationContext(), "Switched to staging", Toast.LENGTH_SHORT).show();
        welcomeText.setText(R.string.staging_mode);
        btnSwitchToProduction.setVisibility(View.VISIBLE);
    }

    private void switchToDeliverinUrl() {
        sharedPreferences.edit().putBoolean(Key.IS_STAGING, true)
                .putString(Key.BASE_URL, App.BASE_URL_DELIVERIN).apply();
        Toast.makeText(getApplicationContext(), "Switched to deliverin base url", Toast.LENGTH_SHORT).show();
        welcomeText.setText(R.string.deliverin);
        btnSwitchToProduction.setVisibility(View.VISIBLE);
    }

    /**
     * method to initialize all the views in this activity
     */
    private void initViews() {

        Button login = findViewById(R.id.btn_login);
        login.setOnClickListener(view -> onLoginButtonClick());

        btnSwitchToProduction = findViewById(R.id.btn_production);
        email = findViewById(R.id.tv_email);
        password = findViewById(R.id.tv_password);
        btnSwitchToProduction = findViewById(R.id.btn_production);
        btnSwitchToProduction.setOnClickListener(view -> switchToProductionMode());
        welcomeText = findViewById(R.id.welcome);

        progressBar = findViewById(R.id.progress_bar);
        mainLayout = findViewById(R.id.main_layout);
    }

    /**
     * method to configure and setup Firebase Remote Config
     */
    private void configureRemoteConfig() {
        FirebaseRemoteConfig mRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings
                .Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        mRemoteConfig.setConfigSettingsAsync(configSettings);
        mRemoteConfig.setDefaultsAsync(R.xml.defaults);

        mRemoteConfig.fetch(0);
        mRemoteConfig.activate();

        stores = new ArrayList<>();

        testUser = mRemoteConfig.getString("test_user");
        testPass = mRemoteConfig.getString("test_pass");

        if (testUser.equals("") || testPass.equals("")) {
            testUser = "qa_user";
            testPass = "qa@kalsym";
        }
    }


    /**
     * onClick method for Login Button
     */
    private void onLoginButtonClick() {
        String emailInput = email.getEditText() != null ? email.getEditText().getText().toString() : "";
        String passwordInput = password.getEditText() != null ? password.getEditText().getText().toString() : "";
        if (emailInput.equals(testUser) && passwordInput.equals(testPass)) {
            switchToStagingMode();
            email.getEditText().getText().clear();
            password.getEditText().getText().clear();
            email.getEditText().requestFocus();
        } else if (emailInput.equals(deliverinUser) && passwordInput.equals(deliverinPass)) {
            switchToDeliverinUrl();
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
                public void onResponse(@NonNull Call<LoginResponse> call,
                                       @NonNull Response<LoginResponse> response) {
                    if (response.isSuccessful()) {
                        LoginData res = response.body().data;
                        sharedPreferences.edit()
                                .putString("accessToken", res.session.accessToken)
                                .putString("refreshToken", res.session.refreshToken)
                                .putString("ownerId", res.session.ownerId)
                                .apply();
                        getStoresAndRegister();
                    } else {
                        handleError(response.raw().toString());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<LoginResponse> call,
                                      @NonNull Throwable t) {
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
     */
    private void setStoreDataAndProceed() {

        final SharedPreferences.Editor editor = sharedPreferences.edit();
        StringBuilder timeZoneList = new StringBuilder();
        StringBuilder storeIdList = new StringBuilder();
        for (Store store : stores) {
            timeZoneList.append(store.regionCountry.timezone).append(" ");
            storeIdList.append(store.id).append(" ");
            editor.putString(store.id + "-name", store.name).apply();
        }
        editor.putString("currency", stores.get(0).regionCountry.currencySymbol)
                .putString("storeId", storeIdList.toString().split(" ")[0])
                .putString("timezone", timeZoneList.toString())
                .putString("storeIdList", storeIdList.toString())
                .putInt("hasLogos", 0)
                .putBoolean(Key.IS_LOGGED_IN, true)
                .apply();

        Intent intent = new Intent(getApplicationContext(), OrdersActivity.class);
        startActivity(intent);
    }

    int subscriptionCount = 0;

    /**
     * method to make the api call to get all the stores of user from backend
     *
     */
    private void getStoresAndRegister() {

        String clientId = sharedPreferences.getString("ownerId", null);

        StoreApi storeApiService = ServiceGenerator.createStoreService();
        Call<StoreResponse> storeResponse = storeApiService.getStores(clientId);
        storeResponse.clone().enqueue(new Callback<StoreResponse>() {
            @Override
            public void onResponse(@NonNull Call<StoreResponse> call,
                                   @NonNull Response<StoreResponse> response) {
                if (response.isSuccessful()) {
                    stores = response.body().data.content;

                    subscriptionCount = 0;
                    for (Store store : stores) {
                        FirebaseMessaging.getInstance().subscribeToTopic(store.id)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        subscriptionCount++;
                                        Log.d("login-activity", "Subscribed to store " + store.name + ", subCount: " + subscriptionCount);
                                        if (subscriptionCount >= stores.size()) {
                                            setStoreDataAndProceed();
                                        }
                                    } else {
                                        removeUserData();
                                        stopLoading();

                                        handleError("Failed to subscribe to firebase");
                                    }
                                });
                    }
                } else {
                    handleError(response.raw().toString());
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
            removeUserData();
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

    private void removeUserData() {
        String storeIdList = sharedPreferences.getString("storeIdList", null);
        if (storeIdList != null) {
            for (String storeId : storeIdList.split(" ")) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(storeId);
            }
        }
        boolean isStaging = sharedPreferences.getBoolean(Key.IS_STAGING, false);
        String baseUrl = sharedPreferences.getString(Key.BASE_URL, App.BASE_URL_PRODUCTION);

        sharedPreferences.edit().clear().apply();
        sharedPreferences.edit()
                .putBoolean(Key.IS_STAGING, isStaging)
                .putString(Key.BASE_URL, baseUrl)
                .apply();
    }

    private void handleError(String errorStr) {
        Toast.makeText(this, R.string.request_failure, Toast.LENGTH_SHORT).show();
        email.getEditText().setText("");
        password.getEditText().setText("");
        stopLoading();
        email.getEditText().requestFocus();
        Log.e("login-activity", errorStr);
    }
}