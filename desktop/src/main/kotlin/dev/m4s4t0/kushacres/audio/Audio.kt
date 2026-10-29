package dev.m4s4t0.kushacres.audio

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread

/**
 * The desktop sound player: the same synthesized sounds as on the phone (rendered once on a background thread),
 * mixed in software into one Java sound line: effects as voices, the music and the chosen ambience as loops.
 */
object Audio {
    /** No vibration on a computer, so Settings hides the switch. */
    const val CAN_BUZZ = false
    var music by mutableFloatStateOf(0.5f); private set
    var sound by mutableFloatStateOf(0.8f); private set
    var haptics by mutableStateOf(true); private set
    /** The radio station: "local" (each place's own tune), "shuffle", or a track id (see [Radio]). */
    var station by mutableStateOf("local"); private set
    /** The music mix: 0 plays the written tunes, anything else a remix of them all (see [Tunes.of]). */
    var mix by mutableIntStateOf(0); private set

    private lateinit var app: Context
    private var sfx: Array<FloatArray>? = null
    private var song: FloatArray? = null
    private var amb: Array<FloatArray>? = null
    private val last = LongArray(Sfx.entries.size)

    private class Voice(val data: FloatArray, val vol: Float, val step: Float) { var pos = 0f }
    private val voices = ArrayList<Voice>()
    @Volatile private var ambWant: Amb? = null
    @Volatile private var foreground = false
    private var songPos = 0.0
    /** Music speed and pitch while you're high (see [trip]), with a slow wobble on top. */
    @Volatile private var rate = 1f
    private var wob = 0.0
    /** A loop waiting to take over once the current one has faded out. */
    @Volatile private var nextSong: FloatArray? = null
    private var songGain = 0f
    @Volatile private var areaWant = "home"
    private var areaHave: String? = null
    private val tunes = LinkedHashMap<String, FloatArray>()
    private val ambPos = IntArray(Amb.entries.size)
    /** Ambience gains, eased toward their targets so a change of weather fades over. */
    private val ambGain = FloatArray(Amb.entries.size)

    fun init(ctx: Context) {
        app = ctx.applicationContext
        val p = prefs()
        music = p.getFloat("music", 0.5f); sound = p.getFloat("sound", 0.8f); haptics = p.getBoolean("haptics", false)
        station = p.getString("station", "local") ?: "local"
        mix = p.getInt("mix", 0); Tunes.mix = mix
        thread(name = "synth", isDaemon = true, priority = Thread.MIN_PRIORITY) {
            runCatching {
                sfx = Array(Sfx.entries.size) { Synth.sfx(Sfx.entries[it]) }
                amb = Array(Amb.entries.size) { Ambience.loop(Amb.entries[it]) }
                // Render each area's tune when it's wanted; keep the last few in memory.
                while (true) {
                    val id = areaWant
                    val salt = Tunes.mix
                    val key = "$id#$salt"
                    if (key != areaHave) {
                        val t = tunes.remove(key) ?: Synth.music(Tunes.of(id, salt))
                        tunes[key] = t
                        if (tunes.size > 4) tunes.remove(tunes.keys.first())
                        if (song == null) song = t else nextSong = t
                        areaHave = key
                    }
                    Thread.sleep(200)
                }
            }.onFailure { Log.e("KushAcres", "audio failed", it) }
        }
        thread(name = "mixer", isDaemon = true) { runCatching { mix() }.onFailure { Log.e("KushAcres", "no sound output", it) } }
    }

    private fun prefs() = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** [pitch] 0.5..2; the same effect at most every 45 ms, so dragging across a row doesn't turn into a buzz. */
    fun play(s: Sfx, vol: Float = 1f, pitch: Float = 1f) {
        val data = sfx?.get(s.ordinal) ?: return
        if (sound <= 0f || !foreground) return
        val t = System.nanoTime() / 1_000_000
        if (t - last[s.ordinal] < 45) return
        last[s.ordinal] = t
        val v = (vol * sound).coerceIn(0f, 1f)
        synchronized(voices) { if (voices.size < 10) voices.add(Voice(data, v, pitch.coerceIn(0.5f, 2f))) }
    }

