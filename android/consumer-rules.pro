# Awqat Plugin - Keep native classes from obfuscation
# These classes are used by Android's AlarmManager to trigger prayer notifications
-keep class dev.awqat.awqat.** { *; }
-keepclassmembers class dev.awqat.awqat.** { *; }

# Keep BroadcastReceiver implementations (AlarmReceiver, BootReceiver)
-keep class * extends android.content.BroadcastReceiver { *; }
