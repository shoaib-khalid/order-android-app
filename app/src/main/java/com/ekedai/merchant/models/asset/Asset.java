package com.ekedai.merchant.models.asset;

import com.ekedai.merchant.models.HttpResponse;

import java.io.Serializable;
import java.util.List;

public class Asset {
    public String storeId;
    public String logoUrl;
    public String bannerUrl;

    public static class AssetResponse extends HttpResponse implements Serializable {
        public Asset data;
    }
    public static class AssetListResponse extends HttpResponse implements Serializable {
        public List<Asset> data;
    }
}
