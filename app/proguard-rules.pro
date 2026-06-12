-keep,allowoptimization,allowobfuscation class com.jaredrummler.android.colorpicker.**
-dontwarn sun.security.internal.spec.**
-dontwarn sun.security.provider.**
-dontwarn com.jaredrummler.android.colorpicker.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.lang.model.element.Modifier

-keepattributes Exceptions,LineNumberTable,Signature,SourceFile

-keepclasseswithmembernames,allowoptimization,allowobfuscation class * {
    native <methods>;
}

-keepclassmembers,allowoptimization,allowobfuscation enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class com.drdisagree.pixellauncherenhanced.ui.activities.**
-keep class com.drdisagree.pixellauncherenhanced.ui.fragments.**

-keep class io.github.libxposed.api.** { *; }
-keep class io.github.libxposed.service.** { *; }

-keep class com.drdisagree.pixellauncherenhanced.xposed.PLEnhancedModule {
    public <init>(io.github.libxposed.api.XposedModuleInterface$ModuleLoadedParam);
}

-keepclassmembers,allowoptimization,allowobfuscation class * extends com.drdisagree.pixellauncherenhanced.xposed.ModPack {
    public <init>(android.content.Context);
}

-keepnames class com.drdisagree.pixellauncherenhanced.xposed.utils.XPrefs
-keepclassmembers,allowoptimization,allowobfuscation class com.drdisagree.pixellauncherenhanced.xposed.utils.XPrefs {
    public *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keep interface **.I* { *; }
-keep class **.I*$Stub { *; }
-keep class **.I*$Stub$Proxy { *; }

-repackageclasses
-allowaccessmodification

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
	public static void check*(...);
	public static void throw*(...);
}
-assumenosideeffects class java.util.Objects {
    public static ** requireNonNull(...);
}
