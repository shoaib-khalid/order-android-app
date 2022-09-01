package com.symplified.order;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Application file to have properties used throughout the lifecycle of app.
 */
public class App extends Application {

    private static Context context;
    public static final String DEV_TAG = "dev-logging";

    public static String BASE_URL = "https://api.symplified.biz/";
    public static String BASE_URL_STAGING = "https://api.symplified.it/";
    public static final String USER_SERVICE_URL = "user-service/v1/clients/";
    public static final String PRODUCT_SERVICE_URL = "product-service/v1/";
    public static final String ORDER_SERVICE_URL = "order-service/v1/";
    public static final String DELIVERY_SERVICE_URL = "delivery-service/v1/";
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static final String SESSION_DETAILS_TITLE = "session";
    public static final String CHANNEL_ID = "CHANNEL_ID";
    public static final String ORDERS = "ORDERS";

    @Override
    public void onCreate(){
        super.onCreate();

        //restrict devices from forcing the dark mode on the app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        //initialize the notification manager to receive and display notifications.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ){
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,"Symplified", NotificationManager.IMPORTANCE_HIGH
            );

            NotificationChannel orders = new NotificationChannel(
                    ORDERS,"Orders", NotificationManager.IMPORTANCE_HIGH
            );
            orders.setSound(null, null);
            channel.setSound(null,null);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            notificationManager.createNotificationChannel(orders);
        }

        App.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return App.context;
    }
}
