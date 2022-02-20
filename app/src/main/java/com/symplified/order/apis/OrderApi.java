package com.symplified.order.apis;

import com.symplified.order.models.Store.StoreResponse;
import com.symplified.order.models.item.ItemResponse;
import com.symplified.order.models.item.UpdatedItem;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderDeliveryDetailsResponse;
import com.symplified.order.models.order.OrderResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface OrderApi {

////    https://api.symplified.it/order-service/v1/orders
//    @GET("orders?receiverName=&phoneNumber=&completionStatus=PAYMENT_CONFIRMED&pageSize=10&")
//    Call<ResponseBody> getNewOrders(@HeaderMap Map<String, String> headers, @Query("storeId") String storeId);


    @GET("orders?receiverName=&phoneNumber=&completionStatus=PAYMENT_CONFIRMED&pageSize=10&")
    Call<ResponseBody> getNewOrdersByClientId(@HeaderMap Map<String, String> headers, @Query("clientId") String clientId);



    @GET("orders?receiverName=&phoneNumber=&completionStatus=AWAITING_PICKUP&pageSize=10&")
    Call<ResponseBody> getPickupOrdersByClientId(@HeaderMap Map<String, String> headers, @Query("clientId") String clientId);
//
//    @GET("orders?receiverName=&phoneNumber=&completionStatus=BEING_PREPARED&pageSize=10&")
//    Call<ResponseBody> getProcessedOrders (@HeaderMap Map<String, String> headers, @Query("storeId") String storeId);

    @GET("orders?receiverName=&phoneNumber=&completionStatus=BEING_PREPARED&pageSize=10&")
    Call<ResponseBody> getProcessedOrdersByClientId (@HeaderMap Map<String, String> headers, @Query("clientId") String clientId);

//
//    @GET("orders?receiverName=&phoneNumber=&completionStatus=BEING_DELIVERED&pageSize=10&")
//    Call<ResponseBody> getSentOrders (@HeaderMap Map<String, String> headers, @Query("storeId") String storeId);


    @GET("orders?receiverName=&phoneNumber=&completionStatus=BEING_DELIVERED&pageSize=10&")
    Call<ResponseBody> getSentOrdersByClientId (@HeaderMap Map<String, String> headers, @Query("clientId") String clientId);


    @GET("orders/{orderId}/items")
    Call<ItemResponse> getItemsForOrder(@HeaderMap Map<String,String> headers , @Path("orderId") String storeId);

    @PUT("orders/{orderId}/completion-status-updates")
    Call<ResponseBody> updateOrderStatus(@HeaderMap Map<String, String> headers, @Body Order.OrderUpdate body, @Path("orderId") String orderId);

    @GET("orders/details/{orderId}")
    Call<ResponseBody> getOrderStatusDetails(@HeaderMap Map<String,String> headers, @Path("orderId") String orderId);

    @GET("orders/{orderId}")
    Call<Order.OrderByIdResponse> getOrderById(@HeaderMap Map<String,String> headers, @Path("orderId") String orderId);

    @PUT("orders/reviseitem/{orderId}")
    Call<ResponseBody> reviseOrderItem(@HeaderMap Map<String,String> headers, @Path("orderId") String orderId, @Body List<UpdatedItem> bodyOrderItemList);

}
