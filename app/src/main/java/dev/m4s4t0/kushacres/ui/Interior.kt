package dev.m4s4t0.kushacres.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.art.InteriorArt
import dev.m4s4t0.kushacres.art.InteriorArt.CD
import dev.m4s4t0.kushacres.art.InteriorArt.CW
import dev.m4s4t0.kushacres.art.People
import dev.m4s4t0.kushacres.model.Furn
import dev.m4s4t0.kushacres.model.Interiors
import dev.m4s4t0.kushacres.model.Placed
import dev.m4s4t0.kushacres.model.Result
import dev.m4s4t0.kushacres.model.RoomKind
import dev.m4s4t0.kushacres.model.SHACKS
import dev.m4s4t0.kushacres.model.Storefronts
import kotlin.math.floor
import kotlin.math.min

/** Art pixels of back wall above the floor; wall pieces hang [WALL_TOP] down from the ceiling. */
private const val WALL = 30
private const val WALL_TOP = 7

/**
 * Inside your house or your storefront: buy furniture and posters, tap a spot to put them down, tap one to move or
 * sell it, and repaint. A bigger shack (or store) means a bigger room.
 */
@Composable
fun Interior(ui: UiState) {
    val k = ui.room ?: return
    val f = Store.farm
    Store.rev
    var buying by remember(k) { mutableStateOf<Furn?>(null) }
    var picked by remember(k) { mutableStateOf<Placed?>(null) }
    var moving by remember(k) { mutableStateOf(false) }
    var tab by remember(k) { mutableIntStateOf(0) }
    fun idle() { buying = null; picked = null; moving = false }
    BackHandler { if (buying != null || picked != null) idle() else ui.room = null }

    fun tap(x: Int, y: Int, wall: Boolean, hit: Placed?) {
        val held = if (moving) picked else null
        val furn = buying ?: held?.furn
        if (furn == null) { picked = hit; return }
        if (furn.wall != wall) { Store.toast(if (furn.wall) "That goes up on the wall" else "That goes on the floor"); return }
        // The tapped square is the piece's corner if it fits there, or else any spot that still covers it.
        val spot = (0 until furn.h).flatMap { j -> (0 until furn.w).map { i -> x - i to y - j } }
            .firstOrNull { (sx, sy) -> Interiors.free(f, k, furn, sx, sy, held) }
        if (spot == null) { Store.toast("It doesn't fit there"); return }
        val r = if (held != null) Interiors.move(f, k, held, spot.first, spot.second) else Interiors.buy(f, k, furn, spot.first, spot.second)
        if (r == Result.OK) {
            Store.changed()
            if (held != null) idle() else if (f.cash < furn.price) buying = null
        } else report(r)
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF15110D)).clickable(remember { MutableInteractionSource() }, null) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val (cols, rows) = Interiors.size(f, k)
            val score = Interiors.score(f, k)
            Row(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    T(if (k == RoomKind.HOME) "Inside your ${SHACKS[f.bestShack()].title}" else "Inside your ${Storefronts.name(Interiors.storeLevel(f))}", 14.sp, Gold)
                    T(if (k == RoomKind.HOME) "Comfort $score/${Interiors.MAX_SCORE} · +${Interiors.xpBonus(f)}% XP"
                        else "Style $score/${Interiors.MAX_SCORE} · +${score / 10}% on store sales", 11.sp, Leaf)
                    T("$cols × $rows room · " + if (k == RoomKind.HOME) (SHACKS.getOrNull(f.bestShack() + 1)?.let { "more space as a ${it.title}" } ?: "as big as it gets")
                        else "more space as the store grows", 10.sp, Dim, bold = false)
                }
                Btn("Leave", color = WoodEdge) { ui.room = null }
            }
            RoomCanvas(Modifier.weight(1f).fillMaxWidth(), k, picked, buying ?: picked?.furn?.takeIf { moving }, if (moving) picked else null, ::tap)
            val p = picked
            when {
                buying != null -> Bar("Tap where the ${buying!!.title} goes · ${money(buying!!.price)} each") { Btn("Done", color = Gold) { idle() } }
                p != null && moving -> Bar("Tap where the ${p.furn.title} goes") { Btn("Cancel", color = WoodEdge) { idle() } }
                p != null -> Bar("${p.furn.title} · +${p.furn.score} ${if (k == RoomKind.HOME) "comfort" else "style"}") {
                    Btn("Move") { moving = true }
                    Btn("Sell ${money(p.furn.refund)}", color = Bad) { report(Interiors.sell(f, k, p)); idle(); Store.changed() }
                    Btn("Done", color = WoodEdge) { idle() }
                }
                else -> Catalog(k, tab, { tab = it }) { furn ->
                    when {
                        f.level < furn.level -> report(Result.LOCKED)
                        f.cash < furn.price -> report(Result.NO_CASH)
                        else -> buying = furn
                    }
                }
            }
        }
    }
}

