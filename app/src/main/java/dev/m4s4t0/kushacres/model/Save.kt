package dev.m4s4t0.kushacres.model

import org.json.JSONArray
import org.json.JSONObject

/** The farm as JSON. Unknown or missing fields fall back to defaults, so old saves keep loading. */
object Save {
    const val VERSION = 5

    private fun ints(m: Map<String, Int>) = JSONObject().apply { m.forEach { (k, v) -> put(k, v) } }
    private fun readInts(o: JSONObject?, into: MutableMap<String, Int>, ok: (String) -> Boolean) {
        into.clear()
        o?.keys()?.forEach { k -> if (ok(k)) o.optInt(k).takeIf { it > 0 }?.let { into[k] = it } }
    }

    private fun writeTiles(tiles: Array<Tile>) = JSONArray().apply {
        tiles.forEachIndexed { i, t ->
            if (t.kind == Kind.GRASS) return@forEachIndexed
            put(JSONObject().apply {
                put("i", i); put("k", t.kind.name)
                t.strain?.let { put("s", it) }
                if (t.progress != 0.0) put("p", t.progress)
                if (t.wetUntil > 0) put("w", t.wetUntil)
                t.last?.let { put("l", it) }
                t.item?.let { put("it", it) }
                if (t.doneAt > 0) put("d", t.doneAt)
                if (t.amount > 0) put("a", t.amount)
                if (t.lvl > 1) put("lv", t.lvl)
                if (t.pest) put("pe", true)
                if (t.mould) put("mo", true)
                if (t.anchor >= 0) put("an", t.anchor)
            })
        }
    }

    /** Tile indices are y*stride+x; saves before version 4 used a 16-wide grid. */
    private fun readTiles(arr: JSONArray?, tiles: Array<Tile>, stride: Int) {
        tiles.forEach { it.clear() }
        fun remap(i: Int): Int {
            val x = i % stride; val y = i / stride
            return if (i < 0 || x >= Farm.MAX || y >= Farm.MAX) -1 else y * Farm.MAX + x
        }
        if (arr == null) return
        for (j in 0 until arr.length()) {
            val t = arr.getJSONObject(j)
            val tile = tiles.getOrNull(remap(t.getInt("i"))) ?: continue
            tile.kind = runCatching { Kind.valueOf(t.getString("k")) }.getOrDefault(Kind.GRASS)
            tile.strain = t.optString("s", "").takeIf { Strains[it] != null }
            tile.progress = t.optDouble("p", 0.0)
            tile.wetUntil = t.optLong("w")
            tile.last = t.optString("l", "").takeIf { Strains[it] != null }
            tile.item = t.optString("it", "").takeIf { it.isNotEmpty() }
            tile.doneAt = t.optLong("d"); tile.amount = t.optInt("a")
            tile.lvl = t.optInt("lv", 1).coerceIn(1, Facility.MAX_LEVEL)
            tile.pest = t.optBoolean("pe"); tile.mould = t.optBoolean("mo"); tile.anchor = t.optInt("an", -1).let { if (it < 0) -1 else remap(it) }
            val bad = when (tile.kind) {
                Kind.FACILITY -> Facility.of(tile.item) == null
                Kind.HOUSE -> WorkHouse.of(tile.item) == null
                Kind.POWER -> Generator.of(tile.item) == null
                Kind.SPECIAL -> Special.of(tile.item) == null
                Kind.DECOR, Kind.PATH -> Decor.of(tile.item) == null
                Kind.VEHICLE -> Vehicle.of(tile.item) == null
                else -> false
            }
            if (bad) tile.clear()
        }
        // A vehicle needs all four of its squares; drop any half-parked one.
        for ((i, t) in tiles.withIndex()) if (t.kind == Kind.VEHICLE) t.anchor = i
        for ((i, t) in tiles.withIndex()) {
            if (t.kind != Kind.VEHICLE) continue
            val parts = tiles.count { it.kind == Kind.CAB && it.anchor == i }
            if (parts != 3) tiles.forEach { if ((it.kind == Kind.CAB || it.kind == Kind.VEHICLE) && it.anchor == i) it.clear() }
        }
        tiles.forEach { t -> if (t.kind == Kind.CAB && tiles.getOrNull(t.anchor)?.kind != Kind.VEHICLE) t.clear() }
    }

