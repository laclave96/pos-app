package com.savent.erp.presentation.ui.model

data class ProductItem(
    val localId: Int,
    val name: String,
    val image: String?,
    val description: String?,
    val price: Float,
    val selectedUnits: Int,
    val remainingUnits: Int,
)
