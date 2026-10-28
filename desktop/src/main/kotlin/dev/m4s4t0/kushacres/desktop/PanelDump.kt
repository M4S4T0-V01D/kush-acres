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
    shot("cup", 3)
    shot("hunts", 4)
    Hunt.send(f, Trip.VALLEY, now)
    shot("hunts-away", 4)
    f.huntBack = now - 1
    shot("hunts-back", 4)
    shot("stats", 7)
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
    shot("lab", 2)
    shot("stores", 0) {
        androidx.compose.foundation.layout.Row { for (i in 1..Storefronts.MAX) Px(Buildings.store(i), 60.dp) }
    }
    // Skia's teardown can crash the JVM on the way out; the pictures are written by now.
    Runtime.getRuntime().halt(0)
}
