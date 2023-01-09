package com.savent.erp.data.repository

import com.savent.erp.data.local.datasource.DebtPaymentLocalDatasource
import com.savent.erp.data.local.model.DebtPaymentEntity
import com.savent.erp.data.remote.model.DebtPayment
import com.savent.erp.domain.repository.DebtPaymentRepository
import com.savent.erp.utils.PendingRemoteAction
import com.savent.erp.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

class DebtPaymentRepositoryImpl(
    private val localDatasource: DebtPaymentLocalDatasource,
): DebtPaymentRepository {

    override suspend fun insertDebtPayment(debtPayment: DebtPayment): Resource<Int> =
        localDatasource.insertDebtPayment(mapToEntity(debtPayment))

    override fun getDebtPayments(): Flow<Resource<List<DebtPaymentEntity>>> = flow {
        localDatasource.getDebtPayments().onEach { emit(it) }.collect()
    }

    private fun mapToEntity(debtPayment: DebtPayment): DebtPaymentEntity{
        return DebtPaymentEntity(
            0,
            debtPayment.saleId,
            debtPayment.clientId,
            System.currentTimeMillis(),
            debtPayment.toPay,
            debtPayment.paid,
            debtPayment.paymentMethod,
            PendingRemoteAction.INSERT
        )
    }
}