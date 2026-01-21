import 'prayer_type.dart';

/// Calculated prayer times for a specific date and location.
///
/// This class contains the calculated times for all five daily prayers
/// plus sunrise. Times are returned as [DateTime] objects in the local timezone.
class PrayerTimes {
  /// Time for Fajr (pre-dawn) prayer
  final DateTime fajr;

  /// Time of sunrise (marks end of Fajr time)
  final DateTime sunrise;

  /// Time for Dhuhr (noon) prayer
  final DateTime dhuhr;

  /// Time for Asr (afternoon) prayer
  final DateTime asr;

  /// Time for Maghrib (sunset) prayer
  final DateTime maghrib;

  /// Time for Isha (night) prayer
  final DateTime isha;

  /// The date these prayer times are for
  final DateTime date;

  const PrayerTimes({
    required this.fajr,
    required this.sunrise,
    required this.dhuhr,
    required this.asr,
    required this.maghrib,
    required this.isha,
    required this.date,
  });

  /// Get the time for a specific prayer type
  DateTime timeForPrayer(PrayerType prayer) {
    switch (prayer) {
      case PrayerType.fajr:
        return fajr;
      case PrayerType.sunrise:
        return sunrise;
      case PrayerType.dhuhr:
        return dhuhr;
      case PrayerType.asr:
        return asr;
      case PrayerType.maghrib:
        return maghrib;
      case PrayerType.isha:
        return isha;
    }
  }

  /// Get the current or next prayer based on the current time
  PrayerType currentPrayer([DateTime? now]) {
    final currentTime = now ?? DateTime.now();

    if (currentTime.isBefore(fajr)) {
      return PrayerType.isha; // Previous day's Isha
    } else if (currentTime.isBefore(sunrise)) {
      return PrayerType.fajr;
    } else if (currentTime.isBefore(dhuhr)) {
      return PrayerType.sunrise;
    } else if (currentTime.isBefore(asr)) {
      return PrayerType.dhuhr;
    } else if (currentTime.isBefore(maghrib)) {
      return PrayerType.asr;
    } else if (currentTime.isBefore(isha)) {
      return PrayerType.maghrib;
    } else {
      return PrayerType.isha;
    }
  }

  /// Get the next upcoming prayer
  PrayerType nextPrayer([DateTime? now]) {
    final currentTime = now ?? DateTime.now();

    if (currentTime.isBefore(fajr)) {
      return PrayerType.fajr;
    } else if (currentTime.isBefore(sunrise)) {
      return PrayerType.sunrise;
    } else if (currentTime.isBefore(dhuhr)) {
      return PrayerType.dhuhr;
    } else if (currentTime.isBefore(asr)) {
      return PrayerType.asr;
    } else if (currentTime.isBefore(maghrib)) {
      return PrayerType.maghrib;
    } else if (currentTime.isBefore(isha)) {
      return PrayerType.isha;
    } else {
      return PrayerType.fajr; // Next day's Fajr
    }
  }

  /// Get time remaining until the next prayer
  Duration timeUntilNextPrayer([DateTime? now]) {
    final currentTime = now ?? DateTime.now();
    final next = nextPrayer(currentTime);
    final nextTime = timeForPrayer(next);

    if (nextTime.isAfter(currentTime)) {
      return nextTime.difference(currentTime);
    } else {
      // Next prayer is tomorrow's Fajr
      return nextTime.add(const Duration(days: 1)).difference(currentTime);
    }
  }

  /// Create from Map (from platform channel)
  factory PrayerTimes.fromMap(Map<String, dynamic> map) {
    return PrayerTimes(
      fajr: DateTime.fromMillisecondsSinceEpoch(map['fajr'] as int),
      sunrise: DateTime.fromMillisecondsSinceEpoch(map['sunrise'] as int),
      dhuhr: DateTime.fromMillisecondsSinceEpoch(map['dhuhr'] as int),
      asr: DateTime.fromMillisecondsSinceEpoch(map['asr'] as int),
      maghrib: DateTime.fromMillisecondsSinceEpoch(map['maghrib'] as int),
      isha: DateTime.fromMillisecondsSinceEpoch(map['isha'] as int),
      date: DateTime.fromMillisecondsSinceEpoch(map['date'] as int),
    );
  }

  /// Convert to Map for platform channel
  Map<String, dynamic> toMap() {
    return {
      'fajr': fajr.millisecondsSinceEpoch,
      'sunrise': sunrise.millisecondsSinceEpoch,
      'dhuhr': dhuhr.millisecondsSinceEpoch,
      'asr': asr.millisecondsSinceEpoch,
      'maghrib': maghrib.millisecondsSinceEpoch,
      'isha': isha.millisecondsSinceEpoch,
      'date': date.millisecondsSinceEpoch,
    };
  }

  @override
  String toString() {
    return 'PrayerTimes(date: ${date.toIso8601String()}, fajr: $fajr, dhuhr: $dhuhr, asr: $asr, maghrib: $maghrib, isha: $isha)';
  }
}
