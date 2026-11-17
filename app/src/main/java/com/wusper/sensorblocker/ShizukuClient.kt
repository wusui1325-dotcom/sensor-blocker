package com.wusper.sensorblocker

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import com.wusper.sensorblocker.shell.IShellService
import com.wusper.sensorblocker.shell.ShellUserService
import rikka.shizuku.Shizuku
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object ShizukuClient {
    const val REQUEST_CODE_PERMISSION = 1001
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val BIND_TIMEOUT_MS = 4_000L
    private const val MAIN_DISPATCH_TIMEOUT_MS = 2_000L

    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val readyWaiters = CopyOnWriteArrayList<CountDownLatch>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile var shell: IShellService? = null
        private set
    @Volatile private var bound = false
    @Volatile private var bindStartedAt = 0L
    @Volatile var lastBindError: String? = null
        private set
    @Volatile var lastPermissionError: String? = null
        private set
    private var appContext: Context? = null

    private val binderReceived = Shizuku.OnBinderReceivedListener {
        lastPermissionError = null
        permissionState()
        notifyChanged()
        bindIfPossible()
    }

    private val binderDead = Shizuku.OnBinderDeadListener {
        shell = null
        bound = false
        bindStartedAt = 0L
        lastBindError = "Shizuku binder died"
        releaseWaiters()
        notifyChanged()
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        lastPermissionError = null
        notifyChanged()
        bindIfPossible()
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            shell = IShellService.Stub.asInterface(service)
            lastBindError = null
            releaseWaiters()
            notifyChanged()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            shell = null
            bound = false
            bindStartedAt = 0L
            lastBindError = "UserService disconnected"
            releaseWaiters()
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

    private fun releaseWaiters() {
        readyWaiters.forEach { it.countDown() }
    }

    fun isBinderAlive(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun permissionState(): ShizukuPermissionState {
        if (!isBinderAlive()) return ShizukuPermissionState.NO_BINDER
        return try {
            checkPermissionOnce()
        } catch (_: Throwable) {
            // Binder may have just become sticky; retry once before surfacing an error.
            try {
                if (!isBinderAlive()) return ShizukuPermissionState.NO_BINDER
                checkPermissionOnce()
            } catch (retry: Throwable) {
                lastPermissionError = shortError(retry)
                ShizukuPermissionState.CHECK_FAILED
            }
        }
    }

    private fun checkPermissionOnce(): ShizukuPermissionState {
        val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        lastPermissionError = null
        return if (granted) ShizukuPermissionState.GRANTED else ShizukuPermissionState.DENIED
    }

    fun hasPermission(): Boolean = permissionState() == ShizukuPermissionState.GRANTED

    fun isReady(): Boolean = hasPermission() && shell != null

    fun snapshot(): ShizukuSnapshot {
        val now = SystemClock.elapsedRealtime()
        val inProgress = bound && shell == null && (now - bindStartedAt) < BIND_TIMEOUT_MS
        return ShizukuSnapshot(
            binderAlive = isBinderAlive(),
            permission = permissionState(),
            shellConnected = shell != null,
            bindInProgress = inProgress,
            lastBindError = lastBindError,
            lastPermissionError = lastPermissionError
        )
    }

    fun requestPermission() {
        if (!isBinderAlive()) return
        try {
            if (Shizuku.isPreV11()) return
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
            } else {
                bindIfPossible(force = true)
            }
        } catch (t: Throwable) {
            lastPermissionError = shortError(t)
            notifyChanged()
        }
    }

    fun bindIfPossible(force: Boolean = false) {
        runOnMain(wait = false) { bindOnMain(force) }
    }

    /**
     * Bind the UserService on the main thread. Background callers wait until
     * onServiceConnected, a bind failure, or [timeoutMs]. The main thread never
     * blocks waiting for the connection, to avoid ANRs.
     */
    fun bindAndWait(timeoutMs: Long = BIND_TIMEOUT_MS): Boolean {
        if (isReady()) return true
        if (!MainThreadBindPolicy.mayBlockForConnection(isMainThread())) {
            bindOnMain(force = false)
            return isReady()
        }
        val latch = CountDownLatch(1)
        readyWaiters.add(latch)
        try {
            runOnMain(wait = true, timeoutMs = MAIN_DISPATCH_TIMEOUT_MS) { bindOnMain(force = false) }
            if (isReady()) return true
            latch.await(timeoutMs, TimeUnit.MILLISECONDS)
            if (!isReady() && lastBindError == null) {
                lastBindError = "UserService did not connect"
                notifyChanged()
            }
            return isReady()
        } finally {
            readyWaiters.remove(latch)
        }
    }

    fun unbind() {
        runOnMain(wait = false) { unbindOnMain() }
    }

    fun unbindAndWait(timeoutMs: Long = MAIN_DISPATCH_TIMEOUT_MS) {
        if (isMainThread()) {
            unbindOnMain()
            return
        }
        runOnMain(wait = true, timeoutMs = timeoutMs) { unbindOnMain() }
    }

    private fun isMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()

    private fun runOnMain(wait: Boolean, timeoutMs: Long = MAIN_DISPATCH_TIMEOUT_MS, block: () -> Unit) {
        when (MainThreadBindPolicy.action(isMainThread(), wait)) {
            MainThreadAction.RUN_INLINE -> block()
            MainThreadAction.POST -> mainHandler.post(block)
            MainThreadAction.POST_AND_WAIT -> {
                val done = CountDownLatch(1)
                val error = AtomicReference<Throwable>()
                mainHandler.post {
                    try {
                        block()
                    } catch (t: Throwable) {
                        error.set(t)
                    } finally {
                        done.countDown()
                    }
                }
                if (!done.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                    lastBindError = "main-thread dispatch timed out"
                    notifyChanged()
                    return
                }
                error.get()?.let { t ->
                    lastBindError = shortError(t)
                    notifyChanged()
                }
            }
        }
    }

    private fun bindOnMain(force: Boolean) {
        val ctx = appContext ?: return
        if (!isMainThread()) {
            lastBindError = "bind called off the main thread"
            notifyChanged()
            return
        }
        if (!hasPermission()) return
        if (shell != null && !force) return
        val now = SystemClock.elapsedRealtime()
        if (bound && !force) {
            if (now - bindStartedAt < BIND_TIMEOUT_MS) return
            lastBindError = "UserService did not connect"
            unbindOnMain()
        }
        if (force && (bound || shell != null)) {
            unbindOnMain()
        }
        lastBindError = null
        try {
            Shizuku.bindUserService(userServiceArgs(ctx), connection)
            bound = true
            bindStartedAt = SystemClock.elapsedRealtime()
            notifyChanged()
        } catch (t: Throwable) {
            bound = false
            bindStartedAt = 0L
            lastBindError = shortError(t)
            releaseWaiters()
            notifyChanged()
        }
    }

    private fun unbindOnMain() {
        val ctx = appContext ?: return
        try {
            if (bound) {
                Shizuku.unbindUserService(userServiceArgs(ctx), connection, true)
            }
        } catch (_: Throwable) {
            // Already gone
        }
        bound = false
        bindStartedAt = 0L
        shell = null
    }

    private fun userServiceArgs(ctx: Context): Shizuku.UserServiceArgs {
        return Shizuku.UserServiceArgs(ComponentName(ctx, ShellUserService::class.java))
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
            .tag("sensorblocker")
    }

    fun cancel() {
        try {
            shell?.cancel()
        } catch (_: Exception) {
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

    private fun shortError(t: Throwable): String {
        val name = t.javaClass.simpleName.ifBlank { t.javaClass.name }
        val msg = t.message?.trim().orEmpty()
        return if (msg.isEmpty()) name else "$name: ${msg.take(80)}"
    }
}
