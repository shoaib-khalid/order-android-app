package com.ekedai.merchant;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import com.ekedai.merchant.models.interfaces.Printer;
import com.ekedai.merchant.models.interfaces.PrinterObserver;
import com.ekedai.merchant.models.item.Item;
import com.ekedai.merchant.models.order.Order;
import com.ekedai.merchant.utils.ChannelId;
import com.ekedai.merchant.utils.SunmiPrintHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Application file to have properties used throughout the lifecycle of app.
 */
public class App extends Application implements PrinterObserver {

    public static final String DEV_TAG = "dev-logging";

    public static final String BASE_URL_PRODUCTION = "https://api.e-kedai.my/";
    public static final String BASE_URL_STAGING = "https://uatapi.e-kedai.my/";

    public static final String USER_CLIENT_SERVICE_URL = "user-service/clients/";
    public static final String USER_SERVICE_URL = "user-service/";
    public static final String PRODUCT_SERVICE_URL = "product-service/";
    public static final String ORDER_SERVICE_URL = "order-service/";
    public static final String DELIVERY_SERVICE_URL = "delivery-service/";

    public static final String SESSION = "session";
    public static final String PRINT_TAG = "bluetooth-printer";

    private static Printer connectedPrinter;
    public static final Set<BluetoothDevice> btDevices = new HashSet<>();
    public static boolean isAddingBluetoothDevice = false;

    private static App instance;

    public App() {
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannels();

        //restrict devices from forcing the dark mode on the app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        SunmiPrintHelper.getInstance().addObserver(this);
        SunmiPrintHelper.getInstance().initPrinterService(this);
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(
                    ChannelId.PRINTING_NEW_ORDER,
                    "Printing new orders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("For when the app is printing a new order");
            manager.createNotificationChannel(channel);
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

    public static void printOrderReceipt(
            Order order,
            List<Item> items,
            String currency,
            Context context
    ) {
        if (connectedPrinter != null && connectedPrinter.isPrinterConnected()) {
            try {
                connectedPrinter.printOrderReceipt(order, items, currency);
            } catch (Exception e) {
                Log.e("printer-helper", "Failed to print receipt: ", e);
                Toast.makeText(context, "Failed to print receipt. Please try again.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Failed to print receipt. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
}
