package com.ekedai.merchant.models.product;

import com.ekedai.merchant.enums.ProductStatus;

import java.io.Serializable;

public class UpdatedProduct implements Serializable {
    public String name;
    public ProductStatus status;
    public UpdatedProduct(Product product) {
        this.name = product.name;
        this.status = product.status;
    }
}
