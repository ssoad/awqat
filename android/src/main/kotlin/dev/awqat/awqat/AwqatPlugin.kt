package dev.awqat.awqat

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.util.*

/** AwqatPlugin - Native prayer time reminders for Flutter */
class AwqatPlugin : FlutterPlugin, MethodCallHandler {
    
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    
    // Stored configuration
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var calculationMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE
    private var madhab: Madhab = Madhab.SHAFI
    
    companion object {
        const val CHANNEL_ID = "awqat_prayer_reminders"
        const val CHANNEL_NAME = "Prayer Reminders"
        
        // Notification IDs for each prayer
        const val NOTIFICATION_ID_FAJR = 1001
        const val NOTIFICATION_ID_DHUHR = 1002
        const val NOTIFICATION_ID_ASR = 1003
        const val NOTIFICATION_ID_MAGHRIB = 1004
        const val NOTIFICATION_ID_ISHA = 1005
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "dev.awqat/awqat")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
        createNotificationChannel()
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${Build.VERSION.RELEASE}")
            }
            "initialize" -> {
                handleInitialize(call, result)
            }
            "updateConfig" -> {
                handleInitialize(call, result) // Same logic
            }
            "getPrayerTimes" -> {
                handleGetPrayerTimes(call, result)
            }
            "scheduleReminders" -> {
                handleScheduleReminders(call, result)
            }
            "cancelAllReminders" -> {
                handleCancelAllReminders(result)
            }
            "cancelReminder" -> {
                handleCancelReminder(call, result)
            }
            "requestPermission" -> {
                handleRequestPermission(result)
            }
            "hasPermission" -> {
                handleHasPermission(result)
            }
            else -> {
                result.notImplemented()
            }
        }
    }
    
    private fun handleInitialize(call: MethodCall, result: Result) {
        try {
            latitude = call.argument<Double>("latitude") ?: 0.0
            longitude = call.argument<Double>("longitude") ?: 0.0
            
            val methodStr = call.argument<String>("method") ?: "muslim_world_league"
            calculationMethod = parseCalculationMethod(methodStr)
            
            val madhabStr = call.argument<String>("madhab") ?: "shafi"
            madhab = if (madhabStr == "hanafi") Madhab.HANAFI else Madhab.SHAFI
            
            result.success(true)
        } catch (e: Exception) {
            result.error("INIT_ERROR", e.message, null)
        }
    }
    
    private fun handleGetPrayerTimes(call: MethodCall, result: Result) {
        try {
            val dateMillis = call.argument<Long>("date") ?: System.currentTimeMillis()
            val calendar = Calendar.getInstance().apply {
                timeInMillis = dateMillis
            }
            
            val coordinates = Coordinates(latitude, longitude)
            val dateComponents = DateComponents(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            
            val params = calculationMethod.parameters.copy(madhab = madhab)
            val prayerTimes = PrayerTimes(coordinates, dateComponents, params)
            
            val resultMap = mapOf(
                "fajr" to prayerTimes.fajr.time,
                "sunrise" to prayerTimes.sunrise.time,
                "dhuhr" to prayerTimes.dhuhr.time,
                "asr" to prayerTimes.asr.time,
                "maghrib" to prayerTimes.maghrib.time,
                "isha" to prayerTimes.isha.time,
                "date" to dateMillis
            )
            
            result.success(resultMap)
        } catch (e: Exception) {
            result.error("PRAYER_TIMES_ERROR", e.message, null)
        }
    }
    
    private fun handleScheduleReminders(call: MethodCall, result: Result) {
        try {
            val prayers = call.argument<List<String>>("prayers") ?: listOf()
            val offsetMinutes = call.argument<Int>("offsetMinutes") ?: 0
            val customTitle = call.argument<String>("title")
            val customBody = call.argument<String>("body")
            
            // Get today's prayer times
            val calendar = Calendar.getInstance()
            val coordinates = Coordinates(latitude, longitude)
            val dateComponents = DateComponents(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            val params = calculationMethod.parameters.copy(madhab = madhab)
            val prayerTimes = PrayerTimes(coordinates, dateComponents, params)
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            for (prayerStr in prayers) {
                val (prayerTime, notificationId, prayerName) = when (prayerStr) {
                    "fajr" -> Triple(prayerTimes.fajr, NOTIFICATION_ID_FAJR, "Fajr")
                    "dhuhr" -> Triple(prayerTimes.dhuhr, NOTIFICATION_ID_DHUHR, "Dhuhr")
                    "asr" -> Triple(prayerTimes.asr, NOTIFICATION_ID_ASR, "Asr")
                    "maghrib" -> Triple(prayerTimes.maghrib, NOTIFICATION_ID_MAGHRIB, "Maghrib")
                    "isha" -> Triple(prayerTimes.isha, NOTIFICATION_ID_ISHA, "Isha")
                    else -> continue
                }
                
                val triggerTime = prayerTime.time + (offsetMinutes * 60 * 1000L)
                
                // Skip if time has already passed
                if (triggerTime <= System.currentTimeMillis()) continue
                
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("notification_id", notificationId)
                    putExtra("prayer_name", prayerName)
                    putExtra("title", customTitle ?: "Time for $prayerName")
                    putExtra("body", customBody ?: "It's time for $prayerName prayer")
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // Use setAlarmClock for highest reliability on all Android versions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAlarmClock(
                            AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                            pendingIntent
                        )
                    } else {
                        // Fallback for devices without exact alarm permission
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                        pendingIntent
                    )
                }
            }
            
            result.success(true)
        } catch (e: Exception) {
            result.error("SCHEDULE_ERROR", e.message, null)
        }
    }
    
    private fun handleCancelAllReminders(result: Result) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            listOf(
                NOTIFICATION_ID_FAJR,
                NOTIFICATION_ID_DHUHR,
                NOTIFICATION_ID_ASR,
                NOTIFICATION_ID_MAGHRIB,
                NOTIFICATION_ID_ISHA
            ).forEach { id ->
                val intent = Intent(context, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    id,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                pendingIntent?.let { alarmManager.cancel(it) }
            }
            
            result.success(true)
        } catch (e: Exception) {
            result.error("CANCEL_ERROR", e.message, null)
        }
    }
    
    private fun handleCancelReminder(call: MethodCall, result: Result) {
        try {
            val prayerStr = call.argument<String>("prayer") ?: ""
            val notificationId = when (prayerStr) {
                "fajr" -> NOTIFICATION_ID_FAJR
                "dhuhr" -> NOTIFICATION_ID_DHUHR
                "asr" -> NOTIFICATION_ID_ASR
                "maghrib" -> NOTIFICATION_ID_MAGHRIB
                "isha" -> NOTIFICATION_ID_ISHA
                else -> {
                    result.error("INVALID_PRAYER", "Unknown prayer: $prayerStr", null)
                    return
                }
            }
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
            
            result.success(true)
        } catch (e: Exception) {
            result.error("CANCEL_ERROR", e.message, null)
        }
    }
    
    private fun handleRequestPermission(result: Result) {
        // On Android 13+, POST_NOTIFICATIONS permission is required
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Permission request must be handled by the app, not the plugin
            // Return current status
            val notificationManager = NotificationManagerCompat.from(context)
            result.success(notificationManager.areNotificationsEnabled())
        } else {
            result.success(true)
        }
    }
    
    private fun handleHasPermission(result: Result) {
        val notificationManager = NotificationManagerCompat.from(context)
        result.success(notificationManager.areNotificationsEnabled())
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Prayer time reminders"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun parseCalculationMethod(method: String): CalculationMethod {
        return when (method) {
            "muslim_world_league" -> CalculationMethod.MUSLIM_WORLD_LEAGUE
            "egyptian" -> CalculationMethod.EGYPTIAN
            "karachi" -> CalculationMethod.KARACHI
            "umm_al_qura" -> CalculationMethod.UMM_AL_QURA
            "north_america" -> CalculationMethod.NORTH_AMERICA
            "dubai" -> CalculationMethod.DUBAI
            "moonsighting_committee" -> CalculationMethod.MOON_SIGHTING_COMMITTEE
            "kuwait" -> CalculationMethod.KUWAIT
            "qatar" -> CalculationMethod.QATAR
            "singapore" -> CalculationMethod.SINGAPORE
            "turkey" -> CalculationMethod.TURKEY
            "tehran" -> CalculationMethod.TEHRAN
            else -> CalculationMethod.MUSLIM_WORLD_LEAGUE
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
