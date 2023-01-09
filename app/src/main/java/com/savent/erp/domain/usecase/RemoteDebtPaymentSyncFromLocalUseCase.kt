package com.savent.erp.domain.usecase

import com.savent.erp.data.local.datasource.DebtPaymentLocalDatasource
import com.savent.erp.data.local.model.DebtPaymentEntity
import com.savent.erp.data.remote.datasource.DebtPaymentRemoteDatasource
import com.savent.erp.data.remote.model.DebtPayment
import com.savent.erp.utils.DateFormat
import com.savent.erp.utils.DateTimeObj
import com.savent.erp.utils.PendingRemoteAction
import com.savent.erp.utils.Resource
import kotlinx.coroutines.flow.first

class RemoteDebtPaymentSyncFromLocalUseCase(
    private val localDatasource: DebtPaymentLocalDatasource,
    private val remoteDatasource: DebtPaymentRemoteDatasource
) {

    suspend operator fun invoke(businessId: Int, sellerId: Int, storeId:Int, featureName: String) {
        val sales = localDatasource.getDebtPayments().first()
        var pendingTransactions: List<DebtPaymentEntity>? = null
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
        debtPaymentEntity: DebtPaymentEntity, featureName: String
    ) {
        when (debtPaymentEntity.pendingRemoteAction) {
            PendingRemoteAction.INSERT -> {
                val response = remoteDatasource.insertDebtPayment(
                    businessId,
                    sellerId,
                    storeId,
                    mapToApiModel(debtPaymentEntity),
                    featureName
                )
                if (response is Resource.Success) {
                    debtPaymentEntity.pendingRemoteAction = PendingRemoteAction.COMPLETED
                    localDatasource.updateDebtPayment(debtPaymentEntity)
                }
            }

            else -> {
            }
        }
    }

    private fun mapToApiModel(debtPaymentEntity: DebtPaymentEntity): DebtPayment {
        return DebtPayment(
            debtPaymentEntity.saleId,
            debtPaymentEntity.clientId,
            DateTimeObj(
                DateFormat.getString(debtPaymentEntity.dateTimestamp, "yyyy-MM-dd"),
                DateFormat.getString(debtPaymentEntity.dateTimestamp, "HH:mm")
            ),
            debtPaymentEntity.toPay,
            debtPaymentEntity.paid,
            debtPaymentEntity.paymentMethod,
        )
    }
}