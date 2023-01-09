package com.savent.erp.presentation.ui.model

data class ClientWithDebtItem (
    val localId: Int,
    val name: String,
    val image: String?,
    val address: String,
    val debt: Float,
)