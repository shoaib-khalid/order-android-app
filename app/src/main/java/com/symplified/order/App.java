package com.symplified.order;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import com.symplified.order.helpers.GenericPrintHelper;
import com.symplified.order.helpers.SunmiPrintHelper;
import com.symplified.order.interfaces.Printer;
import com.symplified.order.interfaces.PrinterObserver;

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

    public static final String SESSION_DETAILS_TITLE = "session";
    public static final String CHANNEL_ID = "EASYDUKAN_ID";
    public static final String ORDERS = "ORDERS";

    @Override
    public void onCreate(){
        super.onCreate();

        context = getApplicationContext();
        //restrict devices from forcing the dark mode on the app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        //initialize the notification manager to receive and display notifications.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,"EasyDukan", NotificationManager.IMPORTANCE_HIGH
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

        SunmiPrintHelper.getInstance().addObserver(this);
        SunmiPrintHelper.getInstance().initPrinterService(this);

        GenericPrintHelper.getInstance().addObserver(this);
        GenericPrintHelper.getInstance().initPrinterService(this);
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
}
