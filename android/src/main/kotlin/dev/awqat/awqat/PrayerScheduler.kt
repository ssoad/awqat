package dev.awqat.awqat

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*
import kotlin.math.*

/**
 * Utility class for scheduling prayer reminders.
 * 
 * This class can be used by:
 * - AwqatPlugin (when scheduleReminders is called from Flutter)
 * - AlarmReceiver (to reschedule after showing a notification)
 * - BootReceiver (to reschedule after device reboot)
 * 
 * All configuration is loaded from SharedPreferences.
 */
object PrayerScheduler {
    
    /**
     * Schedule prayer reminders for the next 7 days using saved configuration.
     * 
     * @param context The application context
     * @return true if scheduling was successful, false if reminders are disabled or config missing
     */
    fun scheduleFromSavedConfig(context: Context): Boolean {
        val prefs = context.getSharedPreferences(AwqatPlugin.PREFS_NAME, Context.MODE_PRIVATE)
        
        // Check if reminders are enabled
        if (!prefs.getBoolean("reminders_enabled", false)) {
            android.util.Log.d("PrayerScheduler", "Reminders disabled, skipping schedule")
            return false
        }
        
        // Load configuration
        val latitude = prefs.getFloat("latitude", 0f).toDouble()
        val longitude = prefs.getFloat("longitude", 0f).toDouble()
        val methodId = prefs.getString("method", "muslim_world_league") ?: "muslim_world_league"
        val madhabId = prefs.getString("madhab", "shafi") ?: "shafi"
        val prayersStr = prefs.getString("prayers", "") ?: ""
        val offsetMinutes = prefs.getInt("offset_minutes", 0)
        val showImage = prefs.getBoolean("show_image", true)
        val customTitle = prefs.getString("custom_title", null)
        val customBody = prefs.getString("custom_body", null)
        val messagesStr = prefs.getString("random_messages", null)
        
        if (prayersStr.isEmpty() || latitude == 0.0 && longitude == 0.0) {
            android.util.Log.d("PrayerScheduler", "No prayers configured or invalid location")
            return false
        }
        
        val prayers = prayersStr.split(",").filter { it.isNotEmpty() }
        val messages = messagesStr?.split("|#|")?.filter { it.isNotEmpty() }
        
        scheduleForDays(
            context = context,
            latitude = latitude,
            longitude = longitude,
            methodId = methodId,
            madhabId = madhabId,
            prayers = prayers,
            offsetMinutes = offsetMinutes,
            customTitle = customTitle,
            customBody = customBody,
            showImage = showImage,
            messages = messages
        )
        
        android.util.Log.d("PrayerScheduler", "Scheduled ${prayers.size} prayers for ${AwqatPlugin.DAYS_TO_SCHEDULE} days")
        return true
    }
    
    /**
     * Schedule prayer reminders for the next [AwqatPlugin.DAYS_TO_SCHEDULE] days.
     */
    private fun scheduleForDays(
        context: Context,
        latitude: Double,
        longitude: Double,
        methodId: String,
        madhabId: String,
        prayers: List<String>,
        offsetMinutes: Int,
        customTitle: String?,
        customBody: String?,
        showImage: Boolean,
        messages: List<String>? = null
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        
        for (dayOffset in 0 until AwqatPlugin.DAYS_TO_SCHEDULE) {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val times = calculatePrayerTimes(calendar, latitude, longitude, methodId, madhabId)
            
            for (prayerStr in prayers) {
                val (timeMillis, basePrayerId, prayerName) = when (prayerStr) {
                    "fajr" -> Triple(times["fajr"] as Long, AwqatPlugin.NOTIFICATION_ID_FAJR, "Fajr")
                    "dhuhr" -> Triple(times["dhuhr"] as Long, AwqatPlugin.NOTIFICATION_ID_DHUHR, "Dhuhr")
                    "asr" -> Triple(times["asr"] as Long, AwqatPlugin.NOTIFICATION_ID_ASR, "Asr")
                    "maghrib" -> Triple(times["maghrib"] as Long, AwqatPlugin.NOTIFICATION_ID_MAGHRIB, "Maghrib")
                    "isha" -> Triple(times["isha"] as Long, AwqatPlugin.NOTIFICATION_ID_ISHA, "Isha")
                    else -> continue
                }
                
                val triggerTime = timeMillis + (offsetMinutes * 60 * 1000L)
                
                // Skip if in the past
                if (triggerTime <= now) continue
                
                val notificationId = AwqatPlugin.getNotificationId(basePrayerId, dayOffset)
                
                // Determine notification body
                var notificationBody = customBody ?: "It's time for $prayerName prayer"
                if (messages != null && messages.isNotEmpty()) {
                    notificationBody = messages.random()
                }

                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("notification_id", notificationId)
                    putExtra("prayer_name", prayerName)
                    putExtra("title", customTitle ?: "Time for $prayerName")
                    putExtra("body", notificationBody)
                    putExtra("should_reschedule", true)
                    if (showImage) {
                        putExtra("image_resource", "notification_${prayerName.lowercase()}")
                    }
                }
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context, notificationId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // Use setExactAndAllowWhileIdle when permission is granted (no status bar icon)
                // Fall back to setAlarmClock only if permission denied (shows alarm icon but works)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        // Fallback: setAlarmClock doesn't require permission but shows alarm icon
                        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerTime, pendingIntent), pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            }
        }
    }
    
    // ===== PRAYER TIME CALCULATION (copied from AwqatPlugin) =====
    
    private fun calculatePrayerTimes(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        methodId: String,
        madhabId: String
    ): Map<String, Any> {
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
}
