package com.symplified.order.apis;

import com.symplified.order.models.HttpResponse;
import com.symplified.order.models.item.ItemsResponse;
import com.symplified.order.models.item.UpdatedItem;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderDetailsResponse;
import com.symplified.order.models.order.OrderUpdateResponse;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface OrderApi {

    @GET("orders/search?completionStatus=PAYMENT_CONFIRMED,RECEIVED_AT_STORE&sortingOrder=ASC&pageSize=1000000")
    Call<OrderDetailsResponse> getNewOrdersByClientId(@Query("clientId") String clientId);

    @GET("orders/search?completionStatus=PAYMENT_CONFIRMED,RECEIVED_AT_STORE&sortingOrder=ASC&pageSize=1000000")
    Call<OrderDetailsResponse> getNewOrdersByClientIdAndInvoiceId(@Query("clientId") String clientId, @Query("invoiceId") String invoiceId);

    @GET("orders/search?completionStatus=BEING_PREPARED,AWAITING_PICKUP,BEING_DELIVERED&sortingOrder=ASC&pageSize=1000000")
    Call<OrderDetailsResponse> getOngoingOrdersByClientId (@Query("clientId") String clientId);

    @GET("orders/search?completionStatus=CANCELED_BY_MERCHANT,DELIVERED_TO_CUSTOMER,CANCELED_BY_CUSTOMER")
    Call<OrderDetailsResponse> getSentOrdersByClientId (@Query("clientId") String clientId, @Query("from") String from, @Query("to") String to);

    @GET("orders/{orderId}/items")
    Call<ItemsResponse> getItemsForOrder(@Path("orderId") String orderId);

    @PUT("orders/{orderId}/completion-status-updates")
    Call<OrderUpdateResponse> updateOrderStatus(@Body Order.OrderUpdate body, @Path("orderId") String orderId);

    @GET("orders/details/{orderId}")
    Call<ResponseBody> getOrderStatusDetails(@Path("orderId") String orderId);

    @GET("orders/{orderId}")
    Call<Order.OrderByIdResponse> getOrderById(@Path(value = "orderId", encoded = true) String orderId);

    @PUT("orders/reviseitem/{orderId}")
    Call<HttpResponse> reviseOrderItem(@Path("orderId") String orderId, @Body List<UpdatedItem> bodyOrderItemList);
}
