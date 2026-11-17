# 传感屏蔽（Sensor Blocker）

在 **不需要 root** 的前提下，用 Shizuku（最高到 ADB shell 权限）阻止其它应用读取运动传感器：加速度计、陀螺仪、磁力计（指南针）。主要用途是拦截国产应用里常见的「摇一摇」广告，也可以按名单或全局限制传感器访问。

This README is in Simplified Chinese first, then English.

## 它做什么

系统里这些运动传感器通常是「零权限」的：应用不必申请权限就能读。本应用通过 Android 公开的 **SensorService 受限模式** 改变谁能读到传感器数据：

- `dumpsys sensorservice restrict <包名>`：只有指定应用可以读取传感器
- `dumpsys sensorservice enable`：恢复正常模式（所有应用可读取）

这是 Android SensorService 的公开调试/运维命令，不是第三方私有接口。部分厂商系统可能改动或隐藏该服务，**不能保证每台设备都有效**。

> 旧版骨架曾尝试用 `service call sensor_privacy`（系统「关闭传感器」开发者开关）。那套接口在 AOSP 上主要针对摄像头和麦克风，而且事务号随系统版本变化，**不能可靠地屏蔽陀螺仪 / 加速度计 / 磁力计**。本应用只把它当作后备，不会把它宣传成主路径。

## 为什么需要 Shizuku

更改 SensorService 模式需要 shell 级权限。本应用不需要 root，也不使用 Xposed / LSPosed / VPN / 中间人代理。请先安装并启动 [Shizuku](https://github.com/RikkaApps/Shizuku)。

## 两种模式

- **全局屏蔽（默认，推荐用于拦「摇一摇」）**  
  平时把 SensorService 限制为只有本应用可读取，这样其它应用（包括还在启动页里的广告）都读不到运动传感器。把需要指南针或体感的应用（地图、体感游戏等）加入 **允许列表** 后，当这些应用位于前台时，会临时改为仅该应用可读取；离开后再恢复全局屏蔽。
- **列表屏蔽**  
  平时保持传感器正常；只有封锁名单中的应用出现在前台时才进入受限模式。

首次启动时，会建议把常见的购物、短视频、新闻应用（淘宝、拼多多、抖音、快手、今日头条等）加入封锁名单，方便以后切换到「列表屏蔽」。**地图（高德 / 百度 / 腾讯）、支付宝、微信、QQ 不会被默认勾选**——地图需要指南针和陀螺仪。

## 如何安装

1. 从本仓库的 [Releases](https://github.com/wusui1325-dotcom/sensor-blocker/releases) 下载 `sensor-blocker-1.0.0.apk` 并安装。若系统提示「未知来源」，按提示允许。
2. 安装并启动 Shizuku（见下一节）。
3. 打开「传感屏蔽」，授权 Shizuku，打开「监控」。
4. （可选）授予「使用情况访问」；若使用「列表屏蔽」，建议再打开无障碍中的「前台应用检测」，以便窗口一切换就生效。Android 13 及以上需要通知权限，用于前台服务常驻通知。

## 如何启动 Shizuku（国内用户）

1. 从官方 GitHub 发布页安装 Shizuku：<https://github.com/RikkaApps/Shizuku/releases>。国内使用 Google Play 可能不便，建议直接用 GitHub。
2. Android 11 及以上：在系统设置里打开「开发者选项」→「无线调试」，再按 Shizuku 应用内说明，用无线调试启动并完成配对。
3. 回到本应用，点击「授权 Shizuku」并允许。
4. 重启手机后，无线调试通常会关闭，需要重新启动 Shizuku。若本应用的监控是打开的，会在 Shizuku 再次就绪后尝试重新应用受限模式。

## 限制（请先读）

- 本应用只控制运动传感器访问，**不是通用广告屏蔽工具**。不用这些传感器的广告仍会出现。
- 部分厂商可能改动 SensorService，效果因设备而异，不做「全机型保证」。
- **重启后受限模式通常会被清除**。监控打开时，应用会在启动后、以及 Shizuku 重新连接时尝试再次应用。
- 在「全局屏蔽」下，若不把地图或体感游戏加入允许列表，导航指南针或体感操作会失效。
- 需要 Shizuku 或无线调试，不需要 root。
- 本仓库的维护者无法在你的手机上实地测试每一家 OEM。

## 编译

需要 JDK 17 和 Android SDK（compileSdk 35）。

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

发布包必须通过环境变量签名，仓库里 **不会** 存放密钥或密码：

```
KEYSTORE_FILE
KEYSTORE_PASSWORD
KEY_ALIAS
KEY_PASSWORD
```

未设置这些变量时，`assembleRelease` 会生成未签名包（供 CI 使用），不会在构建脚本里写入任何默认密码。

## License

[MIT](LICENSE)

---

# Sensor Blocker

Block other apps from reading the accelerometer, gyroscope, and magnetometer **without root**, using at most [Shizuku](https://github.com/RikkaApps/Shizuku) (ADB shell). The usual goal is to stop “shake to open” ads; you can also keep motion sensors off more generally.

## How it works

Those sensors are normally zero-permission on stock Android. This app uses the public Android **SensorService restricted mode**:

- `dumpsys sensorservice restrict <package>` — only that package may read sensors
- `dumpsys sensorservice enable` — restore normal mode

This is a public SensorService feature, not a third-party private API. Some OEMs change or hide the service, so results vary.

A `service call sensor_privacy` path exists only as a fallback. On AOSP that interface is primarily a camera/microphone toggle and is not a reliable way to block motion sensors.

## Modes

- **Global block (default)** — keep SensorService restricted to this app, with an allowlist (maps, motion games). When an allowlisted app is in the foreground, restriction is temporarily moved to that app; when it leaves, the global block returns.
- **List block** — sensors stay normal until a blocklisted app is in the foreground.

First-launch suggestions cover shopping / short-video / news apps for the block list. Maps, Alipay, WeChat, and QQ are never auto-checked.

## Limitations

This is not a general ad blocker. Reboot usually clears restricted mode until Shizuku is running again and the app re-applies it. Allowlist maps or games, or navigation/compass will break. Shizuku (or wireless debugging) is required. Effectiveness is not guaranteed on every OEM.

## Build

JDK 17, Android SDK compileSdk 35. See the environment-variable signing notes above. Do not put a keystore or password in the repository.
