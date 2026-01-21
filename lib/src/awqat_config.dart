import 'calculation_method.dart';
import 'madhab.dart';

/// Configuration for prayer time calculations and notifications.
///
/// This class holds all the necessary parameters for calculating accurate
/// prayer times based on the user's location and preferred calculation method.
class AwqatConfig {
  /// Latitude of the location (required)
  final double latitude;

  /// Longitude of the location (required)
  final double longitude;

  /// Timezone identifier (e.g., 'Asia/Dhaka', 'America/New_York')
  /// If null, the device's local timezone is used.
  final String? timezone;

  /// Calculation method for Fajr and Isha angles
  final CalculationMethod method;

  /// School of jurisprudence for Asr calculation
  final Madhab madhab;

  /// High latitude adjustment rule
  /// Used for locations where twilight may persist throughout the night
  final HighLatitudeRule highLatitudeRule;

  /// Manual adjustment in minutes for each prayer (can be negative)
  /// Keys should match [PrayerType] identifiers
  final Map<String, int> adjustments;

  const AwqatConfig({
    required this.latitude,
    required this.longitude,
    this.timezone,
    this.method = CalculationMethod.muslimWorldLeague,
    this.madhab = Madhab.shafi,
    this.highLatitudeRule = HighLatitudeRule.middleOfTheNight,
    this.adjustments = const {},
  });

  /// Create a copy with modified values
  AwqatConfig copyWith({
    double? latitude,
    double? longitude,
    String? timezone,
    CalculationMethod? method,
    Madhab? madhab,
    HighLatitudeRule? highLatitudeRule,
    Map<String, int>? adjustments,
  }) {
    return AwqatConfig(
      latitude: latitude ?? this.latitude,
      longitude: longitude ?? this.longitude,
      timezone: timezone ?? this.timezone,
      method: method ?? this.method,
      madhab: madhab ?? this.madhab,
      highLatitudeRule: highLatitudeRule ?? this.highLatitudeRule,
      adjustments: adjustments ?? this.adjustments,
    );
  }

  /// Convert to Map for platform channel communication
  Map<String, dynamic> toMap() {
    return {
      'latitude': latitude,
      'longitude': longitude,
      'timezone': timezone,
      'method': method.identifier,
      'madhab': madhab.identifier,
      'highLatitudeRule': highLatitudeRule.identifier,
      'adjustments': adjustments,
    };
  }

  /// Create from Map (from platform channel)
  factory AwqatConfig.fromMap(Map<String, dynamic> map) {
    return AwqatConfig(
      latitude: map['latitude'] as double,
      longitude: map['longitude'] as double,
      timezone: map['timezone'] as String?,
      method: CalculationMethodExtension.fromIdentifier(
        map['method'] as String? ?? 'muslim_world_league',
      ),
      madhab: MadhabExtension.fromIdentifier(
        map['madhab'] as String? ?? 'shafi',
      ),
      highLatitudeRule: HighLatitudeRuleExtension.fromIdentifier(
        map['highLatitudeRule'] as String? ?? 'middle_of_the_night',
      ),
      adjustments: Map<String, int>.from(map['adjustments'] ?? {}),
    );
  }

  @override
  String toString() {
    return 'AwqatConfig(lat: $latitude, lng: $longitude, method: ${method.identifier}, madhab: ${madhab.identifier})';
  }
}

/// Rules for calculating prayer times in high latitude regions
enum HighLatitudeRule {
  /// Fajr will never be earlier than the middle of the night
  middleOfTheNight,

  /// Fajr will never be earlier than 1/7th of the night
  seventhOfTheNight,

  /// Similar to SeventhOfTheNight but twilight angle is used
  twilightAngle,
}

extension HighLatitudeRuleExtension on HighLatitudeRule {
  String get identifier {
    switch (this) {
      case HighLatitudeRule.middleOfTheNight:
        return 'middle_of_the_night';
      case HighLatitudeRule.seventhOfTheNight:
        return 'seventh_of_the_night';
      case HighLatitudeRule.twilightAngle:
        return 'twilight_angle';
    }
  }

  static HighLatitudeRule fromIdentifier(String id) {
    switch (id) {
      case 'seventh_of_the_night':
        return HighLatitudeRule.seventhOfTheNight;
      case 'twilight_angle':
        return HighLatitudeRule.twilightAngle;
      case 'middle_of_the_night':
      default:
        return HighLatitudeRule.middleOfTheNight;
    }
  }
}
