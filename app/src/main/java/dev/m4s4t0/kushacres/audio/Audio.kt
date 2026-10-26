package dev.m4s4t0.kushacres.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import kotlin.concurrent.thread

/**
 * Plays the synthesized sounds. They are rendered once into WAV files in the cache (a second or two on
 * first launch, on a background thread), then effects go through a SoundPool and the loops through MediaPlayers.
 */
object Audio {
    /** Bump when Synth changes, so the cached WAVs are rendered again. */
    private const val VERSION = 1

    var music by mutableFloatStateOf(0.5f); private set
    var sound by mutableFloatStateOf(0.8f); private set
    var haptics by mutableStateOf(true); private set

    private lateinit var app: Context
    private val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val pool = SoundPool.Builder().setMaxStreams(10).setAudioAttributes(attrs).build()
    private val ids = IntArray(Sfx.entries.size)
    private val loaded = BooleanArray(Sfx.entries.size)
    private val last = LongArray(Sfx.entries.size)
    private var song: MediaPlayer? = null
    private val amb = arrayOfNulls<MediaPlayer>(Amb.entries.size)
    private var ambWant: Amb? = null
    private var foreground = false

    fun init(ctx: Context) {
        app = ctx.applicationContext
        val p = prefs()
        music = p.getFloat("music", 0.5f); sound = p.getFloat("sound", 0.8f); haptics = p.getBoolean("haptics", true)
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ids.indexOf(id).let { if (it >= 0) loaded[it] = true } }
        thread(name = "synth", priority = Thread.MIN_PRIORITY) {
            runCatching {
                val dir = File(app.cacheDir, "audio-v$VERSION").apply { mkdirs() }
                app.cacheDir.listFiles { f -> f.name.startsWith("audio-v") && f != dir }?.forEach { it.deleteRecursively() }
                fun file(name: String, make: () -> FloatArray) = File(dir, "$name.wav").also { f ->
                    if (!f.exists()) { val tmp = File(dir, "$name.tmp"); tmp.writeBytes(Synth.wav(make())); tmp.renameTo(f) }
                }
                for (s in Sfx.entries) ids[s.ordinal] = pool.load(file(s.name) { Synth.sfx(s) }.path, 1)
                val m = file("music") { Synth.music() }
                val a = Amb.entries.map { file("amb-${it.name}") { Ambience.loop(it) } }
                android.os.Handler(app.mainLooper).post {
                    song = player(m, music * 0.6f)
                    for (k in Amb.entries) amb[k.ordinal] = player(a[k.ordinal], 0f)
                    refresh()
                }
            }.onFailure { Log.e("KushAcres", "audio failed", it) }
        }
    }

    private fun player(f: File, vol: Float) = runCatching {
        MediaPlayer().apply { setAudioAttributes(attrs); setDataSource(f.path); isLooping = true; prepare(); setVolume(vol, vol) }
    }.onFailure { Log.e("KushAcres", "player", it) }.getOrNull()

    private fun prefs() = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** [pitch] 0.5..2; the same effect at most every 45 ms, so dragging across a row doesn't turn into a buzz. */
    fun play(s: Sfx, vol: Float = 1f, pitch: Float = 1f) {
        if (!loaded[s.ordinal] || sound <= 0f) return
        val t = SystemClock.uptimeMillis()
        if (t - last[s.ordinal] < 45) return
        last[s.ordinal] = t
        val v = (vol * sound).coerceIn(0f, 1f)
        pool.play(ids[s.ordinal], v, v, 1, 0, pitch.coerceIn(0.5f, 2f))
    }

    /** A little random pitch, for sounds that repeat a lot. */
    fun vary(s: Sfx, vol: Float = 1f) = play(s, vol, 0.92f + Math.random().toFloat() * 0.16f)

    fun ambience(a: Amb?) { if (a != ambWant) { ambWant = a; refresh() } }

    fun start() { foreground = true; refresh() }
    fun stop() { foreground = false; refresh() }

    fun changeMusic(v: Float) { music = v; prefs().edit().putFloat("music", v).apply(); refresh() }
    fun changeSound(v: Float) { sound = v; prefs().edit().putFloat("sound", v).apply() }
    fun changeHaptics(v: Boolean) { haptics = v; prefs().edit().putBoolean("haptics", v).apply() }

    private fun refresh() {
        song?.let { p -> val v = music * 0.6f; p.setVolume(v, v); if (foreground && v > 0f) { if (!p.isPlaying) p.start() } else if (p.isPlaying) p.pause() }
        for (k in Amb.entries) amb[k.ordinal]?.let { p ->
            val v = if (k == ambWant) sound * 0.35f else 0f
            p.setVolume(v, v)
            if (foreground && v > 0f) { if (!p.isPlaying) p.start() } else if (p.isPlaying) p.pause()
        }
    }
}
