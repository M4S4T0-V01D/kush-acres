package dev.m4s4t0.kushacres

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Updates the game from the latest GitHub release: a tag vX.Y.Z with KushAcres.apk attached. */
object Updater {
    const val REPO = "M4S4T0-V01D/kush-acres"
    const val ACTION_INSTALLED = "dev.m4s4t0.kushacres.INSTALL_STATUS"
    private const val APK = "KushAcres.apk"
    private const val EVERY_MS = 6 * 3600_000L

    sealed class State {
        data object Idle : State()
        data object Checking : State()
        data object UpToDate : State()
        data class Available(val version: String, val url: String, val notes: String) : State()
        data class Downloading(val version: String, val frac: Float) : State()
        data object Installing : State()
        data class Failed(val why: String) : State()
    }

    var state by mutableStateOf<State>(State.Idle)
    /** The update prompt was waved off this session. */
    var snoozed by mutableStateOf(false)
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun current(ctx: Context): String =
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0"

    /** Newer means a higher number at the first place the dotted versions differ. */
    fun newer(remote: String, local: String): Boolean {
        val a = remote.trimStart('v', 'V').split('.', '-').map { it.toIntOrNull() ?: 0 }
        val b = local.trimStart('v', 'V').split('.', '-').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** Looks for a release; [manual] checks right away and reports "up to date", otherwise at most every few hours. */
    fun check(ctx: Context, manual: Boolean = false) {
        if (state is State.Checking || state is State.Downloading || state is State.Installing) return
        val prefs = ctx.getSharedPreferences("update", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!manual && now - prefs.getLong("checked", 0) < EVERY_MS) {
            // Still show what an earlier check found.
            prefs.getString("found", null)?.let { found(ctx, JSONObject(it)) }
            return
        }
        if (manual) { state = State.Checking; snoozed = false }
        io.launch {
            runCatching {
                val c = open("https://api.github.com/repos/$REPO/releases/latest")
                c.setRequestProperty("Accept", "application/vnd.github+json")
                val code = c.responseCode
                if (code == 404) { prefs.edit().putLong("checked", now).remove("found").apply(); state = if (manual) State.UpToDate else State.Idle; return@launch }
                if (code != 200) error("GitHub said $code")
                val body = c.inputStream.bufferedReader().use { it.readText() }
                val o = JSONObject(body)
                prefs.edit().putLong("checked", now).putString("found", o.toString()).apply()
                if (!found(ctx, o) && manual) state = State.UpToDate
            }.onFailure {
                Log.w("KushAcres", "update check failed", it)
                state = if (manual) State.Failed("Couldn't reach GitHub") else State.Idle
            }
        }
    }

    /** Offers the release in [o] if it's newer than this build and carries the APK. */
    private fun found(ctx: Context, o: JSONObject): Boolean {
        val tag = o.optString("tag_name")
        if (tag.isEmpty() || !newer(tag, current(ctx))) { if (state is State.Available) state = State.Idle; return false }
        val assets = o.optJSONArray("assets") ?: return false
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name") == APK) {
                state = State.Available(tag.trimStart('v', 'V'), a.getString("browser_download_url"), o.optString("body").trim())
                return true
            }
        }
        return false
    }

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        setRequestProperty("User-Agent", "KushAcres")
        connectTimeout = 15_000; readTimeout = 30_000
        instanceFollowRedirects = true
    }

    /** Downloads the offered APK and hands it to the system installer. */
    fun install(act: Activity) {
        val a = state as? State.Available ?: return
        if (!act.packageManager.canRequestPackageInstalls()) {
            // Android wants the player to allow installs from the game once.
            act.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${act.packageName}")))
            return
        }
        state = State.Downloading(a.version, 0f)
        val app = act.applicationContext
        io.launch {
            runCatching {
                val file = File(app.cacheDir, APK)
                val c = open(a.url)
                if (c.responseCode != 200) error("download said ${c.responseCode}")
                val total = c.contentLengthLong
                c.inputStream.use { inp ->
                    file.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var got = 0L; var last = 0L
                        while (true) {
                            val n = inp.read(buf); if (n < 0) break
                            out.write(buf, 0, n); got += n
                            if (total > 0 && got - last > 256 * 1024) { last = got; state = State.Downloading(a.version, got.toFloat() / total) }
                        }
                    }
                }
                state = State.Installing
                val pi = app.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(app.packageName)
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
                val id = pi.createSession(params)
                pi.openSession(id).use { s ->
                    s.openWrite(APK, 0, file.length()).use { out -> file.inputStream().use { it.copyTo(out) }; s.fsync(out) }
                    val back = Intent(app, MainActivity::class.java).setAction(ACTION_INSTALLED)
                    val pending = PendingIntent.getActivity(app, id, back, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                    s.commit(pending.intentSender)
                }
            }.onFailure {
                Log.w("KushAcres", "update install failed", it)
                state = State.Failed("Update failed: ${it.message ?: "unknown error"}")
            }
        }
    }

    /** The installer reporting back through [ACTION_INSTALLED]. */
    fun onStatus(act: Activity, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION ->
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)?.let { act.startActivity(it) }
            PackageInstaller.STATUS_SUCCESS -> state = State.Idle
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.w("KushAcres", "install status $status: $msg")
                state = if (status == PackageInstaller.STATUS_FAILURE_ABORTED) State.Idle else State.Failed("Install failed${msg?.let { ": $it" } ?: ""}")
            }
        }
    }
}
