package com.wusper.sensorblocker

import android.app.Application

class SensorBlockerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ShizukuClient.init(this)
    }
}
