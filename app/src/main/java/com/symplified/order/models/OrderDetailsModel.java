package com.symplified.order.models;

import com.symplified.order.models.order.Order;

public class OrderDetailsModel {

    public String name, phone,quantity,amount, invoice;

    public OrderDetailsModel(String name, String phone, String quantity, String amount, String invoice) {
        this.name = name;
        this.phone = phone;
        this.quantity = quantity;
        this.amount = amount;
        this.invoice = invoice;
    }

    public static OrderDetailsModel fromOrder(Order order)
    {
        return new OrderDetailsModel(order.orderShipmentDetail.receiverName, order.orderShipmentDetail.phoneNumber, "3", order.total+"", order.invoiceId);
    }
}
