package android.text.format

import java.text.SimpleDateFormat

object DateFormat {
    /** Whether the locale writes times as 13:00 rather than 1:00 PM. */
    @JvmStatic fun is24HourFormat(ctx: android.content.Context?): Boolean =
        (java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT) as? SimpleDateFormat)?.toPattern()?.contains('H') ?: true
}
