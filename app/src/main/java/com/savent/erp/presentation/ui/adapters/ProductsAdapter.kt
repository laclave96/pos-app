package com.savent.erp.presentation.ui.adapters

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.savent.erp.R
import com.savent.erp.presentation.ui.model.ProductItem

class ProductsAdapter(private val context: Context?) :
    RecyclerView.Adapter<ProductsAdapter.ProductsViewHolder>() {

    private val products = ArrayList<ProductItem>()
    private var _listener: OnClickListener? = null

    interface OnClickListener {
        fun onViewProductClick(id: Int)
        fun onAddProductClick(id: Int)
        fun onRemoveProductClick(id: Int)
        fun onChangeProductsUnitsClick(id: Int, units: Int)
    }


    fun setOnClickListener(listener: OnClickListener) {
        _listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.product_item, parent, false)
        return ProductsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductsViewHolder, position: Int) {
        val product = products[position]

        product.image?.let { holder.image }
        holder.name.text = product.name
        holder.price.text = context?.getString(R.string.price)?.format("${product.price}")
        holder.remainingUnits.text = context?.getString(R.string.remaining_units)
            ?.format("${product.remainingUnits}")

        holder.unitsSelected.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isEmpty()) {
                    holder.unitsSelected.setText("0")
                    holder.unitsSelected.setSelection(holder.unitsSelected.text.length)
                    return
                }

                _listener?.onChangeProductsUnitsClick(product.localId, s.toString().toInt())
            }

        })

        holder.unitsSelected.setText("${product.selectedUnits}")
        holder.unitsSelected.setSelection(holder.unitsSelected.text.length)

        holder.addUnit.setOnClickListener {
            if (product.remainingUnits != 0)
                _listener?.onAddProductClick(product.localId)
        }
        holder.removeUnit.setOnClickListener {
            if (product.selectedUnits != 0)
                _listener?.onRemoveProductClick(product.localId)
        }

    }

    override fun getItemCount(): Int =
        products.size

    override fun getItemId(position: Int): Long {
        return products[position].localId.toLong()
    }

    fun setData(newProducts: List<ProductItem>) {
        val diffCallback = ProductsDiffCallBack(products, newProducts)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        products.clear()
        products.addAll(newProducts)
        diffResult.dispatchUpdatesTo(this)
    }

    class ProductsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.image)
        val name: TextView = itemView.findViewById(R.id.product_name)
        val price: TextView = itemView.findViewById(R.id.price)
        val remainingUnits: TextView = itemView.findViewById(R.id.stock)
        val unitsSelected: EditText = itemView.findViewById(R.id.units_selected)
        val addUnit: LinearLayout = itemView.findViewById(R.id.add_button)
        val removeUnit: LinearLayout = itemView.findViewById(R.id.remove_button)
        val div: View = itemView.findViewById(R.id.div)
    }


}