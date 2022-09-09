package com.symplified.order.models.order;

import com.symplified.order.models.HttpResponse;

import java.io.Serializable;

public class OrderUpdateResponse extends HttpResponse implements Serializable {
    public UpdatedOrder data;
}
