package com.symplified.easydukan;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;
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
import com.symplified.easydukan.apis.LoginApi;
import com.symplified.easydukan.apis.StoreApi;
import com.symplified.easydukan.firebase.FirebaseHelper;
import com.symplified.easydukan.handlers.LogoHandler;
import com.symplified.easydukan.models.Store.Store;
import com.symplified.easydukan.models.Store.StoreResponse;
import com.symplified.easydukan.models.login.LoginData;
import com.symplified.easydukan.models.login.LoginRequest;
import com.symplified.easydukan.models.login.LoginResponse;

import org.json.JSONObject;

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

public class LoginActivity extends AppCompatActivity{

    private static final int UPDATE_REQUEST_CODE = 112;
    private static final String TAG = LoginActivity.class.getName();
    private Button login, productionModeButton;
    private TextInputLayout email;
    private TextInputLayout password;
    private SharedPreferences sharedPreferences;
    private ImageView header;
    private Dialog progressDialog;
    private FirebaseRemoteConfig mRemoteConfig;
    private final String testUser = "qa_user", testPass = "qa@kalsym";
    private String BASE_URL;
    private List<Store> stores;
    private TextView stagingModeIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_SymplifiedOrderUpdate);

        callInAppUpdate();

        configureRemoteConfig();

        sharedPreferences = getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        setContentView(R.layout.activity_login);
        initViews();

        login.setOnClickListener(view -> onLoginButtonClick());

        productionModeButton.setOnClickListener(view -> switchToProductionMode());
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
        productionModeButton = findViewById(R.id.btn_production);
        email = findViewById(R.id.tv_email);
        password = findViewById(R.id.tv_password);
        header = findViewById(R.id.iv_header);
        stagingModeIndicator = findViewById(R.id.staging_mode_text);

        TextView appVersionText = findViewById(R.id.app_version_text);
        appVersionText.setText("Build " + BuildConfig.VERSION_NAME);
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
//        BASE_URL = mRemoteConfig.getString("base_url");

//        if(BASE_URL.equals("")){
            BASE_URL = App.BASE_URL;
//        }

        Log.i("TAG", "BASE_URL : "+ BASE_URL.length());

//        testUser = mRemoteConfig.getString("test_user");
//        testPass = mRemoteConfig.getString("test_pass");

