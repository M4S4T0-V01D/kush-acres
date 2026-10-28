// On the desktop, Compose's nativeCanvas is a Skia canvas rather than an android.graphics.Canvas;
// these let the shared drawing code call it the same way.
package dev.m4s4t0.kushacres.ui

import android.graphics.Paint

fun org.jetbrains.skia.Canvas.drawText(text: String, x: Float, y: Float, p: Paint) = android.graphics.Canvas(this).drawText(text, x, y, p)

fun Fx.drawSky(c: org.jetbrains.skia.Canvas, w: Float, h: Float, scale: Float, t: Float, light: Light) =
    drawSky(android.graphics.Canvas(c), w, h, scale, t, light)

fun Fx.drawHoliday(c: org.jetbrains.skia.Canvas, w: Float, h: Float, scale: Float, t: Float, light: Light, holiday: dev.m4s4t0.kushacres.model.Holiday?) =
    drawHoliday(android.graphics.Canvas(c), w, h, scale, t, light, holiday)
