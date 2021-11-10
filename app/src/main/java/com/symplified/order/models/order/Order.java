package com.symplified.order.models.order;

import androidx.annotation.NonNull;

import com.symplified.order.enums.Status;
import com.symplified.order.models.HttpResponse;

import java.io.Serializable;
import java.util.List;

public class Order implements Serializable {
    public String id;
    public String storeId;
    public double subTotal;

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", storeId='" + storeId + '\'' +
                ", subTotal=" + subTotal +
                ", deliveryCharges=" + deliveryCharges +
                ", total=" + total +
                ", completionStatus='" + completionStatus + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", customerNotes='" + customerNotes + '\'' +
                ", privateAdminNotes='" + privateAdminNotes + '\'' +
                ", cartId='" + cartId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", created='" + created + '\'' +
                ", updated='" + updated + '\'' +
                ", invoiceId='" + invoiceId + '\'' +
                ", klCommission=" + klCommission +
                ", storeServiceCharges=" + storeServiceCharges +
                ", storeShare=" + storeShare +
                ", paymentType='" + paymentType + '\'' +
                ", appliedDiscount=" + appliedDiscount +
                ", deliveryDiscount=" + deliveryDiscount +
                ", appliedDiscountDescription='" + appliedDiscountDescription + '\'' +
                ", deliveryDiscountDescription='" + deliveryDiscountDescription + '\'' +
                ", orderShipmentDetail=" + orderShipmentDetail +
                ", orderPaymentDetail=" + orderPaymentDetail +
                '}';
    }

    public double deliveryCharges;
    public double total;
    public String completionStatus;
    public String paymentStatus;
    public String customerNotes;
    public String privateAdminNotes;
    public String cartId;
    public String customerId;
    public String created;
    public String updated;
    public String invoiceId;
    public double klCommission;
    public double storeServiceCharges;
    public double storeShare;
    public String paymentType;
    public double appliedDiscount;
    public double deliveryDiscount;
    public String appliedDiscountDescription;
    public String deliveryDiscountDescription;
    public OrderShipmentDetail orderShipmentDetail;
    public OrderPaymentDetail orderPaymentDetail;


    public static class OrderShipmentDetail implements Serializable{
        public String receiverName;
        public String phoneNumber;
        public String address;
        public String city;
        public String zipcode;
        public String email;
        public int deliveryProviderId;
        public String state;
        public String country;
        public String trackingUrl;
        public String orderId;
        public boolean storePickup;
        public String merchantTrackingUrl;
        public String customerTrackingUrl;
        public String trackingNumber;
    }

    public static class OrderPaymentDetail implements Serializable{
        public String accountName;
        public String gatewayId;
        public Object couponId;
        public Object time;
        public String orderId;
        public String deliveryQuotationReferenceId;
        public double deliveryQuotationAmount;
    }

    public static class OrderList implements Serializable{
        public List<Order> content;
    }

    public static class OrderUpdate{
        public String orderId;
        public Status status;

        public OrderUpdate(String orderId, Status status){
            this.orderId = orderId;
            this.status = status;
        }
    }

    public static class UpdatedOrder extends HttpResponse implements Serializable{

        public Order data;

    }

    public static class OrderStatusDetailsResponse extends HttpResponse implements Serializable{
        public Order order;
        public String currentCompletionStatus;
        public String nextCompletionStatus;
        public String nextActionText;

        @Override
        public String toString() {
            return "OrderStatusDetailsResponse{" +
                    "order=" + order +
                    ", currentCompletionStatus='" + currentCompletionStatus + '\'' +
                    ", nextCompletionStatus='" + nextCompletionStatus + '\'' +
                    ", nextActionText='" + nextActionText + '\'' +
                    '}';
        }
    }

}