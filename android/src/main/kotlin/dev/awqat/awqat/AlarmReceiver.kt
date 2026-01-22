package dev.awqat.awqat

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat

/**
 * BroadcastReceiver that handles prayer reminder alarms.
 * 
 * This receiver is triggered by AlarmManager and shows rich notifications
 * with images and full text, without needing the Flutter engine to be running.
 */
class AlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)
        
        android.util.Log.d("AlarmReceiver", "Alarm received! ID: $notificationId")
        
        val prayerName = intent.getStringExtra("prayer_name") ?: "Prayer"
        val title = intent.getStringExtra("title") ?: "Time for $prayerName"
        val body = intent.getStringExtra("body") ?: "It's time for $prayerName prayer"
        val imageResource = intent.getStringExtra("image_resource")
        val shouldReschedule = intent.getBooleanExtra("should_reschedule", false)
        
        // Show the notification
        showNotification(context, notificationId, title, body, prayerName, imageResource)
        
        // Reschedule reminders for the next 7 days to ensure continuous notifications
        if (shouldReschedule) {
            try {
                PrayerScheduler.scheduleFromSavedConfig(context)
            } catch (e: Exception) {
                android.util.Log.e("AlarmReceiver", "Failed to reschedule: ${e.message}")
            }
        }
    }
    
    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        prayerName: String,
        imageResource: String?
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Get app icon dynamically
        val appIcon = context.applicationInfo.icon
        
        // Try to load prayer-specific image
        val prayerImage = loadPrayerImage(context, prayerName, imageResource)
        
        val builder = NotificationCompat.Builder(context, AwqatPlugin.CHANNEL_ID)
            .setSmallIcon(appIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        
        // Use BigPictureStyle with image, falling back to BigTextStyle
        if (prayerImage != null) {
            // Rich notification with image AND full text
            builder.setLargeIcon(prayerImage)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(prayerImage)
                        .bigLargeIcon(null as Bitmap?)  // Hide large icon when expanded
                        .setSummaryText(body)  // Full text shown below image
                )
        } else {
            // Fallback to text-only expandable notification
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
                    .setBigContentTitle(title)
            )
        }
        
        notificationManager.notify(notificationId, builder.build())
    }
    
    /**
     * Load prayer-specific image from app's drawable resources.
     * 
     * Looks for resources in order:
     * 1. Custom imageResource if provided
     * 2. Prayer-specific: notification_fajr, notification_dhuhr, etc.
     * 3. Generic: notification_prayer
     * 
     * Returns null if no image found (will fallback to BigTextStyle)
     */
    private fun loadPrayerImage(context: Context, prayerName: String, imageResource: String?): Bitmap? {
        val resources = context.resources
        val packageName = context.packageName
        
        // Priority list of resource names to try
        val resourceNames = listOfNotNull(
            imageResource,
            "notification_${prayerName.lowercase()}",
            "notification_prayer",
            "ic_notification_prayer"
        )
        
        for (name in resourceNames) {
            try {
                val resId = resources.getIdentifier(name, "drawable", packageName)
                if (resId != 0) {
                    return BitmapFactory.decodeResource(resources, resId)
                }
            } catch (e: Exception) {
                // Continue to next resource
            }
        }
        
        return null
    }
}
