package com.symplified.order.apis;

import com.symplified.order.models.HttpResponse;
import com.symplified.order.models.Store.StoreResponse;
import com.symplified.order.models.item.Item;
import com.symplified.order.models.item.ItemResponse;
import com.symplified.order.models.item.UpdatedItem;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderDeliveryDetailsResponse;
import com.symplified.order.models.order.OrderDetailsResponse;
import com.symplified.order.models.order.OrderResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Callback;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface OrderApi {

////    https://api.symplified.it/order-service/v1/orders
//    @GET("orders?receiverName=&phoneNumber=&completionStatus=PAYMENT_CONFIRMED&pageSize=10&")
//    Call<ResponseBody> getNewOrders(@HeaderMap Map<String, String> headers, @Query("storeId") String storeId);


    @GET("orders/search?completionStatus=PAYMENT_CONFIRMED&completionStatus=RECEIVED_AT_STORE")
    Call<OrderDetailsResponse> getNewOrdersByClientId(@HeaderMap Map<String, String> headers, @Query("clientId") String clientId);

    @GET("orders/search?completionStatus=PAYMENT_CONFIRMED&completionStatus=RECEIVED_AT_STORE")
    Call<OrderDetailsResponse> getNewOrdersByClientId(@Query("clientId") String clientId);

    @GET("orders?receiverName=&phoneNumber=&completionStatus=AWAITING_PICKUP&pageSize=10&")
    Call<ResponseBody> getPickupOrdersByClientId(@HeaderMap Map<String, String> headers, @Query("clientId") String clientId);
//
//    @GET("orders?receiverName=&phoneNumber=&completionStatus=BEING_PREPARED&pageSize=10&")
//    Call<ResponseBody> getProcessedOrders (@HeaderMap Map<String, String> headers, @Query("storeId") String storeId);

    @GET("orders/search?completionStatus=BEING_PREPARED&completionStatus=AWAITING_PICKUP&completionStatus=BEING_DELIVERED")
    Call<OrderDetailsResponse> getOngoingOrdersByClientId (@HeaderMap Map<String, String> headers, @Query("clientId") String clientId);

//
//    @GET("orders?receiverName=&phoneNumber=&completionStatus=BEING_DELIVERED&pageSize=10&")
//    Call<ResponseBody> getSentOrders (@HeaderMap Map<String, String> headers, @Query("storeId") String storeId);


    @GET("orders/search?completionStatus=CANCELED_BY_MERCHANT&completionStatus=DELIVERED_TO_CUSTOMER&completionStaus=CANCELED_BY_CUSTOMER")
    Call<OrderDetailsResponse> getSentOrdersByClientId (@HeaderMap Map<String, String> headers, @Query("clientId") String clientId);


    @GET("orders/{orderId}/items")
    Call<ItemResponse> getItemsForOrder(@HeaderMap Map<String,String> headers , @Path("orderId") String storeId);

    @PUT("orders/{orderId}/completion-status-updates")
    Call<ResponseBody> updateOrderStatus(@HeaderMap Map<String, String> headers, @Body Order.OrderUpdate body, @Path("orderId") String orderId);

    @GET("orders/details/{orderId}")
    Call<ResponseBody> getOrderStatusDetails(@HeaderMap Map<String,String> headers, @Path("orderId") String orderId);

//    @GET("orders/{orderId}")
//    Call<Order.OrderByIdResponse> getOrderById(@HeaderMap Map<String,String> headers, @Path(value = "orderId", encoded = true) String orderId);

    @GET("orders")
    Call<OrderResponse> getOrderByInvoiceId(@HeaderMap Map<String,String> headers, @Query("invoiceId") String invoiceId);

    @PUT("orders/reviseitem/{orderId}")
    Call<HttpResponse> reviseOrderItem(@HeaderMap Map<String,String> headers, @Path("orderId") String orderId, @Body List<UpdatedItem> bodyOrderItemList);

}
