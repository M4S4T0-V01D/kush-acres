package dev.m4s4t0.kushacres.desktop

import android.app.Activity
import android.view.View
import androidx.activity.compose.Back
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.Updater
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.ui.App

/** The desktop game: one phone-shaped (but resizable) window running the same App() as the phone. */
fun main() {
    System.getenv("KUSH_SPRITES")?.let { dumpSprites(it); return }
    val act = Activity()
    val view = View(act)
    Audio.init(act)
    Store.init(act)
    Audio.start()
    Updater.check(act)
    // Logging out or a kill still keeps the farm.
    Runtime.getRuntime().addShutdownHook(Thread { runCatching { Store.save() } })
    val icon = Activity::class.java.getResourceAsStream("/kush-acres.png")?.use { BitmapPainter(loadImageBitmap(it)) }

    application {
        val state = rememberWindowState(size = DpSize(480.dp, 900.dp))
        fun quit() { Store.save(); Audio.stop(); exitApplication() }
        Window(
            onCloseRequest = ::quit,
            state = state,
            title = "Kush Acres",
            icon = icon,
            onKeyEvent = { e -> e.type == KeyEventType.KeyDown && e.key == Key.Escape && Back.press() },
        ) {
            // Minimised counts as the phone app going to the background: save, go quiet, catch up on return.
            LaunchedEffect(Unit) {
                var away = false
                snapshotFlow { state.isMinimized }.collect { min ->
                    if (min) { Store.save(); Audio.stop(); away = true }
                    else if (away) { Store.resume(); Audio.start(); away = false }
                }
            }
            CompositionLocalProvider(LocalContext provides act, LocalView provides view) { App() }
        }
    }
}
