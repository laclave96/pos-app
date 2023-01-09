package com.savent.erp.data.local.database.dao

import androidx.room.*
import com.savent.erp.data.local.model.DebtPaymentEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface DebtPaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtPayment(debtPayment: DebtPaymentEntity): Long

    @Query("SELECT * FROM debt_payments ORDER BY date_timestamp DESC")
    fun getDebtPayments(): Flow<List<DebtPaymentEntity>?>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateDebtPayment(debtPayment: DebtPaymentEntity): Int

}