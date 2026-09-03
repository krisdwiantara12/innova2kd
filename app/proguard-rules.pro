# Proguard rules for Innova 2KD Launcher
-keep class com.innova.launcher2kd.** { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
