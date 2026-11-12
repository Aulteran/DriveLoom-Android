# kotlinx.serialization: keep generated serializers for the API models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class io.github.aulteran.driveloom.core.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.aulteran.driveloom.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.github.aulteran.driveloom.core.**$$serializer { *; }

# OkHttp optional platform integrations that are not on Android.
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
