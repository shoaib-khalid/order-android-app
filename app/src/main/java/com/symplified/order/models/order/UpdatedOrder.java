package com.symplified.order.models.order;

import com.symplified.order.enums.OrderStatus;

public class UpdatedOrder extends Order {
    OrderStatus nextCompletionStatus;
    String nextActionText;
}
