package com.savent.erp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.savent.erp.data.local.database.dao.*
import com.savent.erp.data.local.model.*
import com.savent.erp.utils.Converters

@Database(entities = [ClientEntity::class, ProductEntity::class, SaleEntity::class,
    IncompletePaymentEntity::class, DebtPaymentEntity::class, StatEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun clientDao(): ClientDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun incompletePaymentDao(): IncompletePaymentDao
    abstract fun debtPaymentDao(): DebtPaymentDao
    abstract fun statDao(): StatDao

    /*companion object {
        @Volatile
        private var sINSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            return sINSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )   .addCallback(object:Callback(){
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        }
                    }
                })
                    .build()
                sINSTANCE = instance
                return instance
            }
        }
    }*/
}