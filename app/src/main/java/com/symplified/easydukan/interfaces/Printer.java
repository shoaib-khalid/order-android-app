package com.symplified.easydukan.interfaces;

import android.content.Context;

import com.symplified.easydukan.models.item.Item;
import com.symplified.easydukan.models.order.Order;

import java.util.List;

public interface Printer {
    boolean isPrinterConnected();
    void initPrinterService(Context context);
    void addObserver(PrinterObserver observer);
    void removeObserver(PrinterObserver observer);
    void printReceipt(Order order, List<Item> items) throws Exception;
}
