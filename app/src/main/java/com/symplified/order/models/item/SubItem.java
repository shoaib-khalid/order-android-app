package com.symplified.order.models.item;

import java.io.Serializable;

public class SubItem implements Serializable {
    public String id;

    public String orderItemId;
    public String productId;
    public Float price;
    public Float productPrice;
    public Float weight;

    public String SKU;
    public int quantity;
    public String itemCode;
    public String productName;
    public String specialInstruction;
    public String productVariant;
}
