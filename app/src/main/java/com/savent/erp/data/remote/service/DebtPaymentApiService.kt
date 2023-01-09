package com.savent.erp.data.remote.service

import com.savent.erp.AppConstants
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Query

interface DebtPaymentApiService {

    @POST(AppConstants.INCOMPLETE_PAYMENTS_API_PATH)
    suspend fun insertDebtPayment(
        @Query("businessId") businessId: Int,
        @Query("sellerId") sellerId: Int,
        @Query("storeId") storeId: Int,
        @Query("debtPayment") debtPayment: String,
        @Query("featureName") featureName: String
    ): Response<Int>
}