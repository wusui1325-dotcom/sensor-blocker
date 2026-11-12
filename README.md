# Sensor Blocker

Android app to block specific apps from accessing gyroscope, accelerometer, and magnetometer (compass) sensors.

**Constraint**: No root. Maximum privilege: Shizuku (ADB shell level).

## Goal
Prevent selected apps from obtaining data from:
- Gyroscope (`TYPE_GYROSCOPE`)
- Accelerometer (`TYPE_ACCELEROMETER`)
- Magnetometer / Compass (`TYPE_MAGNETIC_FIELD`)

## Approach (No Root + Shizuku)

These sensors are zero-permission on stock Android. The practical no-root method is:

1. Use **Shizuku** to call privileged APIs (equivalent to ADB shell).
2. Leverage `SensorPrivacyManager` / `ISensorPrivacyManager.setSensorPrivacy` (Android 12+) to toggle sensor privacy state.
3. Monitor the foreground app (via `UsageStatsManager` or AccessibilityService).
4. When a user-selected "blocked" app comes to the foreground → enable sensor privacy (sensors off).
5. When it leaves → restore previous state (sensors on).

This is the same technique used by the open-source **SensorsOff** app.

### Limitations
- Sensor privacy is system-wide while active (not truly per-app isolation at the sensor service level).
- Effectiveness depends on Android version and OEM implementation of `SensorPrivacyManager`.
- Requires Shizuku running (wireless debugging or ADB once).
- Some system apps / OEM services may still have access.

## Project Structure
Basic Android Gradle skeleton with Shizuku dependency prepared.
Open in Android Studio, sync Gradle, and complete implementation.

## Status
Initial setup + Shizuku-ready skeleton.
