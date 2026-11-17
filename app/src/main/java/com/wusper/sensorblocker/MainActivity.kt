package com.wusper.sensorblocker

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: AppListAdapter
    private lateinit var statusShizuku: TextView
    private lateinit var statusSensor: TextView
    private lateinit var statusMonitor: TextView
    private lateinit var statusForeground: TextView
    private lateinit var modeExplain: TextView
    private lateinit var listTitle: TextView
    private lateinit var monitorSwitch: MaterialSwitch
    private lateinit var modeGroup: MaterialButtonToggleGroup
    private lateinit var btnGrantShizuku: MaterialButton
    private lateinit var usageChip: TextView
    private lateinit var accessibilityChip: TextView
    private lateinit var notificationChip: TextView
    private var apps: List<AppInfo> = emptyList()
    private var updatingSwitch = false
    private var updatingMode = false
    private val bgExecutor = Executors.newSingleThreadExecutor()
    private val loadGeneration = AtomicInteger(0)
    private var firstResume = true

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            refreshStatus()
            if (granted && pendingStart) {
                pendingStart = false
                reallyStartMonitoring()
            }
        }

    private var pendingStart = false

    private val shizukuListener: () -> Unit = {
        runOnUiThread {
            maybeResumeMonitoring()
            refreshStatus()
        }
    }
    private val stateListener: (MonitorState) -> Unit = { runOnUiThread { bindMonitorState(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        statusShizuku = findViewById(R.id.statusShizuku)
        statusSensor = findViewById(R.id.statusSensor)
        statusMonitor = findViewById(R.id.statusMonitor)
        statusForeground = findViewById(R.id.statusForeground)
        modeExplain = findViewById(R.id.modeExplain)
        listTitle = findViewById(R.id.listTitle)
        monitorSwitch = findViewById(R.id.monitorSwitch)
        modeGroup = findViewById(R.id.modeGroup)
        btnGrantShizuku = findViewById(R.id.btnGrantShizuku)
        usageChip = findViewById(R.id.chipUsage)
        accessibilityChip = findViewById(R.id.chipAccessibility)
        notificationChip = findViewById(R.id.chipNotification)

        adapter = AppListAdapter { app, checked ->
            val mode = AppPreferences.blockMode(this)
            if (mode == BlockMode.GLOBAL) {
                val set = AppPreferences.allowedPackages(this).toMutableSet()
                if (checked) set.add(app.packageName) else set.remove(app.packageName)
                AppPreferences.setAllowedPackages(this, set)
            } else {
                val set = AppPreferences.blockedPackages(this).toMutableSet()
                if (checked) set.add(app.packageName) else set.remove(app.packageName)
                AppPreferences.setBlockedPackages(this, set)
            }
        }
        val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.appList)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        recycler.setHasFixedSize(true)

        findViewById<TextInputEditText>(R.id.searchInput).doAfterTextChanged {
            adapter.filter(it?.toString().orEmpty())
        }

        modeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || updatingMode) return@addOnButtonCheckedListener
            val mode = if (checkedId == R.id.btnModeGlobal) BlockMode.GLOBAL else BlockMode.LIST
            AppPreferences.setBlockMode(this, mode)
            bindMode(mode)
            reloadList()
        }

        monitorSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (updatingSwitch) return@setOnCheckedChangeListener
            if (isChecked) startMonitoring() else SensorMonitorService.stop(this)
            refreshStatus()
        }

        btnGrantShizuku.setOnClickListener { onShizukuButton() }
        findViewById<View>(R.id.btnShizukuHelp).setOnClickListener { showShizukuHelp() }
        usageChip.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        accessibilityChip.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        notificationChip.setOnClickListener { requestNotificationPermission() }

        bindMode(AppPreferences.blockMode(this))

        ShizukuClient.addListener(shizukuListener)
        MonitorStateHub.addListener(stateListener)
        maybeResumeMonitoring()
    }

    override fun onDestroy() {
        ShizukuClient.removeListener(shizukuListener)
        MonitorStateHub.removeListener(stateListener)
        bgExecutor.shutdown()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        ShizukuClient.bindIfPossible()
        maybeResumeMonitoring()
        refreshStatus()
        val first = firstResume
        firstResume = false
        reloadAppsAsync(thenFirstRun = first)
    }

    private fun reloadAppsAsync(thenFirstRun: Boolean) {
        val gen = loadGeneration.incrementAndGet()
        bgExecutor.execute {
            ForegroundCatalog.refresh(this)
            val loaded = loadApps()
            runOnUiThread {
                if (isFinishing || isDestroyed || gen != loadGeneration.get()) return@runOnUiThread
                apps = loaded
                reloadList()
                if (thenFirstRun) maybeShowFirstLaunch()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_shizuku_setup -> showShizukuHelp()
            R.id.action_limitations -> showLimitations()
            R.id.action_usage -> startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            R.id.action_accessibility -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun bindMode(mode: BlockMode) {
        updatingMode = true
        modeGroup.check(if (mode == BlockMode.GLOBAL) R.id.btnModeGlobal else R.id.btnModeList)
        updatingMode = false
        modeExplain.setText(
            if (mode == BlockMode.GLOBAL) R.string.mode_global_explain else R.string.mode_list_explain
        )
        listTitle.setText(
            if (mode == BlockMode.GLOBAL) R.string.list_title_allow else R.string.list_title_block
        )
    }

    private fun reloadList() {
        val mode = AppPreferences.blockMode(this)
        val selected = if (mode == BlockMode.GLOBAL) {
            AppPreferences.allowedPackages(this)
        } else {
            AppPreferences.blockedPackages(this)
        }
        adapter.submit(apps, selected, allowMode = mode == BlockMode.GLOBAL)
    }

    private fun startMonitoring() {
        if (!ShizukuClient.isBinderAlive()) {
            resetSwitch(false)
            Toast.makeText(this, R.string.need_shizuku_to_monitor, Toast.LENGTH_LONG).show()
            showShizukuHelp()
            return
        }
        if (!ShizukuClient.hasPermission()) {
            resetSwitch(false)
            ShizukuClient.requestPermission()
            Toast.makeText(this, R.string.need_shizuku_to_monitor, Toast.LENGTH_LONG).show()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission()) {
            pendingStart = true
            requestNotificationPermission()
            resetSwitch(false)
            return
        }
        reallyStartMonitoring()
    }

    private fun reallyStartMonitoring() {
        ShizukuClient.bindIfPossible()
        SensorMonitorService.startSafely(this)
        refreshStatus()
        maybeOfferBatteryOptimization()
    }

    private fun maybeResumeMonitoring() {
        if (!AppPreferences.isMonitoringEnabled(this) || SensorMonitorService.isRunning) return
        if (!ShizukuClient.isReady()) {
            ShizukuClient.bindIfPossible()
            return
        }
        SensorMonitorService.startSafely(this)
    }

    private fun resetSwitch(checked: Boolean) {
        updatingSwitch = true
        monitorSwitch.isChecked = checked
        updatingSwitch = false
    }

    private fun onShizukuButton() {
        if (!ShizukuClient.isBinderAlive()) {
            val launch = packageManager.getLaunchIntentForPackage(ShizukuClient.SHIZUKU_PACKAGE)
            if (launch != null) {
                startActivity(launch)
            } else {
                showShizukuHelp()
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_RELEASES)))
                } catch (_: Exception) {
                    Toast.makeText(this, R.string.toast_shizuku_not_installed, Toast.LENGTH_LONG).show()
                }
            }
        } else if (!ShizukuClient.hasPermission()) {
            ShizukuClient.requestPermission()
        } else {
            ShizukuClient.bindIfPossible()
            refreshStatus()
        }
    }

    private fun refreshStatus() {
        val bound = ShizukuClient.isBinderAlive()
        val granted = ShizukuClient.hasPermission()
        val ready = ShizukuClient.isReady()
        statusShizuku.setText(
            when {
                ready -> R.string.status_shizuku_ready
                bound && !granted -> R.string.status_shizuku_no_permission
                bound -> R.string.status_shizuku_no_permission
                else -> R.string.status_shizuku_disconnected
            }
        )
        btnGrantShizuku.visibility = if (ready) View.GONE else View.VISIBLE
        btnGrantShizuku.setText(
            if (!bound) R.string.open_shizuku else R.string.grant_shizuku
        )

        val mode = MonitorStateHub.latest?.sensorMode ?: SensorServiceMode.Unknown
        bindSensorMode(mode)
        val monitoring = SensorMonitorService.isRunning || AppPreferences.isMonitoringEnabled(this)
        statusMonitor.setText(if (SensorMonitorService.isRunning) R.string.status_monitoring_on else R.string.status_monitoring_off)
        resetSwitch(SensorMonitorService.isRunning || AppPreferences.isMonitoringEnabled(this))
        val fg = ForegroundTracker.current
        if (fg.isNullOrBlank()) {
            statusForeground.visibility = View.GONE
        } else {
            statusForeground.visibility = View.VISIBLE
            statusForeground.text = getString(R.string.status_foreground, fg)
        }
        usageChip.isSelected = hasUsageAccess()
        accessibilityChip.isSelected = WindowChangeAccessibilityService.isEnabled(this)
        notificationChip.isSelected = hasNotificationPermission()
        usageChip.setText(if (hasUsageAccess()) R.string.permission_usage_on else R.string.permission_usage_off)
        accessibilityChip.setText(
            if (WindowChangeAccessibilityService.isEnabled(this)) R.string.permission_accessibility_on
            else R.string.permission_accessibility_off
        )
        notificationChip.setText(
            if (hasNotificationPermission()) R.string.permission_notifications_on
            else R.string.permission_notifications_off
        )
        if (monitoring && !SensorMonitorService.isRunning) {
            // Preference says on, service not yet running (e.g. waiting for Shizuku after boot).
            statusMonitor.setText(R.string.status_monitoring_pending)
        }
    }

    private fun bindMonitorState(state: MonitorState) {
        bindSensorMode(state.sensorMode)
        refreshStatus()
        if (state.usedPrivacyFallback) {
            statusSensor.append("\n" + getString(R.string.status_privacy_fallback))
        }
    }

    private fun bindSensorMode(mode: SensorServiceMode) {
        statusSensor.text = when (mode) {
            is SensorServiceMode.Normal -> getString(R.string.status_mode_normal)
            is SensorServiceMode.Restricted -> {
                val pkg = mode.packageName ?: getString(R.string.app_name)
                getString(R.string.status_mode_restricted, pkg)
            }
            is SensorServiceMode.DataInjection -> getString(R.string.status_mode_unknown)
            is SensorServiceMode.Unknown -> getString(R.string.status_mode_unknown)
        }
    }

    private fun maybeShowFirstLaunch() {
        if (AppPreferences.recommendationShown(this)) return
        val mode = AppPreferences.blockMode(this)
        val offer = FirstRunSeeds.forMode(mode, apps.map { it.packageName }.toSet())
        if (offer.isEmpty) {
            AppPreferences.setRecommendationShown(this, true)
            return
        }
        val labels = apps.associate { it.packageName to it.label }
        fun bullets(packages: Set<String>): String {
            return packages
                .map { "• ${labels[it] ?: it}" }
                .sorted()
                .joinToString("\n")
        }
        val allowNames = bullets(offer.allowPackages)
        val blockNames = bullets(offer.blockPackages)
        val title = if (mode == BlockMode.GLOBAL && offer.allowPackages.isNotEmpty()) {
            R.string.first_run_title_global
        } else {
            R.string.first_run_title
        }
        val message = when {
            offer.allowPackages.isNotEmpty() && offer.blockPackages.isNotEmpty() ->
                getString(R.string.first_run_message_global, allowNames, blockNames)
            offer.allowPackages.isNotEmpty() ->
                getString(R.string.first_run_message_global_allow_only, allowNames)
            else -> getString(R.string.first_run_message, blockNames)
        }
        val positive = if (offer.allowPackages.isNotEmpty()) {
            R.string.first_run_add_global
        } else {
            R.string.first_run_add
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positive) { _, _ ->
                if (offer.allowPackages.isNotEmpty()) {
                    val set = AppPreferences.allowedPackages(this).toMutableSet()
                    set.addAll(offer.allowPackages)
                    AppPreferences.setAllowedPackages(this, set)
                }
                if (offer.blockPackages.isNotEmpty()) {
                    val set = AppPreferences.blockedPackages(this).toMutableSet()
                    set.addAll(offer.blockPackages)
                    AppPreferences.setBlockedPackages(this, set)
                }
                AppPreferences.setRecommendationShown(this, true)
                reloadList()
                val added = offer.allowPackages.size + offer.blockPackages.size
                Toast.makeText(this, getString(R.string.first_run_added, added), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.first_run_later) { _, _ ->
                AppPreferences.setRecommendationShown(this, true)
            }
            .setCancelable(false)
            .show()
    }

    private fun maybeOfferBatteryOptimization() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (AppPreferences.batteryHintShown(this)) return
        val pm = getSystemService(PowerManager::class.java) ?: return
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            AppPreferences.setBatteryHintShown(this, true)
            return
        }
        AppPreferences.setBatteryHintShown(this, true)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.battery_title)
            .setMessage(R.string.battery_message)
            .setPositiveButton(R.string.battery_open_settings) { _, _ ->
                try {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            .setData(Uri.parse("package:$packageName"))
                    )
                } catch (_: Exception) {
                    try {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (_: Exception) {
                    }
                }
            }
            .setNegativeButton(R.string.first_run_later, null)
            .show()
    }

    private fun showShizukuHelp() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.shizuku_setup_title)
            .setMessage(R.string.shizuku_setup_message)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.open_shizuku_github) { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_RELEASES)))
                } catch (_: Exception) {
                    Toast.makeText(this, R.string.toast_shizuku_not_installed, Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    private fun showLimitations() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.limitations_title)
            .setMessage(R.string.limitations_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun loadApps(): List<AppInfo> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val seen = HashSet<String>()
        val list = mutableListOf<AppInfo>()
        val resolved = pm.queryIntentActivities(intent, 0)
        for (ri in resolved) {
            val pkg = ri.activityInfo.packageName
            if (pkg == packageName || !seen.add(pkg)) continue
            val appInfo = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (_: PackageManager.NameNotFoundException) {
                continue
            }
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            list.add(
                AppInfo(
                    packageName = pkg,
                    label = ri.loadLabel(pm).toString(),
                    isSystem = isSystem,
                    isRecommendedBlock = RecommendedApps.isBlockCandidate(pkg),
                    isRecommendedAllow = RecommendedApps.isAllowCandidate(pkg)
                )
            )
        }
        return list.sortedWith(
            compareBy<AppInfo> { it.isSystem }
                .thenBy { it.label.lowercase() }
        )
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        private const val SHIZUKU_RELEASES = "https://github.com/RikkaApps/Shizuku/releases"
    }
}
