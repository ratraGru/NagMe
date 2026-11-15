# Room (KSP) dan Compose membawa aturan consumer sendiri. Aturan tambahan:
-keep class com.gru.nagme.data.** { *; }
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.app.Service
-dontwarn kotlinx.coroutines.**
