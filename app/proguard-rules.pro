# kotlinx.serialization – zachowaj serializatory DTO i tras nawigacji
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.wanderlist.** {
    *** Companion;
}
-keepclasseswithmembers class com.wanderlist.** {
    kotlinx.serialization.KSerializer serializer(...);
}
