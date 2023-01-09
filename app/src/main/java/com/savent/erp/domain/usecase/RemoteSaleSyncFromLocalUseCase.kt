package com.savent.erp.domain.usecase

import android.util.Log
import com.google.gson.Gson
import com.savent.erp.R
import com.savent.erp.data.local.datasource.SalesLocalDatasource
import com.savent.erp.data.local.model.SaleEntity
import com.savent.erp.data.remote.datasource.SalesRemoteDatasource
import com.savent.erp.data.remote.model.Sale
import com.savent.erp.domain.repository.ClientsRepository
import com.savent.erp.domain.repository.ProductsRepository
import com.savent.erp.utils.DateFormat
import com.savent.erp.utils.DateTimeObj
import com.savent.erp.utils.PendingRemoteAction
import com.savent.erp.utils.Resource
import kotlinx.coroutines.flow.first

class RemoteSaleSyncFromLocalUseCase(
    private val localDatasource: SalesLocalDatasource,
    private val remoteDatasource: SalesRemoteDatasource,
    private val clientsRepository: ClientsRepository,
    private val productsRepository: ProductsRepository
) {

    suspend operator fun invoke(businessId: Int, sellerId: Int, storeId:Int, featureName: String) {
        val sales = localDatasource.getSales().first()
        var pendingTransactions: List<SaleEntity>? = null
        if (sales is Resource.Success)
            pendingTransactions = sales.data?.filter { saleEntity ->
                saleEntity.pendingRemoteAction != PendingRemoteAction.COMPLETED
            }
        pendingTransactions?.let { list ->
            list.forEach { saleEntity ->
                executeTransaction(
                    businessId,
                    sellerId,
                    storeId,
                    saleEntity,
                    featureName
                )
            }
        }
    }

    private suspend fun executeTransaction(
        businessId: Int, sellerId: Int, storeId: Int,
        saleEntity: SaleEntity, featureName: String
    ) {
        when (saleEntity.pendingRemoteAction) {
            PendingRemoteAction.INSERT -> {
                val response = remoteDatasource.insertSale(
                    businessId,
                    sellerId,
                    storeId,
                    mapToApiModel(saleEntity),
                    featureName
                )
                Log.d("log_","InsertSale"+"Inserting")
                if (response is Resource.Success) {
                    Log.d("log_","InsertSale"+"Success")
                    saleEntity.remoteId = response.data!!
                    saleEntity.pendingRemoteAction = PendingRemoteAction.COMPLETED
                    localDatasource.updateSale(saleEntity)
                }
            }

            else -> {
            }
        }
    }

    private suspend fun mapToApiModel(sale: SaleEntity): Sale {
        val client = clientsRepository.getClient(sale.clientId)
        val selectedProducts = HashMap<Int, Int>()
        if (client is Resource.Error) return Sale()

        var clientRemoteId = 0
        var clientName = ""
        client.data?.let { clientEntity ->
            clientRemoteId = clientEntity.remoteId ?: 0
            clientName =
                clientEntity.paternalName + " " + clientEntity.maternalName + " " + clientEntity.name
        } ?: Resource.Error<Int>(resId = R.string.unknown_error)

        sale.selectedProducts.entries.forEach { it1 ->
            val product = productsRepository.getProduct(it1.key)
            if (product is Resource.Error) return Sale()

            product.data?.let { productEntity ->
                selectedProducts[productEntity.remoteId] = it1.value
            } ?: Resource.Error<Int>(resId = R.string.unknown_error)

        }

        return Sale(
            sale.remoteId,
            clientRemoteId,
            clientName,
            DateTimeObj(
                DateFormat.getString(sale.dateTimestamp, "yyyy-MM-dd"),
                DateFormat.getString(sale.dateTimestamp, "HH:mm")
            ),
            selectedProducts,
            sale.subtotal,
            sale.discounts,
            sale.IVA,
            sale.IEPS,
            sale.extraDiscountPercent,
            sale.collected,
            sale.total,
            sale.paymentMethod
        )
    }
}