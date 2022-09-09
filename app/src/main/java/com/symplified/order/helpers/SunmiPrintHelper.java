package com.symplified.order.helpers;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.SunmiPrinterService;
import com.symplified.order.App;
import com.symplified.order.observers.PrinterObserver;

import java.util.ArrayList;
import java.util.List;

public class SunmiPrintHelper {
    private static final String TAG = "sunmi-print-helper";
    private SunmiPrinterService printerService;
    private static SunmiPrintHelper helper = new SunmiPrintHelper();
    private boolean isPrinterConnected;
    private List<PrinterObserver> printerObservers = new ArrayList<>();

    private SunmiPrintHelper() {
        this.isPrinterConnected = false;
    }

    public static SunmiPrintHelper getInstance() {
        return helper;
    }

    public boolean isPrinterConnected() {
        try {
            return InnerPrinterManager.getInstance().hasPrinter(printerService);
        } catch (Exception e) {
            handleException("Error while checking service ", e);
        }
        return false;
    }

    public void addObserver(PrinterObserver observer) {
        printerObservers.add(observer);
    }

    public void removeObserver(PrinterObserver observer) {
        printerObservers.remove(observer);
    }

    private InnerPrinterCallback innerPrinterCallback = new InnerPrinterCallback() {
        @Override
        protected void onConnected(SunmiPrinterService service) {
            printerService = service;
            updateSunmiPrinterService(service);
        }

        @Override
        protected void onDisconnected() {
            printerService = null;
            isPrinterConnected = false;

            for (PrinterObserver observer : printerObservers) {
                observer.onPrinterDisconnected();
            }
        }
    };

    public void initSunmiPrinterService(Context context) {
        try {
            boolean isServiceBound = InnerPrinterManager.getInstance()
                    .bindService(context, innerPrinterCallback);
            if (!isServiceBound) {
                isPrinterConnected = false;
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
                isPrinterConnected = false;
            }
        } catch (Exception e) {
            handleException("Error occurred while deInitSunmiPrinterService", e);
        }
    }

    public void printText(String content) {
        if (printerService != null) {
            try {
                printerService.printText(content, null);
            } catch (Exception e) {
                handleException("Error occurred when printing", e);
                Toast.makeText(App.getAppContext(), "Error occurred while printing. Please try again", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void feedPaper() {
        if (printerService != null) {
            try {
                printerService.autoOutPaper(null);
            } catch (Exception e) {
                handleException("Error while calling autoOutPaper. Printing 3 lines instead", e);
                print3Lines();
            }
        }
    }

    public void print3Lines() {
        if (printerService != null) {
            try {
                printerService.lineWrap(3, null);
            } catch (Exception e) {
                handleException("Error while printing 3 lines", e);
            }
        }
    }

    private void updateSunmiPrinterService(SunmiPrinterService service) {
        if (isPrinterConnected()) {
            for (PrinterObserver observer : printerObservers) {
                observer.onPrinterConnected();
            }
        }
    }

    private static void handleException(String preamble, Exception ex) {
        String errorMessage = preamble + ": " + ex.getLocalizedMessage();

        Log.e(TAG, errorMessage);
        ex.printStackTrace();
    }
}
