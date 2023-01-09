package com.savent.erp.data.repository

import android.util.Log
import com.google.gson.Gson
import com.savent.erp.R
import com.savent.erp.utils.Resource
import com.savent.erp.data.remote.datasource.BusinessRemoteDatasource
import com.savent.erp.data.remote.model.Business
import com.savent.erp.domain.repository.*
import com.savent.erp.data.remote.model.LoginCredentials

class BusinessRepositoryImpl(
    private val businessBasicsRepository: BusinessBasicsRepository,
    private val clientsRepository: ClientsRepository,
    private val productsRepository: ProductsRepository,
    private val salesRepository: SalesRepository,
    private val businessRemoteDatasource: BusinessRemoteDatasource
) : BusinessRepository {

    override suspend fun insertBusiness(business: Business): Resource<Int> {
        val resultBasics: Resource<Int> = businessBasicsRepository.insertBusinessBasics(business.basics)
        val resultClients: Resource<Int> = clientsRepository.insertClients(business.clients)
        val resultProducts: Resource<Int> =
            productsRepository.insertProducts(business.products)
        val resultSales: Resource<Int> = salesRepository.insertSales(business.sales)

        if (resultBasics is Resource.Error || resultClients is Resource.Error
            || resultProducts is Resource.Error || resultSales is Resource.Error
        )
            return Resource.Error(resId = R.string.insert_business_error)

        return Resource.Success()
    }

    override suspend fun fetchBusiness(credentials: LoginCredentials): Resource<Business> {
        val response = businessRemoteDatasource.getBusiness(credentials)
        if (response is Resource.Success) {
            response.data?.let {
                insertBusiness(it)
            }
            return response
        }
        return Resource.Error(resId = response.resId, message = response.message)

    }



}