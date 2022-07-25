package com.symplified.order.apis;

import com.symplified.order.models.product.Product;
import com.symplified.order.models.product.ProductResponse;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ProductApi {

    @GET("stores/{storeId}/products?page=0&pageSize=10&sortByCol=name&sortingOrder=ASC&name=&status=ACTIVE,INACTIVE,OUTOFSTOCK")
    Call<ProductResponse> getProducts(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId);

    @GET("stores/{storeId}/products/{productId}")
    Call<ResponseBody> getProductById(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId, @Path("productId") String productId);

    @POST("stores/{storeId}/products")
    Call<ResponseBody> postProduct(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId, @Body Product product);

    @POST("stores/{storeId}/products/{productId}/inventory")
    Call<ResponseBody> postProductInventory(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId, @Path("productId") String productId, @Body Product.ProductInventory productInventory);

    @POST("stores/{storeId}/products/{productId}/assets")
    Call<ResponseBody> postProductAsset(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId, @Path("productId") String productId, @Part MultipartBody.Part file, @Body Product.ProductAsset productAsset);

    @PUT("stores/{storeId}/products/{productId}")
    Call<ResponseBody> updateProduct(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId, @Path("productId") String productId, @Body Product product);

    @PUT("stores/{storeId}/products/{productId}/inventory/{inventoryId}")
    Call<ResponseBody> updateProductInventory(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId, @Path("productId") String productId, @Path("inventoryId") String inventoryId, @Body Product.ProductInventory productInventory);

    @Multipart
    @PUT("stores/{storeId}/products/{productId}/assets/{assetId}")
    Call<ResponseBody> updateProductAsset(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId, @Path("productId") String productId, @Path("assetId") String assetId, @Part("file") MultipartBody.Part file);

    @DELETE("stores/{storeId}/products/{productId}")
    Call<ResponseBody> deleteProduct(@HeaderMap Map<String, String> headers, @Path("storeId") String storeId, @Path("productId") String productId);
}
