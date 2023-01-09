package com.savent.erp.presentation.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.LatLng
import com.savent.erp.AppConstants
import com.savent.erp.R
import com.savent.erp.data.remote.model.Client
import com.savent.erp.databinding.FragmentAddClientBinding
import com.savent.erp.domain.usecase.LocationRequestUseCase
import com.savent.erp.presentation.ui.CustomSnackBar
import com.savent.erp.presentation.viewmodel.ClientsViewModel
import com.savent.erp.presentation.viewmodel.MainViewModel
import com.savent.erp.utils.DateFormat
import com.savent.erp.utils.DateTimeObj
import com.savent.erp.utils.IsLocationGranted
import com.savent.erp.utils.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AddClientFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AddClientFragment : Fragment() {
    private lateinit var binding: FragmentAddClientBinding
    private val mainViewModel: MainViewModel by sharedViewModel()
    private val clientsViewModel: ClientsViewModel by sharedViewModel()
    private var currentLatLong: LatLng? = null
    private var isWaitingForLatLong = false
    private val locationRequestUseCase = LocationRequestUseCase()
    private var requestLocationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subscribeToObservables()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_add_client,
            container, false
        )
        binding.lifecycleOwner = this
        binding.viewModel = clientsViewModel

        initDefaultStates()
        initEvents()
        return binding.root
    }

    private fun initDefaultStates() {
        val arrayAdapter = context?.let {
            ArrayAdapter<String>(
                it,
                R.layout.spinner_item,
                resources.getStringArray(R.array.mexico_states)
            )
        }
        binding.stateEdit.adapter = arrayAdapter
    }

    private fun initEvents() {
        binding.addClient.setOnClickListener {
            if (currentLatLong != null) {
                saveClient()
                return@setOnClickListener
            }
            isWaitingForLatLong = true
            if (!IsLocationGranted(context!!)) {
                requestLocationPermission()
                return@setOnClickListener
            }
            runLocationRequest()
        }

        binding.backButton.setOnClickListener {
            mainViewModel.back()
        }
    }

    private fun subscribeToObservables() {

        mainViewModel.latLng.observe(this) {
            if (currentLatLong == null) {
                currentLatLong = it
                mainViewModel.removeLocationUpdates()
            }
            if (isWaitingForLatLong) {
                saveClient()
            }
        }

        mainViewModel.loading.observe(this) {
            if (it) binding.progressLayout.visibility = View.VISIBLE
            else binding.progressLayout.visibility = View.GONE
        }

        clientsViewModel.loading.observe(this) {
            if (it) binding.progressLayout.visibility = View.VISIBLE
            else binding.progressLayout.visibility = View.GONE
        }

        clientsViewModel.clientError.observe(this) { result ->
            binding.nameEdit.error = result.nameError?.let { getString(it) }
            binding.paternalNameEdit.error = result.paternalError?.let { getString(it) }
            binding.maternalNameEdit.error = result.maternalError?.let { getString(it) }
            binding.socialReasonEdit.error = result.socialReasonError?.let { getString(it) }
            binding.rfcEdit.error = result.rfcError?.let { getString(it) }
            binding.phoneNumberEdit.error = result.phoneError?.let { getString(it) }
            binding.emailEdit.error = result.emailError?.let { getString(it) }
            binding.streetEdit.error = result.streetError?.let { getString(it) }
            binding.noEdit.error = result.noExteriorError?.let { getString(it) }
            binding.coloniaEdit.error = result.coloniaError?.let { getString(it) }
            binding.postalCodeEdit.error = result.postalCodeError?.let { getString(it) }
            binding.cityEdit.error = result.cityError?.let { getString(it) }
            binding.countryEdit.error = result.countryError?.let { getString(it) }
        }

        lifecycleScope.launchWhenCreated{
            clientsViewModel.uiEvent.collectLatest { uiEvent ->
                when (uiEvent) {
                    is ClientsViewModel.UiEvent.ShowMessage -> {
                        CustomSnackBar.make(
                            binding.root,
                            getString(uiEvent.resId ?: R.string.unknown_error),
                            CustomSnackBar.LENGTH_LONG
                        ).show()
                    }
                    is ClientsViewModel.UiEvent.SaveClient -> {
                        if (uiEvent.success) {
                            mainViewModel.goOn()
                        }
                    }
                    else -> {
                    }
                }
            }

        }

        lifecycleScope.launchWhenCreated{
            mainViewModel.message.collectLatest { message ->
                CustomSnackBar.make(
                    binding.root,
                    getString(message.resId ?: R.string.unknown_error),
                    CustomSnackBar.LENGTH_LONG
                ).show()
            }

        }
    }



    private fun runLocationRequest() {
        requestLocationJob?.cancel()
        requestLocationJob = lifecycleScope.launch {
            activity?.let { it1 ->
                locationRequestUseCase(it1).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            mainViewModel.runLocationUpdates()
                        }
                        else -> {
                            requestLocationJob?.cancel()
                            return@collectLatest
                        }
                    }
                }
            }
        }
    }

    private fun saveClient() {
        val time = System.currentTimeMillis()
        val client = Client(
            0,
            null,
            binding.nameEdit.text.toString().trim(),
            "",
            binding.paternalNameEdit.text.toString().trim(),
            binding.maternalNameEdit.text.toString().trim(),
            binding.socialReasonEdit.text.toString().trim(),
            binding.rfcEdit.text.toString().trim(),
            if (binding.phoneNumberEdit.text.toString().isEmpty()) 0
            else binding.phoneNumberEdit.text.toString().toLong(),
            binding.emailEdit.text.toString().trim(),
            binding.streetEdit.text.toString().trim(),
            binding.noEdit.text.toString().trim(),
            binding.coloniaEdit.text.toString().trim(),
            binding.postalCodeEdit.text.toString().trim(),
            binding.cityEdit.text.toString().trim(),
            resources.getStringArray(R.array.mexico_states)
                    [binding.stateEdit.selectedItemPosition],
            binding.countryEdit.text.toString().trim(),
            currentLatLong ?: LatLng(1.0, 1.0),
            DateTimeObj(
                DateFormat.getString(time, "yyyy-MM-dd"),
                DateFormat.getString(time, "HH:mm")
            )
        )
        clientsViewModel.saveNewClient(client)
    }


    private fun requestLocationPermission() {
        activity?.requestPermissions(
            AppConstants.LOCATION_PERMISSIONS, AppConstants.REQUEST_LOCATION_PERMISSION_CODE
        )
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AddClientFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AddClientFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }


}