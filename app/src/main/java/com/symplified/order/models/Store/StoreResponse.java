package com.symplified.order.models.Store;

import com.symplified.order.models.HttpResponse;

public class StoreResponse extends HttpResponse {

    public Store.StoreList data;

    public static class SingleStoreResponse extends HttpResponse{
        public Store store;
    }

}
