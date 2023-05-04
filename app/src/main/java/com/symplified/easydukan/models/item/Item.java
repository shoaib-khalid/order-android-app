package com.symplified.easydukan.models.item;

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

    public static class ItemList {
        public List<Item> content = new ArrayList<>();
    }
}
