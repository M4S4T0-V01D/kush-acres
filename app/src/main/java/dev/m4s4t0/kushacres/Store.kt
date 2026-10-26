package dev.m4s4t0.kushacres

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.m4s4t0.kushacres.audio.Amb
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.audio.Sfx
import dev.m4s4t0.kushacres.model.Away
import dev.m4s4t0.kushacres.model.EventKind
import dev.m4s4t0.kushacres.model.Farm
import dev.m4s4t0.kushacres.model.FarmEvent
import dev.m4s4t0.kushacres.model.Save
import dev.m4s4t0.kushacres.model.Season
import dev.m4s4t0.kushacres.model.Sky
import dev.m4s4t0.kushacres.model.Weather

/** The one farm, its save file, and the bits of UI state that come out of it (toasts, level-ups, the away report). */
object Store {
    val farm = Farm()
    /** Bumped whenever the farm changes, so Compose redraws what reads it. */
    var rev by mutableIntStateOf(0)
    var toast by mutableStateOf<Pair<String, Long>?>(null)
    val levelUps = mutableStateListOf<FarmEvent.LevelUp>()
    var awayReport by mutableStateOf<Away?>(null)
    var cashPop by mutableStateOf<Pair<Long, Long>?>(null)
    /** Harvests and such for the farm view to float numbers over. */
    val pending = ArrayList<FarmEvent>()

    private lateinit var app: Context
    private var dirty = false
    private var lastSave = 0L
    private var rainSaid = false

    fun now() = System.currentTimeMillis()

    fun init(ctx: Context) {
        app = ctx.applicationContext
        val json = prefs().getString("save", null)
        val loaded = json != null && runCatching { Save.read(json, farm) }
            .onFailure { Log.e("KushAcres", "save unreadable, starting over", it) }.isSuccess
        if (!loaded) farm.newGame(now())
        farm.checkTrophies(silent = true)
        loadLevelUps()
        resume()
    }

    private fun prefs() = app.getSharedPreferences("farm", Context.MODE_PRIVATE)

    /** Catches up on the time the app was closed and reports what the farmhand did. */
    fun resume() {
        farm.away = Away()
        farm.tick(now())
        if (farm.away.any()) awayReport = farm.away
        farm.away = Away()
        farm.delayEvents(now())
        drain()
    }

    fun tick() {
        farm.tick(now())
        val hour = Sky.hour(now()).toInt()
        val sky = Sky.weather(now())
        Audio.ambience(if (sky == Weather.RAIN) Amb.RAIN else if (hour in 6..19) Amb.DAY else Amb.NIGHT)
        if (sky == Weather.RAIN && farm.rain(now()) > 0) { dirty = true; if (!rainSaid) toast(if (farm.season() == Season.WINTER) "Snow! Your plants are watered" else "Rain! Your plants are watered") ; rainSaid = true }
        if (sky != Weather.RAIN) rainSaid = false
        if (farm.rollEvent(now())) {
            dirty = true
            Audio.play(if (farm.happening?.kind == EventKind.RAID) Sfx.RAID else Sfx.EVENT)
        }
        drain()
        if (dirty && now() - lastSave > 10_000) save()
    }

    /** Call after any action: picks up events, marks the save dirty and redraws. */
    fun changed() {
        dirty = true
        drain()
    }

    private fun drain() {
        farm.checkTrophies()
        for (e in farm.events) when (e) {
            is FarmEvent.LevelUp -> { levelUps += e; Audio.play(Sfx.LEVEL_UP) }
            is FarmEvent.Cash -> { cashPop = e.amount to now(); Audio.play(Sfx.CASH) }
            is FarmEvent.Harvest -> { pending += e; if (e.golden) Audio.play(Sfx.GOLDEN) else Audio.vary(Sfx.HARVEST) }
            is FarmEvent.Award -> { toast("Trophy: ${e.trophy.title}  +${e.xp} XP"); Audio.play(Sfx.UNLOCK) }
        }
        if (farm.events.isNotEmpty()) dirty = true
        farm.events.clear()
        rev++
    }

    fun toast(msg: String) { toast = msg to now() }

    /** Dismisses the top level-up card; the rest are kept in case the app closes before they're seen. */
    fun dismissLevelUp() {
        if (levelUps.isNotEmpty()) levelUps.removeAt(0)
        if (::app.isInitialized) prefs().edit().putString("levelUps", levelUpText()).apply()
    }

    private fun levelUpText() = levelUps.joinToString("\n") { "${it.level}|${it.bonus}|${it.unlocks.joinToString("\t")}" }

    private fun loadLevelUps() {
        val text = prefs().getString("levelUps", null) ?: return
        for (line in text.split("\n")) {
            val p = line.split("|", limit = 3)
            if (p.size < 3) continue
            val lv = p[0].toIntOrNull() ?: continue
            levelUps += FarmEvent.LevelUp(lv, p[1].toLongOrNull() ?: 0, p[2].split("\t").filter { it.isNotEmpty() })
        }
    }

    fun save() {
        if (!::app.isInitialized) return
        prefs().edit().putString("save", Save.write(farm)).putString("levelUps", levelUpText()).apply()
        dirty = false
        lastSave = now()
    }
}
