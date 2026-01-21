/// Islamic schools of jurisprudence (madhab) for Asr calculation.
///
/// The two main opinions differ on when Asr time begins:
/// - **Shafi/Maliki/Hanbali**: When shadow length equals object height
/// - **Hanafi**: When shadow length equals twice the object height
enum Madhab {
  /// Shafi, Maliki, and Hanbali schools
  /// Asr begins when shadow length = object height + noon shadow
  shafi,

  /// Hanafi school
  /// Asr begins when shadow length = 2x object height + noon shadow
  hanafi,
}

/// Extension to get string identifier for the madhab
extension MadhabExtension on Madhab {
  String get identifier {
    switch (this) {
      case Madhab.shafi:
        return 'shafi';
      case Madhab.hanafi:
        return 'hanafi';
    }
  }

  static Madhab fromIdentifier(String id) {
    switch (id) {
      case 'hanafi':
        return Madhab.hanafi;
      case 'shafi':
      default:
        return Madhab.shafi;
    }
  }
}
