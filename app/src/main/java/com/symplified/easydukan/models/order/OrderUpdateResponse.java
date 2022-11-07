package com.symplified.easydukan.models.order;

import com.symplified.easydukan.models.HttpResponse;
import com.symplified.easydukan.models.order.UpdatedOrder;

import java.io.Serializable;

public class OrderUpdateResponse extends HttpResponse implements Serializable {
    public UpdatedOrder data;
}
