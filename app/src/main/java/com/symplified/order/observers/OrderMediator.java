package com.symplified.order.observers;

import com.symplified.order.models.order.Order;

public interface OrderMediator {
    void addOrderToOngoingTab(Order.OrderDetails orderDetails);
}
