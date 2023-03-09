package com.symplified.order;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.symplified.order.models.item.Item;
import com.symplified.order.models.order.Order;
import com.symplified.order.utils.GenericPrintHelper;
import com.symplified.order.utils.PrinterUtility;
import com.symplified.order.utils.SunmiPrintHelper;
import com.symplified.order.interfaces.Printer;
import com.symplified.order.interfaces.PrinterObserver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private static Printer connectedPrinter;
    public static final Set<BluetoothDevice> btPrinters = new HashSet<>();
    public static final int PERMISSION_REQUEST_CODE = 10000;

    @Override
    public void onCreate() {
        super.onCreate();

        //restrict devices from forcing the dark mode on the app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

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

    public static void addBtPrinter(BluetoothDevice pairedPrinter) {
        btPrinters.add(pairedPrinter);
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
                Log.d("printer-helper", "Failed to print receipt: " + e.getLocalizedMessage());
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                context,
                BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_DENIED
        ) {
            return;
        }

        for (BluetoothDevice btPrinter : btPrinters) {
            if (btPrinter.getUuids().length == 0) {
                continue;
            }

            new Thread() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                            ContextCompat.checkSelfPermission(context, BLUETOOTH_CONNECT)
                                    == PackageManager.PERMISSION_GRANTED
                    ) {
                        try {
                            BluetoothSocket socket = btPrinter
                                    .createRfcommSocketToServiceRecord(
                                            btPrinter.getUuids()[0].getUuid()
                                    );

                            socket.connect();
                            socket.getOutputStream().write(
                                    PrinterUtility.generateReceiptText(order, items, currency)
                                            .getBytes(StandardCharsets.UTF_8)
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }.start();
        }
    }

}
