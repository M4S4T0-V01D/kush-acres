// android.graphics.Bitmap <-> ImageBitmap, as on Android: the ImageBitmap shows the bitmap's pixels as
// they are when drawn, and asAndroidBitmap() finds the bitmap again (until it's reallocated at a new size).
package androidx.compose.ui.graphics

import java.util.Collections
import java.util.WeakHashMap

private val owners = Collections.synchronizedMap(WeakHashMap<ImageBitmap, Pair<android.graphics.Bitmap, org.jetbrains.skia.Bitmap>>())
private val stale = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)

fun android.graphics.Bitmap.asImageBitmap(): ImageBitmap = sk.asComposeImageBitmap().also { owners[it] = this to sk }

fun ImageBitmap.asAndroidBitmap(): android.graphics.Bitmap {
    val (b, sk) = owners[this] ?: return stale
    return if (b.sk === sk) b else stale
}
