package com.savent.erp.data.local.datasource

import com.savent.erp.data.local.model.DebtPaymentEntity
import com.savent.erp.utils.Resource
import kotlinx.coroutines.flow.Flow

interface DebtPaymentLocalDatasource {

    suspend fun insertDebtPayment(debtPayment: DebtPaymentEntity): Resource<Int>

    fun getDebtPayments(): Flow<Resource<List<DebtPaymentEntity>>>

    suspend fun updateDebtPayment(debtPayment: DebtPaymentEntity): Resource<Int>
}