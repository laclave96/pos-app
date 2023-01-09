package com.savent.erp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.savent.erp.domain.usecase.IsLoggedUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class OpeningViewModel(val isLoggedUseCase: IsLoggedUseCase): ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading


    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowMessage(val resId: Int? = null) : UiEvent()
        data class LoggedIn(val success: Boolean):UiEvent()
    }

    private var openingJob: Job? = null

    fun isLogged() {
        openingJob?.cancel()
        openingJob = viewModelScope.launch(Dispatchers.IO) {
            _uiEvent.emit(UiEvent.LoggedIn(isLoggedUseCase()))
        }

    }
}