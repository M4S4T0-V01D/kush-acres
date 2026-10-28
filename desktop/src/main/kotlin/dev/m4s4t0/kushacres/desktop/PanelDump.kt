package dev.m4s4t0.kushacres.desktop

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
        shot("trim", 0, 300) { androidx.compose.foundation.layout.Column(Modifier.padding(8.dp)) { dev.m4s4t0.kushacres.ui.TrimGame { } } }
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
    // Skia's teardown can crash the JVM on the way out; the pictures are written by now.
    Runtime.getRuntime().halt(0)
}
