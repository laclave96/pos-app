package com.savent.erp.data.remote.service

import com.savent.erp.AppConstants
import com.savent.erp.data.remote.model.Client
import retrofit2.Response
import retrofit2.http.*

interface ClientApiService {

    @POST(AppConstants.CLIENTS_API_PATH)
    suspend fun insertClient(
        @Query("sellerId") sellerId: Int,
        @Query("storeId") storeId: Int,
        @Query("client") client: String,
        @Query("featureName") featureName: String
    ): Response<Int>

    @GET(AppConstants.CLIENTS_API_PATH)
    suspend fun getClients(
        @Query("sellerId") sellerId: Int,
        @Query("storeId") storeId: Int?,
        @Query("featureName") featureName: String,
        @Query("filter") filter: String
    ): Response<List<Client>>

    @GET(AppConstants.CLIENTS_API_PATH)
    suspend fun getClient(
        @Query("sellerId") sellerId: Int,
        @Query("storeId") storeId: Int,
        @Query("id") clientId: Int,
        @Query("featureName") featureName: String
    ): Response<Client>

    @PUT(AppConstants.CLIENTS_API_PATH)
    suspend fun updateClient(
        @Query("sellerId") sellerId: Int,
        @Body client: Client
    ): Response<Int>

    @DELETE(AppConstants.CLIENTS_API_PATH)
    suspend fun deleteClient(
        @Query("sellerId") sellerId: Int,
        @Query("id") clientId: Int
    ): Response<Int>

}