package com.savent.erp.data.local.datasource

import android.util.Log
import com.savent.erp.R
import com.savent.erp.data.local.database.dao.SaleDao
import com.savent.erp.data.local.model.SaleEntity
import com.savent.erp.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

class SalesLocalDatasourceImpl(private val saleDao: SaleDao) : SalesLocalDatasource {

    override suspend fun insertSale(sale: SaleEntity): Resource<Int> {
        try {
            saleDao.insertSale(sale)
        } catch (e: Exception) {
            return Resource.Error(resId = R.string.insert_sale_error)
        }
        return Resource.Success()
    }

    override suspend fun insertSales(sales: List<SaleEntity>): Resource<Int> {
        try {
            saleDao.insertSales(sales)
        } catch (e: Exception) {
            return Resource.Error(resId = R.string.insert_sales_error)
        }
        return Resource.Success()
    }

    override fun getSales(): Flow<Resource<List<SaleEntity>>> = flow {
        saleDao.getSales().onEach {
            it?.let { it1 -> emit(Resource.Success(it1)) }
                ?: emit(Resource.Error<List<SaleEntity>>(resId = R.string.get_sales_error))
        }.collect()
    }


    override suspend fun updateSale(sale: SaleEntity): Resource<Int> {
        try {
            saleDao.updateSale(sale)
        } catch (e: Exception) {
            return Resource.Error(resId = R.string.update_sale_error)
        }
        return Resource.Success()
    }

}