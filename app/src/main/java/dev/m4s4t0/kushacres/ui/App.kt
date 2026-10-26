package dev.m4s4t0.kushacres.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.withFrameMillis
import android.text.format.DateFormat
import dev.m4s4t0.kushacres.model.Sky as Clock
import dev.m4s4t0.kushacres.model.Weather
import dev.m4s4t0.kushacres.model.Season
import dev.m4s4t0.kushacres.model.Stat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.audio.Sfx
import dev.m4s4t0.kushacres.art.Keeper
import dev.m4s4t0.kushacres.art.Sprites
import dev.m4s4t0.kushacres.model.Decor
import dev.m4s4t0.kushacres.model.Facility
import dev.m4s4t0.kushacres.model.Kind
import dev.m4s4t0.kushacres.model.Strains
import kotlinx.coroutines.delay
import java.util.IdentityHashMap
import kotlin.math.abs

/** What touching the farm does right now. */
sealed class Mode {
    data object Farm : Mode()
    data class Build(val kind: Kind) : Mode()
    data class PlaceFacility(val f: Facility) : Mode()
    data class PlaceDecor(val d: Decor) : Mode()
    /** Pick something up, then tap where it goes. */
    data class Move(val from: Pair<Int, Int>? = null) : Mode()
    data object Demolish : Mode()
}

enum class Panel { NONE, SEEDS, ORDERS, TOWN, BUILD, SHACK, FACILITY, MENU }

class UiState {
    var mode by mutableStateOf<Mode>(Mode.Farm)
    var seed by mutableStateOf("ditch")
    var selected by mutableStateOf<Pair<Int, Int>?>(null)
    var panel by mutableStateOf(Panel.NONE)
    /** The shop open in town, or null for Main Street. */
    var shop by mutableStateOf<Keeper?>(null)
    /** The facility whose sheet is open. */
    var fac by mutableStateOf<Pair<Int, Int>?>(null)
    var recenter by mutableIntStateOf(0)
    /** Quarter turns of the farm view (-1 until the saved one is loaded). */
    var rot by mutableIntStateOf(-1)
    fun turn(d: Int) { rot = Math.floorMod(rot.coerceAtLeast(0) + d, 4) }
}

// Wood-and-parchment pixel palette.
val Wood = Color(0xFF2B2118)
val WoodEdge = Color(0xFF8A6238)
val WoodIn = Color(0xFF3B2D20)
val Cream = Color(0xFFF5E6C8)
val Dim = Color(0xFFB8A687)
val Leaf = Color(0xFF7BD34B)
val Gold = Color(0xFFF2C440)
val Bad = Color(0xFFE8664A)
val Sky = Color(0xFF8FD4FF)

fun fmt(n: Long): String {
    val a = abs(n)
    val s = when {
        a >= 1_000_000_000_000L -> "%.2fT".format(a / 1e12)
        a >= 1_000_000_000L -> "%.2fB".format(a / 1e9)
        a >= 10_000_000L -> "%.1fM".format(a / 1e6)
        a >= 1_000_000L -> "%.2fM".format(a / 1e6)
        else -> "%,d".format(a)
    }
    return if (n < 0) "-$s" else s
}

fun money(n: Long) = "$" + fmt(n)

fun dur(seconds: Long): String {
    val s = maxOf(0, seconds)
    return when {
        s < 60 -> "${s}s"
        s < 3600 -> "${s / 60}m %02ds".format(s % 60)
        else -> "${s / 3600}h %02dm".format(s / 60 % 60)
    }
}

private val imageCache = IdentityHashMap<Bitmap, ImageBitmap>()
fun Bitmap.img(): ImageBitmap = imageCache.getOrPut(this) { asImageBitmap() }

@Composable
fun Px(bmp: Bitmap, size: Dp, modifier: Modifier = Modifier) {
    Image(bmp.img(), null, modifier.size(size), filterQuality = FilterQuality.None)
}

