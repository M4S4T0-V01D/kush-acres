package dev.m4s4t0.kushacres.desktop

import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.model.Decor
import dev.m4s4t0.kushacres.model.Facility
import dev.m4s4t0.kushacres.model.Farm
import dev.m4s4t0.kushacres.model.Farmer
import dev.m4s4t0.kushacres.model.Helper
import dev.m4s4t0.kushacres.model.Kind
import dev.m4s4t0.kushacres.model.Season
import dev.m4s4t0.kushacres.model.Sites
import dev.m4s4t0.kushacres.model.Strains
import dev.m4s4t0.kushacres.model.Weather
import dev.m4s4t0.kushacres.model.WorkHouse
import dev.m4s4t0.kushacres.ui.Light
import dev.m4s4t0.kushacres.ui.World
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.random.Random

/**
 * A developer aid: builds a well-grown farm and renders stills and animation frames of it into [dir] (set
 * KUSH_SHOWCASE to it), for the README and the website. Nothing is saved.
 */
internal fun dumpShowcase(dir: String) {
    File(dir).mkdirs()
    val now = System.currentTimeMillis()
    val f = Store.farm
    f.newGame(now)
    f.level = 40; f.cash = 9_000_000_000
    f.farmer = Farmer(hat = 2, outfit = 1)
    val rng = Random(420)

    fun light(hour: Float, w: Weather? = null): Light {
        Light.photoHour = hour; Light.photoWeather = w
        val l = Light.at(now)
        Light.photoHour = null; Light.photoWeather = null
        return Light(l.night, l.tint, l.rain, l.fog, l.cloud, Season.SUMMER)
    }
    val day = light(13f)

    /** Fills the current site's land: houses, facilities, rows of plants at every stage, paths and decor. */
    fun dress(houses: Boolean) {
        val strains = Strains.ladder.filter { f.unlocked(it) }.takeLast(8)
        val x0 = f.ox; val y0 = f.oy; val n = f.land
        if (houses) {
            val want = listOf(WorkHouse.GROW, WorkHouse.DRYING, WorkHouse.STORE, WorkHouse.TRIM)
            var i = 0
            for (y in y0 until y0 + n) for (x in x0 until x0 + n) {
                val h = want.getOrNull(i) ?: break
                if ((x - x0) % 4 == 0 && (y - y0) < 3 && f.fits(x, y, 3) && runCatching { f.buildHouse(x, y, h) }.getOrNull() == dev.m4s4t0.kushacres.model.Result.OK) i++
            }
        }
        for (y in y0 until y0 + n) for (x in x0 until x0 + n) {
            if (f.tile(x, y).kind != Kind.GRASS) continue
            val u = x - x0; val v = y - y0
            when {
                v % 4 == 3 -> f.placeDecor(x, y, Decor.BRICK)
                u % 5 == 4 && v % 4 == 1 -> f.build(x, y, Kind.SPRINKLER)
                u % 5 == 4 && v % 4 == 2 -> f.build(x, y, Kind.LAMP)
                u % 5 == 4 -> {}
                else -> if (f.build(x, y, Kind.PLOT) == dev.m4s4t0.kushacres.model.Result.OK) {
                    val s = strains[rng.nextInt(strains.size)]
                    if (f.plant(x, y, s.id, now) == dev.m4s4t0.kushacres.model.Result.OK) {
                        f.tile(x, y).progress = s.seconds * if (rng.nextInt(3) == 0) 1.0 else rng.nextDouble(0.15, 0.95)
                    }
                }
            }
        }
        // Swap a few plots near the edges for decor and facilities, so the farm looks lived in.
        val decor = listOf(Decor.TIKI, Decor.FLOWERS, Decor.GNOME, Decor.FLAMINGO, Decor.BEEHIVE, Decor.HAMMOCK, Decor.SOUNDSYSTEM,
            Decor.LION, Decor.SHROOMS, Decor.BUDDHA, Decor.TIKIBAR, Decor.KOI, Decor.WINDMILL, Decor.JOINT, Decor.UFO, Decor.DOG, Decor.VAN, Decor.NEON)
        for (x in decor) if (x.black) f.decorInv[x.name] = 2
        var d = 0
        for (y in y0 + n - 1 downTo y0) for (x in x0 + n - 1 downTo x0) {
            if (d >= decor.size) break
            val t = f.tile(x, y)
            val edge = x == x0 || y == y0 + n - 1 || (x - x0) % 5 == 4
            if (edge && t.kind == Kind.PLOT && rng.nextInt(3) == 0) {
                t.clear(); t.kind = Kind.GRASS
                if (f.placeDecor(x, y, decor[d]) == dev.m4s4t0.kushacres.model.Result.OK) d++
            }
        }
        for ((i, fac) in listOf(Facility.ROLLER, Facility.PRESS, Facility.KITCHEN).withIndex()) {
            val x = x0 + n - 1; val y = y0 + 1 + i * 2
            if (f.tile(x, y).kind == Kind.PLOT) { f.tile(x, y).clear(); f.tile(x, y).kind = Kind.GRASS }
            runCatching { f.buildFacility(x, y, fac) }
        }
    }

    repeat(6) { f.expand() }
    dress(true)
    for (h in Helper.entries) runCatching { f.hire(h) }
    f.pets += "PUG"

    val w = World()
    fun save(name: String) = File(dir, "$name.png").writeBytes(Image.makeFromBitmap(w.bitmap.sk).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    /** The art-pixel box around the current site's land, with [pad] around it and room above for tall things. */
    fun box(pad: Int = 16): IntArray {
        val a = f.ox; val b = f.oy; val e = f.ox + f.land - 1; val g = f.oy + f.land - 1
        val xs = listOf(w.centerX(a, b), w.centerX(e, b), w.centerX(a, g), w.centerX(e, g))
        val ys = listOf(w.centerY(a, b), w.centerY(e, b), w.centerY(a, g), w.centerY(e, g))
        return intArrayOf(xs.min() - pad - 16, ys.min() - pad - 60, xs.max() + pad + 16, ys.max() + pad + 16)
    }
    fun shot(name: String, b: IntArray, k: Int, l: Light, t: Float = 1.3f) { w.view(b[0], b[1], b[2], b[3], k); w.render(f, now, t, l, null, false); save(name) }
    fun frames(name: String, b: IntArray, k: Int, count: Int, step: Float, l: (Int) -> Light, each: (Int) -> Unit = {}) {
        val sub = File(dir, name).apply { mkdirs() }
        w.view(b[0], b[1], b[2], b[3], k)
        for (i in 0 until count) {
            each(i)
            w.render(f, now, 1.3f + i * step, l(i), null, false)
            File(sub, "%03d.png".format(i)).writeBytes(Image.makeFromBitmap(w.bitmap.sk).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    val all = box()
    shot("farm-day", all, 3, day)
    shot("farm-night", all, 3, light(23f))
    shot("farm-golden", all, 3, light(18f))
    // Close-ups around the middle of the farm.
    val cx = w.centerX(f.ox + f.land / 2, f.oy + f.land / 2); val cy = w.centerY(f.ox + f.land / 2, f.oy + f.land / 2)
    val close = intArrayOf(cx - 150, cy - 130, cx + 150, cy + 40)
    shot("close-day", close, 4, day)
    shot("close-night", close, 4, light(22f))
    frames("anim-daynight", all, 1, 48, 1f / 12, { i -> light((8f + i * 24f / 48) % 24f) })
    frames("anim-close", close, 2, 48, 1f / 12, { day })
    // A patch of plants growing from seedling to ripe.
    run {
        val plots = (f.oy until f.oy + f.land).flatMap { y -> (f.ox until f.ox + f.land).map { it to y } }
            .filter { (x, y) -> f.tile(x, y).strain != null }
        val mid = plots.sortedBy { (x, y) -> Math.abs(x - f.ox - f.land / 2) + Math.abs(y - f.oy - f.land / 2) }.take(12)
        val px = mid.sumOf { w.centerX(it.first, it.second) } / mid.size; val py = mid.sumOf { w.centerY(it.first, it.second) } / mid.size
        val start = mid.associateWith { rng.nextDouble(0.0, 0.3) }
        frames("anim-grow", intArrayOf(px - 110, py - 90, px + 110, py + 40), 3, 40, 1f / 12, { day }) { i ->
            for (p in mid) { val t = f.tile(p.first, p.second); val s = Strains[t.strain!!]!!; t.progress = s.seconds * minOf(1.0, start[p]!! + i / 34.0) }
        }
    }

    // Other maps.
    fun visit(id: String) {
        val d = Sites[id] ?: return
        f.level = maxOf(f.level, d.level); f.cash = d.price * 2 + 1_000_000_000
        val h = dev.m4s4t0.kushacres.model.Holiday.entries.firstOrNull { it.id == d.holiday }
        when {
            h != null -> { dev.m4s4t0.kushacres.model.Holiday.force = h; f.addTokens(h, 10_000_000); f.buyHolidayFarm(h, d); dev.m4s4t0.kushacres.model.Holiday.force = null }
            d in Sites.premium -> { f.potBucks += d.price; f.buyBucksFarm(d) }
            else -> f.buySite(d)
        }
        f.switchTo(id)
    }
    for (id in listOf("zion", "manor", "fort20", "volcano", "neon", "spooky")) runCatching {
        visit(id)
        if (f.site.def.id != id) return@runCatching
        repeat(4) { f.expand() }
        dress(false)
        val b = if (id == "fort20") {
            val M = Farm.MAX - 1
            val xs = listOf(w.centerX(0, 0), w.centerX(M, 0), w.centerX(0, M), w.centerX(M, M))
            val ys = listOf(w.centerY(0, 0), w.centerY(M, 0), w.centerY(0, M), w.centerY(M, M))
            intArrayOf(xs.min() - 20, ys.min() - 90, xs.max() + 20, ys.max() + 20)
        } else box(60)
        val k = if (id == "fort20") 1 else 2
        shot("$id-day", b, k, day)
        shot("$id-night", b, k, light(23f))
        if (id == "zion") {
            val zc = intArrayOf((b[0] + b[2]) / 2 - 170, (b[1] + b[3]) / 2 - 120, (b[0] + b[2]) / 2 + 170, (b[1] + b[3]) / 2 + 70)
            frames("anim-zion", zc, 2, 48, 1f / 12, { day })
        }
    }
    File(dir, "sites.txt").writeText((Sites.all + Sites.holidays + Sites.premium).joinToString("\n") { "${it.id} ${it.level} ${it.landmark?.id}" })
    Runtime.getRuntime().halt(0)
}
