package com.savent.erp.data.remote.service

import com.savent.erp.AppConstants
import com.savent.erp.data.remote.model.Sale
import retrofit2.Response
import retrofit2.http.*

interface SaleApiService {

    @POST(AppConstants.SALES_API_PATH)
    suspend fun insertSale(
        @Query("businessId") businessId: Int,
        @Query("sellerId") sellerId: Int,
        @Query("storeId") storeId: Int,
        @Query("sale") sale: String,
        @Query("featureName") featureName: String
    ): Response<Int>

    @GET(AppConstants.SALES_API_PATH)
    suspend fun getSales(
        @Query("businessId") businessId: Int,
        @Query("sellerId") sellerId: Int,
        @Query("storeId") storeId: Int,
        @Query("date") date: String,
        @Query("featureName") featureName: String
    ): Response<List<Sale>>

}