package com.symplified.order.apis;

import com.symplified.order.models.category.CategoryResponse;
import com.symplified.order.models.product.Product;
import com.symplified.order.models.product.ProductResponse;

import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProductApi {

    @GET("stores/{storeId}/products?page=0&pageSize=10&sortByCol=name&sortingOrder=ASC&name=&status=ACTIVE,INACTIVE,OUTOFSTOCK")
    Call<ProductResponse> getProducts(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId);

    @POST("stores/{storeId}/products")
    Call<ResponseBody> addProduct(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId, @Body Product product);

    @DELETE("stores/{storeId}/products/{productId}")
    Call<ResponseBody> deleteProduct(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId, @Path("productId") String productId);

    @PUT("stores/{storeId}/products/{productId}")
    Call<ResponseBody> updateProduct(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId, @Path("productId") String productId, @Body Product product);

    @PUT("stores/{storeId}/products/{productId}/inventory/{inventoryId}")
    Call<ResponseBody> updateProductInventory(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId, @Path("productId") String productId, @Path("inventoryId") String inventoryId, @Body Product.ProductInventory productInventory);
}
