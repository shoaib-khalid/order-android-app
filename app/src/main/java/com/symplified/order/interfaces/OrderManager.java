package com.symplified.order.interfaces;

import com.symplified.order.models.order.Order;

public interface OrderManager {
    void addOrderToOngoingTab(Order.OrderDetails orderDetails);
    void addOrderToHistoryTab(Order.OrderDetails orderDetails);
    void editOrder(Order order);
}
