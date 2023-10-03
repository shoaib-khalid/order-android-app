package com.ekedai.merchant.models.store;

import com.ekedai.merchant.models.HttpResponse;

public class StoreResponse extends HttpResponse {

    public Store.StoreList data;

    public static class SingleStoreResponse extends HttpResponse {
        public Store data;
    }

}
