package com.ekedai.merchant.models.qrorders;

import com.ekedai.merchant.models.item.Item;

import java.io.Serializable;
import java.util.List;

public class ConsolidatedOrder implements Serializable {
    public String invoiceNo;
    public String tableNo;
    public String storeId;
    public String orderTimeConverted;
    public Double subTotal;
    public Double appliedDiscount;
    public Double serviceCharges;
    public Double totalOrderAmount;
    public List<Item> orderItemWithDetails;

    public boolean isPaid = false;
    public Double changeDue;
    public Double localCashPaymentAmount;
}
