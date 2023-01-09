package com.savent.erp.domain.usecase

import com.savent.erp.R
import com.savent.erp.data.local.model.SaleEntity
import com.savent.erp.domain.repository.SalesRepository
import com.savent.erp.presentation.ui.model.SaleItem
import com.savent.erp.utils.DateFormat
import com.savent.erp.utils.NameFormat
import com.savent.erp.utils.Resource
import kotlinx.coroutines.flow.*
import java.util.*

class GetSalesOfDayUseCase(
    private val salesRepository: SalesRepository,
) {

    operator fun invoke(): Flow<Resource<List<SaleItem>>> = flow {
        salesRepository.getSales().onEach {
            if (it is Resource.Error) {
                emit(Resource.Error(resId = R.string.get_sales_error))
                return@onEach
            }
            val now = Date()
            val tempDate = Date()

            val salesOfDay: List<SaleItem> = it.data?.filter { saleEntity ->
                tempDate.time = saleEntity.dateTimestamp
                tempDate.date == now.date
            }?.map { it1 -> mapToUiItem(it1) } ?: listOf()

            emit(Resource.Success(salesOfDay))

        }.collect()
    }

    private fun mapToUiItem(saleEntity: SaleEntity): SaleItem {
        val time = DateFormat.getString(saleEntity.dateTimestamp, "hh:mm a")
        val taxes = saleEntity.IVA + saleEntity.IEPS
        return SaleItem(
            saleEntity.id,
            NameFormat.format(saleEntity.clientName),
            time,
            saleEntity.selectedProducts.size,
            saleEntity.paymentMethod,
            saleEntity.subtotal,
            taxes,
            saleEntity.discounts,
            saleEntity.total,
            saleEntity.collected,
            if (saleEntity.total < saleEntity.collected)
                (saleEntity.collected - saleEntity.total) else 0F
        )
    }


}