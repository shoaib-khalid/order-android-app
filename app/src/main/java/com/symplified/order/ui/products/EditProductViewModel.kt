package com.symplified.order.ui.products

import android.content.Context
import androidx.lifecycle.ViewModel
import com.symplified.order.App
import com.symplified.order.models.product.Product
import com.symplified.order.models.product.ProductListResponse
import com.symplified.order.models.store.Store
import com.symplified.order.networking.RequestInterceptor
import com.symplified.order.networking.apis.ProductApiKt
import com.symplified.order.networking.apis.StoreApiKt
import com.symplified.order.utils.SharedPrefsKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class EditProductViewModel : ViewModel() {

    private val _stores = MutableStateFlow<List<Store>>(listOf())
    val stores: StateFlow<List<Store>> = _stores

    private val _products = MutableStateFlow<List<Product>>(listOf())
    val products: StateFlow<List<Product>> = _products

    fun fetchAll(context: Context) = CoroutineScope(Dispatchers.IO).launch {
        val sharedPrefs = context.getSharedPreferences(App.SESSION, Context.MODE_PRIVATE)
        val clientId = sharedPrefs.getString(SharedPrefsKey.CLIENT_ID, "")!!

        val httpClient: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(RequestInterceptor(sharedPrefs))
            .build()

        val baseURL = sharedPrefs.getString(SharedPrefsKey.BASE_URL, App.BASE_URL_PRODUCTION)

        val retrofitInstance = Retrofit.Builder()
            .client(httpClient)
            .baseUrl(baseURL + App.PRODUCT_SERVICE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val storeApiService = retrofitInstance.create(StoreApiKt::class.java)
        val productApiService = retrofitInstance.create(ProductApiKt::class.java)

        try {
            val storeResponse = storeApiService.getStores(clientId)
            if (storeResponse.isSuccessful) {
                _stores.value = storeResponse.body()!!.data.content
            }
        } catch (_: Throwable) {}

        val products : MutableList<Product> = mutableListOf()
        val productRequests = _stores.value.map { async { productApiService.getProducts(it.id) } }

        productRequests.awaitAll().forEach { response ->
            if (response.isSuccessful) {
                products.addAll(response.body()!!.data.content)
            }
        }

        _products.value = products
    }
}