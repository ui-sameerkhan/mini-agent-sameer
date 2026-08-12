# Apache POI / XMLBeans reflection. poi-ooxml-lite only ships a subset of the
# OOXML schema classes (word-processing/chart schemas are absent), so R8 sees
# "missing class" references from XMLBeans-generated stubs that are never
# actually invoked at runtime for our xlsx-only usage — dontwarn, don't keep.
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.w3c.dom.**
-dontwarn javax.xml.**
-dontwarn javax.activation.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn schemaorg_apache_xmlbeans.**
-dontwarn com.microsoft.schemas.**
-dontwarn com.github.luben.zstd.**
-dontwarn org.brotli.**
-dontwarn org.etsi.uri.**
-dontwarn org.apache.batik.**
-dontwarn com.graphbuilder.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
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
