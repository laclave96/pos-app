package com.savent.erp.presentation.ui.fragment

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.data.printable.Printable
import com.mazenrashed.printooth.ui.ScanningActivity
import com.mazenrashed.printooth.utilities.Printing
import com.mazenrashed.printooth.utilities.PrintingCallback
import com.savent.erp.AppConstants
import com.savent.erp.R
import com.savent.erp.databinding.FragmentCheckoutBinding
import com.savent.erp.presentation.ui.CustomSnackBar
import com.savent.erp.presentation.ui.SendReceiptDialog
import com.savent.erp.presentation.ui.adapters.ProductsAdapter
import com.savent.erp.presentation.viewmodel.CheckoutViewModel
import com.savent.erp.presentation.viewmodel.MainViewModel
import com.savent.erp.presentation.viewmodel.ProductsViewModel
import com.savent.erp.utils.CheckPermissions
import com.savent.erp.utils.DecimalFormat
import com.savent.erp.utils.PaymentMethod
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CheckoutFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CheckoutFragment : Fragment(), ProductsAdapter.OnClickListener,
    SendReceiptDialog.OnClickListener {

    private lateinit var binding: FragmentCheckoutBinding
    private val mainViewModel: MainViewModel by sharedViewModel()
    private val productsViewModel: ProductsViewModel by sharedViewModel()
    private val checkoutViewModel: CheckoutViewModel by viewModel()
    private lateinit var productsAdapter: ProductsAdapter
    private var defaultJob: Job? = null
    private var sendDialog: SendReceiptDialog? = null
    private var sharedReceiptMethod: SharedReceiptMethod? = null
    private var isSentReceipt: Boolean = false
    private var printing: Printing? = null
    private var receiptToPrint: ArrayList<Printable>? = null

    enum class SharedReceiptMethod {
        Whatsapp, Sms, Email, BluetoothPrinter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subscribeToObservables()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_checkout,
            container, false
        )
        binding.lifecycleOwner = this
        binding.viewModel = checkoutViewModel

        initEvents()
        initDefaultStates()
        setupRecyclerView()

        return binding.root
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductsAdapter(context)
        productsAdapter.setOnClickListener(this)
        val recyclerView = binding.productsRecycler
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.itemAnimator = DefaultItemAnimator()
        productsAdapter.setHasStableIds(true)
        recyclerView.adapter = productsAdapter
    }

    private fun subscribeToObservables() {
        checkoutViewModel.checkout.observe(this) {

            binding.subtotal.text = getString(R.string.price)
                .format(DecimalFormat.format(it.subtotal))

            binding.tax.text = getString(R.string.price)
                .format(DecimalFormat.format(it.taxes))

            binding.discounts.text = getString(R.string.price)
                .format(DecimalFormat.format(it.totalDiscounts))

            binding.total.text = getString(R.string.price)
                .format(DecimalFormat.format(it.finalPrice))

            binding.toPay.text = getString(R.string.price)
                .format(DecimalFormat.format(it.finalPrice))

            if (it.finalPrice < it.collected) {
                binding.change.text = getString(R.string.price)
                    .format(DecimalFormat.format(it.collected - it.finalPrice))
            } else
                binding.change.text = getString(R.string.price)
                    .format("0.00")

        }

        checkoutViewModel.products.observe(this) {
            productsAdapter.setData(it)
        }

        checkoutViewModel.saveSaleSuccess.observe(this) {
            if (it) mainViewModel.goOn()
        }

        checkoutViewModel.sharedReceipt.observe(this) {
            if (it == null) return@observe

            when (sharedReceiptMethod) {
                SharedReceiptMethod.Whatsapp -> {
                    sendReceiptByWhatsApp(it.contact.phoneNumber, it.note)
                }
                SharedReceiptMethod.Sms -> {
                    sendReceiptBySms(it.contact.phoneNumber, it.note)
                }
                SharedReceiptMethod.Email -> {
                    sendReceiptByEmail(
                        it.contact.email,
                        getString(R.string.receipt_email_subject),
                        it.note
                    )
                }
                else -> {
                }
            }
        }

        checkoutViewModel.printable.observe(this) {
            receiptToPrint = it
            printReceiptByBluetooth()
        }

        lifecycleScope.launchWhenStarted {
            checkoutViewModel.uiEvent.collectLatest { uiEvent ->
                when (uiEvent) {
                    is CheckoutViewModel.UiEvent.ShowMessage -> {
                        CustomSnackBar.make(
                            binding.root,
                            getString(uiEvent.resId ?: R.string.unknown_error),
                            CustomSnackBar.LENGTH_LONG
                        ).show()
                    }
                }
            }
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

    private fun initDefaultStates() {
        val arrayAdapter = context?.let {
            ArrayAdapter<String>(
                it,
                R.layout.payment_method_item,
                resources.getStringArray(R.array.payment_method)
            )
        }
        binding.paymentMethod.adapter = arrayAdapter
    }

    private fun initEvents() {
        binding.goOn.setOnClickListener {
            if (!isSentReceipt) {
                showDialog()
                return@setOnClickListener
            }
            defaultJob?.cancel()
            defaultJob = lifecycleScope.launch {
                if (productsViewModel.isAtLeastOneProductSelected())
                    checkoutViewModel.recordSale()

            }
        }

        binding.openList.setOnClickListener {
            if (binding.productsRecycler.visibility == View.GONE) {
                binding.openList.text = getString(R.string.hide_list)
                binding.productsRecycler.visibility = View.VISIBLE
            } else {
                binding.openList.text = getString(R.string.see_list)
                binding.productsRecycler.visibility = View.GONE
            }
        }

        binding.sendReceipt.setOnClickListener {
            showDialog()
        }

        binding.addButton.setOnClickListener {
            checkoutViewModel.increaseExtraDiscountPercent()
        }

        binding.removeButton.setOnClickListener {
            checkoutViewModel.decreaseExtraDiscountPercent()
        }

        binding.discountPercent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    binding.discountPercent.setText("0")
                    binding.discountPercent.setSelection(binding.discountPercent.text.length)
                    return
                }

                checkoutViewModel.updateExtraDiscountPayment(s.toString().toInt())
            }

        })

        binding.paymentMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                var method = PaymentMethod.Cash
                when (position) {
                    0 -> {
                        binding.paymentIcon.setImageResource(R.drawable.ic_round_payments_24)
                    }
                    1 -> {
                        method = PaymentMethod.Credit
                        binding.paymentIcon.setImageResource(R.drawable.ic_round_credit_card_24)
                    }
                    2 -> {
                        method = PaymentMethod.Debit
                        binding.paymentIcon.setImageResource(R.drawable.ic_round_money_off_24)
                    }
                }
                checkoutViewModel.updatePaymentMethod(method)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }

        binding.collected.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty() || (s.length == 1 && s[0] == '.')) {
                    binding.collected.setText("0")
                    binding.collected.setSelection(binding.collected.text.length)
                    return
                }
                val strCollected = s.toString()
                if (strCollected[0] == '0' && strCollected.length == 2
                    && strCollected[1].isDigit()
                ) {
                    binding.collected.setText(strCollected.substring(1))
                    binding.collected.setSelection(binding.collected.text.length)
                    return
                }

                checkoutViewModel.updateCollectedPayment(strCollected.replace(",", "").toFloat())
            }

        })

        binding.backButton.setOnClickListener {
            mainViewModel.back()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CheckoutFragment().apply {
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

    private fun sendReceiptByEmail(email: String?, subject: String, body: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:")
            intent.putExtra(Intent.EXTRA_EMAIL, Array(1) { email })
            intent.putExtra(Intent.EXTRA_SUBJECT, subject)
            intent.putExtra(Intent.EXTRA_TEXT, body)
            startActivity(Intent.createChooser(intent, getString(R.string.sending_email)))
        } catch (ex: ActivityNotFoundException) {
            CustomSnackBar.make(
                binding.root,
                getString(R.string.mail_app_not_found),
                CustomSnackBar.LENGTH_LONG
            ).show()
        }

    }

    private fun sendReceiptBySms(phoneNumber: Long?, body: String) {
        try {
            val uri = Uri.parse("smsto:+${phoneNumber}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.putExtra("sms_body", body)
            startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            CustomSnackBar.make(
                binding.root,
                getString(R.string.sms_app_not_found),
                CustomSnackBar.LENGTH_LONG
            ).show()
        }

    }

    private fun sendReceiptByWhatsApp(phoneNumber: Long?, body: String) {
        try {
            val uri =
                Uri.parse("https://api.whatsapp.com/send?phone=${phoneNumber}&text=${body}")
            val sendIntent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(sendIntent)
        } catch (e: ActivityNotFoundException) {
            CustomSnackBar.make(
                binding.root,
                getString(R.string.whatsapp_not_found),
                CustomSnackBar.LENGTH_LONG
            ).show()
        }

    }

    private fun printReceiptByBluetooth() {
        if (sharedReceiptMethod == SharedReceiptMethod.BluetoothPrinter && receiptToPrint != null) {
            printing?.print(receiptToPrint!!)
        }
    }

    override fun sendByWhatsapp() {
        sharedReceiptMethod = SharedReceiptMethod.Whatsapp
        sendDialog?.dismiss()
        checkoutViewModel.loadReceiptToSend()
        isSentReceipt = true
    }

    override fun sendBySms() {
        sharedReceiptMethod = SharedReceiptMethod.Sms
        sendDialog?.dismiss()
        checkoutViewModel.loadReceiptToSend()
        isSentReceipt = true
    }

    override fun sendByEmail() {
        sharedReceiptMethod = SharedReceiptMethod.Email
        sendDialog?.dismiss()
        checkoutViewModel.loadReceiptToSend()
        isSentReceipt = true
    }

    override fun sendPrintable() {
        if(isNeededRequestAndroid12BLEPermission()) return
        sharedReceiptMethod = SharedReceiptMethod.BluetoothPrinter
        sendDialog?.dismiss()
        checkoutViewModel.loadReceiptToPrint()
        resultLauncher.launch(
            Intent(
                activity,
                ScanningActivity::class.java
            )
        )
        isSentReceipt = true
    }

    fun showDialog() {
        sendDialog = SendReceiptDialog(context!!)
        sendDialog?.setOnClickListener(this)
        sendDialog?.show()
    }

    private fun initPrinter() {
        printing = Printooth.printer()
        printing?.printingCallback = object : PrintingCallback {
            override fun connectingWithPrinter() {
                Toast.makeText(context, "Connecting with printer", Toast.LENGTH_SHORT).show()
            }

            override fun printingOrderSentSuccessfully() {
                Toast.makeText(context, "Order sent to printer", Toast.LENGTH_SHORT).show()
            }

            override fun connectionFailed(error: String) {
                Toast.makeText(context, "Failed to connect printer", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: String) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }

            override fun onMessage(message: String) {
                Toast.makeText(context, "Message: $message", Toast.LENGTH_SHORT).show()
            }

            override fun disconnected() {
                Toast.makeText(context, "Disconnected Printer", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == ScanningActivity.SCANNING_FOR_PRINTER || result.resultCode == Activity.RESULT_OK) {
                initPrinter()
                printReceiptByBluetooth()
            }
        }


    private fun isNeededRequestAndroid12BLEPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!CheckPermissions.check(context, AppConstants.ANDROID_12_BLE_PERMISSIONS)) {
                activity?.requestPermissions(
                    AppConstants.ANDROID_12_BLE_PERMISSIONS,
                    AppConstants.REQUEST_12_BLE_CODE
                )
                return true
            }

        }
        return false

    }
}