package com.savent.erp.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.savent.erp.R
import com.savent.erp.databinding.ActivityLoginBinding
import com.savent.erp.presentation.ui.CustomSnackBar
import com.savent.erp.presentation.viewmodel.LoginViewModel
import com.savent.erp.data.remote.model.LoginCredentials
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        subscribeToObservables()
    }

    private fun init() {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        binding.viewModel = loginViewModel
        initDefaultStates()

    }

    private fun initDefaultStates() {
        val featureAdapter = ArrayAdapter<String>(
            this,
            R.layout.spinner_item,
            resources.getStringArray(R.array.features)
        )
        val storeAdapter = ArrayAdapter<String>(
            this,
            R.layout.spinner_item,
            resources.getStringArray(R.array.stores)
        )
        binding.featureEdit.adapter = featureAdapter
        binding.storeEdit.adapter = storeAdapter
    }

    private fun subscribeToObservables() {
        loginViewModel.loginError.observe(this) { result ->
            binding.pinEdit.error = result.rfcError?.let { getString(it) }
            binding.pinEdit.error = result.pinError?.let { getString(it) }
            //binding.storeEdit.error = result.storeError?.let { getString(it) }
        }

        loginViewModel.loggedIn.observe(this) {
            if (it) {
                startActivity(
                    Intent(
                        this@LoginActivity,
                        DashboardActivity::class.java
                    )
                )
                finish()
            }

        }

        lifecycleScope.launchWhenStarted {
            loginViewModel.uiEvent.collectLatest { uiEvent ->
                when (uiEvent) {
                    is LoginViewModel.UiEvent.ShowMessage -> {
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


    fun login(view: View) {
        loginViewModel.login(
            LoginCredentials(
                binding.rfcEdit.text.toString().trim(),
                binding.pinEdit.text.toString().trim(),
                resources.getStringArray(R.array.stores)
                        [binding.storeEdit.selectedItemPosition],
                resources.getStringArray(R.array.features)
                        [binding.featureEdit.selectedItemPosition],

            )
        )

    }
}