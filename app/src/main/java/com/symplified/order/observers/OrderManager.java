package com.symplified.order.observers;

import com.symplified.order.models.order.Order;

public interface OrderManager {
    void addOrderToOngoingTab(Order.OrderDetails orderDetails);
    void editOrder(Order order);
}
