package com.savent.erp.domain.usecase

import android.util.Log
import com.mazenrashed.printooth.data.printable.ImagePrintable
import com.mazenrashed.printooth.data.printable.Printable
import com.mazenrashed.printooth.data.printable.RawPrintable
import com.mazenrashed.printooth.data.printable.TextPrintable
import com.mazenrashed.printooth.data.printer.DefaultPrinter
import com.savent.erp.R
import com.savent.erp.domain.repository.SalesRepository
import com.savent.erp.utils.DateFormat
import com.savent.erp.utils.DecimalFormat
import com.savent.erp.utils.PaymentMethod
import com.savent.erp.utils.Resource
import kotlinx.coroutines.flow.first
import java.text.Normalizer

class GetReceiptToPrint(
    private val businessBasicsUseCase: GetBusinessBasicsUseCase,
    private val salesRepository: SalesRepository,
    private val getSelectedProductsUseCase: GetSelectedProductsUseCase
) {
    suspend operator fun invoke(): Resource<ArrayList<Printable>> {

        val businessBasics = businessBasicsUseCase().first()
        if (businessBasics is Resource.Error)
            return Resource.Error(resId = R.string.get_business_error)

        val sale = salesRepository.getPendingSale().first()
        if (sale is Resource.Error) return Resource.Error()


        if (sale is Resource.Error || sale.data == null)
            return Resource.Error(resId = R.string.get_sales_error)

        val products = getSelectedProductsUseCase().first()
        if (businessBasics is Resource.Error)
            return Resource.Error(resId = R.string.get_products_error)

        val clientName = sale.data.clientName
        val subtotal = sale.data.subtotal
        val taxes = sale.data.IVA.plus(sale.data.IEPS)
        val collected = sale.data.collected
        val total = sale.data.total
        val change = if (collected < total) 0F else collected - total
        val isCreditSale = collected < total
        val discounts = sale.data.discounts
        val receiptFooter =
            businessBasics.data?.receiptFooter?.replace("\r", "")?.withOutAccent()
                ?: "GRACIAS POR SU COMPRA"

        val payMethod = when (sale.data.paymentMethod) {
            PaymentMethod.Credit -> "CREDITO"
            PaymentMethod.Cash -> "EFECTIVO"
            PaymentMethod.Debit -> "DEBITO"
        }

        val printables = ArrayList<Printable>().apply {

            add(RawPrintable.Builder(byteArrayOf(27, 100, 4)).build())

            add(
                TextPrintable.Builder()
                    .setText("RECIBO DE COMPRA")
                    .setLineSpacing(10)
                    .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                    .setFontSize(DefaultPrinter.FONT_SIZE_LARGE)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )

            add(
                TextPrintable.Builder()
                    .setText("${businessBasics.data?.name?.uppercase()?.withOutAccent()}")
                    .setLineSpacing(5)
                    .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )

            add(
                TextPrintable.Builder()
                    .setText("${businessBasics.data?.address?.uppercase()?.withOutAccent()}")
                    .setLineSpacing(5)
                    .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )

            add(
                TextPrintable.Builder()
                    .setText(
                        DateFormat.getString(
                            System.currentTimeMillis(),
                            "dd/MM/yyyy hh:mm a"
                        ).uppercase()
                    )
                    .setLineSpacing(5)
                    .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )

            add(
                TextPrintable.Builder()
                    .setText("********************************")
                    .setLineSpacing(5)
                    .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )

            add(
                TextPrintable.Builder()
                    .setText("CANT. DESCRIPCION        IMPORTE")
                    .setLineSpacing(5)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )

            add(
                TextPrintable.Builder()
                    .setText("--------------------------------")
                    .setLineSpacing(5)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )

            val charsLengthFixed = 22
            val priceLength = 10
            var totalUnits = 0
            products.data?.forEach {
                totalUnits += it.selectedUnits
                val productStr = "${it.selectedUnits.toString().padEnd(3)}${it.name}"
                    .uppercase().withOutAccent()
                var count = 0
                var newLines = 1
                while (count * charsLengthFixed < productStr.length) {
                    val start = count * charsLengthFixed
                    val end = start + charsLengthFixed
                    val str = try {
                        productStr.subSequence(start, end)
                    } catch (e: Exception) {
                        productStr.subSequence(start, productStr.length).padEnd(charsLengthFixed)
                    }
                    count++
                    if (count * charsLengthFixed >= productStr.length) newLines = 0
                    add(
                        TextPrintable.Builder()
                            .setText(str.toString())
                            .setLineSpacing(5)
                            .setNewLinesAfter(newLines)
                            .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                            .build()
                    )

                }
                add(
                    TextPrintable.Builder()
                        .setText("$${it.price * it.selectedUnits}".padStart(priceLength))
                        .setLineSpacing(5)
                        .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                        .setNewLinesAfter(1)
                        .build()
                )


            }

            add(
                TextPrintable.Builder()
                    .setText("NO. DE ARTICULOS: $totalUnits")
                    .setLineSpacing(5)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )

            add(
                TextPrintable.Builder()
                    .setText("--------------------------------")
                    .setLineSpacing(5)
                    .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )

            if (subtotal != total) {
                add(
                    TextPrintable.Builder()
                        .setText("SUBTOTAL:".padEnd(charsLengthFixed))
                        .setLineSpacing(5)
                        .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                        .build()
                )

                add(
                    TextPrintable.Builder()
                        .setText(
                            "$${DecimalFormat.format(subtotal)}"
                                .padStart(priceLength)
                        )
                        .setLineSpacing(5)
                        .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                        .setNewLinesAfter(1)
                        .build()
                )

                if (taxes > 0) {

                    add(
                        TextPrintable.Builder()
                            .setText("IMPUESTOS:".padEnd(charsLengthFixed))
                            .setLineSpacing(5)
                            .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                            .build()
                    )

                    add(
                        TextPrintable.Builder()
                            .setText("+$${DecimalFormat.format(taxes)}".padStart(priceLength))
                            .setLineSpacing(5)
                            .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                            .setNewLinesAfter(1)
                            .build()
                    )
                }


                if (discounts > 0) {
                    add(
                        TextPrintable.Builder()
                            .setText("DESCUENTOS:".padEnd(charsLengthFixed))
                            .setLineSpacing(5)
                            .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                            .build()
                    )

                    add(
                        TextPrintable.Builder()
                            .setText(
                                "-$${DecimalFormat.format(discounts)}"
                                    .padStart(priceLength)
                            )
                            .setLineSpacing(5)
                            .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                            .setNewLinesAfter(1)
                            .build()
                    )

                }

                add(
                    TextPrintable.Builder()
                        .setText("--------------------------------")
                        .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                        .setLineSpacing(5)
                        .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                        .setNewLinesAfter(1)
                        .build()
                )


            }

            add(
                TextPrintable.Builder()
                    .setText("TOTAL:".padEnd(charsLengthFixed))
                    .setLineSpacing(5)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .build()
            )

            add(
                TextPrintable.Builder()
                    .setText("$${DecimalFormat.format(total)}".padStart(priceLength))
                    .setLineSpacing(5)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )

            add(
                TextPrintable.Builder()
                    .setText("SU PAGO:".padEnd(charsLengthFixed))
                    .setLineSpacing(5)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .build()
            )

            add(
                TextPrintable.Builder()
                    .setText("$${DecimalFormat.format(collected)}".padStart(priceLength))
                    .setLineSpacing(5)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )

            add(
                TextPrintable.Builder()
                    .setText("SU CAMBIO:".padEnd(charsLengthFixed))
                    .setLineSpacing(5)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .build()
            )

            add(
                TextPrintable.Builder()
                    .setText("$${DecimalFormat.format(change)}".padStart(priceLength))
                    .setLineSpacing(5)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )

            add(
                TextPrintable.Builder()
                    .setText("METODO DE PAGO: $payMethod")
                    .setLineSpacing(5)
                    .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )


            add(
                TextPrintable.Builder()
                    .setText("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
                    .setLineSpacing(5)
                    .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                    .setFontSize(DefaultPrinter.FONT_SIZE_NORMAL)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )
            if (isCreditSale)
                add(
                    TextPrintable.Builder()
                        .setText("* VENTA A CREDITO *")
                        .setLineSpacing(5)
                        .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                        .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                        .setNewLinesAfter(1)
                        .build()
                )

            add(
                TextPrintable.Builder()
                    .setText("FIRMA DEL CLIENTE:")
                    .setLineSpacing(5)
                    .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )

            add(
                TextPrintable.Builder()
                    .setText("____________________")
                    .setLineSpacing(DefaultPrinter.LINE_SPACING_60)
                    .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                    .setUnderlined(DefaultPrinter.UNDERLINED_MODE_ON)
                    .setNewLinesAfter(1)
                    .build()
            )

            add(
                TextPrintable.Builder()
                    .setText(clientName)
                    .setLineSpacing(5)
                    .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                    .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                    .setNewLinesAfter(1)
                    .build()
            )

            receiptFooter.split("\n").forEach {
                add(
                    TextPrintable.Builder()
                        .setText(it)
                        .setLineSpacing(5)
                        .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
                        .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
                        .setNewLinesAfter(1)
                        .build()
                )
            }
            add(RawPrintable.Builder(byteArrayOf(27, 100, 4)).build())

        }

        return Resource.Success(printables)

    }

    private fun CharSequence.withOutAccent(): String {
        val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(temp, "")
    }
}