    fun write(f: Farm): String {
        val o = JSONObject()
        o.put("v", VERSION)
        o.put("stride", Farm.MAX); o.put("cash", f.cash); o.put("level", f.level); o.put("xp", f.xp)
        o.put("helpers", JSONObject().apply { Helper.entries.forEach { if (f.has(it)) put(it.name, true) } })
        o.put("runnerOn", f.runnerOn); o.put("runnerAt", f.runnerAt)
        o.put("lastTick", f.lastTick)
        o.put("harvests", f.totalHarvests); o.put("earned", f.totalEarned)
        o.put("envelopes", f.envelopes)
        o.put("bmRot", f.bmRot); o.put("bmBought", JSONArray(f.bmBought.toList()))
        o.put("stash", ints(f.stash)); o.put("goods", ints(f.goods)); o.put("seeds", ints(f.seeds)); o.put("decor", ints(f.decorInv))
        o.put("supplies", JSONObject().apply { Supply.entries.forEach { put(it.name, f.supplies[it.ordinal]) } })
        o.put("buffs", JSONObject().apply { f.buffs.forEach { (k, v) -> put(k.name, v) } })
        o.put("demand", JSONObject().apply { f.demand.forEach { (k, v) -> put(k, v) } })
        o.put("site", f.site.def.id)
        o.put("sites", JSONArray().apply {
            f.sites.forEach { st ->
                put(JSONObject().put("id", st.def.id).put("land", st.land).put("roof", st.roof).put("wall", st.wall)
                    .put("workers", st.workers).put("crew", JSONArray(st.crew.toList())).put("hv", st.harvests).put("goals", JSONArray(st.goals.toList())).put("shack", st.shack).put("up", ups(st.upgrades)).put("tiles", writeTiles(st.tiles))
                    .apply { st.cx?.let { put("cx", it) }; st.cy?.let { put("cy", it) }; st.grass?.let { put("grass", it) }; st.dirt?.let { put("dirt", it) } })
            }
        })
        val orders = JSONArray()
        for (i in 0 until Farm.MAX_ORDERS) {
            val ord = f.orders[i]
            orders.put(JSONObject().apply {
                put("at", f.orderAt[i])
                if (f.deliverAt[i] > 0) { put("da", f.deliverAt[i]); put("dp", f.deliverPay[i]) }
                if (ord != null) {
                    put("buyer", ord.buyer); put("reward", ord.reward); put("xp", ord.xp); put("vip", ord.vip)
                    put("items", JSONArray().apply { ord.items.forEach { put(JSONObject().put("s", it.strain).put("g", it.grams)) } })
                }
            })
        }
        o.put("orders", orders)
        o.put("stats", JSONObject().apply { Stat.entries.forEach { if (f.stat(it) > 0) put(it.name, f.stat(it)) } })
        o.put("trophies", JSONArray(f.trophies.toList())); o.put("bred", JSONArray(f.bred.toList())); o.put("grown", JSONArray(f.grown.toList()))
        o.put("legacy", f.legacy); o.put("bestLevel", maxOf(f.bestLevel, f.level))
        o.put("streak", f.streak); o.put("bestStreak", f.bestStreak); o.put("lastDay", f.lastDay)
        o.put("questDay", f.questDay); o.put("questBonus", f.questBonus)
        o.put("cupWeek", f.cupWeek); o.put("cup", JSONArray().apply { f.cupScores.forEach { put(JSONObject().put("s", it.first).put("p", it.second)) } })
        f.hunt?.let { o.put("hunt", it) }; o.put("huntBack", f.huntBack)
        o.put("contracts", Contracts.save(f))
        o.put("quests", JSONArray().apply {
            f.quests.forEach { q ->
                put(JSONObject().put("k", q.kind.name).put("t", q.target).put("b", q.base).put("c", q.cash).put("x", q.xp).put("done", q.claimed))
            }
        })
        o.put("potBucks", f.potBucks)
        o.put("gems", f.gems); o.put("mineAt", f.mineAt)
        o.put("tokens", JSONObject().apply { f.tokens.forEach { (k, v) -> put(k, v) } })
        o.put("holidayStrains", JSONArray(f.unlockedStrains.toList())); o.put("pets", JSONArray(f.pets.toList()))
        f.farmer?.let { o.put("farmerName", it.name); o.put("farmer", it.encode()) }
        o.put("festivalUntil", f.festivalUntil); f.festivalSite?.let { o.put("festivalSite", it) }
        return o.toString()
    }

    private fun ups(a: IntArray) = JSONObject().apply { Upgrade.entries.forEach { put(it.name, a[it.ordinal]) } }
    private fun readUps(u: JSONObject?, into: IntArray) {
        into.fill(0)
        u?.let { Upgrade.entries.forEach { into[it.ordinal] = u.optInt(it.name).coerceIn(0, it.max) } }
    }

