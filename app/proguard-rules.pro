# AutoType X1 Proguard / R8 Rules

# Room Database, DAOs, Entities, and Generated Implementations
-keep class * extends androidx.room.RoomDatabase
-keep class com.shyam.autotypex1.data.local.room.** { *; }
-dontwarn androidx.room.**

# Keep all Domain Models & Typing Models
-keep class com.shyam.autotypex1.domain.model.** { *; }
-keep class com.shyam.autotypex1.domain.typing.** { *; }

# Preserve Enums (e.g., ThemeMode) and their valueOf / values methods
-keepclassmembers enum ** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Hilt & ViewModels
-keep class * extends androidx.lifecycle.ViewModel
-keep class **.*_HiltModules** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

