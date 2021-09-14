package com.symplified.order.models.asset;

import com.symplified.order.models.HttpResponse;

public class Asset {
    public String storeId;
    public String logoUrl;
    public String bannerUrl;

    public static class AssetResponse extends HttpResponse{
        public Asset data;
    }
}
