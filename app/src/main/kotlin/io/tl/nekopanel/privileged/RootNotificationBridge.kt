package io.tl.nekopanel.privileged

import android.net.LocalSocket
import android.net.LocalSocketAddress
import java.io.DataOutputStream
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class RootNotificationBridge(
    private val apkPath: String,
) : TrafficMonitorEngine.NotificationSink, AutoCloseable {
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "NekoPanel-RootNotificationBridge").apply { isDaemon = true }
    }
    private var process: Process? = null
    private var socket: LocalSocket? = null
    private var output: DataOutputStream? = null
    @Volatile
    private var closed = false

    override fun update(content: String) {
        submit(COMMAND_UPDATE, content)
    }

    override fun cancel() {
        submit(COMMAND_CANCEL, "")
    }

    private fun submit(command: Int, content: String) {
        if (closed) return
        executor.execute { write(command, content) }
    }

    private fun write(command: Int, content: String) {
        if (closed) return
        if (output == null && !connect()) return
        runCatching {
            output?.writeInt(command)
            output?.writeUTF(content)
            output?.flush()
        }.onFailure {
            disconnect()
        }
    }

    private fun connect(): Boolean {
        disconnect()
        process = ProcessBuilder(
            "/system/bin/app_process",
            "-Djava.class.path=$apkPath",
            "/system/bin",
            ShellNotificationMain::class.java.name,
        )
            .redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
            .redirectOutput(ProcessBuilder.Redirect.to(File("/dev/null")))
            .redirectError(ProcessBuilder.Redirect.to(File("/dev/null")))
            .start()
        repeat(20) {
            if (closed || process?.isAlive != true) return false
            val candidate = LocalSocket()
            if (runCatching {
                    candidate.connect(LocalSocketAddress(SOCKET_NAME))
                    candidate.soTimeout = 2_000
                }.isSuccess) {
                socket = candidate
                output = DataOutputStream(candidate.outputStream)
                return true
            }
            runCatching { candidate.close() }
            Thread.sleep(100)
        }
        disconnect()
        process?.destroyForcibly()
        process = null
        return false
    }

    private fun disconnect() {
        runCatching { output?.close() }
        runCatching { socket?.close() }
        output = null
        socket = null
    }

    override fun close() {
        if (closed) return
        closed = true
        executor.execute {
            runCatching {
                output?.writeInt(COMMAND_STOP)
                output?.writeUTF("")
                output?.flush()
            }
            disconnect()
            process?.let {
                if (!it.waitFor(500, TimeUnit.MILLISECONDS)) it.destroyForcibly()
            }
            process = null
        }
        executor.shutdown()
        if (!executor.awaitTermination(2, TimeUnit.SECONDS)) executor.shutdownNow()
    }

    companion object {
        const val SOCKET_NAME = "NekoPanel_PrivilegedNotification"
        const val COMMAND_UPDATE = 1
        const val COMMAND_CANCEL = 2
        const val COMMAND_STOP = 3
    }
}
