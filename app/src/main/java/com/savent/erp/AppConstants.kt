package com.savent.erp

import android.Manifest

object AppConstants {
    const val EMPTY_JSON_STRING = "[]"
    const val APP_PREFERENCES = "app_preferences"
    const val LOGIN_CREDENTIALS = "login_credentials_preferences"
    const val BUSINESS_PREFERENCES = "business_basics_preferences"
    const val PENDING_SALE_PREFERENCES = "pending_sale_preferences"
    const val APP_DATABASE_NAME = "app_database"
    const val SAVENT_POS_API_BASE_URL = "your_base_url"
    const val CLIENTS_API_PATH = "pos/api/clients/"
    const val PRODUCTS_API_PATH = "pos/api/products/"
    const val BUSINESS_API_PATH = "pos/api/business/"
    const val SALES_API_PATH = "pos/api/sales/"
    const val INCOMPLETE_PAYMENTS_API_PATH = "pos/api/incomplete_payments/"
    const val AUTHORIZATION = "your_auth"
    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val ANDROID_12_BLE_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    const val REQUEST_LOCATION_CODE = 8989
    const val REQUEST_12_BLE_CODE = 9090
    const val REQUEST_LOCATION_PERMISSION_CODE = 10
    const val CODE_SCANNER = 1
    const val RESULT_CODE_SCANNER = "result_code_scanner"
}