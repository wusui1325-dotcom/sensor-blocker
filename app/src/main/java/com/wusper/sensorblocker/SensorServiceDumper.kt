package com.wusper.sensorblocker

import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicReference

/**
 * Applies and reads SensorService restricted mode through Shizuku's
 * public binder API: wrap [SystemServiceHelper.getSystemService]
 * with [ShizukuBinderWrapper] and issue [IBinder.DUMP_TRANSACTION]
 * (`restrict <pkg>`, `enable`, or a full dump).
 *
 * [ShizukuBinderWrapper.dump] / [ShizukuBinderWrapper.dumpAsync] are
 * public but forward to the *unwrapped* binder, so they would run as
 * the app uid. [ShizukuBinderWrapper.transact] is the method that is
 * actually proxied through Shizuku, and is how a privileged dump is
 * performed without starting a UserService process.
 */
object SensorServiceDumper {
    const val DEFAULT_TIMEOUT_MS = 10_000L
    const val CONFIRM_TIMEOUT_MS = 3_000L

    fun dump(
        args: Array<String>,
        stopAtMode: Boolean,
        maxBytes: Int = BinderDumpReader.MAX_BYTES,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): String {
        val raw = SystemServiceHelper.getSystemService("sensorservice")
            ?: throw IllegalStateException("sensorservice missing")
        val binder = ShizukuBinderWrapper(raw)
        return dumpBinder(binder, args, stopAtMode, maxBytes, timeoutMs)
    }

    internal fun dumpBinder(
        binder: IBinder,
        args: Array<String>,
        stopAtMode: Boolean,
        maxBytes: Int = BinderDumpReader.MAX_BYTES,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): String {
        val ends = ParcelFileDescriptor.createPipe()
        val readPfd = ends[0]
        val writePfd = ends[1]
        val output = StringBuilder()
        val readerError = AtomicReference<Throwable>()
        val reader = Thread({
            try {
                FileInputStream(readPfd.fileDescriptor).bufferedReader().use { input ->
                    output.append(
                        BinderDumpReader.read(
                            input,
                            maxBytes,
                            if (stopAtMode) BinderDumpReader::hasModeLine else { _ -> false }
                        )
                    )
                }
            } catch (t: Throwable) {
                readerError.set(t)
            } finally {
                try {
                    readPfd.close()
                } catch (_: Exception) {
                }
            }
        }, "sensorservice-dump-read")
        reader.start()
        var writeClosed = false
        try {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeFileDescriptor(writePfd.fileDescriptor)
                data.writeStringArray(args)
                binder.transact(IBinder.DUMP_TRANSACTION, data, reply, 0)
                try {
                    reply.readException()
                } catch (_: RuntimeException) {
                    // Some implementations write no exception header on dump.
                }
            } finally {
                data.recycle()
                reply.recycle()
                try {
                    writePfd.close()
                    writeClosed = true
                } catch (_: Exception) {
                }
            }
            if (!joinReader(reader, timeoutMs)) {
                throw IllegalStateException("sensorservice dump timed out")
            }
            readerError.get()?.let { throw it }
            return output.toString()
        } finally {
            if (!writeClosed) {
                try {
                    writePfd.close()
                } catch (_: Exception) {
                }
            }
            if (reader.isAlive) {
                try {
                    readPfd.close()
                } catch (_: Exception) {
                }
                reader.join(500)
            }
        }
    }

    private fun joinReader(reader: Thread, timeoutMs: Long): Boolean {
        reader.join(timeoutMs)
        return !reader.isAlive
    }
}
