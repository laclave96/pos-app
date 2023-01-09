package com.savent.erp.presentation.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.savent.erp.R
import com.savent.erp.presentation.ui.model.SaleItem
import com.savent.erp.utils.DecimalFormat
import com.savent.erp.utils.PaymentMethod

class SalesAdapter(private val context: Context?) :
    RecyclerView.Adapter<SalesAdapter.SalesViewHolder>() {

    private val sales = ArrayList<SaleItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalesViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.sale_item, parent, false)
        return SalesViewHolder(view)
    }

    override fun onBindViewHolder(holder: SalesViewHolder, position: Int) {
        val sale = sales[position]

        holder.clientName.text = sale.clientName
        holder.productUnits.text =
            context?.getString(R.string.articles_format)?.format("${sale.productsUnits}")
        holder.time.text = sale.time

        when (sale.paymentMethod) {
            PaymentMethod.Cash -> {
                holder.paymentMethod.text = context?.getString(R.string.cash)
                holder.payImage.setImageResource(R.drawable.ic_round_payments_24)
            }
            PaymentMethod.Credit -> {
                holder.paymentMethod.text = context?.getString(R.string.credit)
                holder.payImage.setImageResource(R.drawable.ic_round_credit_card_24)
            }
            PaymentMethod.Debit -> {
                holder.paymentMethod.text = context?.getString(R.string.debit)
                holder.payImage.setImageResource(R.drawable.ic_round_money_off_24)
            }
        }

        holder.subtotal.text = context?.getString(R.string.price)
            ?.format(DecimalFormat.format(sale.subtotal))

        holder.taxes.text = context?.getString(R.string.price)
            ?.format(DecimalFormat.format(sale.taxes))

        holder.discounts.text = context?.getString(R.string.price)
                ?.format(DecimalFormat.format(sale.discounts))

        holder.toPay.text = context?.getString(R.string.price)
                    ?.format(DecimalFormat.format(sale.toPay))

        holder.collected.text = context?.getString(R.string.price)
                        ?.format(DecimalFormat.format(sale.collected))

        holder.change.text = context?.getString(R.string.price)
            ?.format(DecimalFormat.format(sale.change))
    }

    override fun getItemCount(): Int =
        sales.size

    override fun getItemId(position: Int): Long {
        return sales[position].localId.toLong()
    }

    fun setData(newSales: List<SaleItem>) {
        val diffCallback = SaleDiffCallBack(sales, newSales)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        sales.clear()
        sales.addAll(newSales)
        diffResult.dispatchUpdatesTo(this)
    }

    class SalesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val clientName: TextView = itemView.findViewById(R.id.client_name)
        val productUnits: TextView = itemView.findViewById(R.id.products_units)
        val time: TextView = itemView.findViewById(R.id.time)
        val paymentMethod: TextView = itemView.findViewById(R.id.payment_method)
        val payImage: ImageView = itemView.findViewById(R.id.pay_image)
        val subtotal: TextView = itemView.findViewById(R.id.subtotal)
        val taxes: TextView = itemView.findViewById(R.id.tax)
        val discounts: TextView = itemView.findViewById(R.id.discounts)
        val toPay: TextView = itemView.findViewById(R.id.to_pay)
        val collected: TextView = itemView.findViewById(R.id.collected)
        val change: TextView = itemView.findViewById(R.id.change)
    }

}