@Composable
private fun Bar(text: String, buttons: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        T(text, 11.sp, Cream, Modifier.weight(1f))
        buttons()
    }
}

/** The shop along the bottom: floor pieces, wall pieces, and paint. */
@Composable
private fun Catalog(k: RoomKind, tab: Int, setTab: (Int) -> Unit, pick: (Furn) -> Unit) {
    val f = Store.farm
    Column(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Furniture", "Wall", "Paint").forEachIndexed { i, s -> Btn(s, color = if (tab == i) Gold else WoodEdge) { setTab(i) } }
        }
        if (tab == 2) {
            T("Wall paint · ${money(Interiors.PAINT)}", 11.sp, Dim)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Interiors.WALLS.forEachIndexed { i, c ->
                    val on = Interiors.room(f, k).wall == i
                    Box(Modifier.size(28.dp).frame(Color(c), if (on) Gold else WoodEdge, if (on) 3.dp else 2.dp)
                        .clickable { val r = Interiors.paint(f, k, true, i); if (r == Result.OK) Store.changed() else report(r) })
                }
            }
            T("Floor · ${money(Interiors.PAINT)}", 11.sp, Dim)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(Interiors.FLOORS.indices.toList()) { i ->
                    Btn(Interiors.FLOORS[i], color = if (Interiors.room(f, k).floor == i) Gold else WoodEdge) {
                        val r = Interiors.paint(f, k, false, i); if (r == Result.OK) Store.changed() else report(r)
                    }
                }
            }
            return@Column
        }
        val list = Furn.entries.filter { it.fits(k) && it.wall == (tab == 1) }.sortedBy { it.level }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list) { furn ->
                val locked = f.level < furn.level
                Column(
                    Modifier.width(78.dp).frame(if (locked) Color(0xFF231B14) else Wood, WoodEdge).clickable { pick(furn) }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        val bmp = InteriorArt.sprite(furn)
                        val s = 44f / maxOf(bmp.width, bmp.height)
                        Px(bmp, (maxOf(bmp.width, bmp.height) * s).dp)
                    }
                    T(furn.title, 9.sp, if (locked) Dim else Cream, align = TextAlign.Center, maxLines = 2)
                    T(if (locked) "Lv ${furn.level}" else money(furn.price), 10.sp, if (locked) Bad else if (f.cash >= furn.price) Gold else Dim)
                }
            }
        }
    }
}

/** Where a piece is drawn, in art pixels: left, top, width, height. */
private fun spriteBox(p: Placed): FloatArray {
    val fu = p.furn
    val w = InteriorArt.width(fu).toFloat(); val h = InteriorArt.height(fu).toFloat()
    return if (fu.wall) floatArrayOf(p.x * CW.toFloat(), WALL_TOP.toFloat(), w, h)
    else floatArrayOf(p.x * CW.toFloat(), WALL + (p.y + fu.h) * CD - h, w, h)
}

