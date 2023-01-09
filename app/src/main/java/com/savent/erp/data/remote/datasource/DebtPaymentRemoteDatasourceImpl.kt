package com.savent.erp.data.remote.datasource

import android.util.Log
import com.google.gson.Gson
import com.savent.erp.R
import com.savent.erp.data.remote.model.DebtPayment
import com.savent.erp.data.remote.service.DebtPaymentApiService
import com.savent.erp.utils.Resource

class DebtPaymentRemoteDatasourceImpl(private val debtPaymentApiService: DebtPaymentApiService) :
    DebtPaymentRemoteDatasource {

    override suspend fun insertDebtPayment(
        businessId: Int,
        sellerId: Int,
        storeId: Int,
        debtPayment: DebtPayment,
        featureName: String
    ): Resource<Int> {
        try {
            val response =
                debtPaymentApiService.insertDebtPayment(
                    businessId,
                    sellerId,
                    storeId,
                    Gson().toJson(debtPayment),
                    featureName

                )
            Log.d("log_",response.toString())
            Log.d("log_",Gson().toJson(response.body()))
            if (response.isSuccessful)
                return Resource.Success(response.body())
            return Resource.Error(resId = R.string.insert_debt_payment_error)
        } catch (e: Exception) {
            Log.d("log_",e.toString())
            return Resource.Error(message = "ConnectionError")
        }
    }
}