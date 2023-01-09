package com.savent.erp.data.remote.datasource

import com.savent.erp.data.remote.model.Sale
import com.savent.erp.utils.Resource

interface SalesRemoteDatasource {

    suspend fun insertSale(
        businessId: Int,
        sellerId: Int,
        storeId: Int,
        sale: Sale,
        featureName: String
    ): Resource<Int>

    suspend fun getSales(
        businessId: Int,
        sellerId: Int,
        storeId: Int,
        date: String,
        featureName: String
    ):
            Resource<List<Sale>>
}