@Composable
fun T(text: String, size: TextUnit = 14.sp, color: Color = Cream, modifier: Modifier = Modifier, align: TextAlign? = null, bold: Boolean = true) {
    Text(text, modifier, color = color, textAlign = align,
        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = size, lineHeight = size * 1.2f,
            shadow = if (color.luminance() > 0.35f) TextShadow else null))
}

/** A hard drop shadow under light text, like a pixel font. */
private val TextShadow = Shadow(Color(0xC0120D08), Offset(0f, 3f), 0f)
private val Ink = Color(0xFF120D08)

/** A chunky pixel frame: a dark outline with notched corners, a rim lit from above, then [bg]. Pads its content by 1.5 * [w]. */
fun Modifier.frame(bg: Color = Wood, edge: Color = WoodEdge, w: Dp = 2.dp) = this.drawBehind {
    val r = w.toPx(); val o = r / 2
    val W = size.width; val H = size.height
    drawRect(Ink, Offset(o, 0f), Size(W - 2 * o, H))
    drawRect(Ink, Offset(0f, o), Size(W, H - 2 * o))
    drawRect(edge, Offset(o, o), Size(W - 2 * o, H - 2 * o))
    drawRect(lerp(edge, Color.White, 0.35f), Offset(o + r, o), Size(W - 2 * (o + r), r / 2))
    drawRect(lerp(edge, Color.Black, 0.4f), Offset(o, H - o - r / 2), Size(W - 2 * o, r / 2))
    val i = o + r
    drawRect(bg, Offset(i, i), Size(W - 2 * i, H - 2 * i))
    drawRect(lerp(bg, Color.White, 0.08f), Offset(i, i), Size(W - 2 * i, r / 2))
    drawRect(lerp(bg, Color.Black, 0.18f), Offset(i, H - i - r / 2), Size(W - 2 * i, r / 2))
}.padding(w * 1.5f)

/** A framed wooden box. */
fun Modifier.woodBox(bg: Color = Wood, edge: Color = WoodEdge, w: Dp = 2.dp) = this.frame(bg, edge, w)

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = Leaf, icon: Bitmap? = null, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Row(
        modifier
            .pressable(src)
            .frame(if (enabled) lerp(Wood, color, 0.32f) else Color(0xFF231B14), if (enabled) color else Color(0xFF4A3E30))
            .clickable(src, LocalIndication.current, enabled = enabled) { Audio.play(Sfx.TAP); onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Px(icon, 16.dp); Spacer(Modifier.width(6.dp)) }
        T(text, 13.sp, if (enabled) Cream else Dim.copy(alpha = 0.6f))
    }
}

/** Sinks a little while [src] is held down. */
@Composable
fun Modifier.pressable(src: MutableInteractionSource): Modifier {
    val pressed by src.collectIsPressedAsState()
    val k by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.45f, stiffness = 900f), label = "press")
    return this.graphicsLayer { scaleX = k; scaleY = k }
}

@Composable
fun App() {
    val ui = remember { UiState() }
    var lastPanel by remember { mutableStateOf(Panel.NONE) }
    LaunchedEffect(ui.panel) {
        if (ui.panel != lastPanel) Audio.play(if (ui.panel == Panel.NONE) Sfx.CLOSE else if (lastPanel == Panel.NONE) Sfx.OPEN else Sfx.TAP)
        lastPanel = ui.panel
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF1B2A1E))) {
        FarmView(ui)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Hud(ui)
            Hints(ui)
            Spacer(Modifier.weight(1f))
            ui.selected?.let { InfoCard(ui, it) }
            BottomBar(ui)
        }
        Toast()
        if (ui.panel != Panel.NONE) PanelHost(ui)
        Dialogs()
    }
}

