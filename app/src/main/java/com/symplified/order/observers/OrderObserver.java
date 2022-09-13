package com.symplified.order.observers;

import com.symplified.order.models.order.Order;

public interface OrderObserver {
    void onOrderReceived(Order.OrderDetails orderDetails);
    void onOrderEdited(String invoiceId);
    void setOrderManager(OrderManager orderManager);
}
