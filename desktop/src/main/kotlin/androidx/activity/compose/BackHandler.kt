// Android's back button is Esc on the desktop: the newest enabled BackHandler gets it.
package androidx.activity.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

object Back {
    private val handlers = ArrayList<() -> Boolean>()

    internal fun add(h: () -> Boolean) = synchronized(handlers) { handlers.add(h) }
    internal fun remove(h: () -> Boolean) = synchronized(handlers) { handlers.remove(h) }

    /** True if a screen took the back press. */
    fun press(): Boolean = synchronized(handlers) { handlers.toList() }.asReversed().any { it() }
}

@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val on = rememberUpdatedState(enabled)
    val back = rememberUpdatedState(onBack)
    DisposableEffect(Unit) {
        val h = { if (on.value) { back.value(); true } else false }
        Back.add(h)
        onDispose { Back.remove(h) }
    }
}