    fun read(json: String, f: Farm) {
        val o = JSONObject(json)
        f.cash = o.optLong("cash", 30); f.level = o.optInt("level", 1); f.xp = o.optLong("xp")
        f.helpers.fill(false)
        o.optJSONObject("helpers")?.let { h -> Helper.entries.forEach { f.helpers[it.ordinal] = h.optBoolean(it.name) } }
        if (o.optBoolean("farmhand")) f.farmhand = true
        if (o.optBoolean("manager")) f.manager = true
        f.runnerOn = o.optBoolean("runnerOn", true); f.runnerAt = o.optLong("runnerAt")
        f.lastTick = o.optLong("lastTick")
        f.totalHarvests = o.optLong("harvests"); f.totalEarned = o.optLong("earned")
        // Before 0.9.0 the shack and Violet's upgrades were shared; old saves give them to every farm they own.
        val oldShack = o.optInt("shack").coerceIn(0, SHACKS.size - 1)
        f.level = f.level.coerceIn(1, Farm.MAX_LEVEL)
        f.envelopes = o.optInt("envelopes")
        f.bmRot = o.optLong("bmRot", -1); f.bmBought.clear()
        o.optJSONArray("bmBought")?.let { a -> for (j in 0 until a.length()) f.bmBought += a.getInt(j) }
        val oldUp = o.optJSONObject("up")
        readInts(o.optJSONObject("stash"), f.stash) { Strains[it] != null }
        readInts(o.optJSONObject("goods"), f.goods) { f.goodsParts(it) != null }
        readInts(o.optJSONObject("seeds"), f.seeds) { Strains[it]?.exotic == true }
        f.stats.fill(0)
        o.optJSONObject("stats")?.let { s -> Stat.entries.forEach { f.stats[it.ordinal] = s.optLong(it.name).coerceAtLeast(0) } }
        // Saves from before stats: start the counters from the old totals.
        f.stats[Stat.HARVESTS.ordinal] = maxOf(f.stat(Stat.HARVESTS), f.totalHarvests)
        f.stats[Stat.EARNED.ordinal] = maxOf(f.stat(Stat.EARNED), f.totalEarned)
        fun strings(key: String, into: MutableSet<String>, ok: (String) -> Boolean) {
            into.clear()
            o.optJSONArray(key)?.let { a -> for (j in 0 until a.length()) a.optString(j).takeIf(ok)?.let { into += it } }
        }
        strings("trophies", f.trophies) { Trophy.of(it) != null }
        strings("bred", f.bred) { Strains[it]?.hybrid == true }
        strings("grown", f.grown) { Strains[it] != null }
        strings("holidayStrains", f.unlockedStrains) { Strains[it]?.holiday != null }
        strings("pets", f.pets) { n -> Pet.entries.any { it.name == n } }
        f.potBucks = o.optLong("potBucks").coerceAtLeast(0)
        f.gems = o.optLong("gems").coerceAtLeast(0); f.mineAt = o.optLong("mineAt")
        f.tokens.clear()
        o.optJSONObject("tokens")?.let { t -> t.keys().forEach { k -> if (Holiday[k] != null) f.tokens[k] = t.optLong(k).coerceAtLeast(0) } }
        f.farmer = if (o.has("farmer")) Farmer.decode(o.optString("farmerName", "Farmer").take(Farmer.NAME_MAX), o.optString("farmer")) else null
        f.festivalUntil = o.optLong("festivalUntil"); f.festivalSite = o.optString("festivalSite", "").takeIf { it.isNotEmpty() }
        f.legacy = o.optInt("legacy").coerceAtLeast(0)
        f.bestLevel = maxOf(o.optInt("bestLevel", 1), f.level)
        f.streak = o.optInt("streak"); f.bestStreak = o.optInt("bestStreak"); f.lastDay = o.optLong("lastDay", -1)
        f.questDay = o.optLong("questDay", -1); f.questBonus = o.optBoolean("questBonus")
        f.cupWeek = o.optLong("cupWeek", -1); f.cupScores.clear()
        f.hunt = Trip.of(o.optString("hunt", ""))?.name; f.huntBack = o.optLong("huntBack", 0)
        Contracts.load(f, o.optJSONObject("contracts"))
        o.optJSONArray("cup")?.let { a -> for (i in 0 until a.length()) a.optJSONObject(i)?.let { e -> f.cupScores += e.optString("s") to e.optInt("p") } }
        f.quests.clear()
        o.optJSONArray("quests")?.let { a ->
            for (j in 0 until a.length()) {
                val q = a.getJSONObject(j)
                val k = runCatching { QuestKind.valueOf(q.getString("k")) }.getOrNull() ?: continue
                f.quests += Quest(k, q.getLong("t"), q.getLong("b"), q.getLong("c"), q.getLong("x"), q.optBoolean("done"))
            }
        }
        if (f.quests.isEmpty()) f.questDay = -1
        readInts(o.optJSONObject("decor"), f.decorInv) { Decor.of(it) != null }
        f.supplies.fill(0)
        o.optJSONObject("supplies")?.let { s -> Supply.entries.forEach { f.supplies[it.ordinal] = s.optInt(it.name).coerceAtLeast(0) } }
        f.buffs.clear()
        o.optJSONObject("buffs")?.let { b -> Buff.entries.forEach { if (b.has(it.name)) f.buffs[it] = b.getLong(it.name) } }
        f.demand.clear()
        o.optJSONObject("demand")?.let { s -> s.keys().forEach { k -> f.demand[k] = s.getDouble(k) } }
        val stride = o.optInt("stride", 16)
        fun land(n: Int) = n.coerceIn(Farm.LAND[0].first, Farm.MAX)
        val home = Site(Sites.HOME)
        f.sites.clear()
        val list = o.optJSONArray("sites")
        if (list == null) {
            // Before version 5 there was one farm, stored at the top level.
            home.land = land(o.optInt("land", 6)); home.roof = o.optInt("roof"); home.wall = o.optInt("wall")
            home.shack = oldShack; readUps(oldUp, home.upgrades)
            readTiles(o.optJSONArray("tiles"), home.tiles, stride)
            f.sites += home
        } else for (j in 0 until list.length()) {
            val so = list.getJSONObject(j)
            val def = Sites[so.optString("id")] ?: continue
            if (f.owns(def.id)) continue
            val st = if (def === Sites.HOME) home else Site(def)
            st.land = land(so.optInt("land", 6)); st.roof = so.optInt("roof", def.roof); st.wall = so.optInt("wall", def.wall)
            st.workers = so.optInt("workers").coerceIn(0, 99)
            st.harvests = so.optLong("hv")
            so.optJSONArray("goals")?.let { a -> for (k in 0 until a.length()) FarmGoal.of(a.optString(k))?.let { st.goals += it.name } }
            so.optJSONArray("crew")?.let { a -> for (k in Crew.entries) st.crew[k.ordinal] = a.optInt(k.ordinal).coerceIn(0, k.max) }
            st.shack = if (so.has("shack")) so.optInt("shack").coerceIn(0, SHACKS.size - 1) else oldShack
            readUps(if (so.has("up")) so.optJSONObject("up") else oldUp, st.upgrades)
            if (so.has("grass")) st.grass = so.optInt("grass")
            if (so.has("dirt")) st.dirt = so.optInt("dirt")
            if (so.has("cx") && so.has("cy")) { st.cx = so.optInt("cx").coerceIn(0, Farm.MAX - 1); st.cy = so.optInt("cy").coerceIn(0, Farm.MAX - 1) }
            readTiles(so.optJSONArray("tiles"), st.tiles, stride)
            f.sites += st
        }
        if (!f.owns(Sites.HOME.id)) f.sites.add(0, home)
        // Home always comes first; it's the one kept when the farm is sold.
        f.sites.sortBy { if (it.def === Sites.HOME) 0 else 1 }
        f.site = f.sites.firstOrNull { it.def.id == o.optString("site") } ?: f.sites[0]
        f.eachSite {
            f.paint(it.roof, it.wall)
            // Version 1 had no shack: put one down wherever it fits.
            f.autoPlaceShack()
        }
        o.optJSONArray("orders")?.let { arr ->
            for (i in 0 until minOf(arr.length(), Farm.MAX_ORDERS)) {
                val ord = arr.getJSONObject(i)
                f.orderAt[i] = ord.optLong("at")
                f.deliverAt[i] = ord.optLong("da"); f.deliverPay[i] = ord.optLong("dp")
                val items = ord.optJSONArray("items")
                f.orders[i] = if (items == null) null else Order(
                    ord.getString("buyer"),
                    (0 until items.length()).map { items.getJSONObject(it) }
                        .filter { Strains[it.getString("s")] != null }
                        .map { OrderItem(it.getString("s"), it.getInt("g")) },
                    ord.getLong("reward"), ord.getInt("xp"), ord.optBoolean("vip"),
                ).takeIf { it.items.isNotEmpty() }
            }
        }
    }
}
