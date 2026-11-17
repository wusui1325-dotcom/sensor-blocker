# Manifest components (extra safety; AGP also keeps these).
-keep public class com.wusper.sensorblocker.SensorBlockerApp { <init>(); }
-keep public class com.wusper.sensorblocker.MainActivity { <init>(); }
-keep public class com.wusper.sensorblocker.SensorMonitorService { <init>(); }
-keep public class com.wusper.sensorblocker.WindowChangeAccessibilityService { <init>(); }
-keep public class com.wusper.sensorblocker.BootReceiver { <init>(); }

# Shizuku UserService is started by class name / reflection, not the manifest.
-keep class com.wusper.sensorblocker.shell.** { *; }
-keep public class com.wusper.sensorblocker.shell.ShellUserService {
    public <init>();
}
-keep class com.wusper.sensorblocker.shell.IShellService { *; }
-keep class com.wusper.sensorblocker.shell.IShellService$Stub { *; }
-keep class com.wusper.sensorblocker.shell.IShellService$Stub$Proxy { *; }

# Provider is declared in the manifest; keep the class name explicitly.
-keep class rikka.shizuku.ShizukuProvider { *; }

# AIDL stubs used across the app / Shizuku process boundary.
-keep class * implements android.os.IInterface { *; }
-keepclassmembers class * implements android.os.IInterface {
    public static class Stub;
    public static class Stub$Proxy;
    public static * asInterface(android.os.IBinder);
}
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-dontwarn rikka.**
-dontwarn moe.shizuku.**

-keep class com.wusper.sensorblocker.BuildConfig { *; }
