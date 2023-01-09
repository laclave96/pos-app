package com.savent.erp.domain.usecase

import com.savent.erp.domain.repository.ClientsRepository
import com.savent.erp.utils.Resource
import kotlinx.coroutines.flow.first

class ReloadClientsUseCase(private val clientsRepository: ClientsRepository) {

    suspend operator fun invoke(sellerId: Int, storeId: Int?, featureName: String, category: String):
            Resource<Int> =
        clientsRepository.fetchClients(sellerId, storeId, featureName, category)


}