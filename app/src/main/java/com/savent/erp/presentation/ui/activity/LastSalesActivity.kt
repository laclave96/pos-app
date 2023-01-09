package com.savent.erp.presentation.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.savent.erp.R
import com.savent.erp.databinding.ActivityLastSalesBinding
import com.savent.erp.presentation.ui.CustomSnackBar
import com.savent.erp.presentation.ui.adapters.SalesAdapter
import com.savent.erp.presentation.viewmodel.SalesViewModel
import com.savent.erp.utils.DecimalFormat
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel

class LastSalesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLastSalesBinding
    private val salesViewModel: SalesViewModel by viewModel()
    private lateinit var salesAdapter: SalesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        subscribeToObservables()
        setupRecyclerView()
    }

    private fun init() {
        binding = ActivityLastSalesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        binding.viewModel = salesViewModel
    }

    private fun setupRecyclerView() {
        salesAdapter = SalesAdapter(this)
        val recyclerView = binding.salesRecycler
        recyclerView.layoutManager = LinearLayoutManager(this)
        salesAdapter.setHasStableIds(true)
        recyclerView.adapter = salesAdapter
        recyclerView.itemAnimator = DefaultItemAnimator()
    }

    private fun subscribeToObservables() {
        salesViewModel.sales.observe(this) {
            salesAdapter.setData(it)
        }
        salesViewModel.balance.observe(this) {

            binding.revenue.text = getString(R.string.price)
                .format(DecimalFormat.format(it.revenues))

            binding.debts.text = getString(R.string.price)
                .format(DecimalFormat.format(it.debts))
        }
        lifecycleScope.launchWhenStarted {
            salesViewModel.uiEvent.collectLatest { uiEvent ->
                when (uiEvent) {
                    is SalesViewModel.UiEvent.ShowMessage -> {
                        CustomSnackBar.make(
                            binding.root,
                            getString(uiEvent.resId ?: R.string.unknown_error),
                            CustomSnackBar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    fun back(view: View) {
        finish()
    }


}