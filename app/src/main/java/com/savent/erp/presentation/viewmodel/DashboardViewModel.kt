package com.savent.erp.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.savent.erp.ConnectivityObserver
import com.savent.erp.MyApplication
import com.savent.erp.NetworkConnectivityObserver
import com.savent.erp.R
import com.savent.erp.domain.usecase.GetBusinessBasicsUseCase
import com.savent.erp.domain.usecase.ReloadBusinessBasicsDataUseCase
import com.savent.erp.domain.usecase.ReloadStatsDataUseCase
import com.savent.erp.presentation.ui.model.BusinessBasicsItem
import com.savent.erp.presentation.ui.model.LoginError
import com.savent.erp.presentation.ui.model.StatItem
import com.savent.erp.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val myApplication: Application,
    private val reloadBusinessBasicsDataUseCase: ReloadBusinessBasicsDataUseCase,
    private val reloadStatsDataUseCase: ReloadStatsDataUseCase,
    private val getBusinessBasicsUseCase: GetBusinessBasicsUseCase,
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private var _networkStatus = ConnectivityObserver.Status.Available

    private val _businessBasics = MutableLiveData<BusinessBasicsItem>()
    val businessBasics: LiveData<BusinessBasicsItem> = _businessBasics

    private val _stats = MutableLiveData<List<StatItem>>()
    val stats: LiveData<List<StatItem>> = _stats

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowMessage(val resId: Int? = null) : UiEvent()
    }

    private var networkObserverJob: Job? = null
    private var loadBusinessBasicsJob: Job? = null

    init {
        observeNetworkChange()
        loadBusinessBasics()
    }

    private fun observeNetworkChange() {
        networkObserverJob?.cancel()
        networkObserverJob = viewModelScope.launch(Dispatchers.IO) {
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

    suspend fun isCreateClientAvailable(): Boolean{
        return getBusinessBasicsUseCase().first().data?.sellerLevel!! > 2
    }


    private suspend fun isInternetAvailable(): Boolean {
        if (_networkStatus != ConnectivityObserver.Status.Available) {
            _uiEvent.emit(UiEvent.ShowMessage(resId = R.string.internet_error))
            return false
        }
        return true
    }


    private fun loadBusinessBasics() {
        loadBusinessBasicsJob?.cancel()
        loadBusinessBasicsJob = viewModelScope.launch(Dispatchers.IO) {
            getBusinessBasicsUseCase().collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        _loading.postValue(true)
                    }
                    is Resource.Success -> {
                        _loading.postValue(false)

                        _businessBasics.postValue(result.data)
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
    }



}