//        if(testUser.equals("") || testPass.equals(""))
//        {
//            testUser = "qa_user";
//            testPass = "qa@kalsym";
//        }

        Log.i("TAG", "test credentials  : user : "+ testUser+" : password : "+testPass );
    }


    /**
     * onClick method for Login Button
     */
    private void onLoginButtonClick() {
        if (testUser.equals(email.getEditText().getText().toString())
                && testPass.equals(password.getEditText().getText().toString()))
        {
            switchToStagingMode();
            email.getEditText().setText("");
            password.getEditText().setText("");
            email.getEditText().requestFocus();
            return;
        }
        progressDialog.show();

        sendLoginRequest();
    }

    private void sendLoginRequest() {
        Retrofit retrofit = new Retrofit.Builder()
                .client(new OkHttpClient())
                .baseUrl(BASE_URL+App.USER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        LoginApi loginApiService = retrofit.create(LoginApi.class);

        Call<LoginResponse> loginResponse = loginApiService.login("application/json",
                new LoginRequest(email.getEditText().getText().toString(),
                        password.getEditText().getText().toString()));

        loginResponse.clone().enqueue(new Callback<LoginResponse>() {

            String loginMessage = "An error occurred. Please try again.";
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                int toastLength = Toast.LENGTH_SHORT;
                if(response.isSuccessful())
                {
                    loginMessage = "Logged In Successfully !";

                    LoginData res = response.body().data;
                    Log.d("TAG", "Login Response : "+ response.body().data.toString());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    if(!sharedPreferences.contains("isLoggedIn") || sharedPreferences.getInt("isLoggedIn", -1) == 0)
                    {
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
                        Log.i("getAllStore", "onResponse: " + stores );
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), loginMessage, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), OrdersActivity.class);
                        startActivity(intent);
                    }, 0);
                } else if (response.code() == 401) {
                    loginMessage = "Username or password is incorrect. Please try again";
                }

                Toast.makeText(getApplicationContext(), loginMessage, toastLength).show();
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                Log.e("TAG", "onFailure: ", t.getCause());
                Toast.makeText(getApplicationContext(), "Error: " + t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }

        });
    }

    /**
     * method to store information to sharedPreferences for user session management
     * @param context
     * @param items
     * @param sharedPreferences
     */
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

    /**
     * method to subscribe the user to all the stores that belong to user, to receive new easydukan notifications
     * @param stores
     * @param context
     */
    private void subscribeStores(List<Store> stores, Context context) {
        Log.i("TAG", "subscribeStores: "+ stores);

        for(Store store : stores)
        {
            FirebaseHelper.initializeFirebase(store.id, context);
        }

    }

    /**
     * method to download and store Store logos Asynchronously to sharedPreferences to avoid frequent downloads.
     * @param stores
     * @param context
     * @param clientId
     */
    public void downloadAndSaveLogos(String[] stores, Context context, String clientId){
        LogoHandler logoHandler = new LogoHandler(stores, context, new Handler(), clientId);
        Thread thread = new Thread(logoHandler);
            thread.setName("Logo Fetcher Thread");
            thread.start();
    }


    /**
     * method to make the api call to get all the stores of user from backend
     * @param sharedPreferences
     */
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

            @Override
            public void onResponse(Call<StoreResponse> call, Response<StoreResponse> response) {
                if (response.isSuccessful()) {
                    stores = response.body().data.content;
//                    int storeCount = stores.size();
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

    /**
     * overridden onStart method to accomplish persistent login
     */
    @Override
    protected void onStart() {
        callInAppUpdate();
        //check if user session already exists, for persistent login
        if(sharedPreferences.getInt("isLoggedIn",-1) == 1
                && sharedPreferences.contains("storeIdList")
                && sharedPreferences.getInt("versionCode", 0) == BuildConfig.VERSION_CODE) {
            Intent intent = new Intent(getApplicationContext(), OrdersActivity.class);
            startActivity(intent);
            finish();
        }
        else{
            String storeIdList = sharedPreferences.getString("storeIdList", null);
            if(storeIdList != null )
            {
                for(String storeId : storeIdList.split(" ")){
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(storeId);
                }
            }
            sharedPreferences.edit().clear().apply();
        }
//        else if(sharedPreferences.getInt("isLoggedIn",-1) == 1 && sharedPreferences.contains("storeId")){
//            Intent intent = new Intent(getApplicationContext(), Orders.class);
//            startActivity(intent);
//            finish();
//        }

        super.onStart();
    }

    /**
     * overridden onBackPressed method to cater persistent login flow
     */
    @Override
    public void onBackPressed() {
        if(sharedPreferences.getInt("isLoggedIn", -1) == 1)
        {
            this.finishAffinity();
        }
        else
            super.onBackPressed();
    }

    public void callInAppUpdate(){
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
        } else if(resultCode == ActivityResult.RESULT_IN_APP_UPDATE_FAILED) {
            Log.d(TAG, "onActivityResult: " + "Update flow failed! Result code: " + resultCode);
            Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        callInAppUpdate();
    }

    private void switchToStagingMode() {
        BASE_URL = App.BASE_URL_STAGING;
        sharedPreferences.edit().putBoolean("isStaging", true).apply();
        sharedPreferences.edit().putString("base_url", BASE_URL).apply();
        productionModeButton.setVisibility(View.VISIBLE);
        stagingModeIndicator.setVisibility(View.VISIBLE);
        Toast.makeText(getApplicationContext(), "Switched to staging mode", Toast.LENGTH_SHORT).show();
    }

    private void switchToProductionMode() {
        BASE_URL = App.BASE_URL;
        sharedPreferences.edit().putBoolean("isStaging", false).apply();
        sharedPreferences.edit().putString("base_url", BASE_URL).apply();
        productionModeButton.setVisibility(View.GONE);
        stagingModeIndicator.setVisibility(View.GONE);
        Toast.makeText(getApplicationContext(), "Switched to production mode", Toast.LENGTH_SHORT).show();
    }
}