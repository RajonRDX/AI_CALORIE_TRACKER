-keepattributes *Annotation*,InnerClasses,Signature
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep class com.bdcalorietracker.data.remote.** { *; }
