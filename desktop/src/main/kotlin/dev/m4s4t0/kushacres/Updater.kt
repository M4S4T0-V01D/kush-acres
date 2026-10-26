package dev.m4s4t0.kushacres

import android.app.Activity
import android.content.Context
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
import kotlin.system.exitProcess

/**
 * The desktop updater: the same GitHub releases as the phone, but it takes the Linux build attached to them,
 * unpacks it next to the installed app, swaps the two folders and starts the new one.
 */
object Updater {
    const val REPO = "M4S4T0-V01D/kush-acres"
    const val ASSET = "KushAcres-linux.tar.gz"
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
        Updater::class.java.getResource("/kushacres-version.txt")?.readText()?.trim() ?: "0"

    /** The installed app's folder (holding bin/ and lib/), or null when running from Gradle. */
    private fun appDir(): File? =
        System.getProperty("jpackage.app-path")?.let { File(it).absoluteFile.parentFile?.parentFile }?.takeIf { File(it, "lib").isDirectory }

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
                val o = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                prefs.edit().putLong("checked", now).putString("found", o.toString()).apply()
                if (!found(ctx, o) && manual) state = State.UpToDate
            }.onFailure {
                Log.w("KushAcres", "update check failed", it)
                state = if (manual) State.Failed("Couldn't reach GitHub") else State.Idle
            }
        }
    }

    /** Offers the release in [o] if it's newer than this build and carries the Linux build. */
    private fun found(ctx: Context, o: JSONObject): Boolean {
        val tag = o.optString("tag_name")
        if (tag.isEmpty() || !newer(tag, current(ctx))) { if (state is State.Available) state = State.Idle; return false }
        val assets = o.optJSONArray("assets") ?: return false
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name") == ASSET) {
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

    /** Downloads the offered build, puts it in place of this one and restarts into it. */
    fun install(act: Activity) {
        val a = state as? State.Available ?: return
        val dir = appDir() ?: run { state = State.Failed("Not an installed copy"); return }
        state = State.Downloading(a.version, 0f)
        io.launch {
            runCatching {
                val parent = dir.parentFile
                val stage = File(parent, ".${dir.name}-update").apply { deleteRecursively(); mkdirs() }
                val file = File(stage, ASSET)
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
                            if (total > 0 && got - last > total / 50) { last = got; state = State.Downloading(a.version, got.toFloat() / total) }
                        }
                    }
                }
                state = State.Installing
                // The tarball holds one folder, KushAcres/, with bin/ and lib/.
                val tar = ProcessBuilder("tar", "-xzf", file.path, "-C", stage.path).redirectErrorStream(true).start()
                val log = tar.inputStream.bufferedReader().readText()
                if (tar.waitFor() != 0) error("unpack failed: $log")
                val fresh = stage.listFiles()?.firstOrNull { it.isDirectory && File(it, "bin").isDirectory } ?: error("no app in the download")
                val old = File(parent, ".${dir.name}-old").apply { deleteRecursively() }
                if (!dir.renameTo(old)) error("couldn't move the old version")
                if (!fresh.renameTo(dir)) { old.renameTo(dir); error("couldn't put the new version in place") }
                stage.deleteRecursively()
                old.deleteRecursively()
                Store.save()
                ProcessBuilder(File(dir, "bin/${File(System.getProperty("jpackage.app-path")).name}").path).start()
                exitProcess(0)
            }.onFailure {
                Log.w("KushAcres", "update failed", it)
                state = State.Failed("Update failed: ${it.message}")
            }
        }
    }
}