    /** A little random pitch, for sounds that repeat a lot. */
    fun vary(s: Sfx, vol: Float = 1f) = play(s, vol, 0.92f + Math.random().toFloat() * 0.16f)

    fun ambience(a: Amb?) { ambWant = a }

    /** Plays [id]'s own tune: the farm you're on, or "town". */
    fun area(id: String) { areaWant = id }

    fun start() { foreground = true }
    fun stop() { foreground = false }

    /** Slows or speeds the music, pitch and all, while a sesh lasts; 1 puts it back. */
    fun trip(r: Float) { rate = r }

    private fun at(s: FloatArray, p: Double): Float {
        val i = p.toInt(); val fr = (p - i).toFloat()
        return s[i] + (s[(i + 1) % s.size] - s[i]) * fr
    }
    private fun step(p: Double, size: Int): Double {
        val r = rate
        if (r == 1f) return (p + 1) % size
        wob += 0.000_02
        return (p + r * (1 + 0.02 * kotlin.math.sin(wob))) % size
    }

    fun changeMusic(v: Float) { music = v; prefs().edit().putFloat("music", v).apply() }
    fun changeSound(v: Float) { sound = v; prefs().edit().putFloat("sound", v).apply() }
    fun changeHaptics(v: Boolean) { haptics = v; prefs().edit().putBoolean("haptics", v).apply() }
    fun changeStation(v: String) { station = v; prefs().edit().putString("station", v).apply() }

    /** Deals every tune afresh: new keys, tempos, leads and melodies, each in its own genre. */
    fun remix() = useMix(generateSequence { (1..999_999).random() }.first { it != mix })
    /** Back to the tunes as written. */
    fun original() = useMix(0)

    private fun useMix(v: Int) { mix = v; Tunes.mix = v; prefs().edit().putInt("mix", v).apply() }

    private fun mix() {
        val format = AudioFormat(Synth.RATE.toFloat(), 16, 1, true, false)
        val line: SourceDataLine = AudioSystem.getSourceDataLine(format)
        // About 60 ms of buffer: small enough that taps sound right away.
        line.open(format, Synth.RATE / 16 * 2)
        line.start()
        val n = 512
        val buf = FloatArray(n)
        val out = ByteArray(n * 2)
        while (true) {
            buf.fill(0f)
            val on = foreground
            song?.let { s ->
                val v = if (on && nextSong == null) music * 0.6f else 0f
                var g = songGain
                if (g > 0f || v > 0f) for (i in 0 until n) { g += (v - g) * 0.0008f; buf[i] += at(s, songPos) * g; songPos = step(songPos, s.size) }
                songGain = if (v == 0f && g < 0.001f) 0f else g
                nextSong?.let { if (songGain == 0f) { song = it; songPos = 0.0; nextSong = null } }
            }
            amb?.let { loops ->
                for (k in Amb.entries) {
                    val want = if (on && k == ambWant) sound * 0.35f else 0f
                    val s = loops[k.ordinal]
                    var g = ambGain[k.ordinal]
                    if (g == 0f && want == 0f) continue
                    var p = ambPos[k.ordinal]
                    for (i in 0 until n) { g += (want - g) * 0.0005f; buf[i] += s[p] * g; p = (p + 1) % s.size }
                    ambGain[k.ordinal] = if (want == 0f && g < 0.001f) 0f else g
                    ambPos[k.ordinal] = p
                }
            }
            synchronized(voices) {
                val it = voices.iterator()
                while (it.hasNext()) {
                    val v = it.next()
                    for (i in 0 until n) {
                        val j = v.pos.toInt()
                        if (j >= v.data.size - 1) break
                        val f = v.pos - j
                        buf[i] += (v.data[j] * (1 - f) + v.data[j + 1] * f) * v.vol
                        v.pos += v.step
                    }
                    if (v.pos.toInt() >= v.data.size - 1) it.remove()
                }
            }
            for (i in 0 until n) {
                val x = (buf[i].coerceIn(-1f, 1f) * 32767).toInt()
                out[2 * i] = x.toByte(); out[2 * i + 1] = (x shr 8).toByte()
            }
            line.write(out, 0, out.size)
        }
    }
}
