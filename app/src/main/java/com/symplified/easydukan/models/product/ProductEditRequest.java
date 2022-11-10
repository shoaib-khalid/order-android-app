package com.symplified.easydukan.models.product;

import java.io.Serializable;

public class ProductEditRequest implements Serializable {
    String name;
    String status;
    public ProductEditRequest(Product product) {
        this.name = product.name;
        this.status = product.status;
    }
}
