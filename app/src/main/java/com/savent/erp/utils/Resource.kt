package com.savent.erp.utils

sealed class Resource<T>(val data: T? = null, val resId: Int? = null, val message: String? = null) {

    class Loading<T>(data: T? = null) : Resource<T>(data)
    class Success<T>(data: T? = null, resId: Int? = null) : Resource<T>(data, resId)
    class Error<T>(data: T? = null, resId: Int? = null, message: String? = null) :
        Resource<T>(data, resId, message)

}
