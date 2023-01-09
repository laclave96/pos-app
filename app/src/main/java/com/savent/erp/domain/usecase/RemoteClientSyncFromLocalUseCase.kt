package com.savent.erp.domain.usecase

import android.util.Log
import com.savent.erp.data.local.datasource.ClientsLocalDatasource
import com.savent.erp.data.local.model.ClientEntity
import com.savent.erp.data.remote.datasource.ClientsRemoteDatasource
import com.savent.erp.data.remote.model.Client
import com.savent.erp.utils.DateFormat
import com.savent.erp.utils.DateTimeObj
import com.savent.erp.utils.PendingRemoteAction
import com.savent.erp.utils.Resource
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach

class RemoteClientSyncFromLocalUseCase(
    private val localDatasource: ClientsLocalDatasource,
    private val remoteDatasource: ClientsRemoteDatasource
) {

    suspend operator fun invoke(sellerId: Int, storeId: Int, featureName: String) {
        val clients = localDatasource.getClients().first()
        var pendingTransactions: List<ClientEntity>? = null
        if (clients is Resource.Success)
            pendingTransactions = clients.data?.filter { clientEntity ->
                clientEntity.pendingRemoteAction != PendingRemoteAction.COMPLETED
            }
        pendingTransactions?.let { list ->
            list.forEach { clientEntity ->
                executeTransaction(
                    sellerId,
                    storeId,
                    clientEntity,
                    featureName
                )
            }
        }
    }

    private suspend fun executeTransaction(
        sellerId: Int,
        storeId: Int,
        clientEntity: ClientEntity,
        featureName: String
    ) {
        when (clientEntity.pendingRemoteAction) {
            PendingRemoteAction.INSERT -> {
                val response = remoteDatasource.insertClient(
                    sellerId,
                    storeId,
                    mapToApiModel(clientEntity),
                    featureName
                )
                if (response is Resource.Success) {
                    clientEntity.remoteId = response.data!!
                    clientEntity.pendingRemoteAction = PendingRemoteAction.COMPLETED
                    localDatasource.updateClient(clientEntity)
                }
            }
            PendingRemoteAction.UPDATE -> {
                val response = remoteDatasource.updateClient(
                    sellerId,
                    mapToApiModel(clientEntity)
                )
                if (response is Resource.Success) {
                    clientEntity.pendingRemoteAction = PendingRemoteAction.COMPLETED
                    localDatasource.updateClient(clientEntity)
                }
            }
            PendingRemoteAction.DELETE -> {
                val response = remoteDatasource.deleteClient(
                    sellerId,
                    clientEntity.remoteId ?: 0
                )
                if (response is Resource.Success) {
                    localDatasource.deleteClient(clientEntity.id, PendingRemoteAction.COMPLETED)
                }
            }
            else -> {
            }
        }
    }

    private fun mapToApiModel(clientEntity: ClientEntity): Client {
        return Client(
            clientEntity.remoteId,
            clientEntity.image,
            clientEntity.name,
            clientEntity.tradeName,
            clientEntity.paternalName,
            clientEntity.maternalName,
            clientEntity.socialReason,
            clientEntity.rfc,
            clientEntity.phoneNumber,
            clientEntity.email,
            clientEntity.street,
            clientEntity.noExterior,
            clientEntity.colonia,
            clientEntity.postalCode,
            clientEntity.city,
            clientEntity.state,
            clientEntity.country,
            clientEntity.location,
            DateTimeObj(
                DateFormat.getString(clientEntity.dateTimestamp ?: 0, "yyyy-MM-dd"),
                DateFormat.getString(clientEntity.dateTimestamp ?: 0, "HH:mm")
            )
        )
    }
}