@Composable
private fun Hud(ui: UiState) {
    val f = Store.farm
    Store.rev
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        // Level and XP.
        // A red dot when there's a daily reward or a quest to claim in the Farm Book.
        Box {
            Row(Modifier.woodBox().clickable { ui.panel = Panel.MENU }.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Px(Sprites.star, 20.dp)
                Spacer(Modifier.width(4.dp))
                Column {
                    T("Lv ${f.level}", 13.sp, Gold)
                    val need = dev.m4s4t0.kushacres.model.Farm.xpNeed(f.level)
                    val frac = (f.xp.toFloat() / need).coerceIn(0f, 1f)
                    Box(Modifier.width(64.dp).height(6.dp).background(Color(0xFF14100B))) {
                        Box(Modifier.fillMaxWidth(frac).height(6.dp).background(Sky))
                    }
                }
            }
            if (f.canClaim(Store.now())) Box(Modifier.align(Alignment.TopEnd).size(10.dp).border(2.dp, Color(0xFF14100B)).background(Bad))
        }
        Spacer(Modifier.width(6.dp))
        // Cash, counting up (or down) to the new amount, with a little bounce when it changes.
        var shown by remember { mutableLongStateOf(f.cash) }
        LaunchedEffect(f.cash) {
            val from = shown; val to = f.cash
            val start = withFrameMillis { it }
            while (true) {
                val k = ((withFrameMillis { it } - start) / 500f).coerceAtMost(1f)
                val e = 1 - (1 - k) * (1 - k) * (1 - k)
                shown = from + ((to - from) * e.toDouble()).toLong()
                if (k >= 1f) break
            }
        }
        val pop = Store.cashPop
        var bounce by remember { mutableStateOf(1f) }
        LaunchedEffect(pop) {
            if (pop != null) { bounce = 1.25f; delay(90); bounce = 1.1f; delay(90); bounce = 1f }
        }
        Row(Modifier.weight(1f).woodBox().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Px(Sprites.coin, 20.dp)
            Spacer(Modifier.width(6.dp))
            T(money(shown), 16.sp, Gold, Modifier.scale(bounce))
        }
        Spacer(Modifier.width(6.dp))
        // Turn the farm a quarter round either way.
        for ((d, label) in listOf(-1 to "↺", 1 to "↻")) {
            Box(Modifier.woodBox().clickable { ui.turn(d) }.padding(horizontal = 7.dp, vertical = 4.dp)) { T(label, 16.sp, Cream) }
            Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.width(2.dp))
        val used = f.stashUsed(); val cap = f.stashCap()
        Row(Modifier.woodBox().clickable { ui.shop = Keeper.DEX; ui.panel = Panel.TOWN }.padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Px(Sprites.jar, 20.dp)
            Spacer(Modifier.width(4.dp))
            T("$used/${cap}g", 13.sp, if (used >= cap) Bad else if (used > cap * 0.8) Gold else Cream)
        }
    }
    SkyChip()
}

/** The weather and the farm's clock, under the cash. */
@Composable
private fun SkyChip() {
    val now = Store.now()
    val ctx = LocalContext.current
    val w = Clock.weather(now)
    val h = Clock.hour(now)
    val night = Clock.daylight(h) < 0.5f
    val season = Store.farm.season()
    val snow = season == Season.WINTER && w == Weather.RAIN
    val icon = when (w) {
        Weather.RAIN -> if (snow) Sprites.snow else Sprites.rain
        Weather.FOG -> Sprites.fog
        Weather.CLOUDY -> Sprites.cloud
        Weather.CLEAR -> if (night) Sprites.moon else Sprites.sun
    }
    val hh = h.toInt(); val mm = ((h - hh) * 60).toInt()
    val clock = if (DateFormat.is24HourFormat(ctx)) "%d:%02d".format(hh, mm)
        else "%d:%02d %s".format(if (hh % 12 == 0) 12 else hh % 12, mm, if (hh < 12) "am" else "pm")
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
        Row(
            Modifier.background(Color(0xAA1C1A14)).clickable {
                Store.toast("${season.title}: ${season.desc}. " + when (w) {
                    Weather.RAIN -> if (snow) "Snow is watering every plant for you" else "Rain is watering every plant for you"
                    Weather.FOG -> "Foggy out. Fewer eyes on your farm"
                    Weather.CLOUDY -> "Clouds rolling over"
                    Weather.CLEAR -> if (night) "A clear night. Lamps and lights glow" else "Clear skies"
                } + ".")
            }.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Px(icon, 16.dp)
            Spacer(Modifier.width(4.dp))
            T("${season.title} · ${if (snow) "Snow" else w.label} · $clock", 11.sp, Cream)
        }
    }
}

