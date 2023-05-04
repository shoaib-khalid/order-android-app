package com.symplified.easydukan.models.store;

import com.symplified.easydukan.models.HttpResponse;

public class StoreResponse extends HttpResponse {

    public Store.StoreList data;

    public static class SingleStoreResponse extends HttpResponse{
        public Store data;
    }

}
