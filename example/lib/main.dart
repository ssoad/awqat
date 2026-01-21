import 'package:flutter/material.dart';
import 'package:awqat/awqat.dart';

void main() {
  runApp(const AwqatExampleApp());
}

class AwqatExampleApp extends StatelessWidget {
  const AwqatExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Awqat Example',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF10B981),
          brightness: Brightness.light,
        ),
        useMaterial3: true,
      ),
      darkTheme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF10B981),
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
      ),
      home: const PrayerTimesScreen(),
    );
  }
}

class PrayerTimesScreen extends StatefulWidget {
  const PrayerTimesScreen({super.key});

  @override
  State<PrayerTimesScreen> createState() => _PrayerTimesScreenState();
}

class _PrayerTimesScreenState extends State<PrayerTimesScreen> {
  PrayerTimes? _prayerTimes;
  String _status = 'Not initialized';
  bool _isLoading = true;
  bool _hasPermission = false;

  // Example: Dhaka, Bangladesh
  static const double _latitude = 23.8103;
  static const double _longitude = 90.4125;

  @override
  void initState() {
    super.initState();
    _initializeAwqat();
  }

  Future<void> _initializeAwqat() async {
    try {
      // Initialize with location and preferences
      await Awqat.initialize(
        config: AwqatConfig(
          latitude: _latitude,
          longitude: _longitude,
          method: CalculationMethod.karachi,
          madhab: Madhab.hanafi,
        ),
      );

      // Check notification permission
      _hasPermission = await Awqat.hasPermission();

      // Get today's prayer times
      final times = await Awqat.getPrayerTimes();

      setState(() {
        _prayerTimes = times;
        _status = 'Initialized successfully';
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _status = 'Error: $e';
        _isLoading = false;
      });
    }
  }

  Future<void> _requestPermission() async {
    try {
      final granted = await Awqat.requestPermission();
      setState(() {
        _hasPermission = granted;
      });

      if (granted) {
        _showSnackBar('Permission granted!');
      } else {
        _showSnackBar('Permission denied');
      }
    } catch (e) {
      _showSnackBar('Error: $e');
    }
  }

  Future<void> _scheduleReminders() async {
    if (!_hasPermission) {
      _showSnackBar('Please grant notification permission first');
      return;
    }

    try {
      await Awqat.scheduleReminders(
        prayers: [
          PrayerType.fajr,
          PrayerType.dhuhr,
          PrayerType.asr,
          PrayerType.maghrib,
          PrayerType.isha,
        ],
        offsetMinutes: -5, // 5 minutes before
      );
      _showSnackBar('Reminders scheduled successfully!');
    } catch (e) {
      _showSnackBar('Error: $e');
    }
  }

  Future<void> _cancelReminders() async {
    try {
      await Awqat.cancelAllReminders();
      _showSnackBar('All reminders cancelled');
    } catch (e) {
      _showSnackBar('Error: $e');
    }
  }

  void _showSnackBar(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Awqat Example'),
        centerTitle: true,
        backgroundColor: theme.colorScheme.primary,
        foregroundColor: Colors.white,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Status Card
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Status',
                            style: theme.textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 8),
                          Text(_status),
                          const SizedBox(height: 8),
                          Row(
                            children: [
                              Icon(
                                _hasPermission
                                    ? Icons.check_circle
                                    : Icons.cancel,
                                color: _hasPermission
                                    ? Colors.green
                                    : Colors.red,
                                size: 20,
                              ),
                              const SizedBox(width: 8),
                              Text(
                                _hasPermission
                                    ? 'Notification permission granted'
                                    : 'Notification permission not granted',
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Prayer Times Card
                  if (_prayerTimes != null)
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Prayer Times (Dhaka)',
                              style: theme.textTheme.titleMedium?.copyWith(
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            const SizedBox(height: 16),
                            _buildPrayerRow(
                              'Fajr',
                              _prayerTimes!.fajr,
                              PrayerType.fajr,
                            ),
                            _buildPrayerRow(
                              'Sunrise',
                              _prayerTimes!.sunrise,
                              PrayerType.sunrise,
                            ),
                            _buildPrayerRow(
                              'Dhuhr',
                              _prayerTimes!.dhuhr,
                              PrayerType.dhuhr,
                            ),
                            _buildPrayerRow(
                              'Asr',
                              _prayerTimes!.asr,
                              PrayerType.asr,
                            ),
                            _buildPrayerRow(
                              'Maghrib',
                              _prayerTimes!.maghrib,
                              PrayerType.maghrib,
                            ),
                            _buildPrayerRow(
                              'Isha',
                              _prayerTimes!.isha,
                              PrayerType.isha,
                            ),
                            const SizedBox(height: 16),
                            Container(
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: theme.colorScheme.primaryContainer,
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Row(
                                children: [
                                  Icon(
                                    Icons.access_time,
                                    color: theme.colorScheme.onPrimaryContainer,
                                  ),
                                  const SizedBox(width: 12),
                                  Text(
                                    'Next: ${_prayerTimes!.nextPrayer().name.toUpperCase()}',
                                    style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                      color:
                                          theme.colorScheme.onPrimaryContainer,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  const SizedBox(height: 24),

                  // Action Buttons
                  if (!_hasPermission)
                    ElevatedButton.icon(
                      onPressed: _requestPermission,
                      icon: const Icon(Icons.notifications),
                      label: const Text('Request Permission'),
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                      ),
                    ),
                  if (_hasPermission) ...[
                    ElevatedButton.icon(
                      onPressed: _scheduleReminders,
                      icon: const Icon(Icons.alarm_add),
                      label: const Text('Schedule All Reminders'),
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        backgroundColor: theme.colorScheme.primary,
                        foregroundColor: Colors.white,
                      ),
                    ),
                    const SizedBox(height: 12),
                    OutlinedButton.icon(
                      onPressed: _cancelReminders,
                      icon: const Icon(Icons.cancel),
                      label: const Text('Cancel All Reminders'),
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                      ),
                    ),
                  ],
                ],
              ),
            ),
    );
  }

  Widget _buildPrayerRow(String name, DateTime time, PrayerType type) {
    final theme = Theme.of(context);
    final isNext = _prayerTimes?.nextPrayer() == type;
    final isPast = time.isBefore(DateTime.now());

    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
      decoration: BoxDecoration(
        color: isNext ? theme.colorScheme.primaryContainer.withAlpha(76) : null,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            children: [
              if (isNext)
                Container(
                  width: 8,
                  height: 8,
                  margin: const EdgeInsets.only(right: 8),
                  decoration: BoxDecoration(
                    color: theme.colorScheme.primary,
                    shape: BoxShape.circle,
                  ),
                ),
              Text(
                name,
                style: TextStyle(
                  fontWeight: isNext ? FontWeight.bold : FontWeight.normal,
                  color: isPast ? theme.disabledColor : null,
                ),
              ),
            ],
          ),
          Text(
            _formatTime(time),
            style: TextStyle(
              fontWeight: isNext ? FontWeight.bold : FontWeight.normal,
              fontFamily: 'monospace',
              color: isPast ? theme.disabledColor : null,
            ),
          ),
        ],
      ),
    );
  }

  String _formatTime(DateTime time) {
    final hour = time.hour > 12
        ? time.hour - 12
        : (time.hour == 0 ? 12 : time.hour);
    final period = time.hour >= 12 ? 'PM' : 'AM';
    final minute = time.minute.toString().padLeft(2, '0');
    return '$hour:$minute $period';
  }
}
