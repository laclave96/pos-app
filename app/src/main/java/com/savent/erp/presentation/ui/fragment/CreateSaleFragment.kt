package com.savent.erp.presentation.ui.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.savent.erp.R
import com.savent.erp.databinding.FragmentCreateSaleBinding
import com.savent.erp.presentation.ui.CustomSnackBar
import com.savent.erp.presentation.ui.ClientsFilterDialog
import com.savent.erp.presentation.ui.adapters.ClientsAdapter
import com.savent.erp.presentation.ui.model.FilterItem
import com.savent.erp.presentation.viewmodel.ClientsViewModel
import com.savent.erp.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CreateSaleFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateSaleFragment : Fragment(), ClientsAdapter.OnClickListener{
    private lateinit var binding: FragmentCreateSaleBinding
    private val mainViewModel: MainViewModel by sharedViewModel()
    private val clientsViewModel: ClientsViewModel by viewModel()
    private lateinit var clientsAdapter: ClientsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_create_sale,
            container, false
        )
        binding.lifecycleOwner = this
        binding.viewModel = clientsViewModel

        initEvents()
        setupRecyclerView()
        subscribeToObservables()
        return binding.root
    }

    private fun initEvents() {
        binding.goOn.setOnClickListener {
            clientsViewModel.await()
        }

        lifecycleScope.launchWhenCreated {
            if (!clientsViewModel.isCreateClientAvailable())
                binding.addClient.visibility = View.GONE
        }

        binding.searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                s?.let { clientsViewModel.loadClients(it.toString()) }
            }

        })

        binding.addClient.setOnClickListener {
            mainViewModel.goToDestination(R.id.addClientFragment)
        }

        binding.backButton.setOnClickListener {
            mainViewModel.back()
        }
    }

    private fun setupRecyclerView() {
        clientsAdapter = ClientsAdapter()
        clientsAdapter.setOnClickListener(this)
        val recyclerView = binding.clientsRecycler
        recyclerView.layoutManager = LinearLayoutManager(context)
        clientsAdapter.setHasStableIds(true)
        recyclerView.adapter = clientsAdapter
        recyclerView.itemAnimator = DefaultItemAnimator()

    }

    private fun subscribeToObservables() {
        clientsViewModel.clients.observe(this) {
            clientsAdapter.setData(it)
        }

        lifecycleScope.launchWhenCreated {
            clientsViewModel.uiEvent.collectLatest { uiEvent ->
                when (uiEvent) {
                    is ClientsViewModel.UiEvent.ShowMessage -> {
                        try {
                            CustomSnackBar.make(
                                binding.root,
                                getString(uiEvent.resId ?: R.string.unknown_error),
                                CustomSnackBar.LENGTH_LONG
                            ).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    is ClientsViewModel.UiEvent.Continue -> {
                        if (uiEvent.available)
                            mainViewModel.goOn()
                    }
                    else -> {

                    }
                }
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CreateSaleFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CreateSaleFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onClick(id: Int) {
        clientsViewModel.addClientToSale(id)
    }

}