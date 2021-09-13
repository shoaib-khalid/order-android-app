package com.symplified.order;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class App extends Application {

    public static final String BASE_URL = "https://api.symplified.it/";
    public static final String USER_SERVICE_URL = "user-service/v1/clients/";
    public static final String PRODUCT_SERVICE_URL = "https://api.symplified.biz/product-service/v1/";
    public static final String ORDER_SERVICE_URL = "https://api.symplified.biz/order-service/v1/";
    public static final String SESSION_DETAILS_TITLE = "session";
    public static final String CHANNEL_ID = "CHANNEL_ID";
    public static final String ORDERS = "ORDERS";
    public static final Uri SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    public static Ringtone ringtone;

    public static Ringtone play(Context context){
        ringtone = RingtoneManager.getRingtone(context, SOUND);
        return ringtone;
    }

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
