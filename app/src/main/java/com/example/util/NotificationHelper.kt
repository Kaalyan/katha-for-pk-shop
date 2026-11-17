package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

object NotificationHelper {
  private const val CHANNEL_ID = "kirana_khata_owner_alerts"
  private const val CHANNEL_NAME = "Shop Owner Alerts"

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      val existing = manager?.getNotificationChannel(CHANNEL_ID)
      if (existing == null) {
        val channel =
          NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Real-time notifications when co-owners update customer credit or payments."
          }
        manager?.createNotificationChannel(channel)
      }
    }
  }

  fun showOwnerAlert(context: Context, idSeed: String, title: String, body: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }
    ensureChannel(context)
    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    runCatching {
      NotificationManagerCompat.from(context).notify(idSeed.hashCode(), notification)
    }
  }
}
