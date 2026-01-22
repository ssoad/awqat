## 0.1.7
- Fixed notification images: converted to proper PNG format (was JPEG with .png extension causing AAPT build failures).

## 0.1.6
- Added `showImage` parameter to `scheduleReminders`, `showTestNotification`, and `scheduleTestReminder` to toggle rich notifications.
- Added default notification images (Fajr, Dhuhr, Asr, Maghrib, Isha) to plugin resources.

## 0.1.5
- Added rich notification support for Android (big picture style).
- Added `imageResource` parameter to `AlarmReceiver`.
- Notifications now display full text using `setSummaryText`.

## 0.1.4
- Added consumer ProGuard rules to `android/build.gradle`.
- Fixed `BroadcastReceiver` stripping issue in Release mode.

## 0.1.3
- Added `Awqat.cancelReminder(PrayerType)` method.
- Improved error handling for scheduling.

## 0.1.2
- Fixed Android 12+ scheduling permission.
- Updates documentation.

## 0.1.1
- iOS support for `UNNotificationCenter`.
- Improved documentation.

## 0.1.0
- Initial release.
