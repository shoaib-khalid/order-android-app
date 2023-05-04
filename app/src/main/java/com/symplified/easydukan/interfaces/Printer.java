package com.symplified.easydukan.interfaces;

import android.content.Context;

import com.symplified.easydukan.models.item.Item;
import com.symplified.easydukan.models.order.Order;
import com.symplified.easydukan.models.qrorders.ConsolidatedOrder;
import com.symplified.easydukan.models.staff.StaffMember;
import com.symplified.easydukan.models.staff.shift.SummaryDetails;

import java.util.List;

public interface Printer {
    boolean isPrinterConnected();
    void initPrinterService(Context context);
    void addObserver(PrinterObserver observer);
    void removeObserver(PrinterObserver observer);
    void printOrderReceipt(
            Order order,
            List<Item> items,
            String currency
    ) throws Exception;
    void printSalesSummary(
            StaffMember staffMember,
            List<SummaryDetails> summaryDetails,
            String currency
    );
    void printConsolidatedOrderReceipt(
            ConsolidatedOrder consolidatedOrder,
            String currency
    ) throws Exception;
}
