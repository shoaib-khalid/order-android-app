package com.symplified.order.helpers;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.SunmiPrinterService;
import com.sunmi.peripheral.printer.WoyouConsts;
import com.symplified.order.enums.ServiceType;
import com.symplified.order.interfaces.Printer;
import com.symplified.order.interfaces.PrinterObserver;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.item.ItemAddOn;
import com.symplified.order.models.item.SubItem;
import com.symplified.order.models.order.Order;
import com.symplified.order.utils.Utility;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class SunmiPrintHelper implements Printer {

    private static final String TAG = "sunmi-print-helper";
    private SunmiPrinterService printerService;
    private static final SunmiPrintHelper helper = new SunmiPrintHelper();
    private boolean isPrinterConnected;
    private final List<PrinterObserver> printerObservers = new ArrayList<>();

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

    private final InnerPrinterCallback innerPrinterCallback = new InnerPrinterCallback() {
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
        }
    };

    public void initPrinterService(Context context) {
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

    private void feedPaper() {
        if (printerService != null) {
            try {
                printerService.autoOutPaper(null);
            } catch (Exception e) {
                handleException("Error while calling autoOutPaper. Printing 3 lines instead", e);
                print3Lines();
            }
        }
    }

    private void print3Lines() {
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
                observer.onPrinterConnected(helper);
            }
        }
    }

    private static void handleException(String preamble, Exception ex) {
        String errorMessage = preamble + ": " + ex.getLocalizedMessage();

        Log.e(TAG, errorMessage);
        ex.printStackTrace();
    }

    public void printReceipt(Order order, List<Item> items, Context context) throws Exception {

        if (!isPrinterConnected()) {
            return;
        }

        String currency = Utility.getCurrencySymbol(order, context);
        DecimalFormat formatter = Utility.getMonetaryAmountFormat();

        String divider = "\n----------------------------";
        String divider2 = "\n****************************";
        StringBuilder prefix = new StringBuilder();
        StringBuilder suffix = new StringBuilder();
        StringBuilder itemText = new StringBuilder();

        String customerNotes = order.customerNotes != null ? order.customerNotes : "";
        switch (customerNotes.toUpperCase()) {
            case "TAKEAWAY":
                customerNotes = "Take Away";
                break;
            case "SELFCOLLECT":
                customerNotes = "Self Collect";
                break;
        }

        String title = "\n\t" + (order.serviceType == ServiceType.DINEIN
                ? customerNotes
                : order.store != null ? order.store.name : "Deliverin.MY Order Chit");

        prefix.append(divider);
        prefix.append("\nOrder Id: ").append(order.invoiceId);

        if (order.created != null) {
            TimeZone storeTimeZone = order.store != null && order.store.regionCountry != null && order.store.regionCountry.timezone != null
                    ? TimeZone.getTimeZone(order.store.regionCountry.timezone)
                    : TimeZone.getDefault();
            prefix.append("\n").append(Utility.convertUtcTimeToLocalTimezone(order.created, storeTimeZone));
        }

        prefix.append("\nOrder Type: ");
        prefix.append(order.serviceType == ServiceType.DINEIN
                ? "Dine In"
                : order.orderShipmentDetail.storePickup ? "Self-Pickup" : "Delivery");

        if (order.orderShipmentDetail.phoneNumber != null
                && !"".equals(order.orderShipmentDetail.phoneNumber)) {
            prefix.append("\nCustomer no.: ").append(order.orderShipmentDetail.phoneNumber);
        }

        if (!"".equals(customerNotes) && order.serviceType != ServiceType.DINEIN) {
            prefix.append("\nNotes: ").append(customerNotes);
        }

        prefix.append(divider).append("\n");

        for (Item item : items) {
            itemText.append("\n").append(item.quantity).append(" x ").append(item.productName);
            String spacing = Integer.toString(item.quantity).replaceAll("\\d", " ") + " * ";

            if (item.productVariant != null && !item.productVariant.equals("")) {
                itemText.append("\n").append(spacing).append(item.productVariant);
            }

            for (SubItem subItem : item.orderSubItem) {
                itemText.append("\n").append(spacing).append(subItem.productName);
            }

            for (ItemAddOn itemAddOn : item.orderItemAddOn) {
                itemText.append("\n").append(spacing).append(itemAddOn.productAddOn.addOnTemplateItem.name);
            }

            if (item.specialInstruction != null && !"".equals(item.specialInstruction)) {
                itemText.append("\nInstructions: ").append(item.specialInstruction);
            }

            itemText.append("\nPrice: ")
                    .append(currency)
                    .append(" ")
                    .append(formatter.format(item.price))
                    .append("\n");
        }

        suffix.append(divider)
                .append("\nSub-total           ").append(currency).append(" ").append(formatter.format(order.subTotal))
                .append("\nService Charges     ").append(currency).append(" ")
                .append(order.storeServiceCharges != null
                        ? formatter.format(order.storeServiceCharges)
                        : " 0.00");

        if (order.serviceType != ServiceType.DINEIN) {
            suffix.append("\nDelivery Charges    ").append(currency).append(" ")
                    .append(order.deliveryCharges != null
                            ? formatter.format(order.deliveryCharges)
                            : " 0.00");
        }

        suffix.append(divider)
                .append("\nTotal               ").append(currency).append(" ").append(formatter.format(order.total))
                .append(divider2)
                .append("\n");

        if (printerService != null) {
            printerService.printTextWithFont(title, null, 34, null);
            printerService.printTextWithFont(String.valueOf(prefix), null, 26, null);
            printerService.printTextWithFont(String.valueOf(itemText), null, 30, null);
            printerService.printTextWithFont(String.valueOf(suffix), null, 26, null);
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
            } catch (RemoteException ignored) {}
        }
    }
}
