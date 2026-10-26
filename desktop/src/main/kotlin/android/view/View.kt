package android.view

object HapticFeedbackConstants {
    const val LONG_PRESS = 0
    const val VIRTUAL_KEY = 1
    const val KEYBOARD_TAP = 3
    const val CLOCK_TICK = 4
    const val CONFIRM = 16
    const val REJECT = 17
}

/** A computer has nothing to buzz, so feedback is a no-op. */
open class View(val context: android.content.Context) {
    fun performHapticFeedback(type: Int) = false
}
