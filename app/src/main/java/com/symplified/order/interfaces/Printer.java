package com.symplified.order.interfaces;

import android.content.Context;

import com.symplified.order.models.item.Item;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.staff.StaffMember;
import com.symplified.order.models.staff.shift.SummaryDetails;

import java.util.List;

public interface Printer {
    boolean isPrinterConnected();
    void initPrinterService(Context context);
    void addObserver(PrinterObserver observer);
    void removeObserver(PrinterObserver observer);
    void printReceipt(Order order, List<Item> items, Context context) throws Exception;
    void printSalesSummary(
            StaffMember staffMember,
            List<SummaryDetails> summaryDetails,
            String currency
    );
}
