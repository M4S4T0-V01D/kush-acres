package dev.m4s4t0.kushacres.ui

import kotlin.random.Random
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.m4s4t0.kushacres.art.Keeper
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.audio.Sfx
import dev.m4s4t0.kushacres.art.Buildings
import dev.m4s4t0.kushacres.art.DecorArt
import dev.m4s4t0.kushacres.art.People
import dev.m4s4t0.kushacres.art.S
import dev.m4s4t0.kushacres.art.Sprites
import dev.m4s4t0.kushacres.art.Sprites.TH
import dev.m4s4t0.kushacres.art.Sprites.TW
import dev.m4s4t0.kushacres.art.Town
import dev.m4s4t0.kushacres.model.Decor
import dev.m4s4t0.kushacres.model.Pheno
import dev.m4s4t0.kushacres.model.Generator
import dev.m4s4t0.kushacres.model.Special
import dev.m4s4t0.kushacres.model.Vehicle
import dev.m4s4t0.kushacres.model.Facility
import dev.m4s4t0.kushacres.model.Storefronts
import dev.m4s4t0.kushacres.model.WorkHouse
import dev.m4s4t0.kushacres.model.Farm
import dev.m4s4t0.kushacres.model.FarmEvent
import dev.m4s4t0.kushacres.model.Helper
import dev.m4s4t0.kushacres.model.Kind
import dev.m4s4t0.kushacres.model.Pet
import dev.m4s4t0.kushacres.model.Tile
import dev.m4s4t0.kushacres.model.Result
import dev.m4s4t0.kushacres.model.Strains
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** What a touch does to the squares it crosses. */
enum class Act { HARVEST, PLANT, WATER, BUILD, DEMOLISH, SHOO, COLLECT }

/** The farm, drawn each frame into a bitmap that covers just the part on screen, then scaled up with crisp pixels. */
class World {
    companion object {
        const val PAD_X = 28
        const val PAD_TOP = 44
        val W = Farm.MAX * TW + PAD_X * 2
        val H = Farm.MAX * TH + Sprites.DEPTH + PAD_TOP + 12
        val OX = W / 2
        const val OY = PAD_TOP
    }

