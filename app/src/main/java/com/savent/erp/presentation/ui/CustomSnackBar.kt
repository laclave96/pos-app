package com.savent.erp.presentation.ui

import android.view.View
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.savent.erp.R

class CustomSnackBar{

    companion object{
        const val LENGTH_LONG = Snackbar.LENGTH_LONG
        fun make(view: View, text: CharSequence, duration: Int): Snackbar{
            val snackbar = Snackbar.make(view, text, duration)
            val tv = snackbar.view.findViewById(R.id.snackbar_text) as TextView
            tv.textSize = 21F
            return snackbar
        }
    }
}