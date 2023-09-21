package com.hpn.hmessager.ui.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hpn.hmessager.ui.activity.ConvActivity

class Notification {

    private var notifChannelSetup = false

    private fun createNotificationChannel(context: Context) {
        if (notifChannelSetup) return

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "HMessager"
            val descriptionText = "HMessager"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        notifChannelSetup = true
    }

    fun sendNotification(context: Context, title: String, content: String, convId: Int) {
        createNotificationChannel(context)

        val intent = Intent(context, ConvActivity::class.java).apply {
            putExtra("convId", convId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
           // .setSmallIcon(R.drawable.hmessager_notification).setContentTitle(title)
            .setContentText(content).setContentIntent(pendingIntent).setAutoCancel(true)


        if (ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) NotificationManagerCompat.from(context).notify(1, builder.build())
    }

    companion object {
        private const val CHANNEL_ID = "com.hpn.hmessager"
    }
}