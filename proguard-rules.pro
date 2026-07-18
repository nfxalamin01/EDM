# EDM Proguard Rules
-keep class com.edm.downloadmanager.data.db.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <fields>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