    /** The part of the world being drawn, in art pixels, and how many bitmap pixels each art pixel gets. */
    var vx = 0; private set
    var vy = 0; private set
    var vw = W; private set
    var vh = H; private set
    var k = S; private set
    /** Holds art pixels [vx, vx+vw) × [vy, vy+vh), [k]× finer than the art grid; everything below works in art pixels. */
    var bitmap: Bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888); private set
    private var canvas = Canvas(bitmap)

    /** Aims the next render at art pixels [x0, x1) × [y0, y1), drawn at [density] bitmap pixels each. */
    fun view(x0: Int, y0: Int, x1: Int, y1: Int, density: Int) {
        vx = x0.coerceIn(0, W - 1); vy = y0.coerceIn(0, H - 1)
        vw = (x1.coerceAtMost(W) - vx).coerceAtLeast(1); vh = (y1.coerceAtMost(H) - vy).coerceAtLeast(1)
        k = density
        val bw = vw * k; val bh = vh * k
        if (bitmap.width == bw && bitmap.height == bh) return
        if (bw.toLong() * bh * 4 > bitmap.allocationByteCount) {
            // Some room to spare, so zooming out a little doesn't allocate every frame.
            bitmap = Bitmap.createBitmap(bw, bh + bh / 3, Bitmap.Config.ARGB_8888)
        }
        bitmap.reconfigure(bw, bh, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
    }

    /** Whether something standing on the tile centred at cx,cy can show inside the view. */
    private fun seen(cx: Int, cy: Int) = cx + 40 > vx && cx - 40 < vx + vw && cy + 24 > vy && cy - 84 < vy + vh
    private val dst = RectF()

    /** Draws a sprite (S× finer than the art grid) with its top-left at art pixel x,y. */
    private fun blit(b: Bitmap, x: Float, y: Float) {
        dst.set(x, y, x + b.width / S.toFloat(), y + b.height / S.toFloat())
        canvas.drawBitmap(b, null, dst, paint)
    }
    private val paint = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val fill = Paint().apply { isAntiAlias = false }
    private val mul = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY) }
    val fx = Fx()
    private var lastT = -1f
    private val rnd = kotlin.random.Random(3)
    /** Lights found while drawing, five floats each: x, y, radius, colour, strength. */
    private var lights = FloatArray(160)
    private var nLights = 0
    /** Mites drawn this frame (x, y, pulse), drawn again over the night tint so they keep glowing in the dark. */
    private var mites = FloatArray(96)
    private var nMites = 0
    private fun light(x: Float, y: Float, radius: Float, color: Int, strength: Float) {
        if (nLights * 5 + 5 > lights.size) lights = lights.copyOf(lights.size * 2)
        val i = nLights++ * 5
        lights[i] = x; lights[i + 1] = y; lights[i + 2] = radius; lights[i + 3] = Float.fromBits(color); lights[i + 4] = strength
    }

    /**
     * Quarter turns the farm is seen from. Tiles keep their farm coordinates x,y; the screen lays them out
     * by view coordinates u,v (u runs down-right, v down-left), which the turn maps them to.
     */
    var rot = 0
        set(v) { field = Math.floorMod(v, 4) }
    /** Odd turns show everything from the other side: sprites are mirrored. */
    private val flip get() = rot % 2 == 1
    private val M = Farm.MAX

    fun viewU(x: Int, y: Int) = when (rot) { 1 -> M - 1 - y; 2 -> M - 1 - x; 3 -> y; else -> x }
    fun viewV(x: Int, y: Int) = when (rot) { 1 -> x; 2 -> M - 1 - y; 3 -> M - 1 - x; else -> y }
    private fun viewU(x: Float, y: Float) = when (rot) { 1 -> M - 1 - y; 2 -> M - 1 - x; 3 -> y; else -> x }
    private fun viewV(x: Float, y: Float) = when (rot) { 1 -> x; 2 -> M - 1 - y; 3 -> M - 1 - x; else -> y }
    /** The farm tile at view square u,v. */
    fun tileOf(u: Int, v: Int): Pair<Int, Int> = when (rot) { 1 -> v to M - 1 - u; 2 -> M - 1 - u to M - 1 - v; 3 -> M - 1 - v to u; else -> u to v }

    /** Top-left pixel of the tile sprite at x,y. */
    fun tileX(x: Int, y: Int) = OX + (viewU(x, y) - viewV(x, y)) * (TW / 2) - TW / 2
    fun tileY(x: Int, y: Int) = OY + (viewU(x, y) + viewV(x, y)) * (TH / 2)
    /** The tile's centre, where things stand. */
    fun centerX(x: Int, y: Int) = OX + (viewU(x, y) - viewV(x, y)) * (TW / 2)
    fun centerY(x: Int, y: Int) = OY + (viewU(x, y) + viewV(x, y)) * (TH / 2) + TH / 2
    /** The centre of a fractional tile position (tile centres at whole numbers). */
    fun centerX(x: Float, y: Float) = OX + (viewU(x, y) - viewV(x, y)) * (TW / 2)
    fun centerY(x: Float, y: Float) = OY + (viewU(x, y) + viewV(x, y)) * (TH / 2) + TH / 2
    /** The fractional farm position (tile centres at whole numbers) under world pixel wx,wy on the ground. */
    fun groundAt(wx: Float, wy: Float): Pair<Float, Float> {
        val a = (wx - OX) / (TW / 2f); val b = (wy - OY - TH / 2f) / (TH / 2f)
        val u = (a + b) / 2; val v = (b - a) / 2
        return when (rot) { 1 -> v to M - 1 - u; 2 -> M - 1 - u to M - 1 - v; 3 -> M - 1 - v to u; else -> u to v }
    }
    /** A corner of the tile grid (tile x,y spans corners x..x+1, y..y+1). */
    private fun cornerX(a: Int, b: Int) = OX + (cornerU(a, b) - cornerV(a, b)) * (TW / 2)
    private fun cornerY(a: Int, b: Int) = OY + (cornerU(a, b) + cornerV(a, b)) * (TH / 2)
    private fun cornerU(a: Int, b: Int) = when (rot) { 1 -> M - b; 2 -> M - a; 3 -> b; else -> a }
    private fun cornerV(a: Int, b: Int) = when (rot) { 1 -> a; 2 -> M - b; 3 -> M - a; else -> b }

    /** Which tile a world pixel falls on (its top face), or null off the land. */
    fun hit(wx: Float, wy: Float): Pair<Int, Int>? {
        val a = (wx - OX) / (TW / 2f)
        val b = (wy - OY) / (TH / 2f)
        val u = floor((b + a) / 2).toInt()
        val v = floor((b - a) / 2).toInt()
        return if (u in 0 until M && v in 0 until M) tileOf(u, v) else null
    }

    /** Sprites as seen from the other side, made once each. */
    private val mirrors = object : LinkedHashMap<Bitmap, Bitmap>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Bitmap, Bitmap>?) = size > 800
    }
    private fun mirrored(b: Bitmap): Bitmap = mirrors.getOrPut(b) {
        Bitmap.createBitmap(b, 0, 0, b.width, b.height, android.graphics.Matrix().apply { setScale(-1f, 1f) }, false)
    }
    /** Draws a sprite placed for the front view at left,top; on odd turns mirrored about the line x = [cx]. Returns where it went. */
    private fun put(b: Bitmap, cx: Int, left: Int, top: Int): Pair<Bitmap, Int> {
        val (bmp, l) = if (flip) mirrored(b) to 2 * cx - left - b.width / S else b to left
        blit(bmp, l.toFloat(), top.toFloat())
        return bmp to l
    }

    /** A sprite standing on tile x,y, as last drawn: what a touch on its pixels means. */
    private class Hit(val x: Int, val y: Int, val bmp: Bitmap, val left: Int, val top: Int)
    private val hits = ArrayList<Hit>()

    /** Draws a sprite standing on tile x,y (mirrored about the tile's centre on odd turns) and remembers it for touches. */
    private fun stand(x: Int, y: Int, bmp: Bitmap, left: Int, top: Int) {
        ground(centerX(x, y).toFloat(), centerY(x, y).toFloat(), minOf(bmp.width / S * 0.42f, TW * 0.44f))
        val (b, l) = put(bmp, centerX(x, y), left, top)
        hits += Hit(x, y, b, l, top)
    }

    /** A soft contact shadow, squashed to the ground's 2:1 slope and nudged away from the afternoon sun. */
    private fun ground(cx: Float, cy: Float, half: Float) {
        fill.color = 0x30000000
        canvas.drawOval(cx - half + 1, cy - half * 0.5f + 1, cx + half + 1, cy + half * 0.5f + 1, fill)
        fill.color = 0x22000000
        canvas.drawOval(cx - half * 0.6f + 1, cy - half * 0.3f + 1, cx + half * 0.6f + 1, cy + half * 0.3f + 1, fill)
    }

    private fun solid(b: Bitmap, px: Int, py: Int) = px in 0 until b.width && py in 0 until b.height && (b.getPixel(px, py) ushr 24) > 40

    /** The tile under a world pixel: the front-most sprite whose pixels (give or take one) are there, else the ground. */
    fun pick(wx: Float, wy: Float): Pair<Int, Int>? {
        for (i in hits.indices.reversed()) {
            val h = hits[i]
            val px = floor((wx - h.left) * S).toInt(); val py = floor((wy - h.top) * S).toInt()
            if (px < -S || py < -S || px > h.bmp.width + S || py > h.bmp.height + S) continue
            if (solid(h.bmp, px, py) || solid(h.bmp, px - S, py) || solid(h.bmp, px + S, py) || solid(h.bmp, px, py - S) || solid(h.bmp, px, py + S))
                return h.x to h.y
        }
        return hit(wx, wy)
    }

    private fun hash(x: Int, y: Int) = ((x * 73856093) xor (y * 19349663)).let { if (it < 0) -it else it }

    /** Someone walking the farm, in fractional tile coordinates; [look] gives their sprite for a step frame. */
    private class Walker(
        val look: (Int) -> Bitmap, val fx: Float, val fy: Float, val moving: Boolean,
        val ax: Int = People.P_AX, val ay: Int = People.P_AY, val shadow: Float = 4f,
    )

    /**
     * Helpers stroll near the shack, a few seconds walking, then a breather. Workers keep close to the building they
     * staff; spare workers wander the plots with their watering cans.
     */
    private fun walkers(f: Farm, t: Float): List<Walker> {
        val land = f.land
        fun ok(x: Int, y: Int) = f.inLand(x, y) && f.tile(x, y).kind.let { it == Kind.GRASS || it == Kind.PATH || it == Kind.PLOT }
        /** A spot within [r] squares of [cx],[cy] (the whole farm if [r] is 0) for walker [id] in time segment [seg]. */
        fun spot(id: Int, seg: Int, cx: Int, cy: Int, r: Int, plots: Boolean = false): Pair<Int, Int> {
            for (k in 0 until 10) {
                val v = hash(id * 31 + seg, k * 17 + 5)
                val x: Int; val y: Int
                if (r == 0) { x = f.ox + v % land; y = f.oy + (v / land) % land }
                else { x = cx - r + v % (2 * r + 1); y = cy - r + (v / 97) % (2 * r + 1) }
                if (ok(x, y) && (!plots || k > 6 || f.tile(x, y).kind == Kind.PLOT)) return x to y
            }
            return if (ok(cx, cy)) cx to cy else f.ox to f.oy
        }
        fun walk(id: Int, look: (Int) -> Bitmap, pet: Boolean = false, at: (Int) -> Pair<Int, Int>): Walker {
            val tt = t + id * 1.7f
            val seg = (tt / 6f).toInt()
            val a = at(seg); val b = at(seg + 1)
            val k = ((tt / 6f - seg) * 6f / 3.5f).coerceIn(0f, 1f)
            val fx = a.first + (b.first - a.first) * k; val fy = a.second + (b.second - a.second) * k
            return if (pet) Walker(look, fx, fy, k < 1f && a != b, People.PET_AX, People.PET_AY, 5f)
            else Walker(look, fx, fy, k < 1f && a != b)
        }
        val home = f.shackAt()?.let { (x, y) -> x + 1 to y + 2 } ?: (f.ox + land / 2 to f.oy + land / 2)
        val out = Helper.entries.filter { f.has(it) }.map { h ->
            walk(h.ordinal, { People.helper(h, it) }) { seg -> spot(h.ordinal, seg, home.first, home.second, 5) }
        }.toMutableList()
        f.farmer?.let { me -> out += walk(50, { People.farmer(me, it) }) { seg -> spot(50, seg, home.first, home.second, 3, plots = true) } }
        // Pets roam the whole farm.
        f.pets.mapNotNull { n -> Pet.entries.firstOrNull { it.name == n } }.forEachIndexed { i, p ->
            out += walk(200 + i, { People.pet(p, it) }, pet = true) { seg -> spot(200 + i, seg, 0, 0, 0) }
        }
        // A festival: hippies dancing around the tents.
        if (f.festivalOn() && f.festivalSite == f.site.def.id) {
            for (i in 0 until minOf(4 + land / 3, 14)) out += walk(300 + i, { People.hippie(i, it) }) { seg -> spot(300 + i, seg, 0, 0, 0) }
            for ((i, s) in tents(f).withIndex()) out += Walker({ People.tent(i) }, s.first + 0.5f, s.second + 0.5f, false, People.TENT_AX, People.TENT_AY, 9f)
        }
        val posts = f.houses().filter { it.staffed }
        for (i in 0 until f.site.workers) {
            val post = posts.getOrNull(i)
            out += walk(100 + i, { People.worker(i, it) }) { seg ->
                if (post != null) spot(100 + i, seg, post.x, post.y, 2) else spot(100 + i, seg, 0, 0, 0, plots = true)
            }
        }
        return out
    }

    private fun drawWalker(w: Walker, t: Float) {
        val sx = centerX(w.fx, w.fy)
        val sy = centerY(w.fx, w.fy)
        val frame = if (w.moving) ((t * 6).toInt() and 1) else 0
        fill.color = 0x40000000
        canvas.drawOval(sx - w.shadow, sy - 1, sx + w.shadow, sy + 2, fill)
        blit(w.look(frame), (sx - w.ax).toInt().toFloat(), (sy - w.ay).toInt().toFloat())
    }

    /** Where festival tents pitch: a few open grass squares, the same ones every frame. */
    private fun tents(f: Farm): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>()
        val n = minOf(2 + f.land / 6, 6)
        for (k in 0 until 200) {
            if (out.size >= n) break
            val v = hash(k * 13 + 7, f.land)
            val x = f.ox + v % f.land; val y = f.oy + (v / 31) % f.land
            if (f.tile(x, y).kind == Kind.GRASS && out.none { abs(it.first - x) + abs(it.second - y) < 3 }) out += x to y
        }
        return out
    }

    fun render(f: Farm, now: Long, t: Float, light: Light, selected: Pair<Int, Int>?, placing: Boolean, moveFrom: Pair<Int, Int>? = null) {
        bitmap.eraseColor(0)
        canvas.save()
        canvas.scale(k.toFloat(), k.toFloat())
        canvas.translate(-vx.toFloat(), -vy.toFloat())
        try { draw(f, now, t, light, selected, placing, moveFrom) } finally { canvas.restore() }
    }

    private fun draw(f: Farm, now: Long, t: Float, light: Light, selected: Pair<Int, Int>?, placing: Boolean, moveFrom: Pair<Int, Int>?) {
        hits.clear()
        nLights = 0; nMites = 0
        val dt = if (lastT < 0) 0f else (t - lastT).coerceIn(0f, 0.1f)
        lastT = t
        val land = f.land
        val ox = f.ox; val oy = f.oy
        val def = f.site.def
        val ground = f.site.grass ?: def.ground
        val dark = !f.powered()
        val buildMode = placing
        val wave = (t * 2.5f).toInt()
        // Ground, back to front.
        for (s in 0..(Farm.MAX - 1) * 2) for (u in 0..s) {
            if (u >= Farm.MAX || s - u >= Farm.MAX) continue
            val (x, y) = tileOf(u, s - u)
            if (!seen(centerX(x, y), centerY(x, y))) continue
            val tile = f.tile(x, y)
            val inside = f.inLand(x, y)
            val bmp = when {
                tile.kind == Kind.PLOT -> Sprites.plot(f.wet(x, y, now), f.site.dirt)
                tile.kind == Kind.PATH -> Decor.of(tile.item)?.let { DecorArt.sprite(it) } ?: Sprites.grass(0, false, ground)
                tile.kind == Kind.WATER -> {
                    fun w(a: Int, b: Int) = a !in 0 until Farm.MAX || b !in 0 until Farm.MAX || f.tile(a, b).kind == Kind.WATER
                    val deep = w(x - 1, y) && w(x + 1, y) && w(x, y - 1) && w(x, y + 1)
                    Sprites.water((wave + hash(x, y) % 2) % Sprites.WATER_FRAMES, deep)
                }
                else -> Sprites.grass(hash(x, y) % 4, !inside, if (inside) ground else def.wild)
            }
            blit(bmp, tileX(x, y).toFloat(), tileY(x, y).toFloat())
            if (buildMode && inside && tile.kind == Kind.GRASS) {
                paint.alpha = 110
                blit(Sprites.cursor(0xFFFFFFFF.toInt()), tileX(x, y).toFloat(), tileY(x, y).toFloat())
                paint.alpha = 255
            }
        }
        // A fence along the edge of what you own.
        if (land < Farm.MAX) {
            fill.color = 0xFF8B6A44.toInt()
            fun post(a: Int, b: Int) {
                val px = cornerX(a, b); val py = cornerY(a, b)
                canvas.drawRect(px.toFloat(), py - 5f, px + 1f, py.toFloat(), fill)
            }
            // Only the sides that face the wild; a side on the edge of the map needs no fence.
            for (j in 0..land) {
                if (ox + land < Farm.MAX) post(ox + land, oy + j)
                if (oy + land < Farm.MAX) post(ox + j, oy + land)
                if (ox > 0) post(ox, oy + j)
                if (oy > 0) post(ox + j, oy)
            }
        }
        selected?.let { (x, y) ->
            blit(Sprites.cursor(0xFFFFE27A.toInt()), tileX(x, y).toFloat(), tileY(x, y).toFloat())
        }
        moveFrom?.let { (mx, my) ->
            val t0 = f.tile(mx, my)
            val cells = when (t0.kind) {
                Kind.SHACK, Kind.PART -> f.shackAt()?.let { (ax, ay) -> listOf(ax to ay, ax + 1 to ay, ax to ay + 1, ax + 1 to ay + 1) } ?: emptyList()
                Kind.VEHICLE, Kind.CAB -> {
                    val a = if (t0.kind == Kind.CAB) t0.anchor else my * Farm.MAX + mx
                    val ax = a % Farm.MAX; val ay = a / Farm.MAX
                    listOf(ax to ay, ax + 1 to ay, ax to ay + 1, ax + 1 to ay + 1)
                }
                else -> listOf(mx to my)
            }
            val c = if ((t * 3).toInt() % 2 == 0) 0xFF7BD34B.toInt() else 0xFFB6F27A.toInt()
            cells.forEach { (x, y) -> blit(Sprites.cursor(c), tileX(x, y).toFloat(), tileY(x, y).toFloat()) }
        }
        // Lamp light pools under everything standing.
        fill.color = 0x30FFE27A
        if (!dark) for (y in oy until oy + land) for (x in ox until ox + land) if (f.tile(x, y).kind == Kind.LAMP && seen(centerX(x, y), centerY(x, y))) {
            val cx = centerX(x, y).toFloat(); val cy = centerY(x, y).toFloat()
            canvas.drawOval(cx - TW * 1.5f, cy - TH * 1.5f, cx + TW * 1.5f, cy + TH * 1.5f, fill)
        }
        // Things standing on the land, back to front. The shack is drawn once its whole footprint is behind us,
        // helpers once the diagonal they stand on is done.
        val shackAt = f.shackAt()
        val staffed = f.houses().filter { it.staffed }.map { it.x to it.y }.toSet().ifEmpty { null }
        val walkers = walkers(f, t).groupBy { Math.round(viewU(it.fx, it.fy) + viewV(it.fx, it.fy)) }
        // The shack's 2×2 footprint starts, in view squares, at its corner nearest the back of the screen.
        val shackU = shackAt?.let { (ax, ay) -> minOf(viewU(ax, ay), viewU(ax + 1, ay + 1)) } ?: 0
        val shackV = shackAt?.let { (ax, ay) -> minOf(viewV(ax, ay), viewV(ax + 1, ay + 1)) } ?: 0
        // Turned a half or quarter round the other way, the door (+y side) faces away: show the back.
        val shackBack = rot == 1 || rot == 2
        // Vehicles cover 2×2 like the shack: each is drawn once, at the diagonal past its footprint.
        val vehicles = Vehicle.entries.mapNotNull { v -> f.vehicleAt(v)?.let { (ax, ay) ->
            val u = minOf(viewU(ax, ay), viewU(ax + 1, ay + 1)); val w = minOf(viewV(ax, ay), viewV(ax + 1, ay + 1))
            Triple(v, ax to ay, u to w)
        } }
        for (s in 0..(Farm.MAX - 1) * 2) {
          for ((v, a, uw) in vehicles) {
              val (u, w) = uw
              if (s != u + w + 2) continue
              val bx = OX + (u - w) * (TW / 2); val by = OY + (u + w) * (TH / 2) + 32
              val top = by - Buildings.VEH_AY
              ground(bx.toFloat(), by - TH.toFloat(), TW * 0.82f)
              val (art, sl) = put(Buildings.vehicle(v), bx, bx - Buildings.VEH_AX, top)
              hits += Hit(a.first, a.second, art, sl, top)
          }
          if (shackAt != null && s == shackU + shackV + 2) {
              val (ax, ay) = shackAt
              val bx = OX + (shackU - shackV) * (TW / 2); val by = OY + (shackU + shackV) * (TH / 2) + 32
              ground(bx.toFloat(), by - TH.toFloat(), TW * 0.9f)
              val (art, sl) = put(Buildings.shack(f.shack, f.roof, f.wall, shackBack), bx, bx - Buildings.SHACK_AX, by - Buildings.SHACK_AY)
              val st = by - Buildings.SHACK_AY
              hits += Hit(ax, ay, art, sl, st)
              for ((wx0, wy) in Buildings.shackLights(f.shack, shackBack)) {
                  val wx = if (flip) Buildings.SHACK_W - 1 - wx0 else wx0
                  val flick = 0.9f + 0.1f * sin(t * 7 + wx0)
                  light(sl + wx + 0.5f, st + wy + 0.5f, 9f, 0xFFB050, 0.85f * flick)
              }
              if (f.shack >= 1) {
                  // Chimney smoke.
                  fill.color = 0x90E8E8E8.toInt()
                  for (k in 0 until 3) {
                      val life = (t * 0.6f + k / 3f) % 1f
                      val px = bx + (if (flip) -1 else 1) * (10 + life * 6) + sin(t * 2 + k) * 1.5f
                      val py = by - Buildings.SHACK_AY + 4 - life * 14
                      val r = 1 + life * 2
                      canvas.drawRect(px - r, py - r, px + r, py + r, fill)
                  }
              }
          }
          for (u in 0..s) {
            if (u >= Farm.MAX || s - u >= Farm.MAX) continue
            val (x, y) = tileOf(u, s - u)
            val cx = centerX(x, y); val cy = centerY(x, y)
            if (!seen(cx, cy)) continue
            val tile = f.tile(x, y)
            if (!f.inLand(x, y)) {
                if (tile.kind == Kind.ROCK) { put(Sprites.rock(hash(y, x) % 6), cx, cx - 10, cy - 13); continue }
                if (tile.kind != Kind.GRASS) continue
                val h = hash(x, y) % 7
                // The ring right around the farm stays clear.
                val edge = x in ox - 1..ox + land && y in oy - 1..oy + land
                if (h < def.trees && !edge) put(Sprites.tree(if (def.pines) 2 + 3 * (hash(y, x) % 2) else hash(y, x) % 6), cx, cx - 13, cy - 36)
                else if (h == def.trees && def.trees > 0) put(Sprites.bush(hash(y, x) % 4), cx, cx - 7, cy - 8)
                continue
            }
            when (tile.kind) {
                Kind.PLOT -> {
                    val st = tile.strain?.let { Strains[it] } ?: continue
                    val frac = tile.progress / st.seconds
                    val stage = when { frac >= 1 -> 5; frac >= 0.7 -> 4; frac >= 0.45 -> 3; frac >= 0.22 -> 2; frac >= 0.07 -> 1; else -> 0 }
                    // A gentle breeze: the top leans up to two fine pixels either way, gusts rolling across the field.
                    val sway = if (stage < 2) 0 else Math.round(sin(t * 1.6f + (x + y) * 0.6f + (hash(x, y) % 10) * 0.1f) * 2f)
                    stand(x, y, Sprites.plant(st, stage, sway, if (stage >= 4) Pheno.colour(tile.pheno) else 0), cx - Sprites.PLANT_AX, cy - Sprites.PLANT_AY + 2)
                    if (stage == 5) light(cx.toFloat(), cy - 24f, 10f, 0xC8FF9A, 0.3f + 0.1f * sin(t * 2 + hash(x, y)))
                    if (stage == 5) {
                        // Twinkles over a ripe plant.
                        val k = ((t * 3 + hash(x, y) % 10) % 3).toInt()
                        fill.color = 0xFFFFF6C0.toInt()
                        val sx = cx + intArrayOf(-5, 4, 0)[k]; val sy = cy - 26 + intArrayOf(2, 6, -2)[k]
                        canvas.drawRect(sx.toFloat(), sy - 1f, sx + 1f, sy + 2f, fill)
                        canvas.drawRect(sx - 1f, sy.toFloat(), sx + 2f, sy + 1f, fill)
                        if (f.peakLeft(x, y, now) > 0) drawPeak(cx, cy, t, hash(x, y))
                    } else if (!f.wet(x, y, now) && ((t * 2).toInt() + hash(x, y)) % 4 != 0) {
                        // A little drop asking for water.
                        fill.color = 0xFF6EC3FF.toInt()
                        val dy = cy - 8 - stage * 4 - (if ((t * 2).toInt() % 2 == 0) 0 else 1)
                        canvas.drawRect(cx + 6f, dy.toFloat(), cx + 7f, dy + 1f, fill)
                        canvas.drawRect(cx + 5f, dy + 1f, cx + 8f, dy + 3f, fill)
                        fill.color = 0xFF2C6FA8.toInt()
                        canvas.drawRect(cx + 5f, dy + 3f, cx + 8f, dy + 4f, fill)
                    }
                }
                Kind.SPRINKLER -> {
                    stand(x, y, Sprites.sprinkler, cx - 7, cy - 17)
                    fill.color = 0xCC8FD4FF.toInt()
                    for (k in 0 until 8) {
                        val a = t * 2.2f + k * (PI.toFloat() / 4)
                        val life = ((t * 1.7f + k * 0.37f) % 1f)
                        val r = 4 + life * 26
                        val px = cx + cos(a) * r; val py = cy - 14 + sin(a) * r * 0.5f + (life * life * 30 - life * 20)
                        canvas.drawRect(px, py, px + 1, py + 1, fill)
                    }
                }
                Kind.LAMP -> {
                    stand(x, y, Sprites.lamp, cx - 8, cy - 34)
                    // Short on power the lamps go dark, with a slow red warning blink.
                    if (dark) {
                        if ((t * 1.5f).toInt() % 2 == 0) light(cx.toFloat(), cy - 30f, 4f, 0xFF4030, 0.8f)
                    } else {
                        light(cx.toFloat(), cy - 30f, 9f, 0xFFE8A0, 1f)
                        light(cx.toFloat(), cy.toFloat(), TW * 1.6f, 0xFFC860, 0.55f)
                    }
                }
                Kind.POWER -> Generator.of(tile.item)?.let { g ->
                    stand(x, y, Buildings.generator(g), cx - Buildings.FAC_AX, cy - Buildings.FAC_AY)
                    light(cx.toFloat(), cy - 12f, 7f, 0xFFF080, 0.6f + 0.15f * sin(t * 6 + x * 3))
                }
                Kind.SPECIAL -> Special.of(tile.item)?.let { sp ->
                    stand(x, y, Buildings.special(sp), cx - Buildings.FAC_AX, cy - Buildings.FAC_AY)
                    light(cx.toFloat(), cy - 16f, 12f, 0xD8A0FF, 0.45f + 0.15f * sin(t * 2 + y))
                }
                Kind.ROCK -> stand(x, y, Sprites.rock(hash(y, x) % 6), cx - 10, cy - 13)
                Kind.GRASS -> if (def.trees > 0 && hash(x, y) % 11 == 0) put(Sprites.bush(hash(y, x) % 4), cx, cx - 7, cy - 8)
                Kind.FACILITY -> drawFacility(f, tile, x, y, cx, cy, now, t)
                Kind.HOUSE -> WorkHouse.of(tile.item)?.let { h ->
                    stand(x, y, if (h == WorkHouse.STORE) Buildings.store(tile.lvl) else Buildings.house(h), cx - Buildings.FAC_AX, cy - Buildings.FAC_AY)
                    // A staffed building keeps a light on.
                    if (staffed?.contains(x to y) == true) light(cx.toFloat(), cy - 14f, 8f, 0xFFC060, 0.7f + 0.1f * sin(t * 5 + x))
                }
                Kind.DECOR -> Decor.of(tile.item)?.let {
                    stand(x, y, DecorArt.sprite(it), cx - DecorArt.AX, cy - DecorArt.AY)
                    if (it == Decor.TIKI || it == Decor.LAVA || it == Decor.NEON || it == Decor.DISCO || it == Decor.UFO || it == Decor.CRYSTAL) {
                        fill.color = 0x28FFB040
                        canvas.drawOval(cx - 14f, cy - 7f, cx + 14f, cy + 7f, fill)
                        val c = when (it) {
                            Decor.TIKI -> 0xFF9030; Decor.LAVA -> 0xFF50C0; Decor.DISCO -> discoColor(t)
                            Decor.UFO -> 0x60FFD0; Decor.CRYSTAL -> 0xC080FF; else -> 0x60FF90
                        }
                        val flick = if (it == Decor.TIKI) 0.8f + 0.2f * sin(t * 11 + x) * sin(t * 7 + y) else 1f
                        light(cx.toFloat(), cy - DecorArt.AY + 8f, 12f, c, 0.8f * flick)
                        light(cx.toFloat(), cy.toFloat(), 20f, c, 0.35f * flick)
                    }
                }
                else -> {}
            }
            if (tile.pest && tile.kind == Kind.PLOT) drawPests(cx, cy, t, hash(x, y))
            if (tile.mould && tile.kind == Kind.PLOT) drawMould(cx, cy, t, hash(x, y))
          }
          walkers[s]?.forEach { drawWalker(it, t) }
        }

        // Rain splashing on the farm.
        if (light.rain > 0) repeat((light.rain * 60 * dt + rnd.nextFloat()).toInt()) {
            val x = ox + rnd.nextInt(land); val y = oy + rnd.nextInt(land)
            fx.burst(Burst.SPLASH, centerX(x, y) + (rnd.nextFloat() - 0.5f) * TW * 0.7f, centerY(x, y) + (rnd.nextFloat() - 0.5f) * TH * 0.6f)
        }
        fx.step(dt)
        fx.draw(canvas)

        // Time of day and weather: darken and colour everything, then let the lights shine through.
        if (light.tint != -1) {
            mul.color = light.tint
            canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), mul)
        }
        val glow = ((light.night - 0.1f) / 0.6f).coerceIn(0f, 1f) + light.rain * 0.3f + light.cloud * 0.15f
        if (glow > 0.01f) {
            for (i in 0 until nLights) {
                val k = i * 5
                val radius = lights[k + 2]
                fx.glow(canvas, lights[k], lights[k + 1], radius, lights[k + 3].toRawBits(), lights[k + 4] * minOf(1f, glow),
                    if (radius > 14) 0.5f else 1f)
            }
            // Fireflies drift over the land on clear nights.
            val ff = minOf(1f, glow) * (1f - light.rain) * (1f - light.fog * 0.5f)
            if (ff > 0.05f) for (i in 0 until 6 + land) {
                val bx = ox + hash(i, 11) % land; val by = oy + hash(i, 23) % land
                val fxp = centerX(bx, by) + sin(t * 0.37f + i * 1.9f) * 18f + sin(t * 1.1f + i) * 3f
                val fyp = centerY(bx, by) - 10f + sin(t * 0.29f + i * 2.7f) * 9f + sin(t * 1.7f + i * 0.5f) * 2f
                val blink = ((sin(t * 1.3f + i * 2.1f) - 0.2f) / 0.8f).coerceIn(0f, 1f)
                if (blink <= 0) continue
                fx.glow(canvas, fxp, fyp, 5f, 0xC8FF60, ff * blink * 0.9f)
                fill.color = 0xFFF4FFB0.toInt(); fill.alpha = (255 * ff * blink).toInt()
                canvas.drawRect(floor(fxp * 2) / 2, floor(fyp * 2) / 2, floor(fxp * 2) / 2 + 0.5f, floor(fyp * 2) / 2 + 0.5f, fill)
            }
        }
        if (light.tint != -1) for (i in 0 until nMites) miteArt(mites[i * 3], mites[i * 3 + 1], mites[i * 3 + 2])
    }

    private fun drawFacility(f: Farm, tile: Tile, x: Int, y: Int, cx: Int, cy: Int, now: Long, t: Float) {
        val fac = Facility.of(tile.item) ?: return
        stand(x, y, Buildings.facility(fac), cx - Buildings.FAC_AX, cy - Buildings.FAC_AY)
        // Level pips along the front.
        fill.color = 0xFFF2C440.toInt()
        for (i in 0 until tile.lvl) canvas.drawRect(cx - 5f + i * 2.5f, cy + 3f, cx - 4f + i * 2.5f, cy + 4f, fill)
        if (tile.strain == null) return
        val top = cy - Buildings.FAC_AY - 2
        if (now >= tile.doneAt) {
            // Done: the product bobs above the roof.
            val bob = if ((t * 2.5f).toInt() % 2 == 0) 0 else 1
            stand(x, y, Town.goods(fac), cx - 6, top - 11 - bob)
            return
        }
        val total = (fac.millis(tile.lvl) / (1 + 0.25 * f.houseNear(x, y, WorkHouse.DRYING))).toFloat()
        val frac = (1f - (tile.doneAt - now) / total).coerceIn(0f, 1f)
        fill.color = 0xFF1C1A14.toInt(); canvas.drawRect(cx - 9f, top - 1f, cx + 9f, top + 3f, fill)
        fill.color = 0xFF7BD34B.toInt(); canvas.drawRect(cx - 8f, top.toFloat(), cx - 8f + 16 * frac, top + 2f, fill)
        // Puffs of steam while it works.
        fill.color = 0x80F0F0F0.toInt()
        for (k in 0 until 2) {
            val life = (t * 0.8f + k * 0.5f) % 1f
            val r = 1 + life * 2
            val px = cx + 6 + sin(t * 3 + k * 2) * 2; val py = top - 3 - life * 10
            canvas.drawRect(px - r, py - r, px + r, py + r, fill)
        }
    }

    /** A plant at its peak: a gold glow and gold sparks circling it, so it's worth picking now. */
    private fun drawPeak(cx: Int, cy: Int, t: Float, seed: Int) {
        val pulse = 0.5f + 0.5f * sin(t * 3f + seed % 9)
        light(cx.toFloat(), cy - 20f, 16f, 0xFFD84A, 0.45f + 0.25f * pulse)
        for (k in 0 until 3) {
            val a = t * 1.8f + k * 2.094f + seed % 9
            val sx = cx + cos(a) * 9f; val sy = cy - 20 + sin(a) * 4f - (if (sin(a) < 0) 4 else 0)
            fill.color = 0xFFFFD84A.toInt(); canvas.drawRect(sx - 1, sy - 1, sx + 2, sy + 2, fill)
            fill.color = 0xFFFFFBE0.toInt(); canvas.drawRect(sx, sy, sx + 1, sy + 1, fill)
        }
    }

    private fun drawPests(cx: Int, cy: Int, t: Float, seed: Int) {
        // Red mites crawling over the plant, glowing so they stand out by day and at night.
        val pulse = 0.5f + 0.5f * sin(t * 4f + seed % 7)
        light(cx.toFloat(), cy - 14f, 14f, 0xFF3A2A, 0.35f + 0.25f * pulse)
        for (k in 0 until 6) {
            val a = t * (1.6f + k * 0.45f) + k * 1.7f + seed % 7
            val bx = cx + cos(a) * (4 + k % 3 * 2); val by = cy - 14 + sin(a * 1.3f) * 6
            mite(bx, by, pulse)
        }
        val blink = (t * 3).toInt() % 2 == 0
        fill.color = 0xFF1C1A14.toInt(); canvas.drawRect(cx - 2f, cy - 33f, cx + 2f, cy - 24f, fill)
        fill.color = if (blink) 0xFFFF3A2A.toInt() else 0xFFFF9A7A.toInt()
        canvas.drawRect(cx - 1f, cy - 32f, cx + 1f, cy - 28f, fill)
        canvas.drawRect(cx - 1f, cy - 27f, cx + 1f, cy - 25f, fill)
    }

    /** One mite: a bright red body with a darker rim and a hot core that pulses. */
    private fun mite(x: Float, y: Float, pulse: Float) {
        if (nMites * 3 + 3 > mites.size) mites = mites.copyOf(mites.size * 2)
        mites[nMites * 3] = x; mites[nMites * 3 + 1] = y; mites[nMites * 3 + 2] = pulse; nMites++
        miteArt(x, y, pulse)
    }

    private fun miteArt(x: Float, y: Float, pulse: Float) {
        fill.color = 0xFF7A0E0A.toInt(); canvas.drawRect(x - 1, y - 1, x + 2, y + 2, fill)
        fill.color = 0xFFFF2A1E.toInt(); canvas.drawRect(x, y - 1, x + 1, y + 2, fill); canvas.drawRect(x - 1, y, x + 2, y + 1, fill)
        fill.color = if (pulse > 0.5f) 0xFFFFC0A0.toInt() else 0xFFFF6A4A.toInt(); canvas.drawRect(x, y, x + 1, y + 1, fill)
    }

    /** Mould is a nest of mites: red clumps webbed over the plant, glowing and slowly pulsing. */
    private fun drawMould(cx: Int, cy: Int, t: Float, seed: Int) {
        val pulse = 0.5f + 0.5f * sin(t * 2.5f + seed % 5)
        light(cx.toFloat(), cy - 12f, 15f, 0xFF2A4A, 0.3f + 0.25f * pulse)
        val r = Random(seed)
        fill.color = 0x66FFB0B0
        for (k in 0 until 4) {
            val wx = cx - 7 + r.nextInt(14); val wy = cy - 18 + r.nextInt(12)
            canvas.drawRect(wx.toFloat(), wy.toFloat(), wx + 5f, wy + 1f, fill)
        }
        for (k in 0 until 8) {
            val bx = cx - 7 + r.nextInt(14); val by = cy - 18 + r.nextInt(14)
            mite(bx + sin(t * 1.5f + k) * 0.8f, by.toFloat(), if (k % 2 == 0) pulse else 1 - pulse)
        }
    }
}

