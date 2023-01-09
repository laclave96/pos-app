package com.savent.erp.presentation.ui.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.savent.erp.R
import com.savent.erp.databinding.ActivityOpeningBinding
import com.savent.erp.presentation.viewmodel.LoginViewModel
import com.savent.erp.presentation.viewmodel.OpeningViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class OpeningActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOpeningBinding
    private val openingViewModel: OpeningViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        subscribeToObservables()
        checkIfLoggedIn()
    }


    private fun init() {
        binding = ActivityOpeningBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        binding.viewModel = openingViewModel
    }

    private fun subscribeToObservables() {
        lifecycleScope.launch {
            openingViewModel.uiEvent.collectLatest { uiEvent ->
                when (uiEvent) {
                    is OpeningViewModel.UiEvent.LoggedIn ->{
                        delay(800)
                        val cls = if (uiEvent.success) DashboardActivity::class.java
                        else LoginActivity::class.java
                        startActivity(Intent(this@OpeningActivity, cls))
                        finish()
                    }
                    else -> {}
                }
            }

        }

    }

    private fun checkIfLoggedIn(){
        openingViewModel.isLogged()
    }
}