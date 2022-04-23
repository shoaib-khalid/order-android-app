package com.symplified.order.models.order;

import androidx.annotation.NonNull;

import com.symplified.order.enums.Status;
import com.symplified.order.models.HttpResponse;
import com.symplified.order.models.Store.Store;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Order implements Serializable {
    public String id;
    public String storeId;
    public Double deliveryCharges;
    public Double subTotal;
    public Double total;
    public String completionStatus;
    public String paymentStatus;
    public String customerNotes;
    public String privateAdminNotes;
    public String cartId;
    public String customerId;
    public String created;
    public String updated;
    public String invoiceId;
    public Double klCommission;
    public Double storeServiceCharges;
    public Double storeShare;
    public String paymentType;
    public Double appliedDiscount;
    public Double deliveryDiscount;
    public String appliedDiscountDescription;
    public String deliveryDiscountDescription;
    public OrderShipmentDetail orderShipmentDetail;
    public OrderPaymentDetail orderPaymentDetail;
//    private Store store;
    public Customer customer;
    public List<OrderRefund> orderRefund;
    public boolean isRevised;

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", storeId='" + storeId + '\'' +
                ", deliveryCharges=" + deliveryCharges +
                ", subTotal=" + subTotal +
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
                ", customer=" + customer +
                ", orderRefund=" + orderRefund +
                ", isRevised=" + isRevised +
                '}';
    }

    public static class DeliveryPeriod implements Serializable {
        public String id;

        public String name;

        public String description;

        @Override
        public String toString() {
            return "DeliveryPeriod{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }

    public static class OrderShipmentDetail implements Serializable{
        public String receiverName;
        public String phoneNumber;
        public String address;
        public String city;
        public String zipcode;
        public String email;
        public Integer deliveryProviderId;
        public String state;
        public String country;
        public String trackingUrl;
        public String orderId;
        public boolean storePickup;
        public String merchantTrackingUrl;
        public String customerTrackingUrl;
        public String trackingNumber;
        public DeliveryPeriod deliveryPeriodDetails;
    }

    public static class OrderPaymentDetail implements Serializable{
        public String accountName;
        public String gatewayId;
        public String couponId;
        public String time;
        public String orderId;
        public String deliveryQuotationReferenceId;
        public Double deliveryQuotationAmount;
    }

    public static class Customer implements Serializable{
        public String id;

        public String name;

        public String email;
        public String phoneNumber;
        public String created;
        public String updated;
    }

    public static class OrderRefund implements Serializable{
        private String id;

        public String orderId;
        public Double refundAmount;
        public String remarks;
        public String paymentChannel;
        public String refundStatus;
        public String refundType;
        public String created;
        public String updated;
        public String refunded;

        @Override
        public String toString() {
            return "OrderRefund{" +
                    "id='" + id + '\'' +
                    ", orderId='" + orderId + '\'' +
                    ", refundAmount=" + refundAmount +
                    ", remarks='" + remarks + '\'' +
                    ", paymentChannel='" + paymentChannel + '\'' +
                    ", refundStatus='" + refundStatus + '\'' +
                    ", refundType='" + refundType + '\'' +
                    ", created='" + created + '\'' +
                    ", updated='" + updated + '\'' +
                    ", refunded='" + refunded + '\'' +
                    '}';
        }
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

    public static class OrderByIdResponse extends HttpResponse implements Serializable{
        public Order data;
    }

}