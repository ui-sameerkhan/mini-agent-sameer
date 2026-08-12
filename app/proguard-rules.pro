# Apache POI / XMLBeans reflection
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.w3c.dom.**
-dontwarn javax.xml.**
-dontwarn org.apache.commons.compress.**
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class org.openxmlformats.schemas.** { *; }
-keep class com.microsoft.schemas.** { *; }

# Firebase / Firestore model classes (data classes used with toObject)
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class com.ktc.sitepulse.data.model.** {
  <fields>;
  <init>(...);
}

-dontwarn kotlinx.coroutines.**
