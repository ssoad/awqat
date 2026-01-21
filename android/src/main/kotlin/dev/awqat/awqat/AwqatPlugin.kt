package dev.awqat.awqat

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.util.*
import kotlin.math.*

/** AwqatPlugin - Native prayer time reminders for Flutter */
class AwqatPlugin : FlutterPlugin, MethodCallHandler {
    
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    
    // Stored configuration
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var methodId: String = "muslim_world_league"
    private var madhabId: String = "shafi"
    
    companion object {
        const val CHANNEL_ID = "awqat_prayer_reminders"
        const val CHANNEL_NAME = "Prayer Reminders"
        
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
            "getPlatformVersion" -> result.success("Android ${Build.VERSION.RELEASE}")
            "initialize" -> handleInitialize(call, result)
            "updateConfig" -> handleInitialize(call, result)
            "getPrayerTimes" -> handleGetPrayerTimes(call, result)
            "scheduleReminders" -> handleScheduleReminders(call, result)
            "cancelAllReminders" -> handleCancelAllReminders(result)
            "cancelReminder" -> handleCancelReminder(call, result)
            "requestPermission" -> handleRequestPermission(result)
            "hasPermission" -> handleHasPermission(result)
            else -> result.notImplemented()
        }
    }
    
    private fun handleInitialize(call: MethodCall, result: Result) {
        try {
            latitude = call.argument<Double>("latitude") ?: 0.0
            longitude = call.argument<Double>("longitude") ?: 0.0
            methodId = call.argument<String>("method") ?: "muslim_world_league"
            madhabId = call.argument<String>("madhab") ?: "shafi"
            result.success(true)
        } catch (e: Exception) {
            result.error("INIT_ERROR", e.message, null)
        }
    }
    
    private fun handleGetPrayerTimes(call: MethodCall, result: Result) {
        try {
            val dateMillis = call.argument<Long>("date") ?: System.currentTimeMillis()
            val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val times = calculatePrayerTimes(calendar)
            result.success(times)
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
            
            val calendar = Calendar.getInstance()
            val times = calculatePrayerTimes(calendar)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            for (prayerStr in prayers) {
                val (timeMillis, notificationId, prayerName) = when (prayerStr) {
                    "fajr" -> Triple(times["fajr"] as Long, NOTIFICATION_ID_FAJR, "Fajr")
                    "dhuhr" -> Triple(times["dhuhr"] as Long, NOTIFICATION_ID_DHUHR, "Dhuhr")
                    "asr" -> Triple(times["asr"] as Long, NOTIFICATION_ID_ASR, "Asr")
                    "maghrib" -> Triple(times["maghrib"] as Long, NOTIFICATION_ID_MAGHRIB, "Maghrib")
                    "isha" -> Triple(times["isha"] as Long, NOTIFICATION_ID_ISHA, "Isha")
                    else -> continue
                }
                
                val triggerTime = timeMillis + (offsetMinutes * 60 * 1000L)
                if (triggerTime <= System.currentTimeMillis()) continue
                
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("notification_id", notificationId)
                    putExtra("prayer_name", prayerName)
                    putExtra("title", customTitle ?: "Time for $prayerName")
                    putExtra("body", customBody ?: "It's time for $prayerName prayer")
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context, notificationId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTime, pendingIntent), pendingIntent)
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } else {
                    alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTime, pendingIntent), pendingIntent)
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
            listOf(NOTIFICATION_ID_FAJR, NOTIFICATION_ID_DHUHR, NOTIFICATION_ID_ASR, NOTIFICATION_ID_MAGHRIB, NOTIFICATION_ID_ISHA).forEach { id ->
                val intent = Intent(context, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
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
                else -> { result.error("INVALID_PRAYER", "Unknown prayer", null); return }
            }
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            pendingIntent?.let { alarmManager.cancel(it) }
            result.success(true)
        } catch (e: Exception) {
            result.error("CANCEL_ERROR", e.message, null)
        }
    }
    
    private fun handleRequestPermission(result: Result) {
        val notificationManager = NotificationManagerCompat.from(context)
        result.success(notificationManager.areNotificationsEnabled())
    }
    
    private fun handleHasPermission(result: Result) {
        val notificationManager = NotificationManagerCompat.from(context)
        result.success(notificationManager.areNotificationsEnabled())
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Prayer time reminders"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    // ===== BUILT-IN PRAYER TIME CALCULATION =====
    
    private fun calculatePrayerTimes(calendar: Calendar): Map<String, Any> {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val timezone = TimeZone.getDefault().rawOffset / 3600000.0
        
        val (fajrAngle, ishaAngle) = getAngles(methodId)
        val jd = julianDate(year, month, day)
        
        val fajr = computePrayerTime(jd, latitude, longitude, fajrAngle, timezone, true)
        val sunrise = computeSunrise(jd, latitude, longitude, timezone)
        val dhuhr = computeDhuhr(jd, longitude, timezone)
        val asr = computeAsr(jd, latitude, longitude, timezone, madhabId == "hanafi")
        val maghrib = computeSunset(jd, latitude, longitude, timezone)
        val isha = computePrayerTime(jd, latitude, longitude, ishaAngle, timezone, false)
        
        val baseDate = Calendar.getInstance().apply { set(year, month - 1, day, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
        
        return mapOf(
            "fajr" to timeToMillis(baseDate, fajr),
            "sunrise" to timeToMillis(baseDate, sunrise),
            "dhuhr" to timeToMillis(baseDate, dhuhr),
            "asr" to timeToMillis(baseDate, asr),
            "maghrib" to timeToMillis(baseDate, maghrib),
            "isha" to timeToMillis(baseDate, isha),
            "date" to calendar.timeInMillis
        )
    }
    
    private fun getAngles(method: String): Pair<Double, Double> = when (method) {
        "muslim_world_league" -> 18.0 to 17.0
        "egyptian" -> 19.5 to 17.5
        "karachi" -> 18.0 to 18.0
        "umm_al_qura" -> 18.5 to 90.0
        "north_america" -> 15.0 to 15.0
        "dubai" -> 18.2 to 18.2
        "kuwait" -> 18.0 to 17.5
        "qatar" -> 18.0 to 90.0
        "singapore" -> 20.0 to 18.0
        "turkey" -> 18.0 to 17.0
        "tehran" -> 17.7 to 14.0
        else -> 18.0 to 17.0
    }
    
    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year; var m = month
        if (m <= 2) { y--; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }
    
    private fun computePrayerTime(jd: Double, lat: Double, lng: Double, angle: Double, tz: Double, isFajr: Boolean): Double {
        val d = jd - 2451545.0
        val g = (357.529 + 0.98560028 * d) % 360
        val q = (280.459 + 0.98564736 * d) % 360
        val l = (q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g))) % 360
        val e = 23.439 - 0.00000036 * d
        val ra = Math.toDegrees(atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(l)), cos(Math.toRadians(l))))
        val decl = Math.toDegrees(asin(sin(Math.toRadians(e)) * sin(Math.toRadians(l))))
        val eqt = q / 15 - ra / 15
        val hourAngle = Math.toDegrees(acos((-sin(Math.toRadians(angle)) - sin(Math.toRadians(lat)) * sin(Math.toRadians(decl))) / (cos(Math.toRadians(lat)) * cos(Math.toRadians(decl))))) / 15
        val noon = 12 + tz - lng / 15 - eqt
        return if (isFajr) noon - hourAngle else noon + hourAngle
    }
    
    private fun computeSunrise(jd: Double, lat: Double, lng: Double, tz: Double) = computePrayerTime(jd, lat, lng, 0.833, tz, true)
    private fun computeSunset(jd: Double, lat: Double, lng: Double, tz: Double) = computePrayerTime(jd, lat, lng, 0.833, tz, false)
    
    private fun computeDhuhr(jd: Double, lng: Double, tz: Double): Double {
        val d = jd - 2451545.0
        val g = (357.529 + 0.98560028 * d) % 360
        val q = (280.459 + 0.98564736 * d) % 360
        val l = (q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g))) % 360
        val e = 23.439 - 0.00000036 * d
        val ra = Math.toDegrees(atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(l)), cos(Math.toRadians(l))))
        val eqt = q / 15 - ra / 15
        return 12 + tz - lng / 15 - eqt
    }
    
    private fun computeAsr(jd: Double, lat: Double, lng: Double, tz: Double, isHanafi: Boolean): Double {
        val d = jd - 2451545.0
        val g = (357.529 + 0.98560028 * d) % 360
        val q = (280.459 + 0.98564736 * d) % 360
        val l = (q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g))) % 360
        val e = 23.439 - 0.00000036 * d
        val ra = Math.toDegrees(atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(l)), cos(Math.toRadians(l))))
        val decl = Math.toDegrees(asin(sin(Math.toRadians(e)) * sin(Math.toRadians(l))))
        val eqt = q / 15 - ra / 15
        val factor = if (isHanafi) 2.0 else 1.0
        val angle = Math.toDegrees(atan(1 / (factor + tan(Math.toRadians(abs(lat - decl))))))
        val hourAngle = Math.toDegrees(acos((sin(Math.toRadians(angle)) - sin(Math.toRadians(lat)) * sin(Math.toRadians(decl))) / (cos(Math.toRadians(lat)) * cos(Math.toRadians(decl))))) / 15
        val noon = 12 + tz - lng / 15 - eqt
        return noon + hourAngle
    }
    
    private fun timeToMillis(baseDate: Calendar, hours: Double): Long {
        val clone = baseDate.clone() as Calendar
        clone.add(Calendar.SECOND, (hours * 3600).toInt())
        return clone.timeInMillis
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
