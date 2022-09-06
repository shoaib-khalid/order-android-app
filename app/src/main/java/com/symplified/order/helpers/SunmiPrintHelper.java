package com.symplified.order.helpers;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.SunmiPrinterService;
import com.symplified.order.App;
import com.symplified.order.enums.SunmiPrinterStatus;
import com.symplified.order.utils.Utility;

import java.util.Arrays;

public class SunmiPrintHelper {
    private String TAG = "sunmi-print-helper";

    private SunmiPrinterStatus status = SunmiPrinterStatus.CHECKING;

    private SunmiPrinterService printerService;

    private static SunmiPrintHelper helper = new SunmiPrintHelper();

    private SunmiPrintHelper() {}

    public static SunmiPrintHelper getInstance() {
        return helper;
    }

    public SunmiPrinterStatus getStatus() {
        return status;
    }

    private InnerPrinterCallback innerPrinterCallback = new InnerPrinterCallback() {
        @Override
        protected void onConnected(SunmiPrinterService service) {
            printerService = service;
            Utility.logToFile("\nPrinter connected\n");
            updateSunmiPrinterService(service);
        }

        @Override
        protected void onDisconnected() {
            Utility.logToFile("\nPrinter disconnected\n");
            printerService = null;
            status = SunmiPrinterStatus.LOST;
        }
    };

    public void initSunmiPrinterService(Context context) {
        try {
            boolean isServiceBound = InnerPrinterManager.getInstance()
                    .bindService(context, innerPrinterCallback);
            Utility.logToFile("\ninitSunmiPrinterService\n");
            if (!isServiceBound) {
                status = SunmiPrinterStatus.NOT_FOUND;
            }
        } catch (Exception e) {
            handleException("Error while initSunmiPrinterService", e);
        }
    }

    public void deInitSunmiPrinterService(Context context) {
        try {
            if (printerService != null) {
                InnerPrinterManager.getInstance().unBindService(context, innerPrinterCallback);
                printerService = null;
                status = SunmiPrinterStatus.LOST;
            }
        } catch (Exception e) {
            handleException("Error occurred while deInitSunmiPrinterService", e);
        }
    }

    public void printText(String content) {
        try {
            Utility.logToFile("\nPrinting receipt text\n");
            printerService.printText(content, null);
        } catch (Exception e) {
            handleException("Error occurred when printing", e);
        }
    }

    private void updateSunmiPrinterService(SunmiPrinterService service) {
        boolean hasPrinter = false;
        try {
            hasPrinter = InnerPrinterManager.getInstance().hasPrinter(service);
        } catch (Exception e) {
            handleException("Error while checking service ", e);
        }
        status = hasPrinter
                ? SunmiPrinterStatus.FOUND
                : SunmiPrinterStatus.NOT_FOUND;
    }

    private void handleException(String preamble, Exception ex) {
        String errorMessage = preamble + ": " + ex.getLocalizedMessage();

        Utility.logToFile("\n" + errorMessage + "\n" + Arrays.toString(ex.getStackTrace()) + "\n");

        Toast.makeText(App.getAppContext(), errorMessage, Toast.LENGTH_SHORT).show();
        Log.e(TAG, errorMessage);
        ex.printStackTrace();
    }
}
