package com.wusper.sensorblocker

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: AppListAdapter
    private val blocked = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        recycler = findViewById(R.id.appList)
        recycler.layoutManager = LinearLayoutManager(this)

        loadBlocked()
        adapter = AppListAdapter(loadApps(), blocked) { pkg, isBlocked ->
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
            statusText.text = "監控已啟動"
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, SensorMonitorService::class.java))
            statusText.text = "監控已停止"
        }

        findViewById<Button>(R.id.btnUsage).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        updateShizukuStatus()
    }

    private fun updateShizukuStatus() {
        val available = ShizukuSensorController.isShizukuAvailable()
        statusText.text = if (available) "Shizuku 可用" else "Shizuku 未連線"
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
        val prefs = getSharedPreferences("sensor_blocker", MODE_PRIVATE)
        blocked.clear()
        blocked.addAll(prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet())
    }

    private fun saveBlocked() {
        getSharedPreferences("sensor_blocker", MODE_PRIVATE)
            .edit()
            .putStringSet("blocked_packages", blocked)
            .apply()
    }
}

data class AppInfo(val packageName: String, val label: String)
