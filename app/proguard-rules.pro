# Shizuku UserService and AIDL stubs must stay intact.
-keep class com.wusper.sensorblocker.shell.** { *; }
-keep class rikka.shizuku.** { *; }
-keepclassmembers class * implements android.os.IInterface {
    public static class Stub;
}
