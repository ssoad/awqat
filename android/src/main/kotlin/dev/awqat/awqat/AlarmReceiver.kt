package dev.awqat.awqat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
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
        val playSound = intent.getBooleanExtra("play_sound", true)
        val soundName = intent.getStringExtra("sound") ?: AwqatPlugin.DEFAULT_SOUND_NAME
        val shouldReschedule = intent.getBooleanExtra("should_reschedule", false)
        
        // Ensure notification channel exists (critical for release builds)
        ensureNotificationChannel(context, soundName)
        
        // Show the notification
        showNotification(context, notificationId, title, body, prayerName, imageResource, playSound, soundName)
        
        // Reschedule reminders for the next 7 days to ensure continuous notifications
        if (shouldReschedule) {
            try {
                PrayerScheduler.scheduleFromSavedConfig(context)
            } catch (e: Exception) {
                android.util.Log.e("AlarmReceiver", "Failed to reschedule: ${e.message}")
            }
        }
    }
    
    /**
     * Ensure the notification channel exists.
     * This is critical because in release builds, the app may be killed and
     * the AwqatPlugin.onAttachedToEngine may never have been called.
     */
    private fun ensureNotificationChannel(context: Context, soundName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val normalizedSoundName = soundName.trim().ifEmpty { AwqatPlugin.DEFAULT_SOUND_NAME }
            val soundUri = Uri.parse("android.resource://${context.packageName}/raw/$normalizedSoundName")
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val soundChannel = NotificationChannel(
                AwqatPlugin.CHANNEL_ID,
                AwqatPlugin.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Prayer time reminders"
                enableVibration(true)
                enableLights(true)
                setSound(soundUri, audioAttributes)
            }

            val silentChannel = NotificationChannel(
                AwqatPlugin.CHANNEL_ID_SILENT,
                "${AwqatPlugin.CHANNEL_NAME} (Silent)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Prayer reminders without sound"
                enableVibration(true)
                enableLights(true)
                setSound(null, null)
            }

            notificationManager.createNotificationChannel(soundChannel)
            notificationManager.createNotificationChannel(silentChannel)
        }
    }
    
    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        prayerName: String,
        imageResource: String?,
        playSound: Boolean,
        soundName: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Get app icon dynamically
        val appIcon = context.applicationInfo.icon
        
        // Try to load prayer-specific image
        val prayerImage = loadPrayerImage(context, prayerName, imageResource)
        
        val channelId = if (playSound) AwqatPlugin.CHANNEL_ID else AwqatPlugin.CHANNEL_ID_SILENT
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(appIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (playSound) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                val normalizedSoundName = soundName.trim().ifEmpty { AwqatPlugin.DEFAULT_SOUND_NAME }
                val soundUri = Uri.parse("android.resource://${context.packageName}/raw/$normalizedSoundName")
                builder.setSound(soundUri)
            }
        } else {
            builder.setSilent(true)
            builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS)
        }
        
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
