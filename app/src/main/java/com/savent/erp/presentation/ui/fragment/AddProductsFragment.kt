package com.savent.erp.presentation.ui.fragment

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.savent.erp.AppConstants
import com.savent.erp.R
import com.savent.erp.databinding.FragmentAddProductsBinding
import com.savent.erp.presentation.ui.CustomSnackBar
import com.savent.erp.presentation.ui.ClientsFilterDialog
import com.savent.erp.presentation.ui.ProductsFilterDialog
import com.savent.erp.presentation.ui.activity.ScannerActivity
import com.savent.erp.presentation.ui.adapters.ProductsAdapter
import com.savent.erp.presentation.ui.model.FilterItem
import com.savent.erp.presentation.viewmodel.MainViewModel
import com.savent.erp.presentation.viewmodel.ProductsViewModel
import com.savent.erp.utils.CheckPermissions
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AddProductsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AddProductsFragment : Fragment(), ProductsAdapter.OnClickListener{

    private lateinit var binding: FragmentAddProductsBinding
    private val mainViewModel: MainViewModel by sharedViewModel()
    private val productsViewModel: ProductsViewModel by viewModel()
    private lateinit var productsAdapter: ProductsAdapter
    private val REQUEST_CODE = 0
    private val PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_add_products,
            container, false
        )
        binding.lifecycleOwner = this
        binding.viewModel = productsViewModel

        initEvents()
        setupRecyclerView()
        subscribeToObservables()

        return binding.root
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductsAdapter(context)
        productsAdapter.setOnClickListener(this)
        val recyclerView = binding.productsRecycler
        recyclerView.layoutManager = LinearLayoutManager(context)
        productsAdapter.setHasStableIds(true)
        recyclerView.adapter = productsAdapter
        recyclerView.itemAnimator = DefaultItemAnimator()
    }

    private fun subscribeToObservables() {
        productsViewModel.products.observe(this) {
            productsAdapter.setData(it)
        }

        lifecycleScope.launchWhenStarted {
            productsViewModel.uiEvent.collectLatest { uiEvent ->
                when (uiEvent) {
                    is ProductsViewModel.UiEvent.ShowMessage -> {
                        CustomSnackBar.make(
                            binding.root,
                            getString(uiEvent.resId ?: R.string.unknown_error),
                            CustomSnackBar.LENGTH_LONG
                        ).show()
                    }
                    is ProductsViewModel.UiEvent.Continue -> {
                        if (uiEvent.available)
                            mainViewModel.goOn()
                    }
                }
            }
        }
    }

    private fun initEvents() {
        binding.goOn.setOnClickListener {
            productsViewModel.await()
        }

        binding.searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                //Log.d("afterTextChanged", s?.toString() ?: "empty")
                s?.let { productsViewModel.loadProducts(it.toString()) }
            }

        })

        binding.codeScanner.setOnClickListener {
            if (!CheckPermissions.check(context, PERMISSIONS)) {
                ActivityCompat.requestPermissions(
                    activity!!,
                    PERMISSIONS,
                    REQUEST_CODE
                )
                return@setOnClickListener
            }
            val intent = Intent(activity, ScannerActivity::class.java)
            startActivityForResult(intent, AppConstants.CODE_SCANNER)
        }

        binding.backButton.setOnClickListener {
            mainViewModel.back()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppConstants.CODE_SCANNER && resultCode == Activity.RESULT_OK) {
            binding.searchEdit.setText(data?.getStringExtra(AppConstants.RESULT_CODE_SCANNER))
            binding.searchMotion.transitionToState(binding.screenTitle.id)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AddProductsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AddProductsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewProductClick(id: Int) {
    }

    override fun onAddProductClick(id: Int) {
        productsViewModel.addProductToSale(id)
    }

    override fun onRemoveProductClick(id: Int) {
        productsViewModel.removeProductFromSale(id)
    }

    override fun onChangeProductsUnitsClick(id: Int, units: Int) {
        productsViewModel.changeUnitsOfSelectedProducts(id, units)
    }

}