package com.symplified.order.models;

public class OrderDetailsModel {

    public String name, phone,quantity,amount, invoice;

    public OrderDetailsModel(String name, String phone, String quantity, String amount, String invoice) {
        this.name = name;
        this.phone = phone;
        this.quantity = quantity;
        this.amount = amount;
        this.invoice = invoice;
    }
}
