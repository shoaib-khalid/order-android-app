package com.symplified.order.models.category;

import java.io.Serializable;
import java.util.List;

public class Category implements Serializable {
    public String id;
    public String storeId;
    public String parentCategoryId;
    public String name;
    public String thumbnailUrl;
    public String displaySequence;

    public static class CategoryList {
        public List<Category> content;
    }

    @Override
    public String toString() {
        return "Category{" +
                "id='" + id + '\'' +
                ", storeId='" + storeId + '\'' +
                ", parentCategoryId='" + parentCategoryId + '\'' +
                ", name='" + name + '\'' +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", displaySequence='" + displaySequence + '\'' +
                '}';
    }
}
