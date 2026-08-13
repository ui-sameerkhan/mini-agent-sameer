# Add project specific ProGuard rules here.
-keep class com.lazyshopper.app.data.remote.dto.** { *; }
-keepattributes *Annotation*
-dontwarn kotlinx.serialization.**
