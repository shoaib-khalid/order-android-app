package com.symplified.easydukan.models.order;

import com.symplified.easydukan.models.HttpResponse;

import java.io.Serializable;

public class OrderUpdateResponse extends HttpResponse implements Serializable {
    public UpdatedOrder data;
}
