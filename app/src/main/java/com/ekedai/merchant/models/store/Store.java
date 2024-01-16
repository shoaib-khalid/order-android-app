package com.ekedai.merchant.models.store;

import com.ekedai.merchant.models.category.Category;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Store implements Serializable {

    public String id;
    public String name;
    public String clientId = "";
    public String verticalCode = "fnb";
    public String email = "default@kalsym.com";
    public Boolean isDineIn = true;
    public Boolean dineInConsolidatedOrder = true;
    public String storePrefix;
    public RegionCountry regionCountry = new RegionCountry();
    public List<StoreAsset> storeAssets = new ArrayList<>();
    public List<Category> categories = new ArrayList<>();
    public StoreStatusResponse.StoreStatus status = new StoreStatusResponse.StoreStatus();

    public static class RegionCountry implements Serializable{
        public String currencySymbol = "Rs. ";
        public String timezone = "Asia/Karachi";
    }

    public static class StoreAsset implements Serializable{
        public String assetUrl;
        public String assetType = "LogoUrl";
    }

    public static class StoreList {
        public List<Store> content;
    }

}
