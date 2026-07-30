package io.tl.nekopanel.privileged

import android.net.LocalServerSocket
import android.os.Looper
import android.system.Os
import java.io.DataInputStream
import kotlin.system.exitProcess

object ShellNotificationMain {
    @JvmStatic
    fun main(args: Array<String>) {
        if (Os.getuid() != SHELL_UID) Os.setuid(SHELL_UID)
        Looper.prepare()
        val publisher = ShellNotificationPublisher()
        try {
            LocalServerSocket(RootNotificationBridge.SOCKET_NAME).use { server ->
                server.accept().use { socket ->
                    DataInputStream(socket.inputStream).use { input ->
                        while (true) {
                            when (input.readInt()) {
                                RootNotificationBridge.COMMAND_UPDATE -> publisher.update(input.readUTF())
                                RootNotificationBridge.COMMAND_CANCEL -> {
                                    input.readUTF()
                                    publisher.cancel()
                                }
                                RootNotificationBridge.COMMAND_STOP -> {
                                    input.readUTF()
                                    return
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            runCatching { publisher.cancel() }
        }
        exitProcess(0)
    }

    private const val SHELL_UID = 2000
}
