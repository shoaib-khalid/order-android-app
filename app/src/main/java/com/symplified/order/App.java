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

import com.symplified.order.interfaces.Printer;
import com.symplified.order.interfaces.PrinterObserver;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.order.Order;
import com.symplified.order.utils.GenericPrintHelper;
import com.symplified.order.utils.PrinterUtility;
import com.symplified.order.utils.SunmiPrintHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    public static final Set<PairedDevice> btPrinters = new HashSet<>();
    public static final int PERMISSION_REQUEST_CODE = 10000;
    private static final String PRINTER_UUID = "00001101-0000-1000-8000-00805F9B34FB";

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

    public static void addBtPrinter(
            BluetoothDevice device,
            Context context
    ) {
        new Thread() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ContextCompat.checkSelfPermission(context, BLUETOOTH_CONNECT)
                                == PackageManager.PERMISSION_DENIED
                ) {
                    return;
                }

                if (device == null
                        || device.getName() == null
                        || !device.getName().startsWith("CloudPrint")) {
                    return;
                }

                for (PairedDevice storedPrinter : btPrinters) {
                    if (storedPrinter.device.getName()
                            .equals(device.getName())) {
                        return;
                    }
                }

                try {
                    BluetoothSocket socket = device
                            .createRfcommSocketToServiceRecord(UUID.fromString(PRINTER_UUID));
                    if (socket != null) {
                        socket.connect();
                        btPrinters.add(new PairedDevice(device, socket));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public static void printOrderReceipt(
            Order order,
            List<Item> items,
            String currency,
            Context context
    ) {
        new Thread() {
            @Override
            public  void run() {
                if (connectedPrinter != null && connectedPrinter.isPrinterConnected()) {
                    try {
                        connectedPrinter.printOrderReceipt(order, items, currency);
                    } catch (Exception e) {
                        Log.d("printer-helper", "Failed to print receipt: " + e.getLocalizedMessage());
                    }
                }
            }
        }.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                        context,
                        BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_DENIED
        ) {
            return;
        }

        byte[] dataToPrint = PrinterUtility.generateReceiptText(order, items, currency)
                .getBytes(StandardCharsets.UTF_8);

        for (PairedDevice btPrinter : btPrinters) {

            new Thread() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            ContextCompat.checkSelfPermission(context, BLUETOOTH_CONNECT)
                                    == PackageManager.PERMISSION_DENIED
                    ) {
                        return;
                    }

                    int noOfTries = 0;
                    while (noOfTries < 10) {
                        try {
                            if (btPrinter.socket == null) {
                                btPrinter.socket
                                        = btPrinter.device.createRfcommSocketToServiceRecord(
                                                UUID.fromString(PRINTER_UUID)
                                        );
                            }

                            if (!btPrinter.socket.isConnected()) {
                                btPrinter.socket.connect();
                            }
                            btPrinter.socket.getOutputStream().write(dataToPrint);
                            break;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        noOfTries++;
                    }
                }
            }.start();
        }
    }

    private static class PairedDevice {
        public final BluetoothDevice device;
        public BluetoothSocket socket;

        public PairedDevice(BluetoothDevice device, BluetoothSocket socket) {
            this.device = device;
            this.socket = socket;
        }
    }
}
