package com.savent.erp.data.remote.model

import com.google.gson.annotations.SerializedName

data class LoginCredentials(
    val rfc: String,
    val pin: String,
    @SerializedName("store_name")
    val storeName: String,
    @SerializedName("feature_name")
    val featureName: String
)