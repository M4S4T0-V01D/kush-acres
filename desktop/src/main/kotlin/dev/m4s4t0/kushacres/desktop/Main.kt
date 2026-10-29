package dev.m4s4t0.kushacres.desktop

import android.app.Activity
import android.view.View
import androidx.activity.compose.Back
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
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
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.Updater
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.ui.App
import dev.m4s4t0.kushacres.ui.Keys
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

/** The desktop game: one phone-shaped (but resizable) window running the same App() as the phone. */
@OptIn(FlowPreview::class)
fun main() {
    System.getenv("KUSH_SPRITES")?.let { dumpSprites(it); return }
    System.getenv("KUSH_PANELS")?.let { dumpPanels(it); return }
    System.getenv("KUSH_SHOWCASE")?.let { dumpShowcase(it); return }
    val act = Activity()
    val view = View(act)
    Audio.init(act)
    Store.init(act)
    Audio.start()
    Updater.check(act)
    // Logging out or a kill still keeps the farm.
    Runtime.getRuntime().addShutdownHook(Thread { runCatching { Store.save() } })
    val icon = Activity::class.java.getResourceAsStream("/kush-acres.png")?.use { BitmapPainter(loadImageBitmap(it)) }
    // The window comes back where and how big it was last time.
    val win = act.getSharedPreferences("window", 0)
    val w = win.getFloat("w", 480f).coerceIn(320f, 4000f); val h = win.getFloat("h", 900f).coerceIn(400f, 4000f)
    val pos = if (win.getString("x", null) != null) WindowPosition(win.getFloat("x", 0f).dp, win.getFloat("y", 0f).dp) else WindowPosition.PlatformDefault

    application {
        val state = rememberWindowState(size = DpSize(w.dp, h.dp), position = pos)
        fun remember(s: WindowState) {
            if (s.placement != WindowPlacement.Floating || s.isMinimized) return
            val e = win.edit().putFloat("w", s.size.width.value).putFloat("h", s.size.height.value)
            (s.position as? WindowPosition.Absolute)?.let { e.putFloat("x", it.x.value).putFloat("y", it.y.value) }
            e.apply()
        }
        fun quit() { remember(state); Store.save(); Audio.stop(); exitApplication() }
        Window(
            onCloseRequest = ::quit,
            state = state,
            title = "Kush Acres",
            icon = icon,
            onKeyEvent = { e -> key(e, state) },
        ) {
            // Minimised counts as the phone app going to the background: save, go quiet, catch up on return.
            LaunchedEffect(Unit) {
                var away = false
                snapshotFlow { state.isMinimized }.collect { min ->
                    if (min) { Store.save(); Audio.stop(); away = true }
                    else if (away) { Store.resume(); Audio.start(); away = false }
                }
            }
            LaunchedEffect(Unit) {
                snapshotFlow { state.size to state.position }.debounce(1000).collect { remember(state) }
            }
            CompositionLocalProvider(LocalContext provides act, LocalView provides view) { App() }
        }
    }
}

/** Esc goes back, F11 toggles fullscreen, arrows/WASD pan, Q/E turn, +/- zoom. */
private fun key(e: KeyEvent, state: WindowState): Boolean {
    val down = e.type == KeyEventType.KeyDown
    if (e.type != KeyEventType.KeyDown && e.type != KeyEventType.KeyUp) return false
    fun hold(v: Int) = if (down) v else 0
    when (e.key) {
        Key.Escape -> return down && Back.press()
        Key.F11 -> if (down) state.placement =
            if (state.placement == WindowPlacement.Fullscreen) WindowPlacement.Floating else WindowPlacement.Fullscreen
        Key.DirectionLeft, Key.A -> Keys.dx = hold(-1)
        Key.DirectionRight, Key.D -> Keys.dx = hold(1)
        Key.DirectionUp, Key.W -> Keys.dy = hold(-1)
        Key.DirectionDown, Key.S -> Keys.dy = hold(1)
        Key.Equals, Key.Plus, Key.NumPadAdd -> Keys.zoom = hold(1)
        Key.Minus, Key.NumPadSubtract -> Keys.zoom = hold(-1)
        Key.Q -> if (down) Keys.turns -= 1
        Key.E -> if (down) Keys.turns += 1
        else -> return false
    }
    return true
}
