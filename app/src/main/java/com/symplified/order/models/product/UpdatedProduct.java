package com.symplified.order.models.product;

import com.symplified.order.enums.ProductStatus;

import java.io.Serializable;

public class UpdatedProduct implements Serializable {
    public String name;
    public ProductStatus status;
    public UpdatedProduct(Product product) {
        this.name = product.name;
        this.status = product.status;
    }
}
