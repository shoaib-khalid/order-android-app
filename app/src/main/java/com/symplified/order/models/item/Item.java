package com.symplified.order.models.item;

import java.util.List;

public class Item{
    public String id;
    public String orderId;
    public String productId;
    public double price;
    public double productPrice;
    public Object weight;
    public int quantity;
    public String itemCode;
    public String productName;
    public String specialInstruction;
    public String SKU;
    public String productVariant;

    public static class ItemList {
        public List<Item> content;
    }
}
