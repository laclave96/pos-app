package com.savent.erp.data.remote.datasource

import android.util.Log
import com.google.gson.Gson
import com.savent.erp.R
import com.savent.erp.data.remote.model.Client
import com.savent.erp.data.remote.service.ClientApiService
import com.savent.erp.utils.Resource

class ClientsRemoteDatasourceImpl(
    private val clientsApiService: ClientApiService
) : ClientsRemoteDatasource {
    override suspend fun insertClient(
        sellerId: Int,
        storeId: Int,
        client: Client,
        featureName: String
    ): Resource<Int> {
        try {
            val response = clientsApiService.insertClient(
                sellerId,
                storeId,
                Gson().toJson(client),
                featureName
            )
            //Log.d("log_",response.toString()+response.errorBody().toString())
            if (response.isSuccessful)
                return Resource.Success(response.body())
            return Resource.Error(resId = R.string.add_client_error)
        } catch (e: Exception) {
            //Log.d("log_",e.toString())
            return Resource.Error(message = "Error al conectar")
        }

    }

    override suspend fun getClients(
        sellerId: Int,
        storeId: Int?,
        featureName: String,
        category: String
    ): Resource<List<Client>> {
        try {
            val response = clientsApiService.getClients(sellerId, storeId, featureName, category)
            //Log.d("log_",response.toString())
            //Log.d("log_",Gson().toJson(response.body()))
            if (response.isSuccessful)
                return Resource.Success(response.body())
            return Resource.Error(resId = R.string.get_clients_error)
        } catch (e: Exception) {
            //Log.d("log_",e.toString())
            return Resource.Error(message = "Error al conectar")
        }

    }

    override suspend fun updateClient(sellerId: Int, client: Client): Resource<Int> {
        try {
            val response = clientsApiService.updateClient(sellerId, client)
            if (response.isSuccessful)
                return Resource.Success(response.body())
            return Resource.Error(resId = R.string.update_client_error)
        } catch (e: Exception) {
            return Resource.Error(message = "Error al conectar")
        }

    }

    override suspend fun deleteClient(sellerId: Int, id: Int): Resource<Int> {
        try {
            val response = clientsApiService.deleteClient(sellerId, id)
            if (response.isSuccessful)
                return Resource.Success(response.body())
            return Resource.Error(resId = R.string.delete_client_error)
        } catch (e: Exception) {
            return Resource.Error(message = "Error al conectar")
        }

    }

}