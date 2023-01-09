package com.savent.erp.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.savent.erp.ConnectivityObserver
import com.savent.erp.MyApplication
import com.savent.erp.R
import com.savent.erp.data.local.datasource.AppPreferencesLocalDatasource
import com.savent.erp.data.local.model.AppPreferences
import com.savent.erp.utils.Resource
import com.savent.erp.domain.usecase.LoginUseCase
import com.savent.erp.presentation.ui.model.LoginError
import com.savent.erp.data.remote.model.LoginCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LoginViewModel(
    private val myApplication: Application,
    private val appPreferencesLocalDatasource: AppPreferencesLocalDatasource,
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _loginError = MutableLiveData(LoginError())
    val loginError: LiveData<LoginError> = _loginError

    private val _loggedIn = MutableLiveData(false)
    val loggedIn: LiveData<Boolean> = _loggedIn

    private var _networkStatus = ConnectivityObserver.Status.Available

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowMessage(val resId: Int? = null) : UiEvent()
    }

    private var networkObserverJob: Job? = null
    private var loginJob: Job? = null
    private var appPreferencesJob: Job? = null

    init {
        observeNetworkChange()
    }

    private fun observeNetworkChange() {
        networkObserverJob?.cancel()
        networkObserverJob = viewModelScope.launch(Dispatchers.Main) {
            (myApplication as MyApplication).networkStatus.collectLatest {
                if (_networkStatus != it) {
                    _networkStatus = it

                    if (it != ConnectivityObserver.Status.Losing
                        && it != ConnectivityObserver.Status.Unavailable
                    ) {
                        val resId = when (it) {
                            ConnectivityObserver.Status.Available -> R.string.online
                            else -> {
                                R.string.offline
                            }
                        }
                        _uiEvent.emit(UiEvent.ShowMessage(resId))
                    }
                }
            }
        }

    }

    fun login(loginCredentials: LoginCredentials) {
        loginJob?.cancel()
        loginJob = viewModelScope.launch(Dispatchers.IO) {
            if (!isInternetAvailable()) return@launch
            loginUseCase(loginCredentials).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        _loading.postValue(true)
                    }
                    is Resource.Success -> {
                        loadDefaultAppPreferences()
                        _loading.postValue(false)
                        _loginError.postValue(LoginError())

                        _loggedIn.postValue(true)

                    }
                    is Resource.Error -> {
                        _loading.postValue(false)
                        /*_loginError.postValue(
                            Gson()
                                .fromJson(result.message, object : TypeToken<LoginError>() {}.type)
                        )*/
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

    private suspend fun loadDefaultAppPreferences() {
        val clientsOptions = myApplication.resources.getStringArray(R.array.clients_filter)
        val productsOptions = myApplication.resources.getStringArray(R.array.products_filter)
        appPreferencesLocalDatasource.insertAppPreferences(
            AppPreferences(
                clientsOptions[0],
                productsOptions[0],
                true
            )
        )
    }

    private suspend fun isInternetAvailable(): Boolean {
        if (_networkStatus != ConnectivityObserver.Status.Available) {
            _uiEvent.emit(UiEvent.ShowMessage(resId = R.string.internet_error))
            return false
        }
        return true
    }


}