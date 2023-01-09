package com.savent.erp.domain.usecase

import com.savent.erp.R
import com.savent.erp.domain.repository.ClientsRepository
import com.savent.erp.domain.repository.SalesRepository
import com.savent.erp.presentation.ui.model.Contact
import com.savent.erp.presentation.ui.model.SharedReceipt
import com.savent.erp.utils.DateFormat
import com.savent.erp.utils.DecimalFormat
import com.savent.erp.utils.PaymentMethod
import com.savent.erp.utils.Resource
import kotlinx.coroutines.flow.first


class GetReceiptToSend(
    private val businessBasicsUseCase: GetBusinessBasicsUseCase,
    private val clientsRepository: ClientsRepository,
    private val salesRepository: SalesRepository,
    private val getSelectedProductsUseCase: GetSelectedProductsUseCase
) {
    suspend operator fun invoke(): Resource<SharedReceipt> {

        val businessBasics = businessBasicsUseCase().first()
        if (businessBasics is Resource.Error)
            return Resource.Error(resId = R.string.get_business_error)

        val sale = salesRepository.getPendingSale().first()
        if (sale is Resource.Error) return Resource.Error()

        val clientId = sale.data?.clientId

        val client = clientId?.let { clientsRepository.getClient(it) }
            ?: return Resource.Error(resId = R.string.get_sales_error)
        if (client is Resource.Error) return Resource.Error(resId = R.string.get_clients_error)

        val products = getSelectedProductsUseCase().first()
        if (businessBasics is Resource.Error)
            return Resource.Error(resId = R.string.get_products_error)

        var strProducts = ""
        products.data?.forEach {
            strProducts += "${it.name} x ${it.selectedUnits}      $${it.price * it.selectedUnits} \n"
        }

        val taxes = sale.data.IVA.plus(sale.data.IEPS)
        val collected = sale.data.collected
        val total = sale.data.total
        val change = if (collected < total) 0F else collected - total

        val payMethod = when(sale.data.paymentMethod){
            PaymentMethod.Credit -> "Crédito"
            PaymentMethod.Cash -> "Efectivo"
            PaymentMethod.Debit -> "Débito"
        }

        val note = "\uD83E\uDDFE Recibo de Compra \n" +
                "\uD83C\uDFDB️${businessBasics.data?.name} \n" +
                "\uD83D\uDCCD ${businessBasics.data?.address} \n" +
                "\uD83D\uDDD3️ ${DateFormat.getString(System.currentTimeMillis(), "dd/MM/yyyy")} \n" +
                "*********************************** \n" +
                strProducts +
                "-------------------------------------------- \n" +
                "Subtotal:   $${DecimalFormat.format(sale.data.subtotal)} \n" +
                "Impuestos:   $${DecimalFormat.format(taxes)} \n" +
                "Descuentos:   $${DecimalFormat.format(sale.data.discounts)} \n" +
                "-------------------------------------------- \n" +
                "Total:   $${DecimalFormat.format(total)} \n" +
                "Pagado:   $${DecimalFormat.format(collected)} \n" +
                "Cambio:   $${DecimalFormat.format(change)} \n" +
                "Método de Pago: " + payMethod +"\n"+
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx \n" +
                "Muchas Gracias\n" +
                "Vuelva Pronto \uD83D\uDC4B"

        client.data?.let {
            val contact = Contact(it.phoneNumber, it.email)
            return Resource.Success(SharedReceipt(note, contact))
        }

        return Resource.Error()

    }
}