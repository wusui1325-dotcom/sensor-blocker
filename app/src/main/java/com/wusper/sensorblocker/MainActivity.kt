package com.wusper.sensorblocker

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: AppListAdapter
    private val blocked = mutableSetOf<String>()
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("sensor_blocker", MODE_PRIVATE)

        statusText = findViewById(R.id.statusText)
        recycler = findViewById(R.id.appList)
        recycler.layoutManager = LinearLayoutManager(this)

        loadBlocked()
        val apps = loadApps()
        adapter = AppListAdapter(apps, blocked) { pkg, isBlocked ->
            if (isBlocked) blocked.add(pkg) else blocked.remove(pkg)
            saveBlocked()
        }
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (!ShizukuSensorController.isShizukuAvailable()) {
                Toast.makeText(this, "請先啟動並授權 Shizuku", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            startService(Intent(this, SensorMonitorService::class.java))
            statusText.text = "監控已啟動（精準模式）"
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, SensorMonitorService::class.java))
            statusText.text = "監控已停止"
        }

        findViewById<Button>(R.id.btnUsage).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        updateShizukuStatus()

        // First launch recommendation
        if (!prefs.getBoolean("recommendation_shown", false)) {
            showRecommendationDialog(apps)
        }
    }

    private fun showRecommendationDialog(apps: List<AppInfo>) {
        val installedRecommended = apps.filter { RecommendedApps.packages.contains(it.packageName) }
        if (installedRecommended.isEmpty()) {
            prefs.edit().putBoolean("recommendation_shown", true).apply()
            return
        }

        val names = installedRecommended.joinToString("\n") { "• ${it.label}" }
        AlertDialog.Builder(this)
            .setTitle("推薦開啟屏蔽")
            .setMessage("偵測到以下國產購物、影片與地圖 App，建議預設開啟感測器屏蔽：\n\n$names\n\n是否一鍵加入封鎖名單？")
            .setPositiveButton("一鍵開啟") { _, _ ->
                installedRecommended.forEach { blocked.add(it.packageName) }
                saveBlocked()
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "已加入 ${installedRecommended.size} 個推薦 App", Toast.LENGTH_SHORT).show()
                prefs.edit().putBoolean("recommendation_shown", true).apply()
            }
            .setNegativeButton("稍後") { _, _ ->
                prefs.edit().putBoolean("recommendation_shown", true).apply()
            }
            .setCancelable(false)
            .show()
    }

    private fun updateShizukuStatus() {
        val available = ShizukuSensorController.isShizukuAvailable()
        statusText.text = if (available) "Shizuku 可用（精準偵測已啟用）" else "Shizuku 未連線"
    }

    private fun loadApps(): List<AppInfo> {
        val pm = packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != packageName }
            .map {
                AppInfo(
                    packageName = it.packageName,
                    label = pm.getApplicationLabel(it).toString()
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun loadBlocked() {
        blocked.clear()
        blocked.addAll(prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet())
    }

    private fun saveBlocked() {
        prefs.edit()
            .putStringSet("blocked_packages", blocked)
            .apply()
    }
}
