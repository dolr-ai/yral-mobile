-dontwarn java.lang.invoke.StringConcatFactory

# keep jan classes [https://github.com/mozilla/application-services/blob/main/proguard-rules-consumer-jna.pro]
-dontwarn java.awt.*
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }

# https://help.branch.io/developers-hub/docs/android-basic-integration#7-configure-proguard
-keep class com.google.android.gms.** { *; }

# Snowplow Android Tracker — reflection for platform context (appSetId and AAID)
# https://docs.snowplow.io/docs/sources/mobile-trackers/installation-and-set-up/?platform=android
-keep class com.google.android.gms.appset.AppSet { *; }
-keep class com.google.android.gms.appset.AppSetIdInfo { *; }
-keep class com.google.android.gms.internal.appset.zzr { *; }
-keep class com.google.android.gms.tasks.Tasks { *; }
-keep class com.google.android.gms.ads.identifier.** { *; }

# Keep shared error type names readable in Crashlytics headers.
-keepnames class com.yral.shared.core.exceptions.**
-keepnames class com.yral.shared.http.exception.**
