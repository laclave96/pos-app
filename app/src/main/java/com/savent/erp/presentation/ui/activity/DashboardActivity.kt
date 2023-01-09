package com.savent.erp.presentation.ui.activity

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.savent.erp.R
import com.savent.erp.databinding.ActivityDashboardBinding
import com.savent.erp.presentation.ui.CustomSnackBar
import com.savent.erp.presentation.ui.ExitDialog
import com.savent.erp.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel


class DashboardActivity : AppCompatActivity(), ExitDialog.OnClickListener{
    private lateinit var binding: ActivityDashboardBinding
    private val dashboardViewModel by viewModel<DashboardViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        subscribeToObservables()

    }

    private fun init() {
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        binding.viewModel = dashboardViewModel
        initEvents()
    }

    private fun initEvents(){

        lifecycleScope.launchWhenCreated {
            if (!dashboardViewModel.isCreateClientAvailable())
                binding.addClient.visibility = View.GONE
        }

        binding.addClient.setOnClickListener{
            goAddClient()
        }

        binding.createSale.setOnClickListener {
            goCreateSale()
        }

        binding.getDebts.setOnClickListener {
            getDebts()
        }
    }


    private fun subscribeToObservables() {

        dashboardViewModel.businessBasics.observe(this) {
            it.image?.let {}
        }

        lifecycleScope.launchWhenStarted {
            dashboardViewModel.uiEvent.collectLatest { uiEvent ->
                when (uiEvent) {
                    is DashboardViewModel.UiEvent.ShowMessage -> {
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

    fun seeSales(view: View) {
        startActivity(Intent(this,LastSalesActivity::class.java))
    }

    fun exitStore(view: View) {
        val exitDialog = ExitDialog(this)
        exitDialog.setOnClickListener(this)
        exitDialog.show()
    }

    private fun goCreateSale(){
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("destination", R.id.createSaleFragment)
        startActivity(intent)
    }

    private fun goAddClient(){
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("destination", R.id.addClientFragment)
        startActivity(intent)
    }

    private fun getDebts(){
        startActivity(Intent(this, DebtsActivity::class.java))
    }

    override fun exit() {
        try {
            (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .clearApplicationUserData()
            startActivity(Intent(this,LoginActivity::class.java))
        }catch (e: Exception){
            startActivity(Intent(this,LoginActivity::class.java))
            finish()
        }

    }

}