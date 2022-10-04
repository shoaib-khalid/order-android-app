package com.symplified.order.helpers;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.SunmiPrinterService;
import com.sunmi.peripheral.printer.WoyouConsts;
import com.symplified.order.App;
import com.symplified.order.enums.ServiceType;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.item.SubItem;
import com.symplified.order.models.order.Order;
import com.symplified.order.observers.PrinterObserver;
import com.symplified.order.utils.Utility;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
        return isPrinterConnected;
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
            enableBoldFont();
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
        boolean hasPrinter = false;
        try {
            hasPrinter = InnerPrinterManager.getInstance().hasPrinter(service);
        } catch (Exception e) {
            handleException("Error while checking service ", e);
        }

        isPrinterConnected = hasPrinter;
        if (isPrinterConnected) {
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

    public void printReceipt(Order order, List<Item> items) throws RemoteException {

        if (!SunmiPrintHelper.getInstance().isPrinterConnected()) {
            return;
        }

        String currency = Utility.getCurrencySymbol(order);
        DecimalFormat formatter = Utility.getMonetaryAmountFormat();

        String divider = "\n-------------------------------";
        StringBuilder text = new StringBuilder();

        text.append(divider);
        text.append("\n\t");
        text.append(order.store != null ? order.store.name : "Deliverin.MY Order Chit");

        text.append(divider);
        text.append("\nOrder Id: ").append(order.invoiceId);

        if (order.created != null) {
            TimeZone storeTimeZone = order.store != null && order.store.regionCountry != null && order.store.regionCountry.timezone != null
                    ? TimeZone.getTimeZone(order.store.regionCountry.timezone)
                    : TimeZone.getDefault();
            text.append("\n").append(Utility.convertUtcTimeToLocalTimezone(order.created, storeTimeZone));
        }

        text.append("\nOrder Type: ");
        text.append(order.serviceType == ServiceType.DINEIN
                ? "Dine In"
                : order.orderShipmentDetail.storePickup ? "Self-Pickup" : "Delivery");

        if (order.orderShipmentDetail.phoneNumber != null
                && !"".equals(order.orderShipmentDetail.phoneNumber)) {
            text.append("\nCustomer no.: ").append(order.orderShipmentDetail.phoneNumber);
        }

        String customerNotes = order.customerNotes != null ? order.customerNotes : "";
        switch (customerNotes.toUpperCase()) {
            case "TAKEAWAY":
                customerNotes = "Take Away";
                break;
            case "SELFCOLLECT":
                customerNotes = "Self Collect";
                break;
        }
        if (!"".equals(customerNotes)) {
            text.append("\nNotes: ").append(customerNotes);
        }

        text.append(divider).append("\n");

        for (Item item : items) {
            text.append("\n").append(item.quantity).append(" x ").append(item.productName);
            String spacing = Integer.toString(item.quantity).replaceAll("\\d", " ") + " * ";
            if (item.productVariant != null && !item.productVariant.equals("")) {
                text.append("\n").append(spacing).append(item.productVariant);
            }

            for (SubItem subItem : item.orderSubItem) {
                text.append("\n").append(spacing).append(subItem.productName);
            }

            if (item.specialInstruction != null && !"".equals(item.specialInstruction)) {
                text.append("\nInstructions: ").append(item.specialInstruction);
            }

            text.append("\nPrice: ")
                    .append(currency)
                    .append(" ")
                    .append(formatter.format(item.price))
                    .append("\n");
        }

        text.append(divider);
        text.append("\nSub-total           ");
        text.append(currency).append(" ").append(formatter.format(order.subTotal));
        text.append("\nService Charges     ");
        text.append(currency).append(" ");
        text.append(order.storeServiceCharges != null
                ? formatter.format(order.storeServiceCharges)
                : " 0.00");
        if (order.serviceType != ServiceType.DINEIN) {
            text.append("\nDelivery Charges    ");
            text.append(currency).append(" ");
            text.append(order.deliveryCharges != null
                    ? formatter.format(order.deliveryCharges)
                    : " 0.00");
        }

        text.append(divider);

        text.append("\nTotal               ")
                .append(currency).append(" ")
                .append(formatter.format(order.total));

        if (printerService != null) {
            printerService.printTextWithFont(String.valueOf(text), null, 26, null);
        }
        helper.feedPaper();
    }

    private void enableBoldFont() {
        try {
            printerService.setPrinterStyle(WoyouConsts.ENABLE_BOLD, WoyouConsts.ENABLE);
        } catch (RemoteException e) {
            byte[] result = new byte[3];
            result[0] = 0x1B;
            result[1] = 69;
            result[2] = 0xF;
            try {
                printerService.sendRAWData(result, null);
            } catch (RemoteException ex) {}
        }
    }
}
