package com.ekedai.merchant.models.store;

import com.ekedai.merchant.models.category.Category;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Store implements Serializable {

    public String id;
    public String name;
    public String clientId;
    public String verticalCode;
    public String email;
    public Boolean isDineIn;
    public Boolean dineInConsolidatedOrder;
    public String storePrefix;
    public RegionCountry regionCountry;
    public List<StoreTiming> storeTiming;

    public List<StoreAsset> storeAssets;

    public List<Category> categories = new ArrayList<>();

    public static class RegionCountry implements Serializable{
        public String id;
        public String name;
        public String region;
        public String currency;
        public String currencyCode;
        public String currencySymbol;
        public String timezone;
    }

    static class StoreTiming implements Serializable{
        public String storeId;
        public String day;
        public String openTime;
        public String closeTime;
        public boolean isOff;
    }

    public static class StoreAsset implements Serializable{
        public String id;
        public String storeId;
        public String assetUrl;
        public String assetDescription;
        public String assetType;
    }

    public static class StoreList{
        public List<Store> content;
    }

}
