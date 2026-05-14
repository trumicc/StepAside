# =====================================================================
# StepAside — ProGuard / R8 rules
# =====================================================================

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------- kotlinx.serialization ----------------
# Keep all classes annotated with @Serializable (and their companions/serializers)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class io.stepaside.app.**$$serializer { *; }
-keepclassmembers class io.stepaside.app.** {
    *** Companion;
}
-keepclasseswithmembers class io.stepaside.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Generic serializer keep rules
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------- Ktor ----------------
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ---------------- Supabase ----------------
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# ---------------- Room ----------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ---------------- App-specific data classes (Serializable DTOs) ----------------
-keep class io.stepaside.app.data.remote.** { *; }
-keep class io.stepaside.app.ProfileUpsert { *; }
-keep class io.stepaside.app.sync.** { *; }

# ---------------- Compose ----------------
# Compose handles itself, but keep these for safety
-keep class androidx.compose.runtime.** { *; }

# ---------------- General Kotlin ----------------
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# Suppress harmless warnings
-dontwarn java.lang.invoke.StringConcatFactory
