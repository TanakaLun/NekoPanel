package io.tl.nekopanel.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.tl.nekopanel.data.local.AppDatabase
import io.tl.nekopanel.data.repository.SettingsManager
import io.tl.nekopanel.ui.components.SectionTitle
import io.tl.nekopanel.ui.components.SettingsDropdownMenuInline
import io.tl.nekopanel.ui.components.SplicedColumnGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

@Composable
fun BackupScreen(settings: SettingsManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    var provider by remember { mutableStateOf(if (settings.backupGithubRepo.isNotBlank()) "github" else "webdav") }
    var showAutoBackupDialog by remember { mutableStateOf(false) }
    var autoBackupInterval by remember { mutableStateOf(settings.backupAutoInterval) }
    var autoBackupEnabled by remember { mutableStateOf(settings.backupAutoInterval > 0) }

    var webdavUrl by remember { mutableStateOf(settings.backupWebdavUrl) }
    var webdavUser by remember { mutableStateOf(settings.backupWebdavUser) }
    var webdavPass by remember { mutableStateOf(settings.backupWebdavPass) }
    var showWebdavPass by remember { mutableStateOf(false) }

    var ghRepo by remember { mutableStateOf(settings.backupGithubRepo) }
    var ghToken by remember { mutableStateOf(settings.backupGithubToken) }
    var ghPath by remember { mutableStateOf(settings.backupGithubPath) }
    var showGhToken by remember { mutableStateOf(false) }

    fun saveConfig() {
        settings.backupWebdavUrl = webdavUrl
        settings.backupWebdavUser = webdavUser
        settings.backupWebdavPass = webdavPass
        settings.backupGithubRepo = ghRepo
        settings.backupGithubToken = ghToken
        settings.backupGithubPath = ghPath
        settings.backupAutoInterval = if (autoBackupEnabled) autoBackupInterval else 0
    }

    suspend fun buildBackupPayload(): String {
        val dao = AppDatabase.getInstance(context).settingsDao()
        val allSettings = withContext(Dispatchers.IO) { dao.getAll() }
        val obj = JSONObject()
        val data = JSONObject()
        allSettings.forEach { data.put(it.key, it.value) }
        obj.put("version", 1)
        obj.put("exportedAt", System.currentTimeMillis())
        obj.put("settings", data)
        return obj.toString(2)
    }

    suspend fun restoreFromPayload(json: String) {
        val obj = JSONObject(json)
        val data = obj.optJSONObject("settings") ?: throw IOException("invalid format")
        val dao = AppDatabase.getInstance(context).settingsDao()
        withContext(Dispatchers.IO) {
            data.keys().forEach { key ->
                val value = data.optString(key)
                if (key.isNotEmpty()) {
                    dao.put(io.tl.nekopanel.data.local.SettingsEntity(key, value))
                }
            }
        }
    }

    suspend fun pushToWebdav(payload: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        val url = webdavUrl.trimEnd('/') + "/nekopanel-backup.json"
        val body = payload.toRequestBody("application/json".toMediaType())
        val builder = Request.Builder().url(url).put(body)
        if (webdavUser.isNotBlank() || webdavPass.isNotBlank()) {
            val cred = okhttp3.Credentials.basic(webdavUser, webdavPass)
            builder.header("Authorization", cred)
        }
        val response = withContext(Dispatchers.IO) { client.newCall(builder.build()).execute() }
        if (!response.isSuccessful) throw IOException("WebDAV ${response.code}: ${response.message}")
    }

    suspend fun pullFromWebdav(): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        val url = webdavUrl.trimEnd('/') + "/nekopanel-backup.json"
        val builder = Request.Builder().url(url)
        if (webdavUser.isNotBlank() || webdavPass.isNotBlank()) {
            val cred = okhttp3.Credentials.basic(webdavUser, webdavPass)
            builder.header("Authorization", cred)
        }
        val response = withContext(Dispatchers.IO) { client.newCall(builder.build()).execute() }
        if (!response.isSuccessful) throw IOException("WebDAV ${response.code}: ${response.message}")
        return response.body?.string() ?: throw IOException("empty response")
    }

    suspend fun pushToGithub(payload: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        val apiUrl = "https://api.github.com/repos/$ghRepo/contents/$ghPath"
        val existing = try {
            val getReq = Request.Builder().url(apiUrl)
                .header("Authorization", "Bearer $ghToken")
                .header("Accept", "application/vnd.github.v3+json")
                .build()
            val getResp = withContext(Dispatchers.IO) { client.newCall(getReq).execute() }
            if (getResp.isSuccessful) {
                JSONObject(getResp.body?.string() ?: "").optString("sha", null)
            } else null
        } catch (_: Exception) { null }
        val content = android.util.Base64.encodeToString(payload.toByteArray(), android.util.Base64.NO_WRAP)
        val commitObj = JSONObject().apply {
            put("message", "NekoPanel backup ${System.currentTimeMillis()}")
            put("content", content)
            if (existing != null) put("sha", existing)
        }
        val putReq = Request.Builder().url(apiUrl)
            .header("Authorization", "Bearer $ghToken")
            .header("Accept", "application/vnd.github.v3+json")
            .put(commitObj.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val resp = withContext(Dispatchers.IO) { client.newCall(putReq).execute() }
        if (!resp.isSuccessful) throw IOException("GitHub ${resp.code}: ${resp.message}")
    }

    suspend fun pullFromGithub(): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        val apiUrl = "https://api.github.com/repos/$ghRepo/contents/$ghPath"
        val req = Request.Builder().url(apiUrl)
            .header("Authorization", "Bearer $ghToken")
            .header("Accept", "application/vnd.github.v3+json")
            .build()
        val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
        if (!resp.isSuccessful) throw IOException("GitHub ${resp.code}: ${resp.message}")
        val json = JSONObject(resp.body?.string() ?: throw IOException("empty response"))
        val content = json.optString("content", "").replace("\n", "")
        val decoded = String(android.util.Base64.decode(content, android.util.Base64.DEFAULT))
        return decoded
    }

    fun doBackup() {
        isBackingUp = true
        status = "正在导出..."
        scope.launch {
            try {
                saveConfig()
                val payload = buildBackupPayload()
                when (provider) {
                    "webdav" -> pushToWebdav(payload)
                    "github" -> pushToGithub(payload)
                }
                status = "备份成功 (${provider.uppercase()})"
                Toast.makeText(context, "备份完成", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                status = "备份失败: ${e.message}"
            } finally {
                isBackingUp = false
            }
        }
    }

    fun doRestore() {
        isRestoring = true
        status = "正在恢复..."
        scope.launch {
            try {
                val payload = when (provider) {
                    "webdav" -> pullFromWebdav()
                    "github" -> pullFromGithub()
                    else -> throw IOException("unknown provider")
                }
                restoreFromPayload(payload)
                status = "恢复成功 (${provider.uppercase()})"
                Toast.makeText(context, "已恢复，重启应用后生效", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                status = "恢复失败: ${e.message}"
            } finally {
                isRestoring = false
            }
        }
    }

    if (showAutoBackupDialog) {
        var intervalText by remember { mutableStateOf(autoBackupInterval.toString()) }
        AlertDialog(
            onDismissRequest = { showAutoBackupDialog = false },
            title = { Text("定时自动备份") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("设置自动备份间隔（分钟），设为 0 关闭自动备份", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = { intervalText = it.filter { c -> c.isDigit() } },
                        label = { Text("间隔（分钟）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = intervalText.toIntOrNull() ?: 0
                    autoBackupInterval = v
                    autoBackupEnabled = v > 0
                    showAutoBackupDialog = false
                    settings.backupAutoInterval = v
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showAutoBackupDialog = false }) { Text("取消") }
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(8.dp))
                Text("数据备份", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            }

            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                if (status.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.3f))) {
                        Text(status, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                SplicedColumnGroup(title = "备份方式") {
                    item {
                        SettingsDropdownMenuInline(
                            label = "选择备份方式",
                            currentValue = if (provider == "webdav") "WebDAV" else "GitHub",
                            options = listOf("WebDAV", "GitHub"),
                            onSelected = { provider = if (it == "WebDAV") "webdav" else "github" }
                        )
                    }
                }

                SectionTitle(if (provider == "webdav") "WebDAV 配置" else "GitHub 配置")
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (provider == "webdav") {
                            OutlinedTextField(value = webdavUrl, onValueChange = { webdavUrl = it }, label = { Text("服务器地址") }, placeholder = { Text("https://example.com/dav") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = webdavUser, onValueChange = { webdavUser = it }, label = { Text("用户名") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = webdavPass, onValueChange = { webdavPass = it }, label = { Text("密码") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), visualTransformation = if (showWebdavPass) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = {
                                IconButton(onClick = { showWebdavPass = !showWebdavPass }) { Icon(if (showWebdavPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) }
                            })
                        } else {
                            OutlinedTextField(value = ghRepo, onValueChange = { ghRepo = it }, label = { Text("仓库") }, placeholder = { Text("user/repo") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = ghToken, onValueChange = { ghToken = it }, label = { Text("Token") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), visualTransformation = if (showGhToken) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = {
                                IconButton(onClick = { showGhToken = !showGhToken }) { Icon(if (showGhToken) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) }
                            })
                            OutlinedTextField(value = ghPath, onValueChange = { ghPath = it }, label = { Text("文件路径") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                SectionTitle("操作")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { doBackup() },
                        modifier = Modifier.weight(1f),
                        enabled = !isBackingUp && !isRestoring,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Upload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        if (isBackingUp) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("备份到服务器")
                    }
                    OutlinedButton(
                        onClick = { doRestore() },
                        modifier = Modifier.weight(1f),
                        enabled = !isBackingUp && !isRestoring,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        if (isRestoring) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("从服务器恢复")
                    }
                }

                SectionTitle("自动备份")
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("定时自动备份", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (autoBackupEnabled) "每 ${autoBackupInterval} 分钟执行一次" else "已关闭",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Switch(checked = autoBackupEnabled, onCheckedChange = {
                                autoBackupEnabled = it
                                if (!it) {
                                    autoBackupInterval = 0
                                    settings.backupAutoInterval = 0
                                } else if (autoBackupInterval <= 0) {
                                    autoBackupInterval = 60
                                }
                                settings.backupAutoInterval = if (autoBackupEnabled) autoBackupInterval else 0
                            })
                        }
                        if (autoBackupEnabled) {
                            OutlinedButton(
                                onClick = { showAutoBackupDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Schedule, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("修改间隔时间")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
