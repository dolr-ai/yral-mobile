-dontwarn java.lang.invoke.StringConcatFactory

# keep jan classes [https://github.com/mozilla/application-services/blob/main/proguard-rules-consumer-jna.pro]
-dontwarn java.awt.*
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }

# https://help.branch.io/developers-hub/docs/android-basic-integration#7-configure-proguard
-keep class com.google.android.gms.** { *; }