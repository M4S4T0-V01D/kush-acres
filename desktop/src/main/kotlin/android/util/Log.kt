package android.util

/** Logs go to stderr on the desktop. */
object Log {
    private fun out(level: String, tag: String, msg: String, t: Throwable?): Int {
        System.err.println("$level/$tag: $msg")
        t?.printStackTrace()
        return 0
    }

    @JvmStatic fun e(tag: String, msg: String, t: Throwable? = null) = out("E", tag, msg, t)
    @JvmStatic fun w(tag: String, msg: String, t: Throwable? = null) = out("W", tag, msg, t)
    @JvmStatic fun i(tag: String, msg: String, t: Throwable? = null) = out("I", tag, msg, t)
    @JvmStatic fun d(tag: String, msg: String, t: Throwable? = null) = out("D", tag, msg, t)
}
