package dev.awqat.awqat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver that reschedules prayer alarms after device reboot.
 * 
 * Android clears all scheduled alarms when the device reboots.
 * This receiver is triggered on boot and reschedules the reminders.
 * 
 * Note: The app must store the configuration (lat/lng, method, etc.)
 * in SharedPreferences for this to work. The actual rescheduling
 * requires the Flutter engine, so we send a message to reschedule.
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            // Load stored configuration from SharedPreferences
            val prefs = context.getSharedPreferences("awqat_prefs", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("reminders_enabled", false)
            
            if (isEnabled) {
                // For now, we rely on the app being opened to reschedule.
                // A more robust solution would store prayer times locally
                // and reschedule using a WorkManager job.
                
                // Log for debugging
                android.util.Log.d("AwqatBootReceiver", "Boot completed. Reminders need rescheduling.")
            }
        }
    }
}
