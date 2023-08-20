package com.hpn.hmessager.ui.utils

import android.app.Activity
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat


fun hideSystemBars(view: View) {
    val window = (view.context as Activity).window

    WindowInsetsControllerCompat(window, view).let {
        it.hide(WindowInsetsCompat.Type.systemBars())
        it.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

fun showSystemBars(view: View) {
    val window = (view.context as Activity).window

    WindowInsetsControllerCompat(
        window, view
    ).show(WindowInsetsCompat.Type.systemBars())
}