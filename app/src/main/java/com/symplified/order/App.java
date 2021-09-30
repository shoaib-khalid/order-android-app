package com.symplified.order;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class App extends Application {

    public static String BASE_URL = "https://api.symplified.biz/";
    public static final String USER_SERVICE_URL = "user-service/v1/clients/";
    public static final String PRODUCT_SERVICE_URL = "product-service/v1/";
    public static final String ORDER_SERVICE_URL = "order-service/v1/";

// public static final String BASE_URL = "https://api.symplified.biz/";
//    public static final String USER_SERVICE_URL = "user-service/v1/clients/";
//    public static final String PRODUCT_SERVICE_URL = "https://api.symplified.biz/product-service/v1/";
//    public static final String ORDER_SERVICE_URL = "https://api.symplified.biz/order-service/v1/";
//


    public static final String SESSION_DETAILS_TITLE = "session";
    public static final String CHANNEL_ID = "CHANNEL_ID";
    public static final String ORDERS = "ORDERS";
    public static final Uri SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    public static Ringtone ringtone;
//    private FirebaseRemoteConfig mRemoteConfig;
    public static Ringtone play(Context context){
        ringtone = RingtoneManager.getRingtone(context, SOUND);
        return ringtone;
    }

//    static {
//        FirebaseRemoteConfig mRemoteConfig = FirebaseRemoteConfig.getInstance();
//        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(3600).build();
//        mRemoteConfig.setConfigSettingsAsync(configSettings);
//        mRemoteConfig.setDefaultsAsync(R.xml.defaults);
//
//        BASE_URL = mRemoteConfig.getString("base_url");
//        USER_SERVICE_URL = mRemoteConfig.getString("user_service_url");
//        PRODUCT_SERVICE_URL = mRemoteConfig.getString("product_service_url");
//        ORDER_SERVICE_URL = mRemoteConfig.getString("order_service_url");
//    }

    @Override
    public void onCreate(){
        super.onCreate();


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
    }
}
