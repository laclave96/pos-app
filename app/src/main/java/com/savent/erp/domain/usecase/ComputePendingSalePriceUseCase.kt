package com.savent.erp.domain.usecase


import com.savent.erp.R
import com.savent.erp.data.local.model.SaleEntity
import com.savent.erp.domain.repository.ProductsRepository
import com.savent.erp.domain.repository.SalesRepository
import com.savent.erp.utils.Resource
import kotlinx.coroutines.flow.first

class ComputePendingSalePriceUseCase(
    private val productsRepository: ProductsRepository,
    private val salesRepository: SalesRepository
) {

    suspend operator fun invoke(): Resource<Int> {
        val sale = salesRepository.getPendingSale().first()
        if (sale is Resource.Success) {
            sale.data?.let {
                val productsSelected = it.selectedProducts
                var subtotal = 0F
                var IVA = 0F
                var IEPS = 0F
                var discounts = 0F
                productsSelected.entries.forEach { it1 ->
                    when (val product = productsRepository.getProduct(it1.key)) {
                        is Resource.Success -> {
                            product.data?.let { productEntity ->
                                subtotal += productEntity.price * it1.value
                                IVA += productEntity.IVA.times(it1.value)
                                IEPS += productEntity.IEPS.times(it1.value)
                                discounts += productEntity.discounts.times(it1.value)
                            }
                        }
                    }

                }
                val subtotal1 = subtotal - discounts
                val extraDiscount = (it.extraDiscountPercent / (100 * 1F))  * subtotal1
                val finalPrice = subtotal1 - extraDiscount + IVA + IEPS

                return salesRepository.updatePendingSale(
                    SaleEntity(
                        0,
                        it.id,
                        it.clientId,
                        it.clientName,
                        it.dateTimestamp,
                        it.selectedProducts,
                        subtotal,
                        discounts + extraDiscount,
                        IVA,
                        IEPS,
                        it.extraDiscountPercent,
                        it.collected,
                        finalPrice,
                        it.paymentMethod
                    )
                )

            } ?: return Resource.Error(resId = sale.resId ?: R.string.unknown_error)
        }
        return Resource.Error(resId = sale.resId ?: R.string.unknown_error)
    }

}