package com.symplified.order.ui;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.textfield.TextInputLayout;
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
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.apis.FirebaseApi;
import com.symplified.order.models.login.LoginData;
import com.symplified.order.models.login.LoginRequest;
import com.symplified.order.models.login.LoginResponse;
import com.symplified.order.models.store.Store;
import com.symplified.order.models.store.StoreResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.ui.orders.OrdersActivity;
import com.symplified.order.utils.ChannelId;
import com.symplified.order.utils.Key;
import com.symplified.order.utils.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final int UPDATE_REQUEST_CODE = 112;
    private static final String TAG = "login-activity";
    private Button btnSwitchToProduction;
    private TextInputLayout email;
    private TextInputLayout password;
    private SharedPreferences sharedPreferences;
    private List<Store> stores;
    private TextView welcomeText, progressText;
    private ConstraintLayout mainLayout, progressBarLayout;
    private FirebaseApi firebaseApiService;
    private final Timer timer = new Timer();
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Rect bounds = getSystemService(WindowManager.class).getCurrentWindowMetrics().getBounds();
            Log.d("login-activity", "Width " + bounds.width() + ", height: " + bounds.height());
        }

        setTheme(R.style.Theme_SymplifiedOrderUpdate);
        setContentView(R.layout.activity_login);
        initViews();

        sharedPreferences = getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);
        firebaseApiService = ServiceGenerator.createFirebaseService();

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
        progressText = findViewById(R.id.login_progress_text);

        progressBarLayout = findViewById(R.id.login_progress_layout);
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
    }


    /**
     * onClick method for Login Button
     */
    private void onLoginButtonClick() {
        String testUser = "qa_user", testPass = "qa@kalsym";
        String deliverinUser = "pak_user", deliverinPass = "pak@123";

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
        } else if (Utility.isConnectedToInternet(this)) {
            startLoading();
            progressText.setText("Checking access to firebase servers");

            firebaseApiService.ping().clone().enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        tryLogin();
                    } else {
                        stopLoading();
                        showFirebaseErrorNotification();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    stopLoading();
                    showFirebaseErrorNotification();
                }
            });
        } else {
            Toast.makeText(this, "Not connected to internet.", Toast.LENGTH_SHORT).show();
        }
    }

    private void tryLogin() {
        progressText.setText("Attempting login");

        String emailInput = email.getEditText() != null
                ? email.getEditText().getText().toString() : "";
        String passwordInput = password.getEditText() != null
                ? password.getEditText().getText().toString() : "";

        LoginRequest loginRequest = new LoginRequest(emailInput, passwordInput);
        ServiceGenerator.createUserService(this).login(loginRequest).clone().enqueue(new Callback<LoginResponse>() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call,
                                   @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    LoginData res = response.body().data;
                    sharedPreferences.edit()
                            .putString("accessToken", res.session.accessToken)
                            .putString("refreshToken", res.session.refreshToken)
                            .putString(Key.CLIENT_ID, res.session.ownerId)
                            .commit();
                    getStoresAndRegister(res.session.ownerId);
                } else {
                    stopLoading();
                    if (response.code() == 401) {
                        handleError(response.raw().toString(), "Username or password is incorrect");
                    } else {
                        handleError(response.raw().toString(), null);
                    }
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

    int subscriptionCount = 0;

    /**
     * method to make the api call to get all the stores of user from backend
     */
    private void getStoresAndRegister(String clientId) {
        progressText.setText("Getting store data");

        ServiceGenerator.createStoreService(this)
                .getStores(clientId).clone().enqueue(new Callback<StoreResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<StoreResponse> call,
                                           @NonNull Response<StoreResponse> response) {
                        if (response.isSuccessful()) {
                            progressText.setText("Subscribing to order notifications");

                            subscriptionCount = 0;
                            stores = response.body().data.content;
                            timer.schedule(new SubscribeTimeoutTask(), 8000L);
                            for (Store store : stores) {
                                FirebaseMessaging.getInstance().subscribeToTopic(store.id)
                                        .addOnSuccessListener(unused -> {
                                            getSystemService(NotificationManager.class)
                                                    .cancel(ChannelId.ERRORS_NOTIF_ID);
                                            Log.d(TAG, "Subscribed to " + store.name);
                                            subscriptionCount++;
                                            boolean isLoggedIn = sharedPreferences.getBoolean(Key.IS_LOGGED_IN, false);
                                            if (subscriptionCount >= stores.size()
                                                    && !isLoggedIn) {
                                                sharedPreferences.edit()
                                                        .putBoolean(Key.IS_SUBSCRIBED_TO_NOTIFICATIONS, true)
                                                        .apply();
                                                setStoreDataAndProceed();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            stopLoading();
                                            removeUserData();
                                            handleError(e.getLocalizedMessage(), null);
                                            showFirebaseErrorNotification();
                                        });
                            }
                        } else {
                            handleError(response.raw().toString(), null);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<StoreResponse> call, @NonNull Throwable t) {
                        Log.e("TAG", "onFailure: ", t.getCause());
                        Toast.makeText(getApplicationContext(), t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * method to store information to sharedPreferences for user session management
     */
    private void setStoreDataAndProceed() {
        progressText.setText("Storing data");

        final SharedPreferences.Editor editor = sharedPreferences.edit();
        StringBuilder storeIdList = new StringBuilder();
        for (Store store : stores) {
            storeIdList.append(store.id).append(" ");
        }
        editor.putString("currency", stores.get(0).regionCountry.currencySymbol)
                .putString("storeId", stores.get(0).id)
                .putString(Key.STORE_ID_LIST, storeIdList.toString())
                .putBoolean(Key.IS_LOGGED_IN, true)
                .commit();

        Log.d(TAG, "setStoreDataAndProceed");
        Intent intent = new Intent(getApplicationContext(), OrdersActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * overridden onStart method to accomplish persistent login
     */
    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart");

        callInAppUpdate();
        //check if user session already exists, for persistent login
        if (sharedPreferences.getBoolean(Key.IS_LOGGED_IN, false)
                && sharedPreferences.contains(Key.STORE_ID_LIST)) {
            Log.d(TAG, "Starting orderActivity from onStart");
            Intent intent = new Intent(getApplicationContext(), OrdersActivity.class);
            startActivity(intent);
            finish();
        } else if (!isLoading) {
            removeUserData();
        }

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
        progressBarLayout.setVisibility(View.VISIBLE);
        mainLayout.setVisibility(View.GONE);
        isLoading = true;
    }

    private void stopLoading() {
        progressBarLayout.setVisibility(View.GONE);
        progressText.setText("");
        mainLayout.setVisibility(View.VISIBLE);
        isLoading = false;
    }

    private void removeUserData() {
        String storeIdList = sharedPreferences.getString(Key.STORE_ID_LIST, null);
        if (storeIdList != null) {
            for (String storeId : storeIdList.split(" ")) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(storeId);
            }
        }
        boolean isStaging = sharedPreferences.getBoolean(Key.IS_STAGING, false);
        String baseUrl = sharedPreferences.getString(Key.BASE_URL, App.BASE_URL_PRODUCTION);

        sharedPreferences.edit()
                .clear()
                .putBoolean(Key.IS_STAGING, isStaging)
                .putString(Key.BASE_URL, baseUrl)
                .apply();
    }

    private void handleError(String debugString, String toastMessage) {
        Toast.makeText(this,
                toastMessage != null ? toastMessage : getString(R.string.request_failure),
                Toast.LENGTH_SHORT).show();
        stopLoading();
        Log.e(TAG, debugString);
    }

    private void showFirebaseErrorNotification() {
        Utility.notify(
                getApplicationContext(),
                getString(R.string.notif_firebase_error_title),
                getString(R.string.notif_firebase_error_text),
                getString(R.string.notif_firebase_error_text_full),
                ChannelId.ERRORS,
                ChannelId.ERRORS_NOTIF_ID,
                LoginActivity.class
        );
    }

    private class SubscribeTimeoutTask extends TimerTask {
        @Override
        public void run() {
            Log.d("login-activity", "SubscribeTimeoutTask");
            if (subscriptionCount == 0 && isLoading) {
                runOnUiThread(() -> {
                    stopLoading();
                    removeUserData();
                    Toast.makeText(LoginActivity.this,
                            getString(R.string.request_failure),
                            Toast.LENGTH_SHORT).show();
                    showFirebaseErrorNotification();
                });
            }
        }
    }
}