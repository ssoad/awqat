import Flutter
import UIKit
import UserNotifications

public class AwqatPlugin: NSObject, FlutterPlugin {
    
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var methodId: String = "muslim_world_league"
    private var madhabId: String = "shafi"
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "dev.awqat/awqat", binaryMessenger: registrar.messenger())
        let instance = AwqatPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "getPlatformVersion":
            result("iOS " + UIDevice.current.systemVersion)
            
        case "initialize":
            handleInitialize(call: call, result: result)
            
        case "updateConfig":
            handleInitialize(call: call, result: result)
            
        case "getPrayerTimes":
            handleGetPrayerTimes(call: call, result: result)
            
        case "scheduleReminders":
            handleScheduleReminders(call: call, result: result)
            
        case "cancelAllReminders":
            handleCancelAllReminders(result: result)
            
        case "cancelReminder":
            handleCancelReminder(call: call, result: result)
            
        case "requestPermission":
            handleRequestPermission(result: result)
            
        case "hasPermission":
            handleHasPermission(result: result)
            
        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    // MARK: - Method Handlers
    
    private func handleInitialize(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let args = call.arguments as? [String: Any] else {
            result(FlutterError(code: "INVALID_ARGS", message: "Arguments required", details: nil))
            return
        }
        
        latitude = args["latitude"] as? Double ?? 0.0
        longitude = args["longitude"] as? Double ?? 0.0
        methodId = args["method"] as? String ?? "muslim_world_league"
        madhabId = args["madhab"] as? String ?? "shafi"
        
        result(true)
    }
    
    private func handleGetPrayerTimes(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let args = call.arguments as? [String: Any],
              let dateMillis = args["date"] as? Int64 else {
            result(FlutterError(code: "INVALID_ARGS", message: "Date required", details: nil))
            return
        }
        
        let date = Date(timeIntervalSince1970: TimeInterval(dateMillis) / 1000.0)
        
        // Calculate prayer times using built-in algorithm
        let times = calculatePrayerTimes(date: date)
        
        result(times)
    }
    
    private func handleScheduleReminders(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let args = call.arguments as? [String: Any],
              let prayers = args["prayers"] as? [String] else {
            result(FlutterError(code: "INVALID_ARGS", message: "Prayers list required", details: nil))
            return
        }
        
        let offsetMinutes = args["offsetMinutes"] as? Int ?? 0
        let customTitle = args["title"] as? String
        let customBody = args["body"] as? String
        
        let date = Date()
        let times = calculatePrayerTimes(date: date)
        
        let center = UNUserNotificationCenter.current()
        
        for prayer in prayers {
            guard let timeMillis = times[prayer] as? Int64 else { continue }
            
            let prayerDate = Date(timeIntervalSince1970: TimeInterval(timeMillis) / 1000.0)
            let triggerDate = prayerDate.addingTimeInterval(TimeInterval(offsetMinutes * 60))
            
            // Skip if time has passed
            if triggerDate <= Date() { continue }
            
            let prayerName = prayer.capitalized
            let title = customTitle ?? "Time for \(prayerName)"
            let body = customBody ?? "It's time for \(prayerName) prayer"
            
            let content = UNMutableNotificationContent()
            content.title = title
            content.body = body
            content.sound = .default
            content.categoryIdentifier = "PRAYER_REMINDER"
            
            let components = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: triggerDate)
            let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
            
            let request = UNNotificationRequest(
                identifier: "awqat_\(prayer)",
                content: content,
                trigger: trigger
            )
            
            center.add(request) { error in
                if let error = error {
                    print("Awqat: Failed to schedule \(prayer): \(error)")
                }
            }
        }
        
        result(true)
    }
    
    private func handleCancelAllReminders(result: @escaping FlutterResult) {
        let center = UNUserNotificationCenter.current()
        center.removeAllPendingNotificationRequests()
        result(true)
    }
    
    private func handleCancelReminder(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let args = call.arguments as? [String: Any],
              let prayer = args["prayer"] as? String else {
            result(FlutterError(code: "INVALID_ARGS", message: "Prayer required", details: nil))
            return
        }
        
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: ["awqat_\(prayer)"])
        result(true)
    }
    
    private func handleRequestPermission(result: @escaping FlutterResult) {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            DispatchQueue.main.async {
                result(granted)
            }
        }
    }
    
    private func handleHasPermission(result: @escaping FlutterResult) {
        let center = UNUserNotificationCenter.current()
        center.getNotificationSettings { settings in
            DispatchQueue.main.async {
                result(settings.authorizationStatus == .authorized)
            }
        }
    }
    
    // MARK: - Prayer Time Calculation
    
    /// Simple prayer time calculation algorithm
    /// For production, consider using the Adhan-Swift library
    private func calculatePrayerTimes(date: Date) -> [String: Any] {
        let calendar = Calendar.current
        let components = calendar.dateComponents([.year, .month, .day], from: date)
        
        // Get calculation parameters based on method
        let (fajrAngle, ishaAngle) = getAngles(for: methodId)
        
        // Julian date calculation
        let jd = julianDate(year: components.year!, month: components.month!, day: components.day!)
        
        // Calculate prayer times
        let timezone = TimeZone.current.secondsFromGMT() / 3600.0
        
        let fajr = computePrayerTime(jd: jd, latitude: latitude, longitude: longitude, angle: fajrAngle, timezone: timezone, isFajr: true)
        let sunrise = computeSunrise(jd: jd, latitude: latitude, longitude: longitude, timezone: timezone)
        let dhuhr = computeDhuhr(jd: jd, longitude: longitude, timezone: timezone)
        let asr = computeAsr(jd: jd, latitude: latitude, longitude: longitude, timezone: timezone, isHanafi: madhabId == "hanafi")
        let maghrib = computeSunset(jd: jd, latitude: latitude, longitude: longitude, timezone: timezone)
        let isha = computePrayerTime(jd: jd, latitude: latitude, longitude: longitude, angle: ishaAngle, timezone: timezone, isFajr: false)
        
        // Convert to milliseconds since epoch
        let baseDate = calendar.date(from: components)!
        
        return [
            "fajr": timeToMillis(baseDate: baseDate, hours: fajr),
            "sunrise": timeToMillis(baseDate: baseDate, hours: sunrise),
            "dhuhr": timeToMillis(baseDate: baseDate, hours: dhuhr),
            "asr": timeToMillis(baseDate: baseDate, hours: asr),
            "maghrib": timeToMillis(baseDate: baseDate, hours: maghrib),
            "isha": timeToMillis(baseDate: baseDate, hours: isha),
            "date": Int64(date.timeIntervalSince1970 * 1000)
        ]
    }
    
    private func getAngles(for method: String) -> (Double, Double) {
        switch method {
        case "muslim_world_league": return (18.0, 17.0)
        case "egyptian": return (19.5, 17.5)
        case "karachi": return (18.0, 18.0)
        case "umm_al_qura": return (18.5, 90.0) // 90 min after maghrib
        case "north_america": return (15.0, 15.0)
        case "dubai": return (18.2, 18.2)
        case "kuwait": return (18.0, 17.5)
        case "qatar": return (18.0, 90.0)
        case "singapore": return (20.0, 18.0)
        case "turkey": return (18.0, 17.0)
        case "tehran": return (17.7, 14.0)
        default: return (18.0, 17.0)
        }
    }
    
    // MARK: - Astronomical Calculations
    
    private func julianDate(year: Int, month: Int, day: Int) -> Double {
        var y = year
        var m = month
        if m <= 2 {
            y -= 1
            m += 12
        }
        let a = floor(Double(y) / 100.0)
        let b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (Double(y) + 4716)) + floor(30.6001 * (Double(m) + 1)) + Double(day) + b - 1524.5
    }
    
    private func computePrayerTime(jd: Double, latitude: Double, longitude: Double, angle: Double, timezone: Double, isFajr: Bool) -> Double {
        let d = jd - 2451545.0
        let g = (357.529 + 0.98560028 * d).truncatingRemainder(dividingBy: 360.0)
        let q = (280.459 + 0.98564736 * d).truncatingRemainder(dividingBy: 360.0)
        let l = (q + 1.915 * sin(g * .pi / 180) + 0.020 * sin(2 * g * .pi / 180)).truncatingRemainder(dividingBy: 360.0)
        let e = 23.439 - 0.00000036 * d
        let ra = atan2(cos(e * .pi / 180) * sin(l * .pi / 180), cos(l * .pi / 180)) * 180 / .pi
        let decl = asin(sin(e * .pi / 180) * sin(l * .pi / 180)) * 180 / .pi
        let eqt = q / 15 - ra / 15
        
        let hourAngle = acos((-sin(angle * .pi / 180) - sin(latitude * .pi / 180) * sin(decl * .pi / 180)) / (cos(latitude * .pi / 180) * cos(decl * .pi / 180))) * 180 / .pi / 15
        
        let noon = 12 + timezone - longitude / 15 - eqt
        
        if isFajr {
            return noon - hourAngle
        } else {
            return noon + hourAngle
        }
    }
    
    private func computeSunrise(jd: Double, latitude: Double, longitude: Double, timezone: Double) -> Double {
        return computePrayerTime(jd: jd, latitude: latitude, longitude: longitude, angle: 0.833, timezone: timezone, isFajr: true)
    }
    
    private func computeSunset(jd: Double, latitude: Double, longitude: Double, timezone: Double) -> Double {
        return computePrayerTime(jd: jd, latitude: latitude, longitude: longitude, angle: 0.833, timezone: timezone, isFajr: false)
    }
    
    private func computeDhuhr(jd: Double, longitude: Double, timezone: Double) -> Double {
        let d = jd - 2451545.0
        let g = (357.529 + 0.98560028 * d).truncatingRemainder(dividingBy: 360.0)
        let q = (280.459 + 0.98564736 * d).truncatingRemainder(dividingBy: 360.0)
        let l = (q + 1.915 * sin(g * .pi / 180) + 0.020 * sin(2 * g * .pi / 180)).truncatingRemainder(dividingBy: 360.0)
        let e = 23.439 - 0.00000036 * d
        let ra = atan2(cos(e * .pi / 180) * sin(l * .pi / 180), cos(l * .pi / 180)) * 180 / .pi
        let eqt = q / 15 - ra / 15
        
        return 12 + timezone - longitude / 15 - eqt
    }
    
    private func computeAsr(jd: Double, latitude: Double, longitude: Double, timezone: Double, isHanafi: Bool) -> Double {
        let d = jd - 2451545.0
        let g = (357.529 + 0.98560028 * d).truncatingRemainder(dividingBy: 360.0)
        let q = (280.459 + 0.98564736 * d).truncatingRemainder(dividingBy: 360.0)
        let l = (q + 1.915 * sin(g * .pi / 180) + 0.020 * sin(2 * g * .pi / 180)).truncatingRemainder(dividingBy: 360.0)
        let e = 23.439 - 0.00000036 * d
        let ra = atan2(cos(e * .pi / 180) * sin(l * .pi / 180), cos(l * .pi / 180)) * 180 / .pi
        let decl = asin(sin(e * .pi / 180) * sin(l * .pi / 180)) * 180 / .pi
        let eqt = q / 15 - ra / 15
        
        let factor = isHanafi ? 2.0 : 1.0
        let angle = atan(1 / (factor + tan(abs(latitude - decl) * .pi / 180))) * 180 / .pi
        let hourAngle = acos((sin(angle * .pi / 180) - sin(latitude * .pi / 180) * sin(decl * .pi / 180)) / (cos(latitude * .pi / 180) * cos(decl * .pi / 180))) * 180 / .pi / 15
        
        let noon = 12 + timezone - longitude / 15 - eqt
        return noon + hourAngle
    }
    
    private func timeToMillis(baseDate: Date, hours: Double) -> Int64 {
        let seconds = hours * 3600
        let prayerDate = baseDate.addingTimeInterval(seconds)
        return Int64(prayerDate.timeIntervalSince1970 * 1000)
    }
}
