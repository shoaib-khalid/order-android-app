package com.ekedai.merchant.models.order;

import com.ekedai.merchant.models.HttpResponse;

import java.io.Serializable;

public class OrderUpdateResponse extends HttpResponse implements Serializable {
    public UpdatedOrder data;
}
