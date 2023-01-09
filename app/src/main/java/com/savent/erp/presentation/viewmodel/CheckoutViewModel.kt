package com.savent.erp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mazenrashed.printooth.data.printable.Printable
import com.savent.erp.R
import com.savent.erp.domain.repository.BusinessBasicsRepository
import com.savent.erp.domain.usecase.*
import com.savent.erp.presentation.ui.model.Checkout
import com.savent.erp.presentation.ui.model.ProductItem
import com.savent.erp.presentation.ui.model.SharedReceipt
import com.savent.erp.utils.PaymentMethod
import com.savent.erp.utils.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CheckoutViewModel(
    private val businessBasicsRepository: BusinessBasicsRepository,
    private val getCheckoutSaleUseCase: GetCheckoutSaleUseCase,
    private val getSelectedProductsUseCase: GetSelectedProductsUseCase,
    private val saveCompletedSaleUseCase: SaveCompletedSaleUseCase,
    private val increaseExtraDiscountPercentUseCase: IncreaseExtraDiscountPercentUseCase,
    private val decreaseExtraDiscountPercentUseCase: DecreaseExtraDiscountPercentUseCase,
    private val updateExtraDiscountPercentUseCase: UpdateExtraDiscountPercentUseCase,
    private val updateCollectedPaymentUseCase: UpdateCollectedPaymentUseCase,
    private val updatePaymentMethodUseCase: UpdatePaymentMethodUseCase,
    private val getReceiptToSend: GetReceiptToSend,
    private val getReceiptToPrint: GetReceiptToPrint,
    private val remoteSaleSyncFromLocalUseCase: RemoteSaleSyncFromLocalUseCase
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _saveSaleSuccess = MutableLiveData(false)
    val saveSaleSuccess: LiveData<Boolean> = _saveSaleSuccess

    private val _sharedReceipt = MutableLiveData<SharedReceipt>()
    val sharedReceipt: LiveData<SharedReceipt> = _sharedReceipt

    private val _printable = MutableLiveData<ArrayList<Printable>>()
    val printable: LiveData<ArrayList<Printable>> = _printable

    private val _checkout = MutableLiveData<Checkout>()
    val checkout: LiveData<Checkout> = _checkout

    private val _products = MutableLiveData<List<ProductItem>>()
    val products: LiveData<List<ProductItem>> = _products

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowMessage(val resId: Int? = null) : UiEvent()
    }

    init {
        loadCheckout()
        loadProducts()
    }

    private var checkoutJob: Job? = null
    private var loadSelectedProducts: Job? = null
    private var updateExtraDiscountJob: Job? = null
    private var updatePaymentJob: Job? = null
    private var loadReceiptToSendJob: Job? = null
    private var loadReceiptToPrintJob: Job? = null
    private var recordSaleJob: Job? = null
    private var defaultJob: Job? = null

    private fun loadCheckout() {
        checkoutJob?.cancel()
        checkoutJob = viewModelScope.launch(Dispatchers.IO) {
            getCheckoutSaleUseCase().onEach { result ->
                when (result) {
                    is Resource.Loading -> {
                        _loading.postValue(true)
                    }
                    is Resource.Success -> {
                        _loading.postValue(false)
                        _checkout.postValue(result.data)
                    }
                    is Resource.Error -> {
                        _loading.postValue(false)
                        _uiEvent.emit(
                            UiEvent.ShowMessage(
                                result.resId ?: R.string.unknown_error
                            )
                        )
                    }
                }

            }.collect()
        }
    }

    fun loadProducts() {
        loadSelectedProducts?.cancel()
        loadSelectedProducts = viewModelScope.launch(Dispatchers.IO) {
            getSelectedProductsUseCase().onEach { result ->
                when (result) {
                    is Resource.Loading -> {
                    }
                    is Resource.Success -> {
                        _products.postValue(result.data)
                    }
                    is Resource.Error -> {
                        _uiEvent.emit(
                            UiEvent.ShowMessage(
                                result.resId ?: R.string.unknown_error
                            )
                        )
                    }
                }

            }.collect()
        }
    }

    fun increaseExtraDiscountPercent() {
        updateExtraDiscountJob?.cancel()
        updateExtraDiscountJob = viewModelScope.launch(Dispatchers.IO) {
            when (val result = increaseExtraDiscountPercentUseCase()) {
                is Resource.Error -> {
                    _uiEvent.emit(
                        UiEvent.ShowMessage(
                            result.resId ?: R.string.unknown_error
                        )
                    )
                }
                else -> {
                }
            }
        }
    }

    fun decreaseExtraDiscountPercent() {
        updateExtraDiscountJob?.cancel()
        updateExtraDiscountJob = viewModelScope.launch(Dispatchers.IO) {
            when (val result = decreaseExtraDiscountPercentUseCase()) {
                is Resource.Error -> {
                    _uiEvent.emit(
                        UiEvent.ShowMessage(
                            result.resId ?: R.string.unknown_error
                        )
                    )
                }
                else -> {
                }
            }
        }
    }

    fun updateExtraDiscountPayment(discount: Int) {
        updateExtraDiscountJob?.cancel()
        updateExtraDiscountJob = viewModelScope.launch(Dispatchers.IO) {
            when (val result = updateExtraDiscountPercentUseCase(discount)) {
                is Resource.Error -> {
                    _uiEvent.emit(
                        UiEvent.ShowMessage(
                            result.resId ?: R.string.unknown_error
                        )
                    )
                }
                else -> {
                }
            }
        }
    }

    fun updatePaymentMethod(method: PaymentMethod) {
        updatePaymentJob?.cancel()
        updatePaymentJob = viewModelScope.launch(Dispatchers.IO) {
            when (val result = updatePaymentMethodUseCase(method)) {
                is Resource.Error -> {
                    _uiEvent.emit(
                        UiEvent.ShowMessage(
                            result.resId ?: R.string.unknown_error
                        )
                    )
                }
                else -> {
                }
            }
        }
    }

    fun updateCollectedPayment(collected: Float) {
        updatePaymentJob?.cancel()
        updatePaymentJob = viewModelScope.launch(Dispatchers.IO) {
            when (val result = updateCollectedPaymentUseCase(collected)) {
                is Resource.Error -> {
                    _uiEvent.emit(
                        UiEvent.ShowMessage(
                            result.resId ?: R.string.unknown_error
                        )
                    )
                }
                else -> {
                }
            }
        }
    }

    fun loadReceiptToSend() {
        _loading.postValue(true)
        loadReceiptToSendJob?.cancel()
        loadReceiptToSendJob = viewModelScope.launch(Dispatchers.IO) {
            when (val result = getReceiptToSend()) {
                is Resource.Success -> {
                    _loading.postValue(false)
                    _sharedReceipt.postValue(result.data)
                }
                is Resource.Error -> {
                    _loading.postValue(false)
                    _uiEvent.emit(
                        UiEvent.ShowMessage(
                            result.resId ?: R.string.unknown_error
                        )
                    )
                }
            }
        }
    }

    fun loadReceiptToPrint() {
        _loading.postValue(true)
        loadReceiptToSendJob?.cancel()
        loadReceiptToSendJob = viewModelScope.launch(Dispatchers.IO) {
            Log.d("log_", "loadingReceipt")
            when (val result = getReceiptToPrint()) {
                is Resource.Success -> {
                    _loading.postValue(false)
                    Log.d("log_", Gson().toJson(result.data))
                    _printable.postValue(result.data)
                }
                is Resource.Error -> {
                    _loading.postValue(false)
                    _uiEvent.emit(
                        UiEvent.ShowMessage(
                            result.resId ?: R.string.unknown_error
                        )
                    )
                }
            }
        }
    }


    fun recordSale() {
        defaultJob?.cancel()
        defaultJob = viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)
            recordSaleJob?.cancel()
            recordSaleJob = viewModelScope.launch(Dispatchers.IO) {
                when (val result = saveCompletedSaleUseCase()) {
                    is Resource.Success -> {
                        coroutineScope {
                            _loading.postValue(true)
                            async {
                                businessBasicsRepository.getBusinessBasics().first().data?.let {
                                    remoteSaleSyncFromLocalUseCase(
                                        it.id,
                                        it.sellerId,
                                        it.storeId,
                                        it.featureName
                                    )
                                }
                            }.await()
                            _saveSaleSuccess.postValue(true)
                        }

                    }
                    is Resource.Error -> {
                        _uiEvent.emit(
                            UiEvent.ShowMessage(
                                result.resId ?: R.string.unknown_error
                            )
                        )
                    }
                }
            }
        }
    }


}