@Composable
private fun RoomCanvas(modifier: Modifier, k: RoomKind, picked: Placed?, placing: Furn?, moving: Placed?, onTap: (Int, Int, Boolean, Placed?) -> Unit) {
    val f = Store.farm
    Store.rev
    val (cols, rows) = Interiors.size(f, k)
    val room = Interiors.room(f, k)
    val shown = Interiors.shown(f, k)
    val floorItems = shown.filter { !it.furn.wall }.sortedWith(compareBy({ it.y + it.furn.h }, { it.x }))
    val wallItems = shown.filter { it.furn.wall }
    val aw = cols * CW; val ah = WALL + rows * CD
    // Someone to live there: you at home, a worker (or you) behind the counter at the store.
    val person: Bitmap? = if (k == RoomKind.HOME) (f.farmer?.let { People.farmer(it, 0) } ?: People.worker(0, 0)) else People.worker(2, 0)
    val stand = remember(shown.size, cols, rows, shown.sumOf { it.x * 31 + it.y }) {
        (rows - 1 downTo 0).flatMap { y -> (0 until cols).map { x -> (cols / 2 + x) % cols to y } }
            .firstOrNull { (x, y) -> Interiors.at(f, k, x, y, false) == null }
    }
    Canvas(modifier.pointerInput(k, cols, rows, shown) {
        detectTapGestures { o ->
            val s = min(size.width / aw.toFloat(), size.height / ah.toFloat())
            val ox = (size.width - aw * s) / 2; val oy = (size.height - ah * s) / 2
            val ax = (o.x - ox) / s; val ay = (o.y - oy) / s
            if (ax < 0 || ax >= aw || ay < 0 || ay >= ah) return@detectTapGestures
            // Whatever is drawn on top under the finger, then the square itself.
            val hit = (floorItems.reversed() + wallItems).firstOrNull { p ->
                val b = spriteBox(p); ax >= b[0] && ax < b[0] + b[2] && ay >= b[1] && ay < b[1] + b[3]
            }
            val cx = floor(ax / CW).toInt()
            if (ay < WALL) onTap(cx, 0, true, hit) else onTap(cx, floor((ay - WALL) / CD).toInt(), false, hit)
        }
    }) {
        val s = min(size.width / aw, size.height / ah)
        val ox = (size.width - aw * s) / 2; val oy = (size.height - ah * s) / 2
        fun r(x: Float, y: Float, w: Float, h: Float, c: Color) = drawRect(c, Offset(ox + x * s, oy + y * s), Size(w * s, h * s))
        // Back wall, with a ceiling shadow and a skirting board.
        val wc = Interiors.WALLS[room.wall]
        r(0f, 0f, aw.toFloat(), WALL.toFloat(), Color(wc))
        for (x in 0 until aw step 8) r(x.toFloat(), 0f, 1f, WALL.toFloat(), Color(wc).copy(alpha = 1f).darker(0.95f))
        r(0f, 0f, aw.toFloat(), 3f, Color(wc).darker(0.7f))
        r(0f, WALL - 3f, aw.toFloat(), 3f, Color(0xFF5A3A22))
        drawFloor(room.floor, cols, rows, ::r)
        if (k == RoomKind.STORE) r(aw - 3f, WALL - 22f, 3f, 22f, Color(0xFF6A4325)) // the door
        wallItems.forEach { drawPiece(it, ox, oy, s) }
        // Where the piece in hand could go.
        if (placing != null) {
            for (y in (if (placing.wall) 0..0 else 0 until rows)) for (x in 0 until cols)
                if (Interiors.free(f, k, placing, x, y, moving)) {
                    if (placing.wall) r(x * CW + 6f, 2f, 4f, 2f, Color(0x8860F070))
                    else r(x * CW + 6f, WALL + y * CD + 4f, 4f, 2f, Color(0x8860F070))
                }
        }
        var personDrawn = person == null || stand == null
        for (p in floorItems) {
            if (!personDrawn && stand!!.second < p.y + p.furn.h - 1) { drawPerson(person!!, stand, ox, oy, s); personDrawn = true }
            drawPiece(p, ox, oy, s, alpha = if (p === moving) 0.4f else 1f)
        }
        if (!personDrawn) drawPerson(person!!, stand!!, ox, oy, s)
        picked?.let { p ->
            val b = spriteBox(p)
            drawRect(Gold, Offset(ox + b[0] * s, oy + b[1] * s), Size(b[2] * s, b[3] * s), style = Stroke(2.dp.toPx()))
        }
    }
}

