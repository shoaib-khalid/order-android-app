package com.ekedai.merchant.models.item;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Item implements Serializable {
    public String id;
    public String itemCode;
    public String orderId;
    public List<SubItem> orderSubItem = new ArrayList<>();
    public List<ItemAddOn> orderItemAddOn = new ArrayList<>();
    public Double price;
    public String productId;
    public String productName;
    public Double productPrice;
    public String productVariant;
    public Integer quantity;
    public String specialInstruction;
    public String status;
    public Integer newQuantity;

    public Item(String id, String orderId, String itemCode, List<SubItem> orderSubItem, List<ItemAddOn> orderItemAddOn, Double price, String productId, String productName, Double productPrice, String productVariant, Integer quantity, String specialInstruction, String status, Integer newQuantity) {
        this.id = id;
        this.itemCode = itemCode;
        this.orderId = orderId;
        this.orderSubItem = orderSubItem;
        this.orderItemAddOn = orderItemAddOn;
        this.price = price;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.productVariant = productVariant;
        this.quantity = quantity;
        this.specialInstruction = specialInstruction;
        this.status = status;
        this.newQuantity = newQuantity;
    }

    public static class ItemList {
        public List<Item> content = new ArrayList<>();
    }
}
