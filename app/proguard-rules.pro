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

# Firebase / Firestore model classes (data classes used with toObject/toObjects).
# Firestore's automatic POJO mapping (CustomClassMapper) walks each class's
# getter/setter methods via reflection at runtime to find "properties" to
# read/write — keeping only <fields>/<init>() (as before) let R8 still rename
# or strip those generated getters/setters in the release build, so Firestore
# found zero usable properties and every document failed to (de)serialize.
# This is Firebase's own documented requirement: keep the *entire* class,
# unobfuscated, for anything passed to toObject()/set().
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.ktc.sitepulse.data.model.** { *; }
-keepclassmembers class com.ktc.sitepulse.data.model.** { *; }

-dontwarn kotlinx.coroutines.**
