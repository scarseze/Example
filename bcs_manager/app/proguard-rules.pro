# Retrofit
# --- Retrofit & Gson ---
-keepattributes Signature
-keepattributes Exceptions

# Gson
# Gson uses generic type information stored in serialization/deserialization
-keepattributes Signature

# Retain generic type information for use by serialization/deserialization by Gson
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Data Classes (Models used in JSON parsing)
# --- Data Classes ---
-keep class ru.macht.investmanager.data.** { *; }
-keep class ru.macht.investmanager.api.** { *; }
-keep class ru.macht.investmanager.domain.model.** { *; }

# --- SimpleXML (Critical for RSS) ---
-keep class org.simpleframework.xml.** { *; }
-keep class org.simpleframework.xml.core.** { *; }
-keep class org.simpleframework.xml.util.** { *; }
-dontwarn org.simpleframework.xml.stream.**

# --- Coroutines ---
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }

# --- Room ---
-dontwarn androidx.room.paging.**

# --- Hilt/Dagger ---
-keepattributes RuntimeVisibleAnnotations
-keepattributes AnnotationDefault

# R8 full mode
-ignorewarnings
