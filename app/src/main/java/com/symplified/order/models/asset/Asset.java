package com.symplified.order.models.asset;

import com.symplified.order.models.HttpResponse;

import java.io.Serializable;
import java.util.List;

public class Asset {
    public String storeId;
    public String logoUrl;
    public String bannerUrl;

    public static class AssetResponse extends HttpResponse implements Serializable {
        public Asset data;
    }
    public static class AllAssetResponse extends  HttpResponse implements Serializable {
        public List<Asset> data;
    }
}