private const val HOLD_MS = 140L
/** A still hold this long opens the tile menu (the phone's right click). */
private const val MENU_MS = 520L

private class Floater(val wx: Float, val wy: Float, val text: String, val color: Int, val born: Long, val big: Boolean = false)

@Composable
fun FarmView(ui: UiState, modifier: Modifier = Modifier) {
    val world = remember { World() }
    val image = remember { arrayOfNulls<ImageBitmap>(1) }
    val view = LocalView.current
    val prefs = remember { view.context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    var frame by remember { mutableLongStateOf(0L) }
    var scale by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val floaters = remember { ArrayList<Floater>() }
    val textPaint = remember {
        Paint().apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); isAntiAlias = true; textAlign = Paint.Align.CENTER }
    }

    LaunchedEffect(Unit) {
        var lastTick = 0L
        while (true) {
            withFrameMillis { ms ->
                frame = ms
                if (ms - lastTick > 250) { Store.tick(); lastTick = ms }
            }
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val f = Store.farm
        fun fit(): Float = wPx / (f.land * TW + 24f)
        fun minScale() = wPx / (World.W + 40f)
        fun clamp() {
            // Keep some of the island on screen.
            val bw = World.W * scale; val bh = World.H * scale
            offset = Offset(offset.x.coerceIn(wPx * 0.5f - bw, wPx * 0.5f), offset.y.coerceIn(hPx * 0.35f - bh, hPx * 0.6f))
        }
        fun centerOnLand() {
            scale = fit()
            val cx = f.ox + (f.land - 1) / 2f; val cy = f.oy + (f.land - 1) / 2f
            val wx = world.centerX(cx, cy); val wy = world.centerY(cx, cy)
            offset = Offset(wPx / 2 - wx * scale, hPx * 0.5f - wy * scale)
        }
        if (ui.rot < 0) ui.rot = prefs.getInt("rot", 0)
        if (Keys.turns != 0) { ui.turn(Keys.turns); Keys.turns = 0 }
        // Held pan/zoom keys move the camera a little every frame, at the same speed whatever the zoom.
        LaunchedEffect(wPx, hPx) {
            var last = 0L
            while (true) withFrameMillis { ms ->
                val dt = if (last == 0L) 16f else (ms - last).coerceIn(1, 50).toFloat()
                last = ms
                if (scale == 0f || !Keys.held) return@withFrameMillis
                if (Keys.zoom != 0) {
                    val c = Offset(wPx / 2, hPx / 2)
                    val ns = (scale * 1.0025f.pow(Keys.zoom * dt)).coerceIn(minScale(), 12f)
                    offset = c - (c - offset) * (ns / scale); scale = ns
                }
                val step = 0.9f * dt
                offset -= Offset(Keys.dx * step, Keys.dy * step)
                clamp()
            }
        }
        if (world.rot != ui.rot) {
            // Turn about whatever is in the middle of the screen.
            val mid = (Offset(wPx / 2, hPx / 2) - offset) / scale
            val (gx, gy) = world.groundAt(mid.x, mid.y)
            world.rot = ui.rot
            if (scale != 0f) offset = Offset(wPx / 2 - world.centerX(gx, gy) * scale, hPx / 2 - world.centerY(gx, gy) * scale)
            prefs.edit().putInt("rot", ui.rot).apply()
        }
        // The view (the square in the middle of the screen and the zoom) is kept per farm, so it comes back as you left it.
        val camKey = "cam-" + f.site.def.id
        if (scale == 0f && wPx > 0f) {
            val cam = prefs.getString(camKey, null)?.split(",")?.mapNotNull { it.toFloatOrNull() }
            if (cam?.size == 3 && cam.all { it.isFinite() }) {
                scale = cam[2].coerceIn(minScale(), 12f)
                offset = Offset(wPx / 2 - world.centerX(cam[0], cam[1]) * scale, hPx / 2 - world.centerY(cam[0], cam[1]) * scale)
                clamp()
            } else centerOnLand()
        }
        if (ui.recenter != 0) { centerOnLand(); ui.recenter = 0 }
        val savedCam = remember { arrayOf("", "") }
        val savedAt = remember { longArrayOf(0L) }
        SideEffect {
            // At most every second and a half: on the desktop each write is a file.
            if (scale == 0f || System.currentTimeMillis() - savedAt[0] < 1500) return@SideEffect
            savedAt[0] = System.currentTimeMillis()
            val mid = (Offset(wPx / 2, hPx / 2) - offset) / scale
            val (gx, gy) = world.groundAt(mid.x, mid.y)
            val cam = "%.1f,%.1f,%.3f".format(java.util.Locale.ROOT, gx, gy, scale)
            if (savedCam[0] != camKey || savedCam[1] != cam) {
                savedCam[0] = camKey; savedCam[1] = cam
                prefs.edit().putString(camKey, cam).apply()
            }
        }

        fun toWorld(p: Offset) = (p - offset) / scale
        fun tileAt(p: Offset): Pair<Int, Int>? {
            val w = toWorld(p)
            return world.pick(w.x, w.y)
        }

        fun buzz() { if (Audio.haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }

        fun complain(r: Result) {
            if (r == Result.NO_CASH || r == Result.STASH_FULL || r == Result.LOCKED || r == Result.LIMIT) Audio.play(Sfx.ERROR)
            when (r) {
                Result.NO_CASH -> Store.toast("Not enough cash")
                Result.STASH_FULL -> Store.toast("Stash is full! Sell some bud at ${keeper(Keeper.DEX)}'s in town")
                Result.LOCKED -> Store.toast("Unlocks at a higher level")
                Result.LIMIT -> Store.toast("No room for more decor. Upgrade your shack!")
                Result.NO_POWER -> Store.toast("Not enough power on this farm. Build a generator!")
                else -> {}
            }
        }

        /** What a touch starting on x,y should do, given the current mode. */
        fun actFor(x: Int, y: Int): Act? {
            val now = Store.now()
            val tile = f.tile(x, y)
            if (!f.inLand(x, y)) return null
            return when (ui.mode) {
                is Mode.Build, is Mode.PlaceDecor -> if (tile.kind == Kind.GRASS) Act.BUILD else null
                is Mode.PlaceFacility, is Mode.PlaceHouse, is Mode.PlacePower, is Mode.PlaceSpecial, is Mode.PlaceVehicle -> null
                is Mode.Move -> null
                Mode.Demolish -> null
                Mode.Farm -> when {
                    tile.pest || tile.mould -> Act.SHOO
                    tile.kind == Kind.FACILITY -> if (tile.strain != null && now >= tile.doneAt) Act.COLLECT else null
                    tile.kind != Kind.PLOT -> null
                    tile.ready() -> Act.HARVEST
                    tile.strain == null -> Act.PLANT
                    !f.wet(x, y, now) -> Act.WATER
                    else -> null
                }
            }
        }

        fun apply(act: Act, x: Int, y: Int, first: Boolean): Boolean {
            val now = Store.now()
            val tile = f.tile(x, y)
            val r = when (act) {
                Act.HARVEST -> if (tile.ready()) f.harvest(x, y, now) else return false
                Act.PLANT -> if (tile.kind == Kind.PLOT && tile.strain == null) f.plant(x, y, ui.seed, now) else return false
                Act.WATER -> if (tile.kind == Kind.PLOT && tile.strain != null && !tile.ready() && !f.wet(x, y, now)) f.water(x, y, now) else return false
                Act.BUILD -> if (tile.kind != Kind.GRASS) return false else when (val m = ui.mode) {
                    is Mode.Build -> f.build(x, y, m.kind)
                    is Mode.PlaceDecor -> f.placeDecor(x, y, m.d).also {
                        // Out of stock and can't be bought: back to farming.
                        if (it == Result.OK && m.d.black && (f.decorInv[m.d.name] ?: 0) == 0) ui.mode = Mode.Farm
                    }
                    else -> return false
                }
                Act.SHOO -> f.clearPest(x, y)
                Act.COLLECT -> {
                    val n = tile.amount; val fac = f.facility(x, y)
                    f.collect(x, y, now).also {
                        if (it == Result.OK) floaters += Floater(world.centerX(x, y).toFloat(), world.centerY(x, y) - 30f,
                            "+$n ${fac?.product?.lowercase()}", 0xFFF2C440.toInt(), now)
                    }
                }
                Act.DEMOLISH -> if (first) f.demolish(x, y) else return false
            }
            if (r == Result.OK) {
                buzz()
                when (act) {
                    Act.PLANT -> Audio.vary(Sfx.PLANT)
                    Act.WATER -> Audio.vary(Sfx.WATER)
                    Act.BUILD -> Audio.play(Sfx.BUILD)
                    Act.SHOO -> Audio.vary(Sfx.SHOO)
                    Act.COLLECT -> Audio.play(Sfx.COLLECT)
                    Act.DEMOLISH -> Audio.play(Sfx.DEMOLISH)
                    Act.HARVEST -> {} // Store plays it, golden or not
                }
                val bx = world.centerX(x, y).toFloat(); val by = world.centerY(x, y).toFloat()
                when (act) {
                    Act.WATER -> world.fx.burst(Burst.WATER, bx, by - 4)
                    Act.PLANT -> world.fx.burst(Burst.DIRT, bx, by)
                    Act.BUILD, Act.DEMOLISH -> world.fx.burst(Burst.DUST, bx, by - 2)
                    Act.COLLECT -> world.fx.burst(Burst.COINS, bx, by - 20)
                    Act.SHOO -> world.fx.burst(Burst.DUST, bx, by - 8)
                    Act.HARVEST -> {}
                }
                when (act) {
                    Act.SHOO -> floaters += Floater(world.centerX(x, y).toFloat(), world.centerY(x, y) - 20f, "shoo!", 0xFFFFE27A.toInt(), Store.now())
                    Act.WATER -> floaters += Floater(world.centerX(x, y).toFloat(), world.centerY(x, y) - 12f, "~", 0xFF8FD4FF.toInt(), Store.now())
                    Act.PLANT -> floaters += Floater(world.centerX(x, y).toFloat(), world.centerY(x, y) - 8f,
                        "-$" + fmt(Strains[ui.seed]!!.seed), 0xFFFF9A7A.toInt(), Store.now())
                    else -> {}
                }
                Store.changed()
            } else complain(r)
            return r == Result.OK
        }

        /** A plain tap that didn't do a farming action: place, move or show what's there. */
        fun tap(x: Int, y: Int) {
            val tile = f.tile(x, y)
            when (val m = ui.mode) {
                is Mode.PlaceHouse -> {
                    val r = f.buildHouse(x, y, m.h)
                    if (r == Result.OK) {
                        buzz(); Audio.play(Sfx.BUILD); world.fx.burst(Burst.DUST, world.centerX(x, y).toFloat(), world.centerY(x, y) - 2f)
                        Store.toast(if (m.h == WorkHouse.BUNK) "Bunkhouse built: room for 2 more workers" else if (m.h == WorkHouse.STORE) "Your stand is open! It sells lab-bred bud from the stash" else "${m.h.title} built! Hire a worker to staff it")
                        ui.mode = Mode.Farm; Store.changed()
                    } else if (r == Result.BLOCKED) Store.toast("Pick an empty grass square")
                    else if (r == Result.LIMIT) Store.toast("That's the most of those one farm can hold")
                    else complain(r)
                }
                is Mode.PlaceVehicle -> {
                    val r = f.buyVehicle(x, y, m.v, Store.now())
                    if (r == Result.OK) {
                        buzz(); Audio.play(Sfx.BUILD); world.fx.burst(Burst.DUST, world.centerX(x, y).toFloat(), world.centerY(x, y) + 4f)
                        Store.toast("${m.v.title} parked! ${m.v.desc}."); ui.mode = Mode.Farm; Store.changed()
                    } else if (r == Result.BLOCKED) Store.toast("It needs a free 2×2 grass spot with its back corner here")
                    else if (r == Result.LIMIT) Store.toast("This farm already has one of those")
                    else complain(r)
                }
                is Mode.PlaceFacility -> {
                    val r = f.buildFacility(x, y, m.f)
                    if (r == Result.OK) { buzz(); Audio.play(Sfx.BUILD); world.fx.burst(Burst.DUST, world.centerX(x, y).toFloat(), world.centerY(x, y) - 2f); Store.toast("${m.f.title} built! Tap it to start a batch"); ui.mode = Mode.Farm; Store.changed() }
                    else if (r == Result.BLOCKED) Store.toast("Pick an empty grass square") else complain(r)
                }
                is Mode.PlacePower -> {
                    val r = f.buildPower(x, y, m.g)
                    if (r == Result.OK) {
                        buzz(); Audio.play(Sfx.BUILD); world.fx.burst(Burst.DUST, world.centerX(x, y).toFloat(), world.centerY(x, y) - 2f)
                        Store.toast("${m.g.title} built: +${m.g.watts} ⚡"); ui.mode = Mode.Farm; Store.changed()
                    } else if (r == Result.BLOCKED) Store.toast("Pick an empty grass square") else complain(r)
                }
                is Mode.PlaceSpecial -> {
                    val r = f.buildSpecial(x, y, m.s, Store.now())
                    if (r == Result.OK) {
                        buzz(); Audio.play(Sfx.LEVEL_UP); world.fx.burst(Burst.COINS, world.centerX(x, y).toFloat(), world.centerY(x, y) - 10f)
                        Store.toast("${m.s.title} built! ${m.s.desc}."); ui.mode = Mode.Farm; Store.changed()
                    } else if (r == Result.BLOCKED) Store.toast("Pick an empty grass square")
                    else if (r == Result.NO_CASH) { Audio.play(Sfx.ERROR); Store.toast("Not enough gems or cash") }
                    else if (r == Result.LIMIT) Store.toast("You already have one of those")
                    else complain(r)
                }
                Mode.Demolish -> when {
                    tile.kind == Kind.GRASS -> ui.selected = null
                    // Boulders are broken up from their card; water stays.
                    tile.kind == Kind.ROCK || tile.kind == Kind.WATER -> ui.selected = x to y
                    ui.selected == (x to y) -> {
                        val r = f.demolish(x, y)
                        if (r == Result.OK) { buzz(); Store.changed() } else complain(r)
                        ui.selected = null
                    }
                    else -> { ui.selected = x to y; Store.toast("Tap it again to demolish") }
                }
                is Mode.Move -> {
                    val from = m.from
                    if (from == null) {
                        if (tile.kind == Kind.GRASS) Store.toast("Tap something to pick it up")
                        else if (tile.kind == Kind.ROCK || tile.kind == Kind.WATER) Store.toast("That stays where nature put it")
                        else {
                            ui.mode = Mode.Move(x to y)
                            f.stuntSeconds(x, y).takeIf { it > 0 }?.let { Store.toast("Moving it stunts it: +${dur(it.toLong())} to grow") }
                        }
                    } else if (from == (x to y)) ui.mode = Mode.Move()
                    else {
                        val r = f.move(from.first, from.second, x, y)
                        if (r == Result.OK) { buzz(); ui.mode = Mode.Move(); Store.changed() }
                        else Store.toast(when (f.tile(from.first, from.second).kind) {
                            Kind.SHACK, Kind.PART -> "The shack needs a free 2×2 spot with its back corner here"
                            Kind.VEHICLE, Kind.CAB -> "It needs a free 2×2 spot with its back corner here"
                            else -> "Pick an empty grass square"
                        })
                    }
                }
                Mode.Farm -> when (tile.kind) {
                    Kind.SHACK, Kind.PART -> { ui.selected = null; ui.panel = Panel.SHACK }
                    Kind.FACILITY -> { ui.selected = null; ui.fac = x to y; ui.panel = Panel.FACILITY }
                    Kind.GRASS -> ui.selected = null
                    else -> ui.selected = x to y
                }
                else -> ui.selected = if (tile.kind != Kind.GRASS) x to y else null
            }
        }

        ComposeCanvas(
            Modifier.fillMaxSize().pointerInput(Unit) {
                // A mouse wheel zooms around the pointer, for the desktop build (and mice on phones).
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Scroll) continue
                        val ch = event.changes.firstOrNull() ?: continue
                        val ns = (scale * 1.15f.pow(-ch.scrollDelta.y)).coerceIn(minScale(), 12f)
                        offset = ch.position - (ch.position - offset) * (ns / scale)
                        scale = ns
                        clamp()
                        ch.consume()
                    }
                }
            }.pointerInput(Unit) {
                // A quick touch is a tap. Moving straight off is a pan. Resting a moment first, then moving, paints the
                // action over every square the finger crosses (harvest, plant, water...). Two fingers: zoom, never an action.
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downAt = System.nanoTime()
                    val start = tileAt(down.position)
                    fun openMenu(): Boolean {
                        if (ui.mode != Mode.Farm || start == null || !f.inLand(start.first, start.second)) return false
                        if (f.tile(start.first, start.second).kind == Kind.GRASS) return false
                        ui.selected = null; ui.menuAt = down.position; ui.menu = start
                        buzz(); Audio.play(Sfx.TAP)
                        return true
                    }
                    // A right click opens the tile menu straight away.
                    if (currentEvent.buttons.isSecondaryPressed) {
                        if (!openMenu()) ui.menu = null
                        do { val e = awaitPointerEvent(); e.changes.forEach { it.consume() } } while (e.changes.any { it.pressed })
                        return@awaitEachGesture
                    }
                    ui.menu = null
                    var multi = false
                    var moved = false
                    var ended = false
                    var painting: Act? = null
                    var acted = false
                    var last = start
                    // Wait out the hold: a still finger that stays down becomes a paint stroke.
                    val held = withTimeoutOrNull(HOLD_MS) {
                        while (true) {
                            val pressed = awaitPointerEvent().changes.filter { it.pressed }
                            if (pressed.isEmpty()) { ended = true; break }
                            if (pressed.size >= 2) { multi = true; break }
                            if ((pressed[0].position - down.position).getDistance() > slop) { moved = true; break }
                        }
                    } == null
                    if (ended) {
                        if (start != null && f.inLand(start.first, start.second)) {
                            val act = actFor(start.first, start.second)
                            if (act != null) { apply(act, start.first, start.second, true); ui.selected = null }
                            else tap(start.first, start.second)
                        } else ui.selected = null
                        return@awaitEachGesture
                    }
                    if (held && !multi && start != null) {
                        painting = actFor(start.first, start.second)
                        painting?.let { acted = true; if (apply(it, start.first, start.second, true)) ui.selected = null else painting = null }
                    }
                    var panned = moved
                    // A still hold on the first square (with nothing to paint) opens the tile menu.
                    var waiting = held && !multi && painting == null
                    var opened = false
                    while (true) {
                        val event = if (waiting && !panned && !multi) {
                            val left = MENU_MS - (System.nanoTime() - downAt) / 1_000_000
                            (if (left > 0) withTimeoutOrNull(left) { awaitPointerEvent() } else null) ?: run {
                                waiting = false
                                if (openMenu()) { opened = true; acted = true }
                                null
                            }
                        } else awaitPointerEvent()
                        if (event == null) continue
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (opened) { event.changes.forEach { it.consume() }; continue }
                        if (pressed.size >= 2) {
                            multi = true; painting = null
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val c = event.calculateCentroid()
                            val ns = (scale * zoom).coerceIn(minScale(), 12f)
                            offset = c - (c - offset) * (ns / scale) + pan
                            scale = ns
                            clamp()
                        } else if (!multi) {
                            val ch = pressed[0]
                            val p = painting
                            if (p != null) {
                                val tl = tileAt(ch.position)
                                if (tl != null && tl != last) {
                                    last = tl
                                    if (f.inLand(tl.first, tl.second)) apply(p, tl.first, tl.second, false)
                                }
                            } else {
                                if (!panned && (ch.position - down.position).getDistance() > slop) panned = true
                                if (panned) { offset += ch.position - ch.previousPosition; clamp() }
                            }
                        }
                        event.changes.forEach { it.consume() }
                    }
                    // Held still and let go without moving: still a tap.
                    if (!multi && !panned && !acted && held && start != null && f.inLand(start.first, start.second)) tap(start.first, start.second)
                }
            }
        ) {
            val now = Store.now()
            Store.rev // redraw on farm changes
            val t = frame / 1000f
            // Harvest numbers float up from the plant.
            for (e in Store.pending) if (e is FarmEvent.Harvest) {
                val wx = world.centerX(e.x, e.y).toFloat(); val wy = world.centerY(e.x, e.y) - 18f
                world.fx.burst(if (e.golden) Burst.GOLDEN else Burst.HARVEST, wx, wy + 6)
                if (e.golden) floaters += Floater(wx, wy - 8, "GOLDEN x3!", 0xFFFFD84A.toInt(), now, big = true)
                if (e.perfect) floaters += Floater(wx, wy - 16, "PEAK +15%", 0xFFFFD84A.toInt(), now)
                if (e.trim > 0) floaters += Floater(wx, wy - (if (e.perfect) 24 else 16), "Trim ${e.trim}/5", 0xFF7BD34B.toInt(), now + 60)
                floaters += Floater(wx, wy, "+${e.grams}g", 0xFFB6F27A.toInt(), now)
                floaters += Floater(wx, wy + 8, "+${e.xp}xp", 0xFF9AD4FF.toInt(), now + 120)
            }
            Store.pending.clear()

            val m = ui.mode
            val light = Light.at(now)
            drawRect(Color(backdrop(light.tint)))
            // Only what's on screen gets drawn: finely when zoomed in, one bitmap pixel per art pixel when zoomed out.
            val d = if (scale >= S) S else 1
            val x0 = floor(-offset.x / scale).toInt(); val y0 = floor(-offset.y / scale).toInt()
            world.view(x0, y0, x0 + ceil(size.width / scale).toInt() + 1, y0 + ceil(size.height / scale).toInt() + 1, d)
            world.render(f, now, t, light, ui.selected, m is Mode.Build || m is Mode.PlaceFacility || m is Mode.PlaceDecor || (m is Mode.Move && m.from != null),
                (m as? Mode.Move)?.from)
            val img = image[0]?.takeIf { it.asAndroidBitmap() === world.bitmap } ?: world.bitmap.asImageBitmap().also { image[0] = it }
            val l = (offset.x + world.vx * scale).roundToInt(); val tp = (offset.y + world.vy * scale).roundToInt()
            val r = (offset.x + (world.vx + world.vw) * scale).roundToInt(); val b = (offset.y + (world.vy + world.vh) * scale).roundToInt()
            drawImage(img, srcOffset = IntOffset.Zero, srcSize = IntSize(world.vw * world.k, world.vh * world.k),
                dstOffset = IntOffset(l, tp), dstSize = IntSize(r - l, b - tp), filterQuality = FilterQuality.None)
            drawIntoCanvas { world.fx.drawSky(it.nativeCanvas, size.width, size.height, scale, t, light) }

            textPaint.textSize = max(28f, scale * 6f)
            drawIntoCanvas { c ->
                val nc = c.nativeCanvas
                val it = floaters.iterator()
                while (it.hasNext()) {
                    val fl = it.next()
                    val age = (now - fl.born) / 1000f
                    if (age > 1.3f) { it.remove(); continue }
                    if (age < 0) continue
                    val sx = offset.x + fl.wx * scale
                    val sy = offset.y + (fl.wy - age * 14) * scale
                    val alpha = (255 * min(1f, (1.3f - age) * 2.5f)).toInt()
                    textPaint.textSize = if (fl.big) max(48f, scale * 10f) else max(38f, scale * 8f)
                    textPaint.style = Paint.Style.STROKE; textPaint.strokeWidth = textPaint.textSize / 5
                    textPaint.color = 0xFF1C1A14.toInt(); textPaint.alpha = alpha
                    nc.drawText(fl.text, sx, sy, textPaint)
                    textPaint.style = Paint.Style.FILL
                    textPaint.color = fl.color; textPaint.alpha = alpha
                    nc.drawText(fl.text, sx, sy, textPaint)
                }
            }
        }
    }
}

/** The dark green beyond the trees, lit like the farm. */
private fun backdrop(tint: Int): Long {
    fun ch(base: Int, sh: Int) = (base * ((tint shr sh) and 0xFF) / 255)
    return 0xFF000000L or (ch(0x1B, 16).toLong() shl 16) or (ch(0x2A, 8).toLong() shl 8) or ch(0x1E, 0).toLong()
}

/** The disco ball's light walks round the colour wheel. */
private fun discoColor(t: Float): Int = intArrayOf(0xFF60D0, 0x60D0FF, 0xFFE060, 0x80FF80)[((t * 2).toInt() and 3)]
