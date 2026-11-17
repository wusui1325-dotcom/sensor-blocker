package com.wusper.sensorblocker

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.wusper.sensorblocker.shell.IShellService
import com.wusper.sensorblocker.shell.ShellUserService
import rikka.shizuku.Shizuku
import java.util.concurrent.CopyOnWriteArrayList

object ShizukuClient {
    const val REQUEST_CODE_PERMISSION = 1001
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    @Volatile var shell: IShellService? = null
        private set
    @Volatile private var bound = false
    private var appContext: Context? = null

    private val binderReceived = Shizuku.OnBinderReceivedListener {
        notifyChanged()
        bindIfPossible()
    }

    private val binderDead = Shizuku.OnBinderDeadListener {
        shell = null
        bound = false
        notifyChanged()
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        notifyChanged()
        bindIfPossible()
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            shell = IShellService.Stub.asInterface(service)
            notifyChanged()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            shell = null
            bound = false
            notifyChanged()
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        bindIfPossible()
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyChanged() {
        listeners.forEach { it() }
    }

    fun isBinderAlive(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            isBinderAlive() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun isReady(): Boolean = hasPermission() && shell != null

    fun requestPermission() {
        if (!isBinderAlive()) return
        try {
            if (Shizuku.isPreV11()) return
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
            } else {
                bindIfPossible()
            }
        } catch (_: Throwable) {
            // Shizuku not present
        }
    }

    @Synchronized
    fun bindIfPossible() {
        val ctx = appContext ?: return
        if (!hasPermission() || bound) return
        val args = Shizuku.UserServiceArgs(ComponentName(ctx, ShellUserService::class.java))
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
            .tag("sensorblocker")
        try {
            Shizuku.bindUserService(args, connection)
            bound = true
        } catch (_: Throwable) {
            bound = false
        }
    }

    fun exec(command: String): ShellResult {
        val service = shell ?: return ShellResult(-1, "user service not bound")
        return try {
            ShellOutput.parse(service.exec(command) ?: "")
        } catch (e: Exception) {
            ShellResult(-1, e.message ?: "exec failed")
        }
    }

}
