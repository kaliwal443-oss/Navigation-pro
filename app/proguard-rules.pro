# ProGuard rules for Navigation Pro

# Keep osmdroid classes
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Keep Room entities
-keep class com.navigationpro.data.model.** { *; }
-keepclassmembers class com.navigationpro.data.model.** {
    @androidx.room.PrimaryKey <fields>;
    @androidx.room.ColumnInfo <fields>;
}

# Keep Hilt
-keep class * extends android.app.Application { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }

# Keep GPX library
-keep class io.jenetics.jpx.** { *; }
-dontwarn io.jenetics.jpx.**

# Keep SunCalc
-keep class org.shredzone.commons.suncalc.** { *; }
-dontwarn org.shredzone.commons.suncalc.**

# General Android
-keepattributes Signature
-keepattributes Exceptions
-keepattributes SourceFile
-keepattributes LineNumberTable

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
}