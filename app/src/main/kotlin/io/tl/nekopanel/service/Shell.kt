package io.tl.nekopanel.service

import java.io.BufferedReader
import java.io.InputStreamReader

sealed class ShellResult {
    data class Success(val output: List<String>) : ShellResult()
    data class Failure(val code: Int, val message: String) : ShellResult()
}

interface ShellProvider {
    val available: Boolean
    fun execute(command: String): ShellResult
    fun execute(commands: List<String>): ShellResult
}

object ShizukuShellProvider : ShellProvider {
    override val available: Boolean get() = false
    override fun execute(command: String): ShellResult = ShellResult.Failure(-1, "Shizuku not available")
    override fun execute(commands: List<String>): ShellResult = ShellResult.Failure(-1, "Shizuku not available")
}

object RootShellProvider : ShellProvider {
    override val available: Boolean
        get() = try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo test"))
            val code = proc.waitFor()
            code == 0
        } catch (_: Exception) { false }

    override fun execute(command: String): ShellResult = exec(arrayOf("su", "-c", command))

    override fun execute(commands: List<String>): ShellResult {
        val cmdArray = arrayOf("su", "-c", commands.joinToString("; "))
        return exec(cmdArray)
    }

    private fun exec(cmdArray: Array<String>): ShellResult {
        return try {
            val proc = Runtime.getRuntime().exec(cmdArray)
            val stdout = BufferedReader(InputStreamReader(proc.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(proc.errorStream)).readText()
            val code = proc.waitFor()
            if (code == 0) ShellResult.Success(stdout.lines().filter { it.isNotBlank() })
            else ShellResult.Failure(code, stderr.ifBlank { "exit code $code" })
        } catch (e: Exception) {
            ShellResult.Failure(-1, e.message ?: "Unknown error")
        }
    }
}

object Shell {
    private var provider: ShellProvider = RootShellProvider

    val available: Boolean get() = provider.available

    fun useRoot() { provider = RootShellProvider }
    fun useShizuku() { provider = ShizukuShellProvider }

    fun execute(command: String): ShellResult = provider.execute(command)
    fun execute(commands: List<String>): ShellResult = provider.execute(commands)

    fun checkRootAccess(): Boolean = RootShellProvider.available
}
