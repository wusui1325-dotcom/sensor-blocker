# Sensor Blocker

Android app to block specific apps from accessing gyroscope, accelerometer, compass (magnetometer) sensors.

## Goal
Prevent selected apps from obtaining data from:
- Gyroscope (`TYPE_GYROSCOPE`)
- Accelerometer (`TYPE_ACCELEROMETER`)
- Compass / Magnetometer (`TYPE_MAGNETIC_FIELD`)

## Technical Notes

These sensors are **zero-permission** on stock Android (no runtime permission required, accessible via `SensorManager` without user consent dialog).

### Possible Implementation Paths

1. **Root / Magisk / Xposed / LSPosed**
   - Hook `SensorManager.registerListener` or the sensor service.
   - Use AppOps if the ROM supports it.
   - Magisk module to deny sensor access per package.

2. **GrapheneOS / hardened ROMs**
   - GrapheneOS has a dedicated "Sensors" permission that can be toggled per app.
   - This app could provide a convenient UI or automation for it.

3. **Monitoring + User Alert**
   - Detect sensor usage from user space (e.g. via techniques in research papers like ARMOUR).
   - Notify user when a blocked app is accessing sensors.
   - Cannot fully prevent without system privileges.

4. **System-wide Sensors Off**
   - Android has a hidden "Sensors Off" developer option / Quick Settings tile that disables most sensors system-wide.
   - Can be toggled via `Settings.Global.SENSORS_OFF` or similar on some builds, but requires system privileges.

5. **Device Owner / Enterprise**
   - Some DevicePolicyManager APIs can restrict features, but sensor access is limited.

**Recommended starting point**: Target rooted devices or document GrapheneOS workflow, while providing a package selector UI.

## Project Structure
Basic Android Gradle skeleton. Open in Android Studio, sync, and generate Gradle wrapper if needed.

## Status
Initial setup complete.
