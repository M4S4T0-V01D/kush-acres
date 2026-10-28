package dev.m4s4t0.kushacres.model

import org.json.JSONArray
import org.json.JSONObject

/** The rooms you can walk into: your house and your lab storefront. */
enum class RoomKind(val title: String) { HOME("Home"), STORE("Storefront") }

/**
 * Something to put in a room. Floor pieces take [w]×[h] squares; wall pieces hang on the back wall, [w] slots wide.
 * [score] is the comfort it adds at home or the style it adds to the store.
 */
enum class Furn(val title: String, val price: Long, val level: Int, val w: Int, val h: Int, val wall: Boolean, val home: Boolean, val store: Boolean, val score: Int) {
    // Floor, for home
    BEANBAG("Beanbag", 150, 1, 1, 1, false, true, false, 2),
    CRATE_TABLE("Crate Table", 120, 1, 1, 1, false, true, true, 1),
    PLANT("Potted Plant", 200, 1, 1, 1, false, true, true, 2),
    RUG("Shag Rug", 400, 2, 2, 2, false, true, true, 3),
    ARMCHAIR("Armchair", 600, 3, 1, 1, false, true, false, 4),
    LAMP("Lava Lamp", 350, 3, 1, 1, false, true, true, 3),
    COUCH("Couch", 1_500, 5, 3, 1, false, true, false, 8),
    COFFEE_TABLE("Coffee Table", 900, 5, 2, 1, false, true, false, 4),
    BED("Big Bed", 3_000, 6, 2, 2, false, true, false, 10),
    RECORDS("Record Player", 2_000, 7, 1, 1, false, true, true, 5),
    BOOKSHELF("Bookshelf", 2_500, 8, 2, 1, false, true, false, 5),
    FRIDGE("Snack Fridge", 4_000, 9, 1, 1, false, true, false, 6),
    TV("Big TV", 8_000, 10, 2, 1, false, true, false, 10),
    DINING("Dining Table", 6_000, 12, 2, 2, false, true, false, 8),
    FISH_TANK("Fish Tank", 15_000, 14, 2, 1, false, true, true, 10),
    ARCADE("Arcade Cabinet", 30_000, 16, 1, 1, false, true, true, 12),
    POOL("Pool Table", 90_000, 20, 3, 2, false, true, false, 18),
    JUKEBOX("Jukebox", 150_000, 24, 1, 1, false, true, true, 14),
    HOT_TUB("Hot Tub", 600_000, 30, 3, 3, false, true, false, 28),
    STATUE("Gold Leaf Statue", 5_000_000, 40, 1, 1, false, true, true, 30),

    // Floor, for the store
    COUNTER("Shop Counter", 2_000, 8, 3, 1, false, false, true, 8),
    REGISTER("Till", 800, 8, 1, 1, false, false, true, 4),
    JARS("Jar Stand", 1_200, 8, 1, 1, false, false, true, 5),
    SHELVES("Display Shelves", 3_000, 10, 2, 1, false, false, true, 8),
    CASE("Glass Case", 8_000, 14, 2, 1, false, false, true, 12),
    BENCH("Waiting Bench", 1_500, 9, 2, 1, false, false, true, 4),
    ATM("Cash Machine", 25_000, 18, 1, 1, false, false, true, 10),

    // Wall
    POSTER_LEAF("Leaf Poster", 80, 1, 1, 1, true, true, true, 1),
    POSTER_SUNSET("Sunset Poster", 150, 2, 1, 1, true, true, true, 2),
    POSTER_BAND("Band Poster", 250, 4, 1, 1, true, true, true, 2),
    CLOCK("Wall Clock", 300, 3, 1, 1, true, true, true, 2),
    WINDOW("Window", 800, 4, 1, 1, true, true, true, 3),
    PAINTING("Oil Painting", 3_000, 9, 2, 1, true, true, true, 5),
    TAPESTRY("Tapestry", 1_800, 7, 2, 1, true, true, false, 4),
    NEON_LEAF("Neon Leaf", 5_000, 11, 1, 1, true, true, true, 6),
    GOLD_RECORD("Gold Record", 40_000, 22, 1, 1, true, true, true, 10),
    NEON_OPEN("Neon OPEN Sign", 2_500, 8, 2, 1, true, false, true, 6),
    MENU_BOARD("Menu Board", 4_000, 10, 2, 1, true, false, true, 6),
    ;

    fun fits(k: RoomKind) = if (k == RoomKind.HOME) home else store
    /** What selling it back gets you. */
    val refund get() = price / 2
}

/** One piece in a room; wall pieces sit at [x] along the wall (their [y] is unused). */
class Placed(val furn: Furn, var x: Int, var y: Int)

class Room(val kind: RoomKind) {
    var wall = 0
    var floor = 0
    val items = mutableListOf<Placed>()
}

object Interiors {
    /** Paint for the walls and finishes for the floor (ARGB, and a name each). */
    val WALLS = intArrayOf(0xFFE8DCC0.toInt(), 0xFFB8956A.toInt(), 0xFF8AAEC8.toInt(), 0xFFE0A0B0.toInt(), 0xFF9AB87A.toInt(),
        0xFF6A5A8A.toInt(), 0xFF3A3A44.toInt(), 0xFFD8B860.toInt())
    val FLOORS = listOf("Planks", "Checker", "Carpet", "Concrete", "Marble", "Green Tile")
    /** Repainting or refinishing: cheap, so you can try looks out. */
    const val PAINT = 250L
    /** The most a full room adds: 10% more XP from a comfy home, 10% more for store sales in a stylish one. */
    const val MAX_SCORE = 100
    const val STORE_LEVEL = 8

