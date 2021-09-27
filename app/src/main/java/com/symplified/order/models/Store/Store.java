package com.symplified.order.models.Store;

import java.io.Serializable;
import java.util.List;

public class Store implements Serializable {

    public String id;
    public String name;
    public String city;
    public String address;
    public String clientId;
    public String verticalCode;
    public String storeDescription;
    public String postcode;
    public String email;
    public String domain;
    public String liveChatOrdersGroupId;
    public String liveChatOrdersGroupName;
    public String liveChatCsrGroupId;
    public String liveChatCsrGroupName;
    public String regionCountryId;
    public String phoneNumber;
    public String regionCountryStateId;
    public String paymentType;
    public int serviceChargesPercentage;
    public RegionCountry regionCountry;
    public List<StoreTiming> storeTiming;

    public static class RegionCountry{
        public String id;
        public String name;
        public String region;
        public String currency;
        public String currencyCode;
        public String currencySymbol;
        public String timezone;
    }

    static class StoreTiming{
        public String storeId;
        public String day;
        public String openTime;
        public String closeTime;
        public boolean isOff;
    }

    public static class StoreList{
        public List<Store> content;
    }

}
