package dev.awqat.awqat

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/**
 * BroadcastReceiver that handles prayer reminder alarms.
 * 
 * This receiver is triggered by AlarmManager and shows the notification
 * without needing the Flutter engine to be running.
 */
class AlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)
        val prayerName = intent.getStringExtra("prayer_name") ?: "Prayer"
        val title = intent.getStringExtra("title") ?: "Time for $prayerName"
        val body = intent.getStringExtra("body") ?: "It's time for $prayerName prayer"
        
        showNotification(context, notificationId, title, body, prayerName)
    }
    
    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        prayerName: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Get app icon dynamically
        val appIcon = context.applicationInfo.icon
        
        val notification = NotificationCompat.Builder(context, AwqatPlugin.CHANNEL_ID)
            .setSmallIcon(appIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
}
