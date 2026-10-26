// LocalContext and LocalView aren't part of Compose for Desktop; the window provides these.
package androidx.compose.ui.platform

import androidx.compose.runtime.staticCompositionLocalOf

val LocalContext = staticCompositionLocalOf<android.content.Context> { error("no LocalContext") }
val LocalView = staticCompositionLocalOf<android.view.View> { error("no LocalView") }
