package com.symplified.order.models.item;

import java.util.List;

public class Item{
    public String SKU;
    public String discountCalculationType;
    public Double discountCalculationValue;
    public String discountId;
    public String discountLabel;
    public String id;
    public String itemCode;
    public Double normalPrice;
    public String orderId;
    public List<SubItem> orderSubItem;
    public Integer originalQuantity;
    public Double price;
    public String productId;
    public String productName;
    public Double productPrice;
    public String productVariant;
    public Integer quantity;
    public String specialInstruction;
    public String status;
    public Double weight;

    public static class ItemList {
        public List<Item> content;
    }
}
