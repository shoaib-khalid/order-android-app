package com.ekedai.merchant.models.order;

import com.ekedai.merchant.enums.DineInOption;
import com.ekedai.merchant.enums.DineInPack;
import com.ekedai.merchant.enums.OrderStatus;
import com.ekedai.merchant.enums.ServiceType;
import com.ekedai.merchant.models.HttpResponse;
import com.ekedai.merchant.models.store.Store;

import java.io.Serializable;
import java.util.List;

public class Order implements Serializable {
    public String id;
    public String storeId;
    public Double deliveryCharges;
    public Double subTotal;
    public Double total;
    public OrderStatus completionStatus;
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
    public Double voucherDiscount;
    public Double storeVoucherDiscount;
    public ServiceType serviceType;
    public DineInOption dineInOption;
    public DineInPack dineInPack;
    public OrderShipmentDetail orderShipmentDetail;
    public OrderPaymentDetail orderPaymentDetail;
    public Customer customer;
    public List<OrderRefund> orderRefund;
    public boolean isRevised;
    public Store store;

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
                ",  voucherDiscount='" + voucherDiscount + '\'' +
                ",  storeVoucherDiscount='" + storeVoucherDiscount + '\'' +
                ",  serviceType='" + (serviceType != null ? serviceType : "null") + '\'' +
                ",  dineInOption='" + (dineInOption != null ? dineInOption : "null") + '\'' +
                ",  dineInPack='" + (dineInPack != null ? dineInPack : "null") + '\'' +
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

    public static class OrderShipmentDetail implements Serializable {
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

        @Override
        public String toString() {
            return "OrderShipmentDetail{" +
                    "receiverName='" + receiverName + '\'' +
                    ", phoneNumber='" + phoneNumber + '\'' +
                    ", address='" + address + '\'' +
                    ", city='" + city + '\'' +
                    ", zipcode='" + zipcode + '\'' +
                    ", email='" + email + '\'' +
                    ", deliveryProviderId=" + deliveryProviderId +
                    ", state='" + state + '\'' +
                    ", country='" + country + '\'' +
                    ", trackingUrl='" + trackingUrl + '\'' +
                    ", orderId='" + orderId + '\'' +
                    ", storePickup=" + storePickup +
                    ", merchantTrackingUrl='" + merchantTrackingUrl + '\'' +
                    ", customerTrackingUrl='" + customerTrackingUrl + '\'' +
                    ", trackingNumber='" + trackingNumber + '\'' +
                    ", deliveryPeriodDetails=" + deliveryPeriodDetails +
                    '}';
        }
    }


    public static class OrderPaymentDetail implements Serializable {
        public String orderId;
        public String paymentChannel;
    }

    public static class Customer implements Serializable {
        public String id;

        public String name;

        public String email;
        public String phoneNumber;
        public String created;
        public String updated;
    }

    public static class OrderRefund implements Serializable {
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

    public static class OrderList implements Serializable {
        public List<Order> content;
    }

    public static class OrderDetailsList implements Serializable {
        public List<OrderDetails> content;
    }

    public static class OrderDetails implements Serializable {
        public Order order;
        public OrderStatus currentCompletionStatus;
        public OrderStatus nextCompletionStatus;
        public String nextActionText;

        public OrderDetails() {
        }

        public OrderDetails(UpdatedOrder updatedOrder) {
            this.order = updatedOrder;

            this.currentCompletionStatus = updatedOrder.completionStatus;
            this.nextCompletionStatus = updatedOrder.nextCompletionStatus;
            this.nextActionText = updatedOrder.nextActionText;
        }
    }

    public static class OrderUpdate {
        public String orderId;
        public OrderStatus status;

        public OrderUpdate(String orderId, OrderStatus status) {
            this.orderId = orderId;
            this.status = status;
        }
    }

    public static class OrderByIdResponse extends HttpResponse implements Serializable {
        public Order data;
    }
}