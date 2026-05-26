# Awqat أوقات

[![pub package](https://img.shields.io/pub/v/awqat.svg)](https://pub.dev/packages/awqat)
[![license](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

**Native prayer time reminders for Flutter.** Uses platform-native AlarmManager (Android) and UNNotificationCenter (iOS) for reliable, battery-efficient Islamic prayer notifications.

## ✨ Features

- 🕌 **Accurate Prayer Times** - Built-in calculation using proven astronomical algorithms
- 🔔 **Native Notifications** - No Dart isolate overhead, works even when app is closed
- 🔋 **Battery Efficient** - Uses system-level alarm APIs
- 📱 **Cross Platform** - Full support for Android and iOS
- 🌍 **12 Calculation Methods** - Muslim World League, Egyptian, Karachi, Umm Al-Qura, and more
- 🕋 **Madhab Support** - Shafi and Hanafi Asr calculation
- 🕌 **Jumuah Support** - Friday noon prayer can be labeled as Jumuah
- 🔊 **Adhan Sound Controls** - Built-in `adhan` sound with toggle and custom sound name support

## 📦 Installation

```yaml
dependencies:
  awqat: ^0.1.13
```

## 🚀 Quick Start

```dart
import 'package:awqat/awqat.dart';

// 1. Initialize with your location
await Awqat.initialize(
  config: AwqatConfig(
    latitude: 23.8103,
    longitude: 90.4125,
    method: CalculationMethod.karachi,
    madhab: Madhab.hanafi,
  ),
);

// 2. Get prayer times
final times = await Awqat.getPrayerTimes();
print('Fajr: ${times.fajr}');
print('Next Prayer: ${times.nextPrayer()}');

// 3. Schedule native reminders
await Awqat.scheduleReminders(
  prayers: [PrayerType.fajr, PrayerType.dhuhr, PrayerType.asr],
  offsetMinutes: -5, // 5 minutes before
  playSound: true,
  sound: 'adhan',
  messages: ["Prayer is better than sleep", "Come to prayer"], // Optional: Random messages
);

// 4. Get all prayer slots for a date (Friday-aware Jumuah label)
final all = await Awqat.getAllPrayerTimes(
  includeSunrise: true,
  includeJumuahOnFriday: true,
);
```

## 📱 Platform Setup

### Android

Add the following to your `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

### iOS

Add to your `ios/Runner/Info.plist`:

```xml
<key>UIBackgroundModes</key>
<array>
    <string>fetch</string>
    <string>remote-notification</string>
</array>
```

## 📖 API Reference

### Configuration

```dart
AwqatConfig(
  latitude: 23.8103,
  longitude: 90.4125,
  timezone: 'Asia/Dhaka',          // Optional, uses device timezone
  method: CalculationMethod.karachi,
  madhab: Madhab.hanafi,
  highLatitudeRule: HighLatitudeRule.middleOfTheNight,
  adjustments: {'fajr': 2},        // Manual adjustments in minutes
)
```

### Calculation Methods

| Method | Fajr | Isha |
|--------|------|------|
| `muslimWorldLeague` | 18° | 17° |
| `egyptian` | 19.5° | 17.5° |
| `karachi` | 18° | 18° |
| `ummAlQura` | 18.5° | 90 min |
| `northAmerica` | 15° | 15° |
| `dubai` | 18.2° | 18.2° |
| `kuwait` | 18° | 17.5° |
| `qatar` | 18° | 90 min |
| `singapore` | 20° | 18° |
| `turkey` | 18° | 17° |
| `tehran` | 17.7° | 14° |

### Methods

```dart
// Initialize
await Awqat.initialize(config: AwqatConfig(...));

// Get prayer times
final times = await Awqat.getPrayerTimes(date: DateTime.now());

// Schedule reminders
await Awqat.scheduleReminders(
  prayers: [PrayerType.fajr],
  offsetMinutes: -5,
  playSound: true,
  sound: 'adhan',
);

// Cancel reminders
await Awqat.cancelAllReminders();
await Awqat.cancelReminder(PrayerType.fajr);

// Permissions
final hasPermission = await Awqat.hasPermission();
final granted = await Awqat.requestPermission();
```

### PrayerTimes

```dart
final times = await Awqat.getPrayerTimes();

times.fajr;        // DateTime
times.sunrise;     // DateTime
times.dhuhr;       // DateTime
times.asr;         // DateTime
times.maghrib;     // DateTime
times.isha;        // DateTime

times.currentPrayer();       // PrayerType
times.nextPrayer();          // PrayerType
times.timeUntilNextPrayer(); // Duration
```

## 🔧 Why Native?

Flutter packages like `android_alarm_manager_plus` require spinning up a Dart isolate for each alarm. This:

- ❌ Takes 1-3 seconds to start
- ❌ Consumes more battery
- ❌ Can be killed by aggressive OEMs (Xiaomi, Huawei)
- ❌ Might not wake the device reliably

**Awqat** uses:
- ✅ Native `AlarmManager.setAlarmClock()` on Android
- ✅ Native `UNCalendarNotificationTrigger` on iOS
- ✅ Zero Dart overhead
- ✅ Works with Doze mode
- ✅ Survives device restarts (Android)

## 📄 License

MIT License - see [LICENSE](LICENSE) for details.

## 🤝 Contributing

Contributions are welcome! Please read our contributing guidelines before submitting a PR.

---

Made with ❤️ for the Muslim community.
