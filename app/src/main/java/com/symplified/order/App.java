package com.symplified.order;

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.symplified.order.interfaces.Printer;
import com.symplified.order.interfaces.PrinterObserver;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.order.Order;
import com.symplified.order.ui.orders.OrdersActivity;
import com.symplified.order.utils.ChannelId;
import com.symplified.order.utils.GenericPrintHelper;
import com.symplified.order.utils.PrinterUtility;
import com.symplified.order.utils.SharedPrefsKey;
import com.symplified.order.utils.SunmiPrintHelper;
import com.symplified.order.utils.Utility;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Application file to have properties used throughout the lifecycle of app.
 */
public class App extends Application implements PrinterObserver {

    public static final String DEV_TAG = "dev-logging";

    public static final String BASE_URL_PRODUCTION = "https://api.symplified.biz/";
    public static final String BASE_URL_STAGING = "https://api.symplified.it/";
    public static final String BASE_URL_DELIVERIN = "https://api.deliverin.pk/";

    public static final String USER_CLIENT_SERVICE_URL = "user-service/v1/clients/";
    public static final String USER_SERVICE_URL = "user-service/v1/";
    public static final String PRODUCT_SERVICE_URL = "product-service/v1/";
    public static final String ORDER_SERVICE_URL = "order-service/v1/";
    public static final String DELIVERY_SERVICE_URL = "delivery-service/v1/";

    public static final String SESSION = "session";
    public static final String PRINT_TAG = "bluetooth-printer";

    private static Printer connectedPrinter;
    public static final Set<BluetoothDevice> btDevices = new HashSet<>();
    public static final int PERMISSION_REQUEST_CODE = 10000;
    public static boolean isAddingBluetoothDevice = false;
    private static final String PRINTER_UUID = "00001101-0000-1000-8000-00805F9B34FB";

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

        GenericPrintHelper.getInstance().addObserver(this);
        GenericPrintHelper.getInstance().initPrinterService(this);

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
        new Thread() {
            @Override
            public void run() {
                if (connectedPrinter != null && connectedPrinter.isPrinterConnected()) {
                    try {
                        connectedPrinter.printOrderReceipt(order, items, currency);
                    } catch (Exception e) {
                        Log.e("printer-helper", "Failed to print receipt: ", e);
                    }
                }
            }
        }.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ContextCompat.checkSelfPermission(context, BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_DENIED
        ) {
            return;
        }

        Log.d(PRINT_TAG, "Starting print");
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                SharedPrefsKey.BT_DEVICE_PREFS_FILE_NAME, Context.MODE_PRIVATE);
        byte[] dataToPrint = PrinterUtility.generateReceiptText(order, items, currency)
                .getBytes(StandardCharsets.UTF_8);
        Log.d(PRINT_TAG, "Generated byte data");

        final Set<BluetoothDevice> pairedDevices = ((BluetoothManager) context.getSystemService(
                Context.BLUETOOTH_SERVICE)).getAdapter().getBondedDevices();
        final NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);
        final String NOTIF_GROUP = order.id;

        if (!pairedDevices.isEmpty()
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED)) {
            mNotificationManager.notify(ThreadLocalRandom.current().nextInt(),
                    new NotificationCompat.Builder(context, ChannelId.PRINTING_NEW_ORDER)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentText("Printing order " + order.invoiceId)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setGroup(NOTIF_GROUP)
                            .build()
            );
        }

        for (BluetoothDevice device : pairedDevices) {
            final String deviceName = device.getName();
            if (sharedPrefs.getBoolean(deviceName, false)) {
                new Thread() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                                && ContextCompat.checkSelfPermission(context, BLUETOOTH_CONNECT)
                                == PackageManager.PERMISSION_DENIED) {
                            return;
                        }

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                                || ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS)
                                == PackageManager.PERMISSION_GRANTED) {
                            mNotificationManager.notify(ThreadLocalRandom.current().nextInt(),
                                    new NotificationCompat.Builder(context, ChannelId.PRINTING_NEW_ORDER)
                                            .setSmallIcon(R.drawable.ic_notification)
                                            .setStyle(new NotificationCompat.InboxStyle()
                                                    .setSummaryText("Order " + order.invoiceId))
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                            .setGroup(NOTIF_GROUP)
                                            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                                            .setGroupSummary(true)
                                            .build()
                            );
                        }

                        Log.d(PRINT_TAG, "Creating socket for " + deviceName);
                        try {
                            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID.fromString(PRINTER_UUID));
                            Log.d(PRINT_TAG, "Created socket for " + deviceName);

                            if (!socket.isConnected()) {
                                socket.connect();
                            }
                            Log.d(PRINT_TAG, "Socket connected for " + deviceName);

                            new Thread() {
                                @Override
                                public void run() {
                                    String text = "";
                                    try {
                                        socket.getOutputStream().write(dataToPrint);
                                        Log.d(PRINT_TAG, "Data written to " + deviceName);
                                        text = "Print data send to " + deviceName + " successfully.";
                                    } catch (IOException e) {
                                        Log.e(PRINT_TAG, "Failed to write to socket.", e);
                                        text = "Failed to send print data to " + deviceName;
                                    }

                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                                            || ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS)
                                            == PackageManager.PERMISSION_GRANTED) {
                                        mNotificationManager.notify(ThreadLocalRandom.current().nextInt(),
                                                new NotificationCompat.Builder(context, ChannelId.PRINTING_NEW_ORDER)
                                                        .setSmallIcon(R.drawable.ic_notification)
                                                        .setContentText(text)
                                                        .setStyle(new NotificationCompat.BigTextStyle()
                                                                .bigText(text))
                                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                                        .setGroup(NOTIF_GROUP)
                                                        .build()
                                        );
                                    }

                                    try {
                                        socket.close();
                                        Log.d(PRINT_TAG, "Socket closed for " + deviceName);
                                    } catch (IOException e) {
                                        Log.e(PRINT_TAG, "Failed to close socket.", e);
                                    }
                                }
                            }.start();
                        } catch (Exception e) {
                            String text = "Failed to connect to " + deviceName + " for printing";
                            Log.e(PRINT_TAG, text, e);

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                                    || ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS)
                                    == PackageManager.PERMISSION_GRANTED) {
                                mNotificationManager.notify(ThreadLocalRandom.current().nextInt(),
                                        new NotificationCompat.Builder(context, ChannelId.PRINTING_NEW_ORDER)
                                                .setSmallIcon(R.drawable.ic_notification)
                                                .setContentText(text)
                                                .setStyle(new NotificationCompat.BigTextStyle()
                                                        .bigText(text))
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                                .setGroup(NOTIF_GROUP)
                                                .build()
                                );
                            }
                        }
                    }
                }.start();
            }
        }
    }

    public static boolean isAnyBtPrinterEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                || ContextCompat.checkSelfPermission(context, BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(SharedPrefsKey.BT_DEVICE_PREFS_FILE_NAME, Context.MODE_PRIVATE);
            for (BluetoothDevice device : ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE))
                    .getAdapter().getBondedDevices()) {
                if (sharedPrefs.getBoolean(device.getName(), false)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }

    private static void notifyPrint(

    ) {

    }
}
