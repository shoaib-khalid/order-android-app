package com.symplified.order.models.asset;

import com.symplified.order.models.HttpResponse;

import java.io.Serializable;
import java.util.List;

public class StoreProductAsset {
    public String id;
    public String itemCode;
    public String name;
    public String url;
    public String productId;
    public boolean isThumbnail;

    public static class StoreProductAssetListResponse extends HttpResponse implements Serializable {
        public List<StoreProductAsset> data;
    }
}
