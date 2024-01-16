package com.ekedai.merchant.models.category;

import java.io.Serializable;
import java.util.List;

public class Category implements Serializable {
    public String id;
    public String storeId;
    public String name;

    public static class CategoryList {
        public List<Category> content;
    }
}