@Composable
private fun Hints(ui: UiState) {
    val f = Store.farm
    Store.rev
    val text = when {
        // Starter tips. Lifetime stats drive them, so they don't come back after selling the farm.
        ui.mode != Mode.Farm || f.level >= 5 -> null
        f.stat(Stat.HARVESTS) == 0L -> "Tap the ripe plants to harvest. Swipe across many at once!"
        f.stat(Stat.EARNED) == 0L && f.stashUsed() > 0 -> "Sell your bud at Dex's in Town, or fill an order for a bonus."
        f.stat(Stat.HARVESTS) < 6 && f.tiles.any { it.kind == Kind.PLOT && it.strain == null } -> "Tap empty plots to plant ${Strains[ui.seed]?.name}. Tap a growing plant to water it."
        f.stashUsed() >= f.stashCap() -> "Your stash is full! Sell at Dex's in Town or upgrade your Stash at Violet's."
        f.level >= 4 && f.legacy == 0 && f.count(dev.m4s4t0.kushacres.model.Facility.ROLLER) == 0 -> "Hank's in Town sells a Rolling Table. Pre-rolls sell for more than bud!"
        else -> null
    }
    val now = Store.now()
    val buffs = f.buffs.filter { it.value > now }
    if (buffs.isNotEmpty()) T(buffs.entries.joinToString("   ") { "${it.key.title} ${dur((it.value - now) / 1000)}" }, 11.sp, Gold,
        Modifier.padding(horizontal = 8.dp).fillMaxWidth().background(Color(0xCC1C1A14)).padding(4.dp), TextAlign.Center)
    if (text == null) return
    T(text, 12.sp, Cream, Modifier.padding(horizontal = 8.dp).fillMaxWidth().background(Color(0xCC1C1A14)).padding(8.dp), TextAlign.Center)
}

@Composable
private fun Toast() {
    val t = Store.toast ?: return
    LaunchedEffect(t) { delay(2200); if (Store.toast == t) Store.toast = null }
    Box(Modifier.fillMaxSize().padding(bottom = 170.dp), contentAlignment = Alignment.BottomCenter) {
        T(t.first, 13.sp, Cream, Modifier.widthIn(max = 320.dp).woodBox(Color(0xF02B2118), Bad).padding(10.dp), TextAlign.Center)
    }
}

@Composable
private fun BottomBar(ui: UiState) {
    val f = Store.farm
    Store.rev
    val m = ui.mode
    if (m != Mode.Farm) {
        Row(Modifier.fillMaxWidth().padding(8.dp).woodBox().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val what = when (m) {
                is Mode.Build -> when (m.kind) {
                    Kind.PLOT -> "Placing plots: ${money(f.plotCost())}"
                    Kind.SPRINKLER -> "Placing sprinklers: ${money(f.sprinklerCost())}"
                    Kind.LAMP -> "Placing lamps: ${money(f.lampCost())}"
                    else -> ""
                }
                is Mode.PlaceFacility -> "Tap grass to build the ${m.f.title}: ${money(f.facilityCost(m.f))}"
                is Mode.PlaceDecor -> {
                    val n = f.decorInv[m.d.name] ?: 0
                    "Placing ${m.d.title}: " + if (n > 0) "$n in shed" else money(m.d.cost)
                }
                is Mode.Move -> if (m.from == null) "Tap something to pick it up" else "Tap an empty spot to put it down"
                Mode.Demolish -> "Tap something empty to remove it"
                Mode.Farm -> ""
            }
            T(what, 13.sp, Cream, Modifier.weight(1f))
            Btn("Done") { ui.mode = Mode.Farm }
        }
        return
    }
    val seed = Strains[ui.seed]!!
    val ready = f.orders.withIndex().count { (i, o) -> i < f.orderSlots() && o != null && f.canFill(o) }
    Row(Modifier.fillMaxWidth().frame(Wood, WoodEdge, 3.dp).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BarButton(Modifier.weight(1.3f), Sprites.strainIcon(seed), seed.name, if (seed.exotic) "${f.seeds[seed.id] ?: 0} seeds" else money(seed.seed)) { ui.panel = Panel.SEEDS }
        BarButton(Modifier.weight(1f), Sprites.bag, "Orders", if (ready > 0) "$ready ready" else null, badge = ready > 0) { ui.panel = Panel.ORDERS }
        BarButton(Modifier.weight(1f), Sprites.stall, "Town", null) { ui.shop = null; ui.panel = Panel.TOWN }
        BarButton(Modifier.weight(1f), Sprites.hammer, "Build", null) { ui.panel = Panel.BUILD }
    }
}

@Composable
private fun BarButton(modifier: Modifier, icon: Bitmap, label: String, sub: String?, badge: Boolean = false, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Column(
        modifier.pressable(src).frame(WoodIn, if (badge) Gold else WoodEdge)
            .clickable(src, null, onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Px(icon, 28.dp)
        T(label, 11.sp, Cream)
        T(sub ?: " ", 10.sp, if (badge) Gold else Dim)
    }
}

@Composable
private fun InfoCard(ui: UiState, at: Pair<Int, Int>) {
    val f = Store.farm
    Store.rev
    val (x, y) = at
    val tile = f.tile(x, y)
    val now = Store.now()
    Column(Modifier.fillMaxWidth().padding(8.dp).woodBox().padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val title = when (tile.kind) {
                Kind.PLOT -> tile.strain?.let { Strains[it]?.name } ?: "Empty plot"
                Kind.SPRINKLER -> "Sprinkler"
                Kind.LAMP -> "Grow lamp"
                Kind.GRASS -> "Grass"
                Kind.DECOR, Kind.PATH -> Decor.of(tile.item)?.title ?: ""
                else -> ""
            }
            T(title, 15.sp, Gold, Modifier.weight(1f))
            T("x", 16.sp, Dim, Modifier.clickable { ui.selected = null }.padding(horizontal = 6.dp))
        }
        when (tile.kind) {
            Kind.PLOT -> {
                val s = tile.strain?.let { Strains[it] }
                if (tile.pest) T("Bugs! Tap to shoo them off. It won't grow until you do.", 12.sp, Bad)
                else if (s == null) T("Tap it to plant your selected seed.", 12.sp, Dim)
                else if (tile.ready()) T("Ripe! Tap to harvest.", 12.sp, Leaf)
                else {
                    val frac = (tile.progress / s.seconds).toFloat().coerceIn(0f, 1f)
                    Box(Modifier.padding(vertical = 4.dp).fillMaxWidth().height(8.dp).background(Color(0xFF14100B))) {
                        Box(Modifier.fillMaxWidth(frac).height(8.dp).background(Leaf))
                    }
                    T("Ready in ${dur(f.eta(x, y, now))}", 12.sp, Cream)
                    val wet = f.wet(x, y, now)
                    val extras = buildList {
                        add(if (f.sprinkled(x, y)) "Sprinkler" else if (wet) "Watered ${dur((tile.wetUntil - now) / 1000)}" else "Dry: tap to water (+50% speed)")
                        if (f.lamps(x, y) > 0) add("Lamps +${f.lamps(x, y) * 25}%")
                    }
                    T(extras.joinToString("  ·  "), 11.sp, if (wet) Sky else Dim)
                }
            }
            Kind.SPRINKLER -> T("Keeps the 8 squares around it watered forever.", 12.sp, Dim)
            Kind.LAMP -> T("Plants on the 8 squares around it grow 25% faster (2 lamps max).", 12.sp, Dim)
            Kind.DECOR, Kind.PATH -> T(Decor.of(tile.item)?.desc ?: "", 12.sp, Dim)
            else -> {}
        }
    }
}
