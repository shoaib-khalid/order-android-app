package com.symplified.order;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.messaging.FirebaseMessaging;
import com.symplified.order.apis.FirebaseApi;
import com.symplified.order.helpers.GenericPrintHelper;
import com.symplified.order.helpers.SunmiPrintHelper;
import com.symplified.order.interfaces.Printer;
import com.symplified.order.interfaces.PrinterObserver;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.utils.ChannelId;
import com.symplified.order.utils.Key;
import com.symplified.order.utils.Utility;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Application file to have properties used throughout the lifecycle of app.
 */
public class App extends Application implements PrinterObserver {

    private static Context context;
    private static Printer connectedPrinter;
    public static final String DEV_TAG = "dev-logging";

    public static final String BASE_URL_PRODUCTION = "https://api.symplified.biz/";
    public static final String BASE_URL_STAGING = "https://api.symplified.it/";
    public static final String BASE_URL_DELIVERIN = "https://api.deliverin.pk/";

    public static final String USER_SERVICE_URL = "user-service/v1/clients/";
    public static final String PRODUCT_SERVICE_URL = "product-service/v1/";
    public static final String ORDER_SERVICE_URL = "order-service/v1/";
    public static final String DELIVERY_SERVICE_URL = "delivery-service/v1/";

    public static final String SESSION = "session";

    @Override
    public void onCreate(){
        super.onCreate();

        context = getApplicationContext();
        //restrict devices from forcing the dark mode on the app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        App.context = getApplicationContext();

        SunmiPrintHelper.getInstance().addObserver(this);
        SunmiPrintHelper.getInstance().initPrinterService(this);

        GenericPrintHelper.getInstance().addObserver(this);
        GenericPrintHelper.getInstance().initPrinterService(this);

        boolean isLoggedIn = getSharedPreferences(SESSION, Context.MODE_PRIVATE)
                .getBoolean(Key.IS_LOGGED_IN, false);
        if (Utility.isConnectedToInternet(this) && isLoggedIn) {
            verifyFirebaseConnection();
        }
    }

    public static Printer getPrinter() {
        return connectedPrinter;
    }
    @Override
    public void onPrinterConnected(Printer printer) {
        connectedPrinter = printer;
    }
    public static boolean isPrinterConnected() {
        return connectedPrinter != null && connectedPrinter.isPrinterConnected();
    }

    public static Context getAppContext() { return context; }

    /**
     * Check if firebase server is reachable and logout if not.
     */
    private void verifyFirebaseConnection() {
        FirebaseApi firebaseApiService = ServiceGenerator.createFirebaseService();
        firebaseApiService.ping().clone().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("app-firebase", "Firebase accessible. Subscribing");
                    SharedPreferences sharedPreferences = getSharedPreferences(SESSION, Context.MODE_PRIVATE);
                    String storeIdList = sharedPreferences.getString("storeIdList", null);
                    if (storeIdList != null) {
                        for (String storeId : storeIdList.split(" ")) {
                            FirebaseMessaging.getInstance().subscribeToTopic(storeId);
                        }
                    }
                } else {
                    logout();
                    Utility.notify(
                            getApplicationContext(),
                            getString(R.string.notif_firebase_error_title),
                            getString(R.string.notif_firebase_error_body),
                            ChannelId.ERRORS,
                            ChannelId.ERRORS_NOTIF_ID,
                            LoginActivity.class
                    );
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                logout();
                Utility.notify(
                        getApplicationContext(),
                        getString(R.string.notif_firebase_error_title),
                        getString(R.string.notif_firebase_error_body),
                        ChannelId.ERRORS,
                        ChannelId.ERRORS_NOTIF_ID,
                        LoginActivity.class
                );
            }
        });
    }

    private void logout() {
        SharedPreferences sharedPrefs = getSharedPreferences(SESSION, Context.MODE_PRIVATE);
        String storeIdList = sharedPrefs.getString("storeIdList", null);
        if (storeIdList != null) {
            for (String storeId : storeIdList.split(" ")) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(storeId);
            }
        }
        boolean isStaging = sharedPrefs.getBoolean(Key.IS_STAGING, false);
        String baseUrl = sharedPrefs.getString(Key.BASE_URL, App.BASE_URL_PRODUCTION);
        sharedPrefs.edit().clear().apply();
        sharedPrefs.edit()
                .putBoolean(Key.IS_STAGING, isStaging)
                .putString(Key.BASE_URL, baseUrl)
                .apply();
    }
}
