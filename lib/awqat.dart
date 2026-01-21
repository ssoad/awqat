import 'dart:async';
import 'package:flutter/services.dart';

import 'src/awqat_config.dart';
import 'src/prayer_times.dart';
import 'src/prayer_type.dart';

export 'src/awqat_config.dart';
export 'src/calculation_method.dart';
export 'src/madhab.dart';
export 'src/prayer_times.dart';
export 'src/prayer_type.dart';

/// Native prayer time reminders for Flutter.
///
/// Uses platform-native AlarmManager (Android) and UNNotificationCenter (iOS)
/// for reliable, battery-efficient Islamic prayer notifications.
///
/// ## Quick Start
///
/// ```dart
/// // Initialize with location and preferences
/// await Awqat.initialize(
///   config: AwqatConfig(
///     latitude: 23.8103,
///     longitude: 90.4125,
///     method: CalculationMethod.karachi,
///     madhab: Madhab.hanafi,
///   ),
/// );
///
/// // Get prayer times for today
/// final times = await Awqat.getPrayerTimes();
/// print('Fajr: ${times.fajr}');
/// print('Next: ${times.nextPrayer()}');
///
/// // Schedule native reminders
/// await Awqat.scheduleReminders(
///   prayers: [PrayerType.fajr, PrayerType.dhuhr, PrayerType.asr],
///   offsetMinutes: -5, // 5 minutes before
/// );
/// ```
class Awqat {
  static const MethodChannel _channel = MethodChannel('dev.awqat/awqat');

  static AwqatConfig? _config;
  static bool _isInitialized = false;

  /// Whether the plugin has been initialized
  static bool get isInitialized => _isInitialized;

  /// Current configuration
  static AwqatConfig? get config => _config;

  /// Initialize the Awqat plugin with the given configuration.
  ///
  /// This must be called before any other methods.
  /// Typically called in your app's `main()` or during startup.
  static Future<void> initialize({required AwqatConfig config}) async {
    _config = config;

    try {
      await _channel.invokeMethod('initialize', config.toMap());
      _isInitialized = true;
    } on PlatformException catch (e) {
      throw AwqatException('Failed to initialize: ${e.message}');
    }
  }

  /// Update the configuration without re-initializing.
  ///
  /// Useful when the user changes location or preferences.
  static Future<void> updateConfig(AwqatConfig config) async {
    _ensureInitialized();
    _config = config;

    try {
      await _channel.invokeMethod('updateConfig', config.toMap());
    } on PlatformException catch (e) {
      throw AwqatException('Failed to update config: ${e.message}');
    }
  }

  /// Get prayer times for a specific date.
  ///
  /// If [date] is null, returns prayer times for today.
  static Future<PrayerTimes> getPrayerTimes({DateTime? date}) async {
    _ensureInitialized();

    try {
      final result = await _channel.invokeMethod<Map>('getPrayerTimes', {
        'date': (date ?? DateTime.now()).millisecondsSinceEpoch,
      });

      if (result == null) {
        throw AwqatException('No prayer times returned from native');
      }

      return PrayerTimes.fromMap(Map<String, dynamic>.from(result));
    } on PlatformException catch (e) {
      throw AwqatException('Failed to get prayer times: ${e.message}');
    }
  }

  /// Schedule native reminders for the specified prayers.
  ///
  /// - [prayers]: List of prayers to schedule notifications for.
  ///   If empty, schedules for all prayers.
  /// - [offsetMinutes]: Minutes before the prayer time to trigger.
  ///   Use negative values for "X minutes before" (default: 0).
  /// - [title]: Custom notification title. If null, uses prayer name.
  /// - [body]: Custom notification body.
  /// - [sound]: Custom notification sound asset name.
  static Future<void> scheduleReminders({
    List<PrayerType> prayers = const [],
    int offsetMinutes = 0,
    String? title,
    String? body,
    String? sound,
  }) async {
    _ensureInitialized();

    final prayerIds = prayers.isEmpty
        ? [
            PrayerType.fajr,
            PrayerType.dhuhr,
            PrayerType.asr,
            PrayerType.maghrib,
            PrayerType.isha,
          ].map((p) => p.identifier).toList()
        : prayers.map((p) => p.identifier).toList();

    try {
      await _channel.invokeMethod('scheduleReminders', {
        'prayers': prayerIds,
        'offsetMinutes': offsetMinutes,
        'title': title,
        'body': body,
        'sound': sound,
      });
    } on PlatformException catch (e) {
      throw AwqatException('Failed to schedule reminders: ${e.message}');
    }
  }

  /// Cancel all scheduled reminders.
  static Future<void> cancelAllReminders() async {
    _ensureInitialized();

    try {
      await _channel.invokeMethod('cancelAllReminders');
    } on PlatformException catch (e) {
      throw AwqatException('Failed to cancel reminders: ${e.message}');
    }
  }

  /// Cancel reminder for a specific prayer.
  static Future<void> cancelReminder(PrayerType prayer) async {
    _ensureInitialized();

    try {
      await _channel.invokeMethod('cancelReminder', {
        'prayer': prayer.identifier,
      });
    } on PlatformException catch (e) {
      throw AwqatException('Failed to cancel reminder: ${e.message}');
    }
  }

  /// Request notification permission (Android 13+ / iOS).
  ///
  /// Returns true if permission is granted.
  static Future<bool> requestPermission() async {
    try {
      final result = await _channel.invokeMethod<bool>('requestPermission');
      return result ?? false;
    } on PlatformException catch (e) {
      throw AwqatException('Failed to request permission: ${e.message}');
    }
  }

  /// Check if notification permission is granted.
  static Future<bool> hasPermission() async {
    try {
      final result = await _channel.invokeMethod<bool>('hasPermission');
      return result ?? false;
    } on PlatformException catch (e) {
      throw AwqatException('Failed to check permission: ${e.message}');
    }
  }

  /// Get the platform version (for debugging).
  static Future<String?> getPlatformVersion() async {
    try {
      return await _channel.invokeMethod<String>('getPlatformVersion');
    } on PlatformException {
      return null;
    }
  }

  static void _ensureInitialized() {
    if (!_isInitialized) {
      throw AwqatException(
        'Awqat is not initialized. Call Awqat.initialize() first.',
      );
    }
  }
}

/// Exception thrown by Awqat operations.
class AwqatException implements Exception {
  final String message;

  AwqatException(this.message);

  @override
  String toString() => 'AwqatException: $message';
}
