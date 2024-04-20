package com.hpn.hmessager.presentation.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat


fun hideSystemBars(view: View) {
    val window = (view.context as Activity).window

    WindowInsetsControllerCompat(window, view).let {
        it.hide(WindowInsetsCompat.Type.systemBars())
        it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

fun showSystemBars(view: View) {
    val window = (view.context as Activity).window

    WindowInsetsControllerCompat(
        window, view
    ).show(WindowInsetsCompat.Type.systemBars())
}

fun openWith(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW)
    val type = context.contentResolver.getType(uri)
    intent.setDataAndType(uri, type)
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    context.startActivity(intent)
}

fun persistentUri(context: Context, uris: List<Uri>) {
    for (uri in uris) {
        val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flag)
    }
}

fun persistentUri(context: Context, uri: Uri) {
    val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
    context.contentResolver.takePersistableUriPermission(uri, flag)
}

fun timeToString(time: Int): String {
    val m = time / 60
    val s = time % 60

    val mStr = if (m < 10) "0$m" else "$m"
    val sStr = if (s < 10) "0$s" else "$s"

    return "$mStr:$sStr"
}

@Composable
fun RequestPermission(context: Context, permission: String, onPermissionChange: (Boolean) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            onPermissionChange(granted)
        })

    LaunchedEffect(Unit) {
        onPermissionChange(
            ContextCompat.checkSelfPermission(
                context, permission
            ) == PackageManager.PERMISSION_GRANTED
        )
        launcher.launch(permission)
    }
}