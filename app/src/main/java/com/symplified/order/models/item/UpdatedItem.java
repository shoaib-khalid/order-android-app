package com.symplified.order.models.item;

import java.util.List;

public class UpdatedItem {
    public String id;
    public String itemCode;
    public Integer quantity;

    @Override
    public String toString() {
        return "UpdatedItem{" +
                "id='" + id + '\'' +
                ", itemCode='" + itemCode + '\'' +
                ", quantity=" + quantity +
                '}';
    }

    public UpdatedItem(){}

    public UpdatedItem(String id, String itemCode, Integer quantity) {
        this.id = id;
        this.itemCode = itemCode;
        this.quantity = quantity;
    }

    public static class RequestBodyItemList{
        public List<UpdatedItem> bodyOrderItemList;
    }
}
