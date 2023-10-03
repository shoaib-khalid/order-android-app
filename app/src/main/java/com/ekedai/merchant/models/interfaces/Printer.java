package com.ekedai.merchant.models.interfaces;

import android.content.Context;

import com.ekedai.merchant.models.item.Item;
import com.ekedai.merchant.models.order.Order;
import com.ekedai.merchant.models.qrorders.ConsolidatedOrder;
import com.ekedai.merchant.models.staff.StaffMember;
import com.ekedai.merchant.models.staff.shift.SummaryDetails;

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
