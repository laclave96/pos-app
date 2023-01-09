package com.savent.erp.data.remote.datasource

import com.savent.erp.data.remote.model.Product
import com.savent.erp.utils.Resource

interface ProductsRemoteDatasource {

    suspend fun insertProduct(businessId: Int, product: Product): Resource<Int>

    suspend fun getProducts(
        storeId: Int,
        clientId: Int,
        featureName: String,
        filter: String,
        loadDiscounts: Boolean
    ):
            Resource<List<Product>>

    suspend fun updateProduct(businessId: Int, product: Product): Resource<Int>

    suspend fun deleteProduct(businessId: Int, id: Int): Resource<Int>

}