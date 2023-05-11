package com.symplified.order.models.product;

import com.symplified.order.enums.ProductStatus;

import java.io.Serializable;

public class ProductEditRequest implements Serializable {
    String name;
    ProductStatus status;
    public ProductEditRequest(Product product) {
        this.name = product.name;
        this.status = product.status;
    }
}
