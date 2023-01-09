package com.savent.erp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.savent.erp.R
import com.savent.erp.domain.usecase.ComputeBalanceUseCase
import com.savent.erp.domain.usecase.GetSalesOfDayUseCase
import com.savent.erp.presentation.ui.model.Balance
import com.savent.erp.presentation.ui.model.SaleItem
import com.savent.erp.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SalesViewModel(
    private val getSalesOfDayUseCase: GetSalesOfDayUseCase,
    private val computeBalanceUseCase: ComputeBalanceUseCase
) : ViewModel() {

    private val _sales = MutableLiveData<List<SaleItem>>()
    val sales: LiveData<List<SaleItem>> = _sales

    private val _balance = MutableLiveData<Balance>()
    val balance: LiveData<Balance> = _balance

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowMessage(val resId: Int? = null) : UiEvent()
    }

    private var loadSalesJob: Job? = null
    private var computeBalanceJob: Job? = null

    init {
        loadSales()
        computeBalance()
    }

    private fun loadSales() {
        loadSalesJob?.cancel()
        loadSalesJob = viewModelScope.launch(Dispatchers.IO) {
            getSalesOfDayUseCase().onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let {
                            _sales.postValue(it)
                            if (it.isEmpty()){
                                _uiEvent.emit(UiEvent.ShowMessage(R.string.without_sales))
                            }
                        }
                    }

                    else -> {
                    }
                }

            }.collect()
        }
    }

    private fun computeBalance() {
        computeBalanceJob?.cancel()
        computeBalanceJob = viewModelScope.launch(Dispatchers.IO) {
            computeBalanceUseCase().onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { _balance.postValue(it) }
                    }
                    else -> {
                    }
                }

            }.collect()
        }
    }
}