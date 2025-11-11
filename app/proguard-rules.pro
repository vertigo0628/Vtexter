# Add project specific ProGuard rules here.

# Keep Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Keep data classes
-keep class com.university.vtexter.models.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Remove logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}