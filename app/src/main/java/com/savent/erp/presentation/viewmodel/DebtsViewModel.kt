package com.savent.erp.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savent.erp.ConnectivityObserver
import com.savent.erp.MyApplication
import com.savent.erp.R
import com.savent.erp.data.local.datasource.AppPreferencesLocalDatasource
import com.savent.erp.data.local.model.AppPreferences
import com.savent.erp.data.local.model.BusinessBasicsLocal
import com.savent.erp.data.remote.model.DebtPayment
import com.savent.erp.domain.repository.BusinessBasicsRepository
import com.savent.erp.domain.usecase.*
import com.savent.erp.presentation.ui.model.ClientWithDebtItem
import com.savent.erp.presentation.ui.model.IncompletePaymentItem
import com.savent.erp.utils.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DebtsViewModel(
    private val myApplication: Application,
    private val appPreferencesLocalDatasource: AppPreferencesLocalDatasource,
    private val businessBasicsRepository: BusinessBasicsRepository,
    private val getClientListWithDebts: GetClientListWithDebts,
    private val getIncompletePaymentsUseCase: GetIncompletePaymentsUseCase,
    private val reloadIncompletePaymentsUseCase: ReloadIncompletePaymentsUseCase,
    private val reloadClientsUseCase: ReloadClientsUseCase,
    private val removeAllClientsUseCase: RemoveAllClientsUseCase,
    private val payDebtUseCase: PayDebtUseCase,
    private val remoteDebtPaymentSyncFromLocalUseCase: RemoteDebtPaymentSyncFromLocalUseCase
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _appPreferences = MutableLiveData<AppPreferences>()
    val appPreferences: LiveData<AppPreferences> = _appPreferences

    private val _networkStatus = MutableLiveData(ConnectivityObserver.Status.Available)
    val networkStatus: LiveData<ConnectivityObserver.Status> = _networkStatus

    private val _clients = MutableLiveData<List<ClientWithDebtItem>>()
    val clients: LiveData<List<ClientWithDebtItem>> = _clients

    private var _businessBasics: BusinessBasicsLocal? = null

    private val _incompletePayments = MutableLiveData<List<IncompletePaymentItem>>()
    val incompletePayments: LiveData<List<IncompletePaymentItem>> = _incompletePayments

    private val _clientName = MutableLiveData<String>()
    val clientName : LiveData<String> = _clientName

    private val _clientId = MutableLiveData<Int>()
    val clientId : LiveData<Int> = _clientId

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowMessage(val resId: Int? = null) : UiEvent()
        object Back : UiEvent()
    }

    private var loadDataJob: Job? = null
    private var loadClientsJob: Job? = null
    private var reloadClientsJob: Job? = null
    private var loadIncompletePaymentsJob: Job? = null
    private var reloadIncompletePaymentsJob: Job? = null
    private var payDebtJob: Job? = null
    private var networkObserverJob: Job? = null
    private var changeClientsFilterJob: Job? = null

    init {
        loadData()
        observeNetworkChange()
    }

    private fun observeNetworkChange() {
        networkObserverJob?.cancel()
        networkObserverJob = viewModelScope.launch(Dispatchers.IO) {
            (myApplication as MyApplication).networkStatus.collectLatest {
                _networkStatus.postValue(it)
                if (it == ConnectivityObserver.Status.Available)
                    if (isInternetAvailable()) {
                        fetchClientsFromNetwork(_appPreferences.value?.clientsFilter?:"")
                        fetchIncompletePaymentsFromNetwork()
                    }
            }
        }

    }

    private fun loadData() {
        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch(Dispatchers.IO) {
            loadClients()
            async {loadAppPreferences()}
            loadBusinessBasics()
            delay(1000)
            if(isInternetAvailable()){
                fetchClientsFromNetwork(_appPreferences.value?.clientsFilter?:"")
                fetchIncompletePaymentsFromNetwork()
            }
        }
    }

    private suspend fun loadAppPreferences() {
        appPreferencesLocalDatasource.getAppPreferences().onEach {
            if (it is Resource.Success && it.data != null) _appPreferences.postValue(it.data)
        }.collect()
    }


    private suspend fun loadBusinessBasics() {
        val businessBasics = businessBasicsRepository.getBusinessBasics().first()
        if (businessBasics is Resource.Success && businessBasics.data != null)
            _businessBasics = businessBasics.data
    }

    private fun changeClientsFilter(clientsFilter: String) {
        changeClientsFilterJob?.cancel()
        changeClientsFilterJob = viewModelScope.launch(Dispatchers.IO) {

            if (appPreferencesLocalDatasource.updateAppPreferences(
                    _appPreferences.value!!.copy(
                        clientsFilter = clientsFilter
                    )
                ) is Resource.Success
            )
                _appPreferences.postValue(_appPreferences.value!!.copy( clientsFilter = clientsFilter))

            if (isInternetAvailable()) {
                _loading.postValue(true)
                removeAllClientsUseCase()
                fetchClientsFromNetwork(clientsFilter)
            }

        }

    }

    fun loadClients(query: String = "") {
        loadClientsJob?.cancel()
        loadClientsJob = viewModelScope.launch(Dispatchers.IO) {
            getClientListWithDebts(query).onEach { result ->
                when (result) {
                    is Resource.Loading -> {
                    }
                    is Resource.Success -> {
                        result.data?.let { _clients.postValue(it) }
                        if (result.data?.isEmpty() == true)
                            _uiEvent.emit(
                                UiEvent.ShowMessage(
                                    R.string.get_clients_empty
                                )
                            )
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

    fun loadIncompletePayments(clientId: Int, clientName: String) {
        _clientId.postValue(clientId)
        _clientName.postValue(clientName)
        loadIncompletePaymentsJob?.cancel()
        loadIncompletePaymentsJob = viewModelScope.launch(Dispatchers.IO) {
            getIncompletePaymentsUseCase(clientId).onEach { result ->
                when (result) {
                    is Resource.Loading -> {
                    }
                    is Resource.Success -> {
                        result.data?.let { _incompletePayments.postValue(it) }
                        if (result.data?.isEmpty() == true)
                            _uiEvent.emit(
                                UiEvent.ShowMessage(
                                    R.string.get_clients_empty
                                )
                            )
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

    private fun fetchIncompletePaymentsFromNetwork() {
        reloadIncompletePaymentsJob?.cancel()
        reloadIncompletePaymentsJob = viewModelScope.launch(Dispatchers.IO) {
            reloadIncompletePaymentsUseCase(
                _businessBasics!!.id,
                _businessBasics!!.storeId,
                _businessBasics!!.featureName
            )
        }
    }

    private fun fetchClientsFromNetwork(clientsFilter: String) {
        reloadClientsJob?.cancel()
        reloadClientsJob = viewModelScope.launch(Dispatchers.IO) {
            reloadClientsUseCase(
                _businessBasics!!.sellerId,
                _businessBasics!!.storeId,
                _businessBasics!!.featureName,
                clientsFilter
            )
            _loading.postValue(true)
        }
    }

    fun payDebt(debtPayment: DebtPayment, incompletePaymentId: Int) {
        payDebtJob?.cancel()
        payDebtJob = viewModelScope.launch(Dispatchers.IO) {
            payDebtUseCase(debtPayment, incompletePaymentId)
            if (isInternetAvailable()) {
                remoteDebtPaymentSyncFromLocalUseCase(
                    _businessBasics!!.id,
                    _businessBasics!!.sellerId,
                    _businessBasics!!.storeId,
                    _businessBasics!!.featureName
                )
            }

        }

    }

    private suspend fun isInternetAvailable(): Boolean {
        if (_networkStatus.value != ConnectivityObserver.Status.Available) {
            _uiEvent.emit(UiEvent.ShowMessage(resId = R.string.internet_error))
            return false
        }
        return true
    }

    fun onBackPressed(){
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.Back)
        }
    }


}