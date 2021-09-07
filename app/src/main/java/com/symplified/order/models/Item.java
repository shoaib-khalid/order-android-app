package com.symplified.order.models;

public class Item {

    public String item;
    public String specialInstruction;
    public String qty;
    public String price;

    public Item (String item, String specialInstruction, String qty, String price){

        this.item = item;
        this.specialInstruction = specialInstruction;
        this.qty = qty;
        this.price = price;

    }

}