private fun Color.darker(k: Float) = Color(red * k, green * k, blue * k, alpha)

private fun drawFloor(style: Int, cols: Int, rows: Int, r: (Float, Float, Float, Float, Color) -> Unit) {
    for (y in 0 until rows) for (x in 0 until cols) {
        val fx = x * CW.toFloat(); val fy = WALL + y * CD.toFloat()
        val odd = (x + y) % 2 == 1
        when (style) {
            0 -> { // planks, staggered
                r(fx, fy, CW.toFloat(), CD.toFloat(), Color(if (y % 2 == 0) 0xFFA8784A else 0xFF9A6C40))
                r(fx, fy + CD - 1, CW.toFloat(), 1f, Color(0xFF6A4325))
                r(fx + if (y % 2 == 0) 0f else 8f, fy, 1f, CD.toFloat(), Color(0xFF7A5030))
            }
            1 -> r(fx, fy, CW.toFloat(), CD.toFloat(), Color(if (odd) 0xFF2A2630 else 0xFFE8E0D0))
            2 -> { r(fx, fy, CW.toFloat(), CD.toFloat(), Color(0xFF8A4A5A)); if (odd) r(fx + 5f, fy + 4f, 1f, 1f, Color(0xFF9A5A6A)) }
            3 -> { r(fx, fy, CW.toFloat(), CD.toFloat(), Color(0xFF8A8A88)); r(fx, fy, 1f, CD.toFloat(), Color(0xFF7A7A78)); r(fx, fy, CW.toFloat(), 1f, Color(0xFF7A7A78)) }
            4 -> { r(fx, fy, CW.toFloat(), CD.toFloat(), Color(if (odd) 0xFFE8E8EC else 0xFFD0D0D8)); r(fx + 3f, fy + 2f, 6f, 1f, Color(0xFFC0C0C8)) }
            else -> r(fx, fy, CW.toFloat(), CD.toFloat(), Color(if (odd) 0xFF3A8A5A else 0xFF4A9A6A))
        }
    }
    // Darker toward the back wall, for some depth.
    r(0f, WALL.toFloat(), cols * CW.toFloat(), 3f, Color(0x40000000))
}

private fun DrawScope.drawBmp(bmp: Bitmap, x: Float, y: Float, s: Float, alpha: Float = 1f) {
    // Sprites are drawn at 2 fine pixels per art pixel.
    val w = bmp.width / 2f; val h = bmp.height / 2f
    drawImage(bmp.img(), IntOffset.Zero, IntSize(bmp.width, bmp.height), IntOffset(x.toInt(), y.toInt()),
        IntSize((w * s).toInt(), (h * s).toInt()), alpha = alpha, filterQuality = FilterQuality.None)
}

private fun DrawScope.drawPiece(p: Placed, ox: Float, oy: Float, s: Float, alpha: Float = 1f) {
    val b = spriteBox(p)
    drawBmp(InteriorArt.sprite(p.furn), ox + b[0] * s, oy + b[1] * s, s, alpha)
}

private fun DrawScope.drawPerson(bmp: Bitmap, at: Pair<Int, Int>, ox: Float, oy: Float, s: Float) {
    val w = bmp.width / 2f; val h = bmp.height / 2f
    val x = at.first * CW + (CW - w) / 2; val y = WALL + (at.second + 1) * CD - h - 1
    drawBmp(bmp, ox + x * s, oy + y * s, s)
}
