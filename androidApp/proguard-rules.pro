# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Kotlin Serialization looks up serializers via reflection at runtime, so we need to keep all serializer classes
-keepclassmembers class **$$serializer {
    *** INSTANCE;
}
-keep class kotlinx.serialization.** { *; }

-dontwarn java.lang.invoke.StringConcatFactory

# keep jan classes [https://github.com/mozilla/application-services/blob/main/proguard-rules-consumer-jna.pro]
-dontwarn java.awt.*
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }