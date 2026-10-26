package dev.m4s4t0.kushacres.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import dev.m4s4t0.kushacres.art.S
import dev.m4s4t0.kushacres.art.Sprites
import dev.m4s4t0.kushacres.art.Sprites.TH
import dev.m4s4t0.kushacres.art.Sprites.TW
import dev.m4s4t0.kushacres.art.mix
import dev.m4s4t0.kushacres.art.shade
import dev.m4s4t0.kushacres.model.Farm
import dev.m4s4t0.kushacres.model.Stat
import dev.m4s4t0.kushacres.model.Strains
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A picture card of the farm you're on and your lifetime numbers, coloured like that farm, to save and show off. */
object ShareCard {
    const val W = 720
    const val H = 1080
    private const val CREAM = 0xFFF4E6C4.toInt()
    private const val GOLD = 0xFFFFD25A.toInt()

    fun render(f: Farm): Bitmap {
        val def = f.site.def
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val fill = Paint()
        val art = Paint().apply { isFilterBitmap = false }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }
        fun rect(l: Int, t: Int, r: Int, b: Int, col: Int) { fill.color = col; c.drawRect(l.toFloat(), t.toFloat(), r.toFloat(), b.toFloat(), fill) }
        /** A box with pixel-stepped corners and a [rim] around it. */
        fun box(l: Int, t: Int, r: Int, b: Int, col: Int, rim: Int, s: Int = 6) {
            rect(l + 2 * s, t, r - 2 * s, b, rim); rect(l + s, t + s, r - s, b - s, rim); rect(l, t + 2 * s, r, b - 2 * s, rim)
            rect(l + 2 * s, t + s, r - 2 * s, b - s, col); rect(l + s, t + 2 * s, r - s, b - 2 * s, col)
        }
        /** Pixel art blown up [k] times with its anchor (in art pixels; the bitmaps are drawn at twice that) at [x],[y]. */
        fun sprite(b: Bitmap, x: Int, y: Int, k: Int, ax: Int = b.width / 4, ay: Int = b.height / 2) {
            val l = (x - ax * 2 * k).toFloat(); val t = (y - ay * 2 * k).toFloat()
            c.drawBitmap(b, null, RectF(l, t, l + b.width * k, t + b.height * k), art)
        }
        fun say(s: String, x: Float, y: Float, size: Float, col: Int, align: Paint.Align = Paint.Align.LEFT) {
            text.textSize = size; text.textAlign = align
            text.color = 0x66000000; c.drawText(s, x + size / 12, y + size / 12, text)
            text.color = col; c.drawText(s, x, y, text)
        }

        // Card body in the farm's wood, with its edge colour as the rim.
        c.drawColor(shade(def.wood, 0.7f))
        box(16, 16, W - 16, H - 16, def.woodIn, def.edge, 8)

        // The scene: the farm itself, drawn by the same renderer as the game, at midday, framed around its land.
        val sl = 44; val st = 44; val sr = W - 44; val sb = 500
        rect(sl, st, sr, sb, def.wild)
        val now = System.currentTimeMillis()
        val w = World()
        val x0 = f.ox; val y0 = f.oy; val x1 = f.ox + f.land - 1; val y1 = f.oy + f.land - 1
        var l = w.centerX(x0, y1) - TW / 2 - 8; var r = w.centerX(x1, y0) + TW / 2 + 8
        var t = w.centerY(x0, y0) - TH / 2 - 56; var b = w.centerY(x1, y1) + TH / 2 + Sprites.DEPTH + 8
        // Widen whichever side is short so the view matches the scene box.
        val want = (sr - sl).toFloat() / (sb - st)
        if ((r - l).toFloat() / (b - t) < want) { val nw = ((b - t) * want).toInt(); l -= (nw - (r - l)) / 2; r = l + nw }
        else { val nh = ((r - l) / want).toInt(); t -= (nh - (b - t)) / 2; b = t + nh }
        w.view(l, t, r, b, S)
        val light = Light(0f, 0xFFFFFFFF.toInt(), 0f, 0f, 0f, Light.at(now).season)
        runCatching { w.render(f, now, 12f, light, null, false) }
        val shot = w.bitmap
        val sp = Paint().apply { isFilterBitmap = true; isAntiAlias = false }
        c.drawBitmap(shot, Rect(0, 0, w.vw * S, w.vh * S), RectF(sl.toFloat(), st.toFloat(), sr.toFloat(), sb.toFloat()), sp)
        // A frame around the scene.
        fill.style = Paint.Style.STROKE; fill.strokeWidth = 6f; fill.color = def.edge
        c.drawRect(sl.toFloat(), st.toFloat(), sr.toFloat(), sb.toFloat(), fill)
        fill.style = Paint.Style.FILL

        // Name plate.
        box(44, 516, W - 44, 616, def.wood, def.edge)
        say(def.name, W / 2f, 562f, if (def.name.length > 14) 40f else 48f, GOLD, Paint.Align.CENTER)
        say("Level ${f.level} · ${f.sites.size} farm${if (f.sites.size > 1) "s" else ""} · ${f.land}×${f.land} acres", W / 2f, 598f, 22f, CREAM, Paint.Align.CENTER)

        // The numbers, two to a row.
        val stats = listOf(
            "Grams sold" to "${fmt(f.stat(Stat.SOLD))}g",
            "Cash made" to money(f.stat(Stat.EARNED)),
            "Cash spent" to money(f.stat(Stat.SPENT)),
            "Grams grown" to "${fmt(f.stat(Stat.GRAMS))}g",
            "Harvests" to fmt(f.stat(Stat.HARVESTS)),
            "Golden" to fmt(f.stat(Stat.GOLDEN)),
            "Batches" to fmt(f.stat(Stat.BATCHES)),
            "Workers" to fmt(f.totalWorkers().toLong()),
        )
        val cw = (W - 88 - 16) / 2
        stats.forEachIndexed { i, (k, v) ->
            val l = 44 + (i % 2) * (cw + 16); val t = 632 + (i / 2) * 84
            box(l, t, l + cw, t + 76, shade(def.woodIn, 1.35f), shade(def.edge, 0.8f), 4)
            say(k.uppercase(), l + 20f, t + 28f, 18f, mix(CREAM, def.edge, 0.4f))
            say(v, l + 20f, t + 62f, if (v.length > 14) 24f else 30f, if (i == 1) GOLD else CREAM)
        }

        // Footer.
        val strain = Strains.all.filter { !it.hybrid && !it.exotic && it.level <= f.level }.maxByOrNull { it.level } ?: Strains.all.first()
        sprite(Sprites.plant(strain, 4, 0), 110, H - 30, 1, Sprites.PLANT_AX, Sprites.PLANT_AY)
        sprite(Sprites.plant(strain, 4, 0), W - 110, H - 30, 1, Sprites.PLANT_AX, Sprites.PLANT_AY)
        say("KUSH ACRES", W / 2f, H - 62f, 36f, GOLD, Paint.Align.CENTER)
        say(SimpleDateFormat("d MMM yyyy", Locale.US).format(Date()), W / 2f, H - 34f, 18f, mix(CREAM, def.edge, 0.4f), Paint.Align.CENTER)
        return bmp
    }

    /** File name for a card saved now. */
    fun fileName(f: Farm) = "kush-acres-${f.site.def.id}-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.png"
}
