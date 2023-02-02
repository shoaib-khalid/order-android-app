package com.symplified.order.models.qrorders;

import com.symplified.order.models.item.Item;

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
}
