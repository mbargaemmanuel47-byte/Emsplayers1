# ProGuard / R8 Rules for Ems Player Release & Google Play App Bundle

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Google Play Billing Library
-keep class com.android.billingclient.api.** { *; }

# Firebase AI Logic & App Check
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.* <fields>;
}

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.debug.**
