package com.symplified.order.models.store;

import com.symplified.order.models.HttpResponse;

public class StoreResponse extends HttpResponse {

    public Store.StoreList data;

    public static class SingleStoreResponse extends HttpResponse{
        public Store data;
    }

}