    /** Columns × rows of floor; the back wall has a slot per column. A bigger shack or store means a bigger room. */
    fun size(f: Farm, k: RoomKind): Pair<Int, Int> = when (k) {
        RoomKind.HOME -> f.bestShack().let { s -> (5 + s) to (3 + (s + 1) / 2) }
        RoomKind.STORE -> storeLevel(f).let { l -> (4 + 2 * l) to (3 + l / 2) }
    }

    fun storeLevel(f: Farm) = f.sites.maxOf { s -> s.tiles.maxOfOrNull { if (Storefronts.isStore(it)) it.lvl else 0 } ?: 0 }
    fun canEnter(f: Farm, k: RoomKind) = k == RoomKind.HOME || storeLevel(f) > 0

    fun room(f: Farm, k: RoomKind) = f.rooms[k.ordinal]

    /** The pieces inside the room as it is now (a smaller room after a reset leaves the far ones boxed up). */
    fun shown(f: Farm, k: RoomKind): List<Placed> {
        val (w, h) = size(f, k)
        return room(f, k).items.filter { it.x + it.furn.w <= w && (it.furn.wall || it.y + it.furn.h <= h) }
    }

    fun at(f: Farm, k: RoomKind, x: Int, y: Int, wall: Boolean): Placed? = shown(f, k).firstOrNull {
        it.furn.wall == wall && x in it.x until it.x + it.furn.w && (wall || y in it.y until it.y + it.furn.h)
    }

    /** Whether [furn] fits with its corner at [x],[y], ignoring [moving]. */
    fun free(f: Farm, k: RoomKind, furn: Furn, x: Int, y: Int, moving: Placed? = null): Boolean {
        val (w, h) = size(f, k)
        if (x < 0 || x + furn.w > w) return false
        if (!furn.wall && (y < 0 || y + furn.h > h)) return false
        return shown(f, k).none { o ->
            o !== moving && o.furn.wall == furn.wall && o.x < x + furn.w && x < o.x + o.furn.w &&
                (furn.wall || (o.y < y + furn.h && y < o.y + o.furn.h))
        }
    }

    fun buy(f: Farm, k: RoomKind, furn: Furn, x: Int, y: Int): Result {
        if (!canEnter(f, k) || !furn.fits(k)) return Result.BLOCKED
        if (f.level < furn.level) return Result.LOCKED
        if (!free(f, k, furn, x, y)) return Result.BLOCKED
        if (f.cash < furn.price) return Result.NO_CASH
        f.spend(furn.price)
        room(f, k).items += Placed(furn, x, if (furn.wall) 0 else y)
        return Result.OK
    }

    fun move(f: Farm, k: RoomKind, p: Placed, x: Int, y: Int): Result {
        if (p !in room(f, k).items || !free(f, k, p.furn, x, y, p)) return Result.BLOCKED
        p.x = x; p.y = if (p.furn.wall) 0 else y
        return Result.OK
    }

    fun sell(f: Farm, k: RoomKind, p: Placed): Result {
        if (!room(f, k).items.remove(p)) return Result.BLOCKED
        f.cash += p.furn.refund
        return Result.OK
    }

    /** Sets the wall paint ([wall]) or the floor ([wall] false) to choice [i]. */
    fun paint(f: Farm, k: RoomKind, wall: Boolean, i: Int): Result {
        val r = room(f, k)
        if (i !in (if (wall) WALLS.indices else FLOORS.indices)) return Result.BLOCKED
        if ((if (wall) r.wall else r.floor) == i) return Result.OK
        if (f.cash < PAINT) return Result.NO_CASH
        f.spend(PAINT)
        if (wall) r.wall = i else r.floor = i
        return Result.OK
    }

    /** Comfort (home) or style (store), up to [MAX_SCORE]. */
    fun score(f: Farm, k: RoomKind) = shown(f, k).sumOf { it.furn.score }.coerceAtMost(MAX_SCORE)
    /** Extra XP from a comfy home, in percent. */
    fun xpBonus(f: Farm) = score(f, RoomKind.HOME) / 10
    /** Added to a store's price over market from a stylish shop floor. */
    fun storeBonus(f: Farm) = score(f, RoomKind.STORE) / 1000.0

    fun write(f: Farm) = JSONArray().apply {
        f.rooms.forEach { r ->
            put(JSONObject().apply {
                put("wall", r.wall); put("floor", r.floor)
                put("items", JSONArray().apply { r.items.forEach { put(JSONArray().put(it.furn.name).put(it.x).put(it.y)) } })
            })
        }
    }

    fun read(a: JSONArray?, f: Farm) {
        f.rooms.forEach { it.wall = 0; it.floor = 0; it.items.clear() }
        if (a == null) return
        f.rooms.forEachIndexed { i, r ->
            val o = a.optJSONObject(i) ?: return@forEachIndexed
            r.wall = o.optInt("wall").coerceIn(WALLS.indices); r.floor = o.optInt("floor").coerceIn(FLOORS.indices)
            val items = o.optJSONArray("items") ?: return@forEachIndexed
            for (j in 0 until items.length()) {
                val e = items.optJSONArray(j) ?: continue
                val furn = Furn.entries.firstOrNull { it.name == e.optString(0) } ?: continue
                r.items += Placed(furn, e.optInt(1), e.optInt(2))
            }
        }
    }
}
