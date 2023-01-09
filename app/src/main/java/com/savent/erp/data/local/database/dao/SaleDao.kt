package com.savent.erp.data.local.database.dao

import androidx.room.*
import com.savent.erp.data.local.model.ProductEntity
import com.savent.erp.data.local.model.SaleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    @Insert
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(products: List<SaleEntity>)

    @Query("SELECT * FROM sales WHERE id =:id")
    fun getSale(id:Int): Flow<SaleEntity>?

    @Query("SELECT * FROM sales")
    fun getSales(): Flow<List<SaleEntity>?>

    @Update
    fun updateSale(sale: SaleEntity)

}