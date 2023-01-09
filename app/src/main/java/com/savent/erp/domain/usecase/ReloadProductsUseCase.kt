package com.savent.erp.domain.usecase

import com.savent.erp.domain.repository.ProductsRepository
import com.savent.erp.utils.Resource
import kotlinx.coroutines.flow.first

class ReloadProductsUseCase(private val productsRepository: ProductsRepository) {

    suspend operator fun invoke(
        storeId: Int,
        clientId: Int,
        featureName: String,
        filter: String,
        loadDiscounts: Boolean
    ): Resource<Int> =
        productsRepository.fetchProducts(storeId, clientId, featureName, filter, loadDiscounts)

}