## 0.1.3

* Feature: Added `showTestNotification()` for immediate notification testing
* Feature: Added `scheduleTestReminder()` for testing delayed notifications (approx 1 min)
* Fixed: Android `AlarmReceiver` export status for better reliability

## 0.1.2

* Fixed: Removed external Adhan-Kotlin dependency, now uses built-in prayer time calculation algorithm
* Fixed: No longer requires JitPack or additional Maven repositories

## 0.1.1

* Fixed: Added JitPack and Sonatype repositories for Adhan-Kotlin dependency resolution on Android

## 0.1.0

* Initial release
* Native Android prayer time reminders using AlarmManager
* Native iOS prayer time reminders using UNNotificationCenter
* Built-in prayer time calculation with 12 methods
* Support for Shafi and Hanafi madhab
* Permission handling for both platforms
