package dev.m4s4t0.kushacres.desktop

import dev.m4s4t0.kushacres.ui.FarmArrows
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.model.Contracts
import dev.m4s4t0.kushacres.model.Cup
import dev.m4s4t0.kushacres.model.Hunt
import dev.m4s4t0.kushacres.model.Strains
import dev.m4s4t0.kushacres.model.Storefronts
import dev.m4s4t0.kushacres.model.Kind
import dev.m4s4t0.kushacres.model.WorkHouse
import dev.m4s4t0.kushacres.model.Furn as M
import dev.m4s4t0.kushacres.art.Buildings
import dev.m4s4t0.kushacres.ui.Px
import dev.m4s4t0.kushacres.model.Trip
import dev.m4s4t0.kushacres.ui.ContractsTab
import dev.m4s4t0.kushacres.ui.MenuPanel
import dev.m4s4t0.kushacres.ui.UiState
import dev.m4s4t0.kushacres.ui.Wood
import androidx.compose.ui.graphics.asImageBitmap
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * A developer aid: renders Farm Book tabs for a made-up level 30 farm into [dir] (set KUSH_PANELS to it), so new
 * panels can be looked at without clicking through the game. Nothing is saved.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal fun dumpPanels(dir: String) {
    File(dir).mkdirs()
    val now = System.currentTimeMillis()
    val f = Store.farm
    f.newGame(now)
    f.level = 30; f.cash = 50_000_000
    for (s in Strains.ladder.filter { f.unlocked(it) }.takeLast(4)) f.stash[s.id] = 400
    Cup.enter(f, f.stash.keys.first(), now)
    fun shot(name: String, start: Int, h: Int = 1100, body: @androidx.compose.runtime.Composable () -> Unit = { MenuPanel(UiState(), start) }) {
        val scene = ImageComposeScene(480, h, Density(1.5f)) {
            Column(Modifier.fillMaxSize().background(Wood).padding(10.dp)) { body() }
        }
        scene.render(); scene.render()
        File(dir, "$name.png").writeBytes(scene.render().encodeToData(EncodedImageFormat.PNG)!!.bytes)
        scene.close()
    }
    shot("events", 0, 1300) {
        val kinds = dev.m4s4t0.kushacres.model.EventKind.entries
        for (row in kinds.chunked(3)) androidx.compose.foundation.layout.Row {
            for (k in row) Px(dev.m4s4t0.kushacres.art.EventArt.portrait(k, dev.m4s4t0.kushacres.model.Holiday.entries.first()), 100.dp, Modifier.padding(3.dp))
        }
    }
    shot("rail", 0, 260) { dev.m4s4t0.kushacres.ui.Rail(UiState(), Modifier) }
    shot("cup", 2)
    shot("hunts", 0) { dev.m4s4t0.kushacres.ui.HuntsPanel() }
    Hunt.send(f, Trip.VALLEY, now)
    shot("hunts-away", 0) { dev.m4s4t0.kushacres.ui.HuntsPanel() }
    f.huntBack = now - 1
    shot("hunts-back", 0) { dev.m4s4t0.kushacres.ui.HuntsPanel() }
    run {
        val P = dev.m4s4t0.kushacres.model.Pheno
        val ids = Strains.ladder.take(3).map { it.id }
        f.finds[ids[0]] = P.of(4, 3, 1); f.finds[ids[1]] = P.of(2, 1); f.finds[ids[2]] = P.of(3, 3, 3)
        f.keepMother(ids[0]); f.keepMother(ids[2]); f.toggleHunt(ids[2])
        shot("book", 0, 700) { dev.m4s4t0.kushacres.ui.StrainsPanel() }
        shot("plants", 0, 160) {
            androidx.compose.foundation.layout.Row { for (c in 0..3) Px(dev.m4s4t0.kushacres.art.Sprites.plant(Strains.ladder[8], 5, 0, c), 80.dp) }
        }
    }
    run {
        // Pests and mould on two grown plots, by day and at night.
        val plots = (f.oy until f.oy + f.land).flatMap { b -> (f.ox until f.ox + f.land).map { it to b } }.filter { (a, b) -> f.tile(a, b).kind == Kind.PLOT }.take(3)
        for ((i, xy) in plots.withIndex()) {
            val t = f.tile(xy.first, xy.second)
            f.plant(xy.first, xy.second, Strains.ladder[0].id, now - 86_400_000L)
            when (i) { 0 -> t.pest = true; 1 -> t.mould = true; else -> { t.progress = Strains.ladder[0].seconds.toDouble(); t.ripeAt = now } }
        }
        val w = dev.m4s4t0.kushacres.ui.World()
        val cx = plots.sumOf { w.centerX(it.first, it.second) } / plots.size
        val cy = plots.sumOf { w.centerY(it.first, it.second) } / plots.size
        for ((name, light) in listOf("pests-day" to dev.m4s4t0.kushacres.ui.Light(0f, -1, 0f, 0f, 0f), "pests-night" to dev.m4s4t0.kushacres.ui.Light(1f, 0xFF4C5CAD.toInt(), 0f, 0f, 0f))) {
            w.view(cx - 60, cy - 60, cx + 60, cy + 20, 4)
            w.render(f, now, 1.3f, light, null, false)
            val img = w.bitmap.asImageBitmap()
            shot(name, 0, 340) { androidx.compose.foundation.Image(img, null, Modifier.fillMaxSize(), filterQuality = androidx.compose.ui.graphics.FilterQuality.None) }
        }
        for (xy in plots) f.tile(xy.first, xy.second).let { it.pest = false; it.mould = false }
        // Custom soil: wet plots should be a darker shade of it.
        f.site.dirt = 0xFFC07040.toInt()
        val soil = (f.oy until f.oy + f.land).flatMap { b -> (f.ox until f.ox + f.land).map { it to b } }.filter { (a, b) -> f.tile(a, b).kind == Kind.PLOT && f.tile(a, b).strain == null }.take(4)
        for ((i, xy) in soil.withIndex()) if (i % 2 == 0) f.tile(xy.first, xy.second).wetUntil = now + 3_600_000L
        f.plant(soil[0].first, soil[0].second, Strains.ladder[0].id, now)
        w.view(w.centerX(soil[0].first, soil[0].second) - 80, w.centerY(soil[0].first, soil[0].second) - 50, w.centerX(soil[0].first, soil[0].second) + 80, w.centerY(soil[0].first, soil[0].second) + 50, 4)
        w.render(f, now, 1.3f, dev.m4s4t0.kushacres.ui.Light(0f, -1, 0f, 0f, 0f), null, false)
        val soilImg = w.bitmap.asImageBitmap()
        shot("soil", 0, 340) { androidx.compose.foundation.Image(soilImg, null, Modifier.fillMaxSize(), filterQuality = androidx.compose.ui.graphics.FilterQuality.None) }
        f.site.dirt = null
        // Photo mode at dusk in the rain: the snap's own picture, as it would be saved.
        dev.m4s4t0.kushacres.ui.Light.photoHour = 18.5f; dev.m4s4t0.kushacres.ui.Light.photoWeather = dev.m4s4t0.kushacres.model.Weather.RAIN
        val dusk = dev.m4s4t0.kushacres.ui.Light.at(now)
        dev.m4s4t0.kushacres.ui.Light.photoHour = null; dev.m4s4t0.kushacres.ui.Light.photoWeather = null
        w.view(cx - 120, cy - 80, cx + 120, cy + 40, 2)
        w.render(f, now, 1.3f, dusk, null, false)
        val pic = dev.m4s4t0.kushacres.ui.photo(w, f, 720, 360, android.graphics.RectF(0f, 0f, 720f, 360f), 3f, 1.3f, dusk)
        File(dir, "snap.png").writeBytes(pic.image().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!.bytes)
    }
    f.legacy = 2; f.legacyPerks += dev.m4s4t0.kushacres.model.LegacyPerk.GREEN_THUMB; f.heirlooms += dev.m4s4t0.kushacres.model.Legacy.eligible(f).last().id
    shot("stats", 4, 1600)
    f.legacy = 0; f.legacyPerks.clear(); f.heirlooms.clear()
    f.rep["home"] = 1600; f.chains["home"] = 2
    val offers = Contracts.offers(f, "home", now)
    Contracts.take(f, "home", offers.indexOfFirst { it.tag.isEmpty() }, now)
    Contracts.take(f, "home", 0, now)
    f.deals[0].needs[0].let { it.got = it.amount / 3 }
    shot("contracts", 0, 2000) { ContractsTab() }
    val lab = Strains.hybridList.first { f.unlocked(Strains[it.parents!!.first]!!) && f.unlocked(Strains[it.parents!!.second]!!) }
    f.bred += Strains.hybridList.take(2).map { it.id }.filter { it != lab.id }
    for (p in listOf(lab.parents!!.first, lab.parents!!.second)) f.stash[p] = 999
    val (sx, sy) = (f.oy until f.oy + f.land).flatMap { b -> (f.ox until f.ox + f.land).map { it to b } }.first { (a, b) -> f.tile(a, b).kind == Kind.GRASS }
    f.buildHouse(sx, sy, WorkHouse.STORE); f.tile(sx, sy).lvl = 3
    f.breed(lab.id, now)
    shot("lab", 0) { dev.m4s4t0.kushacres.ui.LabPanel() }
    shot("stores", 0) {
        androidx.compose.foundation.layout.Row { for (i in 1..Storefronts.MAX) Px(Buildings.store(i), 60.dp) }
    }
    shot("big", 0, 600) {
        val B = dev.m4s4t0.kushacres.art.BigHouses
        val all = (1..Storefronts.MAX).map { B.sprite(WorkHouse.STORE, it) } + WorkHouse.entries.filter { it != WorkHouse.STORE && it.size == 2 }.map { B.sprite(it) }
        for (row in all.chunked(4)) androidx.compose.foundation.layout.Row { for (b in row) Px(b, 72.dp) }
    }
    run {
        // Big buildings on the farm itself, to check they sit on their 2×2.
        fun spot() = (f.oy until f.oy + f.land).flatMap { b -> (f.ox until f.ox + f.land).map { it to b } }.first { (a, b) -> f.fits(a, b, 2) }
        val placed = listOf(WorkHouse.BUNK, WorkHouse.STORE, WorkHouse.GROW).map { h -> spot().also { (a, b) -> f.buildHouse(a, b, h) } }
        val w = dev.m4s4t0.kushacres.ui.World()
        val cx = placed.sumOf { w.centerX(it.first, it.second) } / placed.size
        val cy = placed.sumOf { w.centerY(it.first, it.second) } / placed.size
        w.view(cx - 110, cy - 90, cx + 110, cy + 50, 4)
        w.render(f, now, 1.3f, dev.m4s4t0.kushacres.ui.Light(0f, -1, 0f, 0f, 0f), null, false)
        val img = w.bitmap.asImageBitmap()
        shot("farm-big", 0, 500) { androidx.compose.foundation.Image(img, null, Modifier.fillMaxSize(), filterQuality = androidx.compose.ui.graphics.FilterQuality.None) }
    }
    run {
        // The bunker: its stairs up top, then its first floor lit and planted, by day.
        f.level = 40
        val (sx, sy) = (f.oy until f.oy + f.land).flatMap { b -> (f.ox until f.ox + f.land).map { it to b } }.first { (a, b) -> f.canStairs(a, b, 1) && f.canStairs(a, b, 0) }
        f.buyBunker(sx, sy, 1)
        (f.oy until f.oy + f.land).flatMap { b -> (f.ox until f.ox + f.land).map { it to b } }.last { (a, b) -> f.tile(a, b).kind == Kind.GRASS }.let { (a, b) -> f.buildPower(a, b, dev.m4s4t0.kushacres.model.Generator.DIESEL) }
        val w = dev.m4s4t0.kushacres.ui.World()
        val day = dev.m4s4t0.kushacres.ui.Light(0f, -1, 0f, 0f, 0f)
        for (r in 0..1) {
            w.rot = r
            w.view(w.centerX(sx, sy) - 60, w.centerY(sx, sy) - 50, w.centerX(sx, sy) + 60, w.centerY(sx, sy) + 30, 4)
            w.render(f, now, 1.3f, day, null, false)
            val img = w.bitmap.asImageBitmap()
            shot("stairs$r", 0, 340) { androidx.compose.foundation.Image(img, null, Modifier.fillMaxSize(), filterQuality = androidx.compose.ui.graphics.FilterQuality.None) }
        }
        w.rot = 0
        f.goDown(1); f.widen(); f.widen()
        for (yy in f.oy until f.oy + f.land) for (xx in f.ox until f.ox + f.land) if ((xx + yy) % 3 != 0) {
            f.plant(xx, yy, Strains.ladder[(xx + yy) % 6].id, now - 86_400_000L)
            f.tile(xx, yy).progress = Strains.ladder[(xx + yy) % 6].seconds * (0.3 + 0.15 * ((xx * 7 + yy) % 5))
        }
        val cx = w.centerX(f.ox + f.land / 2, f.oy + f.land / 2); val cy = w.centerY(f.ox + f.land / 2, f.oy + f.land / 2)
        w.view(cx - 110, cy - 90, cx + 110, cy + 50, 4)
        w.render(f, now, 1.3f, day, null, false)
        val img = w.bitmap.asImageBitmap()
        shot("bunker", 0, 500) { androidx.compose.foundation.Image(img, null, Modifier.fillMaxSize(), filterQuality = androidx.compose.ui.graphics.FilterQuality.None) }
        f.goDown(0)
    }
    shot("hats", 0, 200) {
        val hs = dev.m4s4t0.kushacres.model.Farmer.HOLIDAY_HATS
        for (row in hs.chunked(5)) androidx.compose.foundation.layout.Row {
            for (h in row) Px(dev.m4s4t0.kushacres.art.People.farmer(dev.m4s4t0.kushacres.model.Farmer(hat = dev.m4s4t0.kushacres.model.Farmer.HATS.indexOf(h)), 0), 48.dp)
        }
    }
    shot("move", 0, 260) { dev.m4s4t0.kushacres.ui.MoveFarm(UiState()) }
    shot("side", 0, 560) {
        val ui = UiState()
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) { dev.m4s4t0.kushacres.ui.Rail(ui, Modifier.align(androidx.compose.ui.Alignment.TopStart)); FarmArrows(ui) }
    }
    shot("photo", 0, 360) { dev.m4s4t0.kushacres.ui.PhotoBar(UiState()) }
    shot("peek", 0, 560) {
        val ui = UiState().apply { peek = dev.m4s4t0.kushacres.model.Sites.all[3] }
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) { FarmArrows(ui) }
    }
    run {
        val I = dev.m4s4t0.kushacres.model.Interiors
        val H = dev.m4s4t0.kushacres.model.RoomKind.HOME; val S = dev.m4s4t0.kushacres.model.RoomKind.STORE
        f.shack = 4
        I.buy(f, H, M.RUG, 1, 1); I.buy(f, H, M.COUCH, 1, 3); I.buy(f, H, M.COFFEE_TABLE, 1, 1); I.buy(f, H, M.TV, 1, 0)
        I.buy(f, H, M.LAMP, 0, 0); I.buy(f, H, M.PLANT, 4, 3); I.buy(f, H, M.BED, 6, 0); I.buy(f, H, M.ARCADE, 8, 2)
        I.buy(f, H, M.FISH_TANK, 5, 4); I.buy(f, H, M.BEANBAG, 0, 4)
        I.buy(f, H, M.POSTER_LEAF, 0, 0); I.buy(f, H, M.WINDOW, 2, 0); I.buy(f, H, M.PAINTING, 4, 0); I.buy(f, H, M.CLOCK, 7, 0); I.buy(f, H, M.NEON_LEAF, 8, 0)
        I.paint(f, H, true, 2)
        I.buy(f, S, M.COUNTER, 1, 1); I.buy(f, S, M.REGISTER, 2, 0); I.buy(f, S, M.JARS, 0, 0); I.buy(f, S, M.SHELVES, 5, 0)
        I.buy(f, S, M.CASE, 7, 0); I.buy(f, S, M.BENCH, 6, 3); I.buy(f, S, M.ATM, 9, 2); I.buy(f, S, M.PLANT, 4, 3)
        I.buy(f, S, M.NEON_OPEN, 0, 0); I.buy(f, S, M.MENU_BOARD, 3, 0); I.buy(f, S, M.POSTER_BAND, 6, 0); I.paint(f, S, false, 1)
    }
    shot("home", 0, 900) { dev.m4s4t0.kushacres.ui.Interior(UiState().apply { room = dev.m4s4t0.kushacres.model.RoomKind.HOME }) }
    shot("store", 0, 900) { dev.m4s4t0.kushacres.ui.Interior(UiState().apply { room = dev.m4s4t0.kushacres.model.RoomKind.STORE }) }
    val grown = (0 until 60).flatMap { yy -> (0 until 60).map { it to yy } }.firstOrNull { (xx, yy) -> f.inLand(xx, yy) && f.tile(xx, yy).strain != null }
    if (grown != null) shot("trim", 0, 1300) { dev.m4s4t0.kushacres.ui.TrimOverlay(UiState().apply { trim = grown }) }
    // Skia's teardown can crash the JVM on the way out; the pictures are written by now.
    Runtime.getRuntime().halt(0)
}
