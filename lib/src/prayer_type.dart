/// Types of Islamic prayers.
enum PrayerType {
  /// Pre-dawn prayer
  fajr,

  /// Sunrise (not a prayer, but marks end of Fajr)
  sunrise,

  /// Noon prayer
  dhuhr,

  /// Afternoon prayer
  asr,

  /// Sunset prayer
  maghrib,

  /// Night prayer
  isha,
}

/// Extension to get localized names and identifiers
extension PrayerTypeExtension on PrayerType {
  String get identifier {
    switch (this) {
      case PrayerType.fajr:
        return 'fajr';
      case PrayerType.sunrise:
        return 'sunrise';
      case PrayerType.dhuhr:
        return 'dhuhr';
      case PrayerType.asr:
        return 'asr';
      case PrayerType.maghrib:
        return 'maghrib';
      case PrayerType.isha:
        return 'isha';
    }
  }

  /// Arabic name of the prayer
  String get arabicName {
    switch (this) {
      case PrayerType.fajr:
        return 'الفجر';
      case PrayerType.sunrise:
        return 'الشروق';
      case PrayerType.dhuhr:
        return 'الظهر';
      case PrayerType.asr:
        return 'العصر';
      case PrayerType.maghrib:
        return 'المغرب';
      case PrayerType.isha:
        return 'العشاء';
    }
  }

  static PrayerType fromIdentifier(String id) {
    switch (id) {
      case 'fajr':
        return PrayerType.fajr;
      case 'sunrise':
        return PrayerType.sunrise;
      case 'dhuhr':
        return PrayerType.dhuhr;
      case 'asr':
        return PrayerType.asr;
      case 'maghrib':
        return PrayerType.maghrib;
      case 'isha':
        return PrayerType.isha;
      default:
        return PrayerType.fajr;
    }
  }
}
