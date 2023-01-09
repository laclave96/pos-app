package com.savent.erp.data.remote.model

import com.savent.erp.data.local.model.BusinessBasicsLocal

class Business(
    val basics: BusinessBasics,
    val clients: List<Client>,
    val products: List<Product>,
    val sales: List<Sale>
) {
}