package com.savent.erp.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.savent.erp.ConnectivityObserver
import com.savent.erp.MyApplication
import com.savent.erp.R
import com.savent.erp.data.local.datasource.AppPreferencesLocalDatasource
import com.savent.erp.data.local.model.AppPreferences
import com.savent.erp.data.local.model.BusinessBasicsLocal
import com.savent.erp.data.remote.model.Client
import com.savent.erp.domain.repository.BusinessBasicsRepository
import com.savent.erp.domain.usecase.*
import com.savent.erp.presentation.ui.model.ClientError
import com.savent.erp.presentation.ui.model.ClientItem
import com.savent.erp.utils.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ClientsViewModel(
    private val myApplication: Application,
    private val appPreferencesLocalDatasource: AppPreferencesLocalDatasource,
    private val businessBasicsRepository: BusinessBasicsRepository,
    private val createPendingSaleUseCase: CreatePendingSaleUseCase,
    private val getClientListUseCase: GetClientListUseCase,
    private val reloadClientsUseCase: ReloadClientsUseCase,
    private val removeAllClientsUseCase: RemoveAllClientsUseCase,
    private val createNewClientUseCase: CreateNewClientUseCase,
    private val addClientToSaleUseCase: AddClientToSaleUseCase,
    private val validateClientUseCase: ValidateClientUseCase,
    private val remoteClientSyncFromLocalUseCase: RemoteClientSyncFromLocalUseCase,
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _networkStatus = MutableLiveData(ConnectivityObserver.Status.Available)
    val networkStatus: LiveData<ConnectivityObserver.Status> = _networkStatus

    private val _appPreferences = MutableLiveData<AppPreferences>()
    val appPreferences: LiveData<AppPreferences> = _appPreferences

    private var _businessBasics: BusinessBasicsLocal? = null

    private val _clients = MutableLiveData<List<ClientItem>>()
    val clients: LiveData<List<ClientItem>> = _clients

    private val _clientError = MutableLiveData<ClientError>()
    val clientError: LiveData<ClientError> = _clientError

    private var isClientSuccessfullyAdded = false

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowMessage(val resId: Int? = null) : UiEvent()
        data class SaveClient(val success: Boolean = false) : UiEvent()
        data class Continue(val available: Boolean = false) : UiEvent()
    }

    private var loadDataJob: Job? = null
    private var loadClientsJob: Job? = null
    private var reloadClientsJob: Job? = null
    private var createPendingSaleJob: Job? = null
    private var addClientJob: Job? = null
    private var networkObserverJob: Job? = null
    private var changeClientsFilterJob: Job? = null
    private var defaultJob: Job? = null

    init {
        loadData()
        createPendingSale()
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
            if(isInternetAvailable())
                fetchClientsFromNetwork(_appPreferences.value?.clientsFilter?:"")
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

    private fun fetchClientsFromNetwork(clientsFilter: String) {
        reloadClientsJob?.cancel()
        reloadClientsJob = viewModelScope.launch(Dispatchers.IO) {
            reloadClientsUseCase(
                _businessBasics!!.sellerId,
                _businessBasics!!.storeId,
                _businessBasics!!.featureName,
                clientsFilter
            )
            _loading.postValue(false)
        }
    }

    suspend fun isCreateClientAvailable(): Boolean {
        return businessBasicsRepository.getBusinessBasics().first().data?.sellerLevel!! > 2
    }

    fun loadClients(query: String = "") {
        loadClientsJob?.cancel()
        loadClientsJob = viewModelScope.launch(Dispatchers.IO) {
            getClientListUseCase(query).onEach { result ->
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

    private fun createPendingSale() {
        createPendingSaleJob?.cancel()
        createPendingSaleJob = viewModelScope.launch(Dispatchers.IO) {
            createPendingSaleUseCase()
        }
    }

    fun saveNewClient(client: Client) {
        _loading.postValue(true)
        addClientJob?.cancel()
        addClientJob = viewModelScope.launch(Dispatchers.IO) {
            when (val result = validateClientUseCase(client)) {
                is Resource.Loading -> {
                }
                is Resource.Success -> {
                    _clientError.postValue(ClientError())
                }
                is Resource.Error -> {
                    _clientError.postValue(
                        Gson()
                            .fromJson(result.message, object : TypeToken<ClientError>() {}.type)
                    )
                    _uiEvent.emit(
                        UiEvent.ShowMessage(
                            result.resId ?: R.string.unknown_error
                        )
                    )
                    _loading.postValue(false)
                    return@launch
                }

            }
            when (val result = createNewClientUseCase(client)) {
                is Resource.Loading -> {
                }
                is Resource.Success -> {
                    coroutineScope {
                        async {
                            businessBasicsRepository.getBusinessBasics().first().data?.let {
                                remoteClientSyncFromLocalUseCase(
                                    it.sellerId,
                                    it.storeId,
                                    it.featureName
                                )
                            }
                        }.await()
                        _loading.postValue(false)
                        _uiEvent.emit(UiEvent.SaveClient(true))
                    }

                }
                is Resource.Error -> {
                    _loading.postValue(false)
                    _uiEvent.emit(UiEvent.SaveClient(false))
                    _uiEvent.emit(
                        UiEvent.ShowMessage(
                            result.resId ?: R.string.unknown_error
                        )
                    )
                }

            }
        }
    }

    fun addClientToSale(clientId: Int) {
        addClientJob?.cancel()
        addClientJob = viewModelScope.launch(Dispatchers.IO) {
            when (val result = addClientToSaleUseCase(clientId)) {
                is Resource.Success -> {
                    isClientSuccessfullyAdded = true
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


    fun await() {
        defaultJob?.cancel()
        defaultJob = viewModelScope.launch(Dispatchers.Main) {
            addClientJob?.join()
            if (!isClientSuccessfullyAdded) {
                _uiEvent.emit(UiEvent.ShowMessage(R.string.client_not_added))
                //defaultJob?.cancel()
                return@launch
            }
            _uiEvent.emit(UiEvent.Continue(true))
        }
    }

    private suspend fun isInternetAvailable(): Boolean {
        if (_networkStatus.value != ConnectivityObserver.Status.Available) {
            _uiEvent.emit(UiEvent.ShowMessage(resId = R.string.internet_error))
            return false
        }
        return true
    }

}