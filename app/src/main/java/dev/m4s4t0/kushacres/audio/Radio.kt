package dev.m4s4t0.kushacres.audio

import dev.m4s4t0.kushacres.model.Farm
import dev.m4s4t0.kushacres.model.Sites

/**
 * The radio: pick what plays. "local" is each place's own tune, "shuffle" moves through every track you have every
 * few minutes, and any track id plays that track everywhere. A farm's tune is yours once you own the farm; the extra
 * tracks come with levels ([LEVELS]) or as prizes from the town board ([BOARD]).
 */
object Radio {
    const val LOCAL = "local"
    const val SHUFFLE = "shuffle"
    /** How long shuffle stays on one track. */
    const val SHUFFLE_MS = 4 * 60_000L

    /** Extra tracks you get by reaching a level. */
    val LEVELS = mapOf("lofi1" to 5, "house1" to 8, "dub1" to 10, "chip1" to 13, "reggae1" to 16, "italo1" to 19, "lofi2" to 22, "jungle1" to 26, "dub2" to 30)
    /** Extra tracks only the town board gives out. */
    val BOARD = Tunes.extras.map { it.first }.filter { it !in LEVELS }

    class Track(val id: String, val name: String, val style: Style, val how: String)

    val tracks: List<Track> = Tunes.areas.map { id ->
        val name = when (id) { "home" -> "Home Grown"; "town" -> "Town Square"; else -> Sites[id]?.name ?: id }
        Track(id, name, Tunes.of(id).style, if (id == "home" || id == "town") "" else "Own ${Sites[id]?.name ?: id}")
    } + Tunes.extras.map { (id, name, tune) ->
        Track(id, name, tune.style, LEVELS[id]?.let { "Reach level $it" } ?: "Town board prize")
    }

    operator fun get(id: String) = tracks.firstOrNull { it.id == id }

    fun has(f: Farm, id: String): Boolean = when {
        id == "home" || id == "town" -> true
        id in Tunes.areas -> f.owns(id)
        else -> id in f.tracks || (LEVELS[id]?.let { f.level >= it } ?: false)
    }

    fun owned(f: Farm) = tracks.filter { has(f, it.id) }

    /** A board prize you don't have yet, or null once you've got them all. */
    fun prize(f: Farm, r: kotlin.random.Random): String? = BOARD.filter { it !in f.tracks }.randomOrNull(r)

    /** What should play in [area] on the current station. */
    fun pick(area: String, f: Farm, now: Long, station: String = Audio.station): String = when (station) {
        LOCAL -> area
        SHUFFLE -> owned(f).let { all -> all[Math.floorMod((now / SHUFFLE_MS).hashCode() * 31 + 7, all.size)].id }
        else -> if (has(f, station)) station else area
    }

    fun styleName(s: Style) = s.label
}
