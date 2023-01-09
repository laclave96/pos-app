package com.savent.erp.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class RemoteDataSyncFromLocalUseCase(
    private val remoteBusinessBasicsSyncFromLocalUseCase: RemoteBusinessBasicsSyncFromLocalUseCase,
    private val remoteClientSyncFromLocalUseCase: RemoteClientSyncFromLocalUseCase,
    private val remoteProductSyncFromLocalUseCase: RemoteProductSyncFromLocalUseCase,
    private val remoteSaleSyncFromLocalUseCase: RemoteSaleSyncFromLocalUseCase,
    private val remoteDebtPaymentSyncFromLocalUseCase: RemoteDebtPaymentSyncFromLocalUseCase

) {

    suspend operator fun invoke(businessId: Int, sellerId: Int, storeId: Int, featureName: String) {
        //remoteBusinessBasicsSyncFromLocalUseCase(businessId)
        //remoteProductSyncFromLocalUseCase(businessId)
        coroutineScope {
            async { remoteClientSyncFromLocalUseCase(sellerId,storeId, featureName) }
            async { remoteSaleSyncFromLocalUseCase(businessId, sellerId, storeId, featureName) }
            async { remoteDebtPaymentSyncFromLocalUseCase(businessId, sellerId, storeId, featureName) }
        }
    }
}