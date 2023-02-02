package com.symplified.order.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.iposprinter.iposprinterservice.IPosPrinterCallback;
import com.iposprinter.iposprinterservice.IPosPrinterService;
import com.symplified.order.enums.ServiceType;
import com.symplified.order.interfaces.Printer;
import com.symplified.order.interfaces.PrinterObserver;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.item.ItemAddOn;
import com.symplified.order.models.item.SubItem;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.qrorders.ConsolidatedOrder;
import com.symplified.order.models.staff.StaffMember;
import com.symplified.order.models.staff.shift.SummaryDetails;
import com.symplified.order.utils.Utility;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class GenericPrintHelper implements Printer {

    private static final String TAG = "generic-print-helper";
    private IPosPrinterService mIPosPrinterService;
    private static final GenericPrintHelper helper = new GenericPrintHelper();
    private boolean isPrinterConnected;
    private final List<PrinterObserver> printerObservers = new ArrayList<>();
    private final IPosPrinterCallback emptyCallback = new IPosPrinterCallback.Stub() {

        @Override
        public void onRunResult(final boolean isSuccess) throws RemoteException {
            Log.i(TAG,"result: " + isSuccess + "\n");
        }

        @Override
        public void onReturnString(final String value) throws RemoteException {
            Log.i(TAG,"result: " + value + "\n");
        }
    };

    private GenericPrintHelper() {
        this.isPrinterConnected = false;
    }

    public static GenericPrintHelper getInstance() {
        return helper;
    }

    @Override
    public void initPrinterService(Context context) {
        Intent intent = new Intent();
        intent.setPackage("com.iposprinter.iposprinterservice");
        intent.setAction("com.iposprinter.iposprinterservice.IPosPrintService");

        context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mIPosPrinterService = IPosPrinterService.Stub.asInterface(service);

                isPrinterConnected = true;
                for (PrinterObserver observer : printerObservers) {
                    observer.onPrinterConnected(helper);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mIPosPrinterService = null;
                isPrinterConnected = false;
            }
        }, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean isPrinterConnected() {
        return isPrinterConnected;
    }

    @Override
    public void addObserver(PrinterObserver observer) {
        printerObservers.add(observer);
    }

    @Override
    public void removeObserver(PrinterObserver observer) {
        printerObservers.remove(observer);
    }

    @Override
    public void printOrderReceipt(Order order, List<Item> items, Context context) throws Exception {
        if (!isPrinterConnected()) {
            return;
        }

        String currency = Utility.getCurrencySymbol(order, context);
        DecimalFormat formatter = Utility.getMonetaryAmountFormat();

        String divider = "\n------------------------";
        String divider2 = "\n************************";
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

        if (order.orderPaymentDetail != null) {
            prefix.append("\nPayment Type: ")
                    .append(order.orderPaymentDetail.paymentChannel);
        }

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
                .append("\nSub-total     ").append(currency).append(" ").append(formatter.format(order.subTotal))
                .append("\nSvc Chg       ").append(currency).append(" ")
                .append(order.storeServiceCharges != null
                        ? formatter.format(order.storeServiceCharges)
                        : " 0.00");

        suffix.append("\nDel. Chg      ").append(currency).append(" ")
                .append(order.deliveryCharges != null
                        ? formatter.format(order.deliveryCharges)
                        : " 0.00");

        suffix.append(divider)
                .append("\nTotal         ").append(currency).append(" ").append(formatter.format(order.total))
                .append(divider2)
                .append("\n");

        if (mIPosPrinterService != null) {
            mIPosPrinterService.printSpecifiedTypeText(title, "ST", 48, emptyCallback);
            mIPosPrinterService.printSpecifiedTypeText(String.valueOf(prefix), "ST", 32, emptyCallback);
            mIPosPrinterService.printSpecifiedTypeText(String.valueOf(itemText), "ST", 32, emptyCallback);
            mIPosPrinterService.printSpecifiedTypeText(String.valueOf(suffix), "ST", 32, emptyCallback);
            mIPosPrinterService.printerPerformPrint(150, emptyCallback);
        }
    }

    @Override
    public void printSalesSummary(StaffMember staffMember, List<SummaryDetails> summaryDetails, String currency) {

    }

    @Override
    public void printConsolidatedOrderReceipt(ConsolidatedOrder consolidatedOrder, String currencySymbol) throws Exception {

    }
}
