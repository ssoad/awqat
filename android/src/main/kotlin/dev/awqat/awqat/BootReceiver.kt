package dev.awqat.awqat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver that reschedules prayer alarms after device reboot.
 * 
 * Android clears all scheduled alarms when the device reboots.
 * This receiver is triggered on boot and reschedules the reminders
 * using saved configuration from SharedPreferences.
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            android.util.Log.d("AwqatBootReceiver", "Boot completed, checking if reminders need rescheduling")
            
            // Load stored configuration and reschedule using the PrayerScheduler
            val prefs = context.getSharedPreferences(AwqatPlugin.PREFS_NAME, Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("reminders_enabled", false)
            
            if (isEnabled) {
                try {
                    val success = PrayerScheduler.scheduleFromSavedConfig(context)
                    if (success) {
                        android.util.Log.d("AwqatBootReceiver", "Successfully rescheduled prayer reminders after boot")
                    } else {
                        android.util.Log.w("AwqatBootReceiver", "Failed to reschedule - config may be missing")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AwqatBootReceiver", "Error rescheduling after boot: ${e.message}")
                }
            } else {
                android.util.Log.d("AwqatBootReceiver", "Reminders disabled, skipping reschedule")
            }
        }
    }
}
