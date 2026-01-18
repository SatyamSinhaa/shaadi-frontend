# Generic Proguard rules for a standard Android app.
-dontobfuscate
-dontoptimize
-dontwarn
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Exceptions

# Keep all of your application's data classes (models). 
-keep class com.example.myapplication.data.model.** { *; }

# Keep all public interfaces that are used by Retrofit.
-keep public interface com.example.myapplication.data.api.** { *; }

# Keep the following for Retrofit to function correctly.
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class com.squareup.okhttp3.** { *; }
-keep interface com.squareup.okhttp3.** { *; }

# Keep the following for Gson to function correctly.
-keep class com.google.gson.** { *; }

# Gson uses generic type information stored in a class file when working with fields.
# R8 removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# Gson specific classes
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with members
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Application classes that will be serialized/deserialized over Gson
-keep class com.example.myapplication.data.model.** { <fields>; }

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep the following for Coroutines to function correctly.
-keep class kotlinx.coroutines.** { *; }

# Keep the following for Firebase and Google Play Services.
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Google Sign-In classes to prevent ClassCastException
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keepclassmembers class com.google.android.gms.** { *; }

# Keep Firebase Auth classes
-keep class com.google.firebase.auth.** { *; }
-keepclassmembers class com.google.firebase.auth.** { *; }

# Keep any Parcelable classes.
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
