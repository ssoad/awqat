/// Calculation methods for determining prayer times.
///
/// Different regions and organizations use different methods for calculating
/// prayer times. This enum provides the most commonly used methods.
enum CalculationMethod {
  /// Muslim World League
  /// Fajr: 18°, Isha: 17°
  muslimWorldLeague,

  /// Egyptian General Authority of Survey
  /// Fajr: 19.5°, Isha: 17.5°
  egyptian,

  /// University of Islamic Sciences, Karachi
  /// Fajr: 18°, Isha: 18°
  karachi,

  /// Umm Al-Qura University, Makkah
  /// Fajr: 18.5°, Isha: 90 min after Maghrib (120 min during Ramadan)
  ummAlQura,

  /// Islamic Society of North America (ISNA)
  /// Fajr: 15°, Isha: 15°
  northAmerica,

  /// Dubai (UAE)
  /// Fajr: 18.2°, Isha: 18.2°
  dubai,

  /// Moonsighting Committee Worldwide
  /// Fajr: 18°, Isha: 18°
  moonsightingCommittee,

  /// Kuwait
  /// Fajr: 18°, Isha: 17.5°
  kuwait,

  /// Qatar
  /// Fajr: 18°, Isha: 90 min after Maghrib
  qatar,

  /// Singapore
  /// Fajr: 20°, Isha: 18°
  singapore,

  /// Turkey (Diyanet İşleri Başkanlığı)
  /// Fajr: 18°, Isha: 17°
  turkey,

  /// Tehran (Institute of Geophysics, University of Tehran)
  /// Fajr: 17.7°, Isha: 14° + 4/60°
  tehran,
}

/// Extension to get string identifier for the calculation method
extension CalculationMethodExtension on CalculationMethod {
  String get identifier {
    switch (this) {
      case CalculationMethod.muslimWorldLeague:
        return 'muslim_world_league';
      case CalculationMethod.egyptian:
        return 'egyptian';
      case CalculationMethod.karachi:
        return 'karachi';
      case CalculationMethod.ummAlQura:
        return 'umm_al_qura';
      case CalculationMethod.northAmerica:
        return 'north_america';
      case CalculationMethod.dubai:
        return 'dubai';
      case CalculationMethod.moonsightingCommittee:
        return 'moonsighting_committee';
      case CalculationMethod.kuwait:
        return 'kuwait';
      case CalculationMethod.qatar:
        return 'qatar';
      case CalculationMethod.singapore:
        return 'singapore';
      case CalculationMethod.turkey:
        return 'turkey';
      case CalculationMethod.tehran:
        return 'tehran';
    }
  }

  static CalculationMethod fromIdentifier(String id) {
    switch (id) {
      case 'muslim_world_league':
        return CalculationMethod.muslimWorldLeague;
      case 'egyptian':
        return CalculationMethod.egyptian;
      case 'karachi':
        return CalculationMethod.karachi;
      case 'umm_al_qura':
        return CalculationMethod.ummAlQura;
      case 'north_america':
        return CalculationMethod.northAmerica;
      case 'dubai':
        return CalculationMethod.dubai;
      case 'moonsighting_committee':
        return CalculationMethod.moonsightingCommittee;
      case 'kuwait':
        return CalculationMethod.kuwait;
      case 'qatar':
        return CalculationMethod.qatar;
      case 'singapore':
        return CalculationMethod.singapore;
      case 'turkey':
        return CalculationMethod.turkey;
      case 'tehran':
        return CalculationMethod.tehran;
      default:
        return CalculationMethod.muslimWorldLeague;
    }
  }
}
