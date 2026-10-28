package dev.m4s4t0.kushacres

import dev.m4s4t0.kushacres.model.BmItem
import dev.m4s4t0.kushacres.model.Cup
import dev.m4s4t0.kushacres.model.Contracts
import dev.m4s4t0.kushacres.model.Chains
import dev.m4s4t0.kushacres.model.Contract
import dev.m4s4t0.kushacres.model.CupClass
import dev.m4s4t0.kushacres.model.Deal
import dev.m4s4t0.kushacres.model.Keepsake
import dev.m4s4t0.kushacres.model.localDay
import dev.m4s4t0.kushacres.model.Hunt
import dev.m4s4t0.kushacres.model.Trip
import dev.m4s4t0.kushacres.model.Buff
import dev.m4s4t0.kushacres.model.Decor
import dev.m4s4t0.kushacres.model.EventKind
import dev.m4s4t0.kushacres.model.Facility
import dev.m4s4t0.kushacres.model.Farm
import dev.m4s4t0.kushacres.model.Farmer
import dev.m4s4t0.kushacres.model.Helper
import dev.m4s4t0.kushacres.model.Holiday
import dev.m4s4t0.kushacres.model.BucksItem
import dev.m4s4t0.kushacres.model.Hybrids
import dev.m4s4t0.kushacres.model.Pet
import dev.m4s4t0.kushacres.model.Progress
import dev.m4s4t0.kushacres.model.Season
import dev.m4s4t0.kushacres.model.Site
import dev.m4s4t0.kushacres.model.Sites
import dev.m4s4t0.kushacres.model.Stat
import dev.m4s4t0.kushacres.model.Trophy
import dev.m4s4t0.kushacres.model.Kind
import dev.m4s4t0.kushacres.model.Result
import dev.m4s4t0.kushacres.model.Save
import dev.m4s4t0.kushacres.model.Strains
import dev.m4s4t0.kushacres.model.Storefronts
import dev.m4s4t0.kushacres.model.Supply
import dev.m4s4t0.kushacres.model.Upgrade
import dev.m4s4t0.kushacres.model.WorkHouse
import dev.m4s4t0.kushacres.model.Vehicle
import dev.m4s4t0.kushacres.model.Crew
import dev.m4s4t0.kushacres.model.FarmEvent
import dev.m4s4t0.kushacres.model.FarmGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.json.JSONObject
import dev.m4s4t0.kushacres.model.Legacy
import dev.m4s4t0.kushacres.model.LegacyPerk
import dev.m4s4t0.kushacres.model.StartMap
import org.junit.Test
import kotlin.random.Random

class FarmTest {
    private val t0 = 1_700_000_000_000L
    private fun farm() = Farm(Random(7)).apply { newGame(t0) }

    @Test fun strainTimesStayInBounds() {
        val first = Strains.ladder.first()
        assertEquals(5, first.minutes)
        assertTrue(Strains.all.all { it.minutes in 5..960 })
        assertTrue(Strains.ladder.size >= 30 && Strains.exotics.isNotEmpty())
        assertTrue(Strains.ladder.all { it.seed >= first.seed })
        // Each strain earns more per hour than the last and is unlocked no earlier.
        Strains.ladder.zipWithNext().forEach { (a, b) ->
            val pa = (a.price * a.yield - a.seed) / a.minutes.toDouble()
            val pb = (b.price * b.yield - b.seed) / b.minutes.toDouble()
            assertTrue("${b.name} should beat ${a.name}", pb > pa && b.level >= a.level && (b.minutes > a.minutes || b.minutes == 960))
        }
    }

    @Test fun starterPlantsAreReady() {
        val f = farm()
        assertEquals(Result.OK, f.harvest(2, 2, t0))
        assertTrue((f.stash["ditch"] ?: 0) > 0)
    }

    @Test fun growsInFiveMinutesDryAndFasterWet() {
        val f = farm()
        assertEquals(Result.OK, f.plant(2, 3, "ditch", t0))
        f.tick(t0 + 4 * 60_000L)
        assertEquals(Result.NOT_READY, f.harvest(2, 3, t0))
        f.tick(t0 + 5 * 60_000L)
        assertTrue(f.tile(2, 3).ready())

        val g = farm()
        g.plant(2, 3, "ditch", t0); g.water(2, 3, t0)
        g.tick(t0 + 200_000L) // 300 s / 1.5
        assertTrue(g.tile(2, 3).ready())
    }

    @Test fun waterRunsOutPartWay() {
        val f = farm()
        f.cash = 1_000_000; f.level = 7
        f.plant(2, 3, "bluedream", t0); f.water(2, 3, t0)
        // 20 min wet = 30 min of growth, then 30 more dry minutes.
        assertEquals(50 * 60L, f.eta(2, 3, t0))
        f.tick(t0 + 49 * 60_000L)
        assertTrue(!f.tile(2, 3).ready())
        f.tick(t0 + 50 * 60_000L)
        assertTrue(f.tile(2, 3).ready())
    }

    @Test fun sprinklerKeepsNeighboursWet() {
        val f = farm()
        f.cash = 1_000_000; f.level = Farm.SPRINKLER_LEVEL
        assertEquals(Result.OK, f.build(1, 3, Kind.SPRINKLER))
        assertTrue(f.wet(2, 3, t0 + 10 * 3_600_000L))
        assertTrue(!f.wet(3, 3, t0 + 10 * 3_600_000L))
    }

    @Test fun levelGatesAndLevelUps() {
        val f = farm()
        assertEquals(Result.LOCKED, f.plant(2, 3, "voidkush", t0))
        f.addXp(Farm.xpNeed(1) + Farm.xpNeed(2))
        assertEquals(3, f.level)
        assertTrue(f.cash > 30)
    }

    @Test fun stashCapBlocksHarvest() {
        val f = farm()
        f.stash["mids"] = f.stashCap()
        assertEquals(Result.STASH_FULL, f.harvest(2, 2, t0))
        assertTrue(f.tile(2, 2).ready())
    }

    @Test fun sellingFloodsTheMarketAndItRecovers() {
        val f = farm()
        f.stash["ditch"] = 60
        val first = f.price("ditch")
        f.sell("ditch", 60)
        assertTrue(f.price("ditch") < first)
        f.tick(t0 + 6 * 3_600_000L)
        assertTrue(f.price("ditch") > first * 0.99)
    }

    @Test fun ordersArriveAndPay() {
        val f = farm()
        f.tick(t0 + 60_000L)
        val o = f.orders[0]!!
        o.items.forEach { f.stash[it.strain] = it.grams }
        val before = f.cash
        assertEquals(Result.OK, f.fill(0, t0 + 60_000L))
        assertEquals(before + o.reward, f.cash)
    }

    @Test fun farmhandWorksOffline() {
        val f = farm()
        f.cash = 10_000_000; f.level = Farm.MANAGER_LEVEL
        f.upgrades[Upgrade.STASH.ordinal] = 10
        f.hireFarmhand(); f.hireManager()
        f.tick(t0 + 8 * 3_600_000L)
        // Two starter ditch plants, replanted all night: dozens of harvests.
        assertTrue(f.away.harvests > 50)
        assertTrue(f.away.replants > 50)
    }

    @Test fun expansionAndPlotCostsClimb() {
        val f = farm()
        f.cash = 1_000_000
        val c1 = f.plotCost()
        assertEquals(Result.OK, f.build(4, 4, Kind.PLOT))
        assertTrue(f.plotCost() > c1)
        assertEquals(Result.LOCKED, f.expand())
        f.level = 5
        assertEquals(Result.OK, f.expand())
        assertEquals(8, f.land)
    }

    @Test fun saveRoundTrips() {
        val f = farm()
        f.cash = 1234; f.level = 9; f.upgrades[Upgrade.MULCH.ordinal] = 3
        f.plant(2, 3, "ditch", t0); f.water(2, 3, t0)
        f.stash["mids"] = 12; f.tick(t0 + 60_000L)
        val g = Farm()
        Save.read(Save.write(f), g)
        assertEquals(Save.write(f), Save.write(g))
        assertEquals("ditch", g.tile(2, 3).strain)
        assertEquals(3, g.up(Upgrade.MULCH))
    }

    @Test fun shackSitsOnTwoByTwoAndUpgrades() {
        val f = farm()
        assertEquals(Kind.SHACK, f.tile(0, 0).kind)
        assertEquals(Kind.PART, f.tile(1, 1).kind)
        assertEquals(Result.BLOCKED, f.build(1, 0, Kind.PLOT))
        assertEquals(Result.BLOCKED, f.demolish(1, 1))
        val cap = f.stashCap()
        f.cash = 10_000; f.level = 5
        assertEquals(Result.OK, f.upgradeShack())
        assertTrue(f.stashCap() > cap)
        assertEquals(Result.OK, f.move(1, 1, 4, 4))
        assertEquals(Kind.SHACK, f.tile(4, 4).kind)
        assertEquals(Kind.GRASS, f.tile(0, 0).kind)
        // Doesn't fit over the plots: stays put.
        assertEquals(Result.BLOCKED, f.move(4, 4, 2, 2))
        assertEquals(Kind.SHACK, f.tile(4, 4).kind)
    }

    @Test fun exoticsNeedSeeds() {
        val f = farm()
        f.harvest(2, 2, t0)
        assertEquals(Result.NO_SEEDS, f.plant(2, 2, "ghost", t0))
        f.seeds["ghost"] = 1
        assertEquals(Result.OK, f.plant(2, 2, "ghost", t0))
        assertTrue(f.seeds["ghost"] == null)
    }

    @Test fun facilityTurnsBudIntoGoods() {
        val f = farm()
        f.cash = 10_000; f.level = Facility.ROLLER.level
        assertEquals(Result.OK, f.buildFacility(4, 4, Facility.ROLLER))
        assertTrue(f.facilityCost(Facility.ROLLER) > Facility.ROLLER.cost)
        assertEquals(Result.NO_BUD, f.startBatch(4, 4, "ditch", t0))
        f.stash["ditch"] = 100
        assertEquals(Result.NO_SUPPLY, f.startBatch(4, 4, "ditch", t0))
        f.buySupply(Supply.PAPERS, 2)
        assertEquals(Result.OK, f.startBatch(4, 4, "ditch", t0))
        assertEquals(Result.BUSY, f.startBatch(4, 4, "ditch", t0))
        assertEquals(Result.NOT_READY, f.collect(4, 4, t0))
        assertEquals(Result.OK, f.collect(4, 4, t0 + Facility.ROLLER.millis(1)))
        val key = f.goodsKey(Facility.ROLLER, "ditch")
        assertEquals(5, f.goods[key])
        // Pre-rolls are worth more than the bud that went in.
        assertTrue(f.goodsPrice(key) > f.price("ditch") * Facility.ROLLER.grams)
        assertTrue(f.sellAllGoods() > 0 && f.goods.isEmpty())
    }

    @Test fun decorHelpsAndHasALimit() {
        val f = farm()
        f.cash = 1_000_000; f.level = 20
        val base = f.speed(2, 3)
        assertEquals(Result.OK, f.placeDecor(1, 3, Decor.GNOME))
        assertTrue(f.speed(2, 3) > base)
        assertEquals(Result.OK, f.placeDecor(1, 4, Decor.BARREL))
        assertTrue(f.sprinkled(2, 4))
        assertEquals(Result.OK, f.placeDecor(1, 5, Decor.FLOWERS))
        assertEquals(Result.LIMIT, f.placeDecor(4, 4, Decor.BENCH))
        assertEquals(Result.OK, f.placeDecor(4, 4, Decor.STONE)) // paths don't count
        assertEquals(Result.OK, f.demolish(1, 3))
        assertEquals(1, f.decorInv["GNOME"])
        assertEquals(Result.NO_CASH, f.placeDecor(5, 5, Decor.NEON))
    }

    @Test fun helpersRunTheFarmWhileAway() {
        val f = farm()
        f.cash = 100_000_000; f.level = 30; f.shack = 3
        f.upgrades[Upgrade.STASH.ordinal] = 10
        Helper.entries.forEach { assertEquals(it.name, Result.OK, f.hire(it)) }
        f.buildFacility(4, 4, Facility.ROLLER)
        f.stash["ditch"] = 50
        f.startBatch(4, 4, "ditch", t0).let { assertEquals(Result.NO_SUPPLY, it) }
        f.tick(t0 + 4 * 3_600_000L)
        assertTrue(f.away.harvests > 20)
        assertTrue(f.away.sold > 0)
        assertTrue(f.supplies[Supply.PAPERS.ordinal] > 0)
    }

    @Test fun hiringNeedsABiggerShack() {
        val f = farm()
        f.cash = 1_000_000; f.level = 30
        assertEquals(Result.SHACK, f.hire(Helper.RUNNER))
        f.shack = 1
        assertEquals(Result.OK, f.hire(Helper.RUNNER))
        assertEquals(Result.BLOCKED, f.hire(Helper.MANAGER))
    }

    @Test fun eventsHappenAndResolve() {
        val f = farm()
        f.cash = 10_000; f.level = 10
        assertTrue(!f.rollEvent(t0)) // first call only schedules
        assertTrue(f.rollEvent(t0 + 3_600_000L))
        f.resolve(false, t0)
        assertTrue(f.happening == null)

        f.happening = f.makeEvent(t0, EventKind.RAID)
        val bribe = f.happening!!.amount
        f.resolve(true, t0)
        assertEquals(10_000 - bribe, f.cash)

        f.plant(2, 3, "ditch", t0)
        f.makeEvent(t0, EventKind.PESTS)
        assertTrue(f.tile(2, 3).pest)
        f.tick(t0 + 3_600_000L)
        assertTrue(!f.tile(2, 3).ready())
        assertEquals(Result.OK, f.clearPest(2, 3))

        f.makeEvent(t0 + 3_600_000L, EventKind.VIBES)
        assertTrue(f.on(Buff.VIBES))
    }

    @Test fun blackMarketSellsExoticsAndRotates() {
        val f = farm()
        f.cash = 100_000_000; f.level = 40
        val stock = f.bmStock(t0)
        assertEquals(5, stock.size) // level 30+ gets a fifth slot
        assertEquals(stock, f.bmStock(t0 + 1000))
        assertEquals(Result.OK, f.bmBuy(0, t0))
        assertEquals(Result.BLOCKED, f.bmBuy(0, t0))
        val item = stock[0]
        if (item.strain != null) assertEquals(BmItem.EXOTIC_PACK, f.seeds[item.strain])
        assertTrue(f.bmRefreshIn(t0) in 1..Farm.BM_ROTATION)
    }

    @Test fun v2SaveKeepsTheNewStuff() {
        val f = farm()
        f.cash = 1_000_000; f.level = 30; f.shack = 2; f.paint(3, 4)
        f.buildFacility(4, 4, Facility.PRESS); f.placeDecor(5, 5, Decor.GNOME)
        f.seeds["ghost"] = 3; f.goods["PRESS:ditch"] = 4; f.supplies[Supply.ICE.ordinal] = 7
        f.hire(Helper.RUNNER); f.envelopes = 2; f.addBuff(Buff.RUSH, t0, 10)
        val g = Farm()
        Save.read(Save.write(f), g)
        assertEquals(Save.write(f), Save.write(g))
        assertEquals(Kind.FACILITY, g.tile(4, 4).kind)
        assertTrue(g.has(Helper.RUNNER))
    }

    @Test fun v1SaveGetsAShack() {
        val g = Farm()
        Save.read("""{"v":1,"land":6,"farmhand":true,"tiles":[{"i":0,"k":"PLOT"}]}""", g)
        assertTrue(g.farmhand)
        assertTrue(g.shackAt() != null)
        assertEquals(Kind.PLOT, g.tile(0, 0).kind)
    }

    @Test fun labBreedsHybridsFromTwoParents() {
        val f = farm()
        val r = Hybrids.recipes.first()
        val s = Strains[r.id]!!
        f.cash = 1_000_000_000
        f.level = Progress.BREED_LEVEL - 1
        assertEquals(Result.LOCKED, f.breed(r.id, t0))
        f.level = 80
        assertEquals(Result.NO_BUD, f.breed(r.id, t0))
        val g = f.breedGrams(s)
        f.stash[r.a] = g; f.stash[r.b] = g + 5
        f.tick(t0)
        assertEquals(Result.OK, f.breed(r.id, t0))
        // The parents go now; the strain comes once the research is done.
        assertFalse(r.id in f.bred)
        assertEquals(r.id, f.research)
        assertEquals(null, f.stash[r.a])
        assertEquals(5, f.stash[r.b])
        assertEquals(Result.BLOCKED, f.breed(r.id, t0))
        assertEquals(Result.BUSY, f.breed(Hybrids.recipes[1].id, t0))
        f.tick(f.researchAt + 1)
        assertTrue(r.id in f.bred)
        assertEquals(null, f.research)
        assertTrue(f.events.any { it is FarmEvent.Bred })
        assertEquals(Result.BLOCKED, f.breed(r.id, t0))
        assertEquals(1, f.stat(Stat.BRED))
        assertEquals(r, Hybrids.of(r.b, r.a))
    }

    @Test fun dailyStreakGrowsAndBreaks() {
        val f = farm()
        val day = 86_400_000L
        assertTrue(f.canClaimDaily(t0))
        assertTrue(f.claimDaily(t0) != null)
        assertNull(f.claimDaily(t0 + 1000))
        assertFalse(f.canClaimDaily(t0 + 1000))
        for (i in 1..6) assertTrue(f.claimDaily(t0 + i * day) != null)
        assertEquals(7, f.streak)
        assertEquals(3, f.seeds.filterKeys { Strains[it]?.exotic == true }.values.sum()) // day 7 pays exotic seeds
        f.claimDaily(t0 + 9 * day)
        assertEquals(1, f.streak)
        assertEquals(7, f.bestStreak)
        f.checkTrophies(silent = true)
        assertTrue(Trophy.STREAK_7.name in f.trophies)
    }

    @Test fun questsPayOnceAndThenTheBonus() {
        val f = farm()
        f.refreshQuests(t0)
        assertEquals(3, f.quests.size)
        assertEquals(Result.NOT_READY, f.claimQuest(0))
        assertFalse(f.canClaimQuestBonus())
        for (q in f.quests) f.bump(q.kind.stat, q.target)
        assertTrue(f.canClaim(t0) )
        for (i in 0..2) assertEquals(Result.OK, f.claimQuest(i))
        assertEquals(Result.BLOCKED, f.claimQuest(0))
        val cash = f.cash
        assertEquals(Result.OK, f.claimQuestBonus(t0))
        assertEquals(cash + f.questBonusCash(), f.cash)
        assertEquals(Result.BLOCKED, f.claimQuestBonus(t0))
        // Same day keeps the quests; the next day brings new ones.
        f.refreshQuests(t0 + 1000)
        assertTrue(f.quests.all { it.claimed })
        f.refreshQuests(t0 + 86_400_000L)
        assertTrue(f.quests.none { it.claimed })
    }

    @Test fun sellingTheFarmKeepsTheLegacy() {
        val f = farm()
        f.cash = 1_000_000; f.level = 39
        assertEquals(Result.LOCKED, f.sellFarm(t0))
        f.level = 40; f.bestLevel = 40; f.placeDecor(5, 5, Decor.GNOME); f.bred += Hybrids.recipes.first().id
        f.bump(Stat.HARVESTS, 50)
        val price = f.priceMult()
        assertEquals(Result.OK, f.sellFarm(t0))
        assertEquals(1, f.legacy); assertEquals(1, f.level); assertEquals(6, f.land)
        assertEquals(1, f.decorInv[Decor.GNOME.name] ?: f.decorInv.values.sum())
        assertTrue(f.bred.isNotEmpty())
        assertEquals(50, f.stat(Stat.HARVESTS))
        assertTrue(f.shackAt() != null)
        assertTrue(f.priceMult() > price * 0.9)
        assertEquals(40, f.bestLevel)
    }

    @Test fun legacyPerksHeirloomsAndStartMaps() {
        val f = farm()
        f.cash = 1_000_000; f.level = 40
        val heir = Legacy.eligible(f).last()
        val junk = Strains.ladder.first { it.level > 40 }.id
        assertTrue(Legacy.slots(f) == 1 && Legacy.points(f) == 0)
        assertEquals(Result.LOCKED, Legacy.take(f, LegacyPerk.GREEN_THUMB))
        assertEquals(Result.OK, f.sellFarm(t0, StartMap.CREEKSIDE, listOf(heir.id, junk)))
        assertEquals(setOf(heir.id), f.heirlooms.toSet())
        assertTrue(f.unlocked(heir) && heir.level > f.level)
        assertEquals(StartMap.CREEKSIDE, f.startMap)
        assertEquals(2, f.tiles.count { it.kind == Kind.WATER })
        // The pond waters the plots beside it.
        assertTrue(f.sprinkled(f.ox + 3, f.oy + 2))
        // One point: a branch's second perk waits for its first.
        assertEquals(1, Legacy.points(f))
        assertEquals(Result.LOCKED, Legacy.take(f, LegacyPerk.BIG_BUDS))
        val speed = f.speed(f.ox + 2, f.oy + 2)
        assertEquals(Result.OK, Legacy.take(f, LegacyPerk.GREEN_THUMB))
        assertEquals(speed * 1.15, f.speed(f.ox + 2, f.oy + 2), 1e-9)
        assertEquals(Result.BLOCKED, Legacy.take(f, LegacyPerk.GREEN_THUMB))
        assertEquals(Result.LOCKED, Legacy.take(f, LegacyPerk.BIG_BUDS))
        // Old Money pays out on the next sale; Quarry Hill starts bigger with boulders.
        f.legacyPerks += listOf(LegacyPerk.SILVER_TONGUE, LegacyPerk.KNOWN_FACE, LegacyPerk.OLD_MONEY, LegacyPerk.BIG_BUDS, LegacyPerk.SEED_VAULT)
        assertEquals(3, Legacy.slots(f))
        f.level = 40
        assertEquals(Result.OK, f.sellFarm(t0, StartMap.QUARRY, Legacy.eligible(f).takeLast(4).map { it.id }))
        assertEquals(3, f.heirlooms.size)
        assertEquals(30 + Legacy.OLD_MONEY, f.cash)
        assertEquals(Legacy.QUARRY_LAND, f.land)
        assertTrue(f.tiles.count { it.kind == Kind.ROCK } >= 6)
        assertEquals(0, f.tiles.count { it.kind == Kind.WATER })
        val g = farm()
        Save.read(Save.write(f), g)
        assertEquals(f.legacyPerks, g.legacyPerks); assertEquals(f.heirlooms, g.heirlooms); assertEquals(StartMap.QUARRY, g.startMap)
        g.restart(t0)
        assertTrue(g.legacyPerks.isEmpty() && g.heirlooms.isEmpty())
    }

    @Test fun seasonsTurnWeeklyAndChangeTheRules() {
        assertEquals(Season.SPRING, Season.at(0))
        assertEquals(Season.SUMMER, Season.at(Season.WEEK))
        assertEquals(Season.SPRING, Season.at(4 * Season.WEEK + 5))
        assertEquals(Season.WEEK - 5, Season.left(4 * Season.WEEK + 5))
        val f = farm()
        try {
            Season.force = Season.SUMMER
            val y = f.yieldMult(); val p = f.priceMult()
            Season.force = Season.AUTUMN
            assertEquals(y * 1.2, f.yieldMult(), 1e-9)
            Season.force = Season.WINTER
            assertEquals(p * 1.25, f.priceMult(), 1e-9)
        } finally { Season.force = null }
    }

    @Test fun progressSurvivesASave() {
        val f = farm()
        f.refreshQuests(t0); f.claimDaily(t0); f.bump(Stat.GOLDEN, 4)
        f.bred += Hybrids.recipes.first().id; f.legacy = 2; f.level = 12
        f.checkTrophies(silent = true)
        val g = Farm()
        Save.read(Save.write(f), g)
        assertEquals(Save.write(f), Save.write(g))
        assertEquals(2, g.legacy); assertEquals(1, g.streak); assertEquals(3, g.quests.size)
        assertTrue(Trophy.GOLDEN_1.name in g.trophies)
    }

    @Test fun oldSaveFromBeforeProgressLoads() {
        // A 0.1.x save has none of the progress keys; its old totals seed the new counters.
        val f = farm()
        f.level = 12; f.totalHarvests = 340; f.totalEarned = 50_000
        val o = org.json.JSONObject(Save.write(f))
        for (k in listOf("stats", "trophies", "bred", "legacy", "bestLevel", "streak", "bestStreak", "lastDay", "questDay", "questBonus", "quests")) o.remove(k)
        val g = Farm()
        Save.read(o.toString(), g)
        assertEquals(340, g.stat(Stat.HARVESTS))
        assertEquals(50_000, g.stat(Stat.EARNED))
        assertEquals(12, g.bestLevel)
        assertEquals(0, g.legacy)
        assertTrue(g.canClaimDaily(t0))
        g.checkTrophies(silent = true)
        assertTrue(Trophy.HARVEST_100.name in g.trophies)
        assertTrue(g.events.isEmpty())
    }

    @Test fun oldSixteenWideSavesKeepTheirLayout() {
        val g = Farm(Random(1))
        // Version 3: index 17 was x=1,y=1 on the 16-wide grid; the shack anchor 34 was x=2,y=2.
        Save.read("""{"v":3,"land":8,"tiles":[{"i":17,"k":"PLOT"},{"i":34,"k":"SHACK","an":34},{"i":35,"k":"PART","an":34},{"i":50,"k":"PART","an":34},{"i":51,"k":"PART","an":34}]}""", g)
        assertEquals(Kind.PLOT, g.tile(1, 1).kind)
        assertEquals(Kind.GRASS, g.tile(17 % Farm.MAX, 17 / Farm.MAX).kind)
        assertEquals(2 to 2, g.shackAt())
    }

    @Test fun landTiersGrowToTheWholeMap() {
        val l = Farm.LAND
        assertEquals(Farm.MAX, l.last().first)
        for (i in 1 until l.size) {
            assertTrue(l[i].first > l[i - 1].first && l[i].second >= l[i - 1].second && l[i].third > l[i - 1].third)
        }
        assertTrue(l.last().second <= Farm.MAX_LEVEL)
    }

    @Test fun updaterComparesVersions() {
        assertTrue(Updater.newer("v0.3.0", "0.2.1"))
        assertTrue(Updater.newer("0.10.0", "0.9.9"))
        assertFalse(Updater.newer("v0.2.1", "0.2.1"))
        assertFalse(Updater.newer("0.2", "0.2.1"))
    }

    // ---- Other farms ----

    private fun rich() = farm().apply { level = 20; bestLevel = 20; cash = 10_000_000_000L }

    @Test fun buyAndSwitchFarms() {
        val f = rich()
        val red = Sites["redwood"]!!
        f.level = 14
        assertEquals(Result.LOCKED, f.canBuy(red))
        f.level = 15
        assertEquals(Result.LOCKED, f.canBuy(Sites["mesa"]!!))
        val cash = f.cash
        assertEquals(Result.OK, f.buySite(red))
        assertEquals(cash - red.price, f.cash)
        assertEquals(Result.BLOCKED, f.buySite(red))
        assertEquals("redwood", f.site.def.id)
        assertEquals(8, f.land)
        assertEquals(Kind.SHACK, f.tile(f.ox, f.oy).kind)
        assertEquals(Kind.PLOT, f.tile(f.ox + 3, f.oy + 3).kind)
        // The bought square and the ring round it are clear of water and rocks.
        for (y in f.oy - 1..f.oy + f.land) for (x in f.ox - 1..f.ox + f.land)
            assertTrue(f.tile(x, y).kind != Kind.WATER && f.tile(x, y).kind != Kind.ROCK)
        // The map itself has some terrain.
        assertTrue(f.tiles.any { it.kind == Kind.WATER })
        assertTrue(f.switchTo("home"))
        assertEquals(6, f.land)
        assertEquals(Kind.PLOT, f.tile(2, 2).kind)
        assertFalse(f.switchTo("mesa"))
    }

    @Test fun upgradesAndShackBelongToOneFarm() {
        val f = rich()
        f.upgrades[Upgrade.STASH.ordinal] = 2
        f.shack = 3
        val homeStash = f.siteStash()
        f.buySite(Sites["redwood"]!!)
        assertEquals(0, f.upgrades[Upgrade.STASH.ordinal])
        assertEquals(0, f.shack)
        assertEquals(3, f.bestShack())
        assertEquals(homeStash + f.siteStash(), f.stashCap())
        // Each farm's upgrades and shack survive a save.
        val g = farm()
        Save.read(Save.write(f), g)
        assertEquals(Save.write(f), Save.write(g))
        g.switchTo("home")
        assertEquals(2, g.upgrades[Upgrade.STASH.ordinal])
        assertEquals(3, g.shack)
    }

    @Test fun oldSavesGiveTheirUpgradesToEveryFarm() {
        val f = rich()
        f.buySite(Sites["redwood"]!!)
        val o = JSONObject(Save.write(f))
        val sites = o.getJSONArray("sites")
        for (i in 0 until sites.length()) sites.getJSONObject(i).apply { remove("shack"); remove("up") }
        o.put("shack", 2).put("up", JSONObject().put(Upgrade.STASH.name, 1))
        val g = farm()
        Save.read(o.toString(), g)
        for (s in g.sites) {
            assertEquals(2, s.shack)
            assertEquals(1, s.upgrades[Upgrade.STASH.ordinal])
        }
    }

    @Test fun everyFarmHasItsOwnTown() {
        for (def in Sites.all + Sites.holidays) {
            val t = dev.m4s4t0.kushacres.model.Towns[def.id]
            assertEquals(6, t.names.size)
            assertTrue(def.id, def.id == "home" || t !== dev.m4s4t0.kushacres.model.Towns.HOME)
        }
        val towns = dev.m4s4t0.kushacres.model.Towns.ids.map { dev.m4s4t0.kushacres.model.Towns[it].town }
        assertEquals(towns.size, towns.toSet().size)
    }

    @Test fun everyFarmGrowsAroundItsSpot() {
        for (def in Sites.all) {
            val st = Site(def)
            for ((a, b) in Farm.LAND.zipWithNext()) {
                st.land = a.first; val ox = st.ox; val oy = st.oy
                st.land = b.first
                assertTrue("${def.id} ${a.first}->${b.first}", st.ox <= ox && st.oy <= oy &&
                    st.ox + b.first >= ox + a.first && st.oy + b.first >= oy + a.first && st.ox + b.first <= Farm.MAX && st.oy + b.first <= Farm.MAX)
            }
        }
        assertEquals(10, Sites.all.size - 1)
        assertEquals(Sites.all.size, Sites.all.map { it.id }.toSet().size)
    }

    @Test fun otherFarmsKeepGrowing() {
        val f = rich()
        assertEquals(Result.OK, f.plant(2, 3, "ditch", t0))
        f.buySite(Sites["redwood"]!!)
        f.plant(f.ox + 3, f.oy + 3, "ditch", t0)
        f.tick(t0 + 6 * 60_000L)
        assertTrue(f.tile(f.ox + 3, f.oy + 3).ready())
        f.switchTo("home")
        assertTrue(f.tile(2, 3).ready())
    }

    @Test fun waterKeepsPlotsWet() {
        val f = farm()
        f.plant(2, 3, "ditch", t0)
        assertFalse(f.sprinkled(2, 3))
        f.tile(1, 3).kind = Kind.WATER
        assertTrue(f.sprinkled(2, 3))
    }

    @Test fun bouldersCanBeBrokenUp() {
        val f = farm()
        f.tile(4, 4).kind = Kind.ROCK
        assertEquals(Result.BLOCKED, f.build(4, 4, Kind.PLOT))
        assertEquals(Result.BLOCKED, f.demolish(4, 4))
        f.cash = f.rockCost() - 1
        assertEquals(Result.NO_CASH, f.clearRock(4, 4))
        f.cash = f.rockCost()
        assertEquals(Result.OK, f.clearRock(4, 4))
        assertEquals(0L, f.cash)
        assertEquals(Kind.GRASS, f.tile(4, 4).kind)
        assertEquals(Result.BLOCKED, f.clearRock(4, 4))
    }

    @Test fun farmsSaveAndOldSavesMigrate() {
        val f = rich()
        f.plant(2, 3, "ditch", t0)
        f.buySite(Sites["mesa"]!!.also { f.level = 30 })
        f.roof = 3
        val g = Farm()
        Save.read(Save.write(f), g)
        assertEquals(Save.write(f), Save.write(g))
        assertEquals("mesa", g.site.def.id)
        assertEquals(listOf("home", "mesa"), g.sites.map { it.def.id })
        assertEquals(3, g.roof)
        g.switchTo("home")
        assertEquals("ditch", g.tile(2, 3).strain)

        // A v4 save kept one farm's tiles at the top level.
        val home = rich(); home.plant(2, 3, "ditch", t0); assertEquals(Result.OK, home.expand())
        val o = JSONObject(Save.write(home))
        val site = o.getJSONArray("sites").getJSONObject(0)
        o.remove("sites"); o.remove("site")
        o.put("land", site.getInt("land")).put("roof", site.getInt("roof")).put("wall", site.getInt("wall")).put("tiles", site.getJSONArray("tiles"))
        val h = Farm()
        Save.read(o.toString(), h)
        assertEquals(1, h.sites.size)
        assertEquals(8, h.land)
        assertEquals("ditch", h.tile(2, 3).strain)
        assertEquals(Kind.SHACK, h.tile(0, 0).kind)
    }

    @Test fun sellingTheFarmKeepsOnlyHome() {
        val f = rich()
        f.level = 40; f.bestLevel = 40
        f.buySite(Sites["redwood"]!!)
        assertEquals(Result.OK, f.sellFarm(t0))
        assertEquals(1, f.sites.size)
        assertEquals("home", f.site.def.id)
        assertEquals(Kind.SHACK, f.tile(0, 0).kind)
    }

    @Test fun workBuildingsAndWorkers() {
        val f = rich()
        fun grass() = (f.oy until f.oy + f.land).flatMap { b -> (f.ox until f.ox + f.land).map { it to b } }.first { (a, b) -> f.tile(a, b).kind == Kind.GRASS }
        val (x, y) = grass()
        f.level = WorkHouse.GROW.level - 1
        assertEquals(Result.LOCKED, f.buildHouse(x, y, WorkHouse.GROW))
        f.level = 30
        val spent = f.stat(Stat.SPENT)
        assertEquals(Result.OK, f.buildHouse(x, y, WorkHouse.GROW))
        assertEquals(spent + WorkHouse.GROW.cost, f.stat(Stat.SPENT))
        assertEquals(Result.BLOCKED, f.buildHouse(x, y, WorkHouse.TOWER))
        assertTrue(f.houseCost(WorkHouse.GROW) > WorkHouse.GROW.cost)
        // Unstaffed it counts once; a worker doubles it.
        assertEquals(1, f.houseNear(x + 2, y, WorkHouse.GROW))
        assertEquals(0, f.houseNear(x + 3, y, WorkHouse.GROW))
        assertEquals(Result.OK, f.hireWorker())
        assertEquals(2, f.houseNear(x + 2, y, WorkHouse.GROW))
        // One bed until there's a bunkhouse.
        assertEquals(Result.LIMIT, f.hireWorker())
        val (bx, by) = grass()
        assertEquals(Result.OK, f.buildHouse(bx, by, WorkHouse.BUNK))
        assertEquals(3, f.workerCap())
        assertEquals(Result.OK, f.hireWorker())
        // Workers are kept with the save.
        val g = farm(); Save.read(Save.write(f), g)
        assertEquals(2, g.site.workers)
        assertEquals(2, g.houseNear(x + 2, y, WorkHouse.GROW))
    }

    @Test fun movingAGrowingPlantStuntsIt() {
        val f = farm()
        val x = f.ox + 3; val y = f.oy + 3
        val s = Strains.ladder.first()
        assertEquals(Result.OK, f.plant(x, y, s.id, t0))
        f.tile(x, y).progress = s.seconds * 0.5
        val stunt = f.stuntSeconds(x, y)
        assertTrue(stunt > 0)
        val tx = f.ox + 5; val ty = f.oy + 5
        assertEquals(Kind.GRASS, f.tile(tx, ty).kind)
        assertEquals(Result.OK, f.move(x, y, tx, ty))
        assertEquals(s.seconds * 0.5 - stunt, f.tile(tx, ty).progress, 1e-6)
        // Negative progress survives a save.
        f.tile(tx, ty).progress = -12.0
        val g = farm(); Save.read(Save.write(f), g)
        assertEquals(-12.0, g.tile(tx, ty).progress, 1e-6)
        assertEquals(Result.OK, f.uproot(tx, ty))
        assertNull(f.tile(tx, ty).strain)
        assertEquals(Kind.PLOT, f.tile(tx, ty).kind)
    }

    @Test fun holidayShopSpendsTokens() {
        Holiday.force = Holiday.HALLOWEEN
        try {
            val f = farm()
            val h = Holiday.HALLOWEEN
            val s = h.strains.first()
            assertFalse(f.unlocked(s))
            assertEquals(Result.NO_CASH, f.buyHolidayStrain(h, s))
            f.addTokens(h, 2000)
            assertEquals(Result.OK, f.buyHolidayStrain(h, s))
            assertTrue(f.unlocked(s))
            assertEquals(Result.OK, f.buyPet(h))
            assertEquals(Result.BLOCKED, f.buyPet(h))
            assertEquals(Result.OK, f.buyHolidayDecor(h, h.decor.first()))
            assertEquals(Result.BLOCKED, f.buySite(h.site))
            assertEquals(Result.OK, f.buyHolidayFarm(h))
            assertTrue(f.owns(h.site.id))
            val g = farm(); Save.read(Save.write(f), g)
            assertTrue(g.unlocked(s)); assertTrue(Pet.CAT.name in g.pets); assertTrue(g.owns(h.site.id))
            assertEquals(f.tokens(h), g.tokens(h))
        } finally { Holiday.force = null }
    }

    @Test fun holidaysFallOnTheirDays() {
        fun at(y: Int, m: Int, d: Int) = java.util.Calendar.getInstance().apply { clear(); set(y, m - 1, d, 12, 0) }.timeInMillis
        assertEquals(Holiday.HALLOWEEN, Holiday.at(at(2026, 10, 26)))
        assertEquals(Holiday.CHRISTMAS, Holiday.at(at(2026, 12, 25)))
        assertEquals(Holiday.NEWYEAR, Holiday.at(at(2027, 1, 1)))
        assertEquals(4 to 5, Holiday.easter(2026))
        assertEquals(26, Holiday.thanksgiving(2026))
        assertNull(Holiday.at(at(2026, 6, 10)))
    }

    @Test fun relocateMovesTheFarm() {
        val f = farm()
        val sx = f.ox; val sy = f.oy
        val shack = (0 until Farm.MAX * Farm.MAX).count { f.tiles[it].kind == Kind.SHACK || f.tiles[it].kind == Kind.PART }
        f.cash = f.relocateCost()
        assertEquals(Result.OK, f.relocate(20, 20))
        assertEquals(0L, f.cash)
        assertTrue(f.ox != sx || f.oy != sy)
        assertEquals(shack, (0 until Farm.MAX * Farm.MAX).count { f.tiles[it].kind == Kind.SHACK || f.tiles[it].kind == Kind.PART })
        assertEquals(Kind.PLOT, f.tile(f.ox + 3, f.oy + 3).kind)
        val g = farm(); Save.read(Save.write(f), g)
        assertEquals(f.ox, g.ox); assertEquals(f.oy, g.oy)
    }

    @Test fun restartWipesAndBucksBuy() {
        val f = farm()
        f.farmer = Farmer("Sage")
        f.potBucks = 30; f.cash = 99_999; f.level = 12
        assertEquals(Result.OK, f.buyWithBucks(BucksItem.JUICE, t0))
        assertEquals(25L, f.potBucks)
        assertTrue(f.on(Buff.JUICE))
        f.restart(t0)
        assertEquals(1, f.level); assertEquals(0L, f.potBucks); assertNull(f.farmer)
        assertEquals("Sage", f.restartLook?.name)
        assertEquals(Kind.PLOT, f.tile(f.ox + 3, f.oy + 3).kind)
        val look = Farmer("Willow", hair = 3, hat = 7)
        assertEquals(look, Farmer.decode(look.name, look.encode()))
    }

    @Test fun shortAmounts() {
        val cases = listOf(0L to "0", 999L to "999", 1000L to "1K", 1250L to "1.25K", 45_600L to "45.6K", 789_000L to "789K",
            999_600L to "1M", 1_230_000L to "1.23M", 12_300_000_000L to "12.3B", -2500L to "-2.5K")
        for ((n, s) in cases) assertEquals(s, dev.m4s4t0.kushacres.ui.short(n))
    }

    /** The first spot on the active farm whose 2×2 is all grass. */
    private fun freeSpot(f: Farm): Pair<Int, Int> {
        for (y in f.oy until f.oy + f.land - 1) for (x in f.ox until f.ox + f.land - 1)
            if (listOf(x to y, x + 1 to y, x to y + 1, x + 1 to y + 1).all { (a, b) -> f.tile(a, b).kind == Kind.GRASS }) return x to y
        error("no room")
    }

    @Test fun vehiclesParkOnTwoByTwoMoveAndSave() {
        val f = farm()
        f.cash = 100_000_000; f.level = 30
        val (x, y) = freeSpot(f)
        assertEquals(Result.OK, f.buyVehicle(x, y, Vehicle.TRUCK, t0))
        assertEquals(Result.LIMIT, f.buyVehicle(x, y, Vehicle.TRUCK, t0))
        assertEquals(Vehicle.TRUCK, f.vehicleOn(x + 1, y + 1))
        assertEquals(Result.BLOCKED, f.demolish(x + 1, y))
        val (nx, ny) = freeSpot(f)
        assertEquals(Result.OK, f.move(x + 1, y + 1, nx, ny))
        assertEquals(nx to ny, f.vehicleAt(Vehicle.TRUCK))
        assertEquals(4, f.tiles.count { it.kind == Kind.VEHICLE || it.kind == Kind.CAB })
        val g = Farm(Random(1))
        Save.read(Save.write(f), g)
        assertEquals(nx to ny, g.vehicleAt(Vehicle.TRUCK))
        // The truck sells goods by itself when it's due.
        f.goods["PRESS:ditch"] = 5
        val cash = f.cash
        f.tick(t0 + 6 * 60_000L)
        assertTrue(f.goods.isEmpty())
        assertTrue(f.cash > cash)
    }

    @Test fun rvAddsBedsAndCrewTakesThem() {
        val f = farm()
        f.cash = 100_000_000; f.level = 30
        val cap = f.workerCap()
        val (x, y) = freeSpot(f)
        assertEquals(Result.OK, f.buyVehicle(x, y, Vehicle.RV, t0))
        assertEquals(cap + 2, f.workerCap())
        assertEquals(Result.OK, f.hireCrew(Crew.DRIVER))
        assertEquals(Result.LIMIT, f.hireCrew(Crew.DRIVER))
        while (f.beds() < f.workerCap()) assertEquals(Result.OK, f.hireCrew(Crew.TRIMMER).takeIf { it == Result.OK } ?: f.hireWorker())
        assertEquals(Result.LIMIT, f.hireWorker())
        val g = Farm(Random(1))
        Save.read(Save.write(f), g)
        assertEquals(1, g.crew(Crew.DRIVER))
    }

    @Test fun seedHuntsCostWaitPayAndSave() {
        val f = farm()
        f.cash = 1_000_000_000
        assertEquals(Result.LOCKED, Hunt.send(f, Trip.HILLS, t0))
        f.level = 15
        val cash = f.cash
        assertEquals(Result.OK, Hunt.send(f, Trip.VALLEY, t0))
        assertEquals(cash - Hunt.cost(f, Trip.VALLEY), f.cash)
        assertEquals(Result.BUSY, Hunt.send(f, Trip.HILLS, t0))
        assertNull(Hunt.claim(f, t0 + 3_600_000L))
        val g = Farm(Random(1))
        Save.read(Save.write(f), g)
        assertEquals(Trip.VALLEY.name, g.hunt)
        assertEquals(f.huntBack, g.huntBack)
        val bucks = g.potBucks
        val l = Hunt.claim(g, t0 + 4 * 3_600_000L)!!
        assertTrue(l.seeds in 4..6)
        assertEquals(l.seeds, g.seeds[l.strain])
        assertTrue(l.strain == Hunt.common(g).strain || l.strain == Hunt.rareOf(g).strain)
        assertTrue(Strains[l.strain]!!.exotic)
        assertEquals(bucks + l.bucks, g.potBucks)
        assertEquals(1L, g.stat(Stat.HUNTS))
        assertFalse(Hunt.away(g))
        assertNull(Hunt.claim(g, t0 + 5 * 3_600_000L))
        assertEquals(Result.OK, Hunt.send(g, Trip.JUNGLE, t0))
    }

    @Test fun botanistsAndChemistSpeedUpTheirFarm() {
        val f = farm()
        f.cash = 1_000_000_000; f.level = 30
        assertEquals(Result.OK, f.buildFacility(4, 4, Facility.ROLLER))
        val (x, y) = freeSpot(f)
        assertEquals(Result.OK, f.buyVehicle(x, y, Vehicle.RV, t0))
        val before = f.speed(4, 4)
        assertEquals(Result.OK, f.hireCrew(Crew.BOTANIST))
        assertEquals(Result.OK, f.hireCrew(Crew.BOTANIST))
        assertEquals(Result.LIMIT, f.hireCrew(Crew.BOTANIST))
        assertEquals(before * 1.2, f.speed(4, 4), 1e-9)
        f.stash["ditch"] = 100; f.buySupply(Supply.PAPERS, 2)
        assertEquals(Result.OK, f.startBatch(4, 4, "ditch", t0))
        val plain = f.tile(4, 4).doneAt - t0
        f.collect(4, 4, t0 + plain)
        assertEquals(Result.OK, f.hireCrew(Crew.CHEMIST))
        assertEquals(Result.OK, f.startBatch(4, 4, "ditch", t0))
        assertEquals(plain / 1.3, (f.tile(4, 4).doneAt - t0).toDouble(), 2.0)
    }

    @Test fun farmGoalsAndGrownSaveAndAward() {
        val f = farm()
        f.site.harvests = 99
        f.checkTrophies(false)
        assertTrue("HARVEST_100" !in f.site.goals)
        f.site.harvests = 100
        f.grown += "ditch"
        val gems = f.gems
        f.checkTrophies(false)
        assertTrue("HARVEST_100" in f.site.goals)
        assertEquals(gems + 1, f.gems)
        assertTrue(f.events.any { it is FarmEvent.Goal })
        assertTrue(!FarmGoal.NATIVES.applies(f.site))
        val g = Farm(Random(1))
        Save.read(Save.write(f), g)
        assertEquals(100L, g.site.harvests)
        assertTrue("HARVEST_100" in g.site.goals && "ditch" in g.grown)
    }

    @Test fun cupWeeksStartMondayAndRivalsAreFixed() {
        val tz = java.util.TimeZone.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
        try {
            val day = 86_400_000L
            // 4 Jan 1970 was a Sunday, 5 Jan a Monday.
            assertEquals(Cup.week(3 * day + day - 1) + 1, Cup.week(4 * day))
            assertEquals(7 * day, Cup.endsIn(4 * day))
            assertEquals(Cup.week(4 * day), Cup.week(10 * day + day - 1))
        } finally { java.util.TimeZone.setDefault(tz) }
        val r = Cup.rivals(2900)
        assertEquals(r, Cup.rivals(2900))
        assertTrue(r[0].second > r[1].second && r[1].second > r[2].second)
        assertEquals(1, Cup.place(2900, r[0].second + 1))
        assertEquals(4, Cup.place(2900, r[2].second))
    }

    @Test fun cupEntriesPayOncePerBetterPlaceAndSave() {
        val f = farm()
        val s = Strains.ladder.first()
        f.stash[s.id] = Cup.grams(s) * 10
        assertEquals(Result.LOCKED, Cup.enter(f, s.id, t0))
        f.level = Cup.LEVEL
        val bucks = f.potBucks; val gems = f.gems
        repeat(Cup.ENTRIES) { assertEquals(Result.OK, Cup.enter(f, s.id, t0)) }
        assertEquals(Result.LIMIT, Cup.enter(f, s.id, t0))
        assertEquals(Cup.grams(s) * (10 - Cup.ENTRIES), f.stash[s.id])
        val wk = Cup.week(t0)
        val base = Cup.base(f, s, wk)
        assertTrue(f.cupScores.all { it.second in base..base + Cup.LUCK })
        // Prizes: one for each time the best placing improved.
        var best = 4; var wantBucks = 0L; var wantGems = 0L; var ribbons = 0L
        for ((_, sc) in f.cupScores) { val pl = minOf(4, Cup.place(wk, sc)); if (pl < best) { best = pl; wantBucks += Cup.prize(f, pl).bucks; wantGems += Cup.prize(f, pl).gems; ribbons++ } }
        assertEquals(bucks + wantBucks, f.potBucks)
        assertEquals(gems + wantGems, f.gems)
        assertEquals(ribbons, f.stat(Stat.PLACED))
        assertEquals(if (best == 1) 1L else 0L, f.stat(Stat.CUPS))
        assertEquals(best, Cup.best(f, t0))
        val g = Farm(Random(1))
        Save.read(Save.write(f), g)
        assertEquals(f.cupScores, g.cupScores)
        assertEquals(0, Cup.left(g, t0))
        // A new week clears the entries.
        assertEquals(Cup.ENTRIES, Cup.left(g, t0 + 7 * 86_400_000L))
        assertEquals(Result.OK, Cup.enter(g, s.id, t0 + 7 * 86_400_000L))
        assertEquals(1, g.cupScores.size)
    }

    @Test fun contractsOfferTakeShipAndPay() {
        val f = farm()
        f.level = 5
        assertTrue(Contracts.offers(f, "home", t0).isEmpty())
        f.level = 10
        f.offerDay.clear()
        val offers = Contracts.offers(f, "home", t0)
        // The daily offers and one event contract; no story yet (that needs a tier), no weekly order.
        assertEquals(Contracts.OFFERS + 1, offers.size)
        assertEquals(1, offers.count { it.event })
        assertTrue(Contracts.offers(f, "home", t0 + 60_000L) === offers)
        // Take the bud ones (Bulk and Rush are always among them) until hands are full.
        assertEquals(Result.OK, Contracts.take(f, "home", offers.indexOfFirst { !it.event && Strains[it.needs[0].what] != null }, t0))
        assertEquals(Result.OK, Contracts.take(f, "home", offers.indexOfFirst { !it.event && Strains[it.needs[0].what] != null }, t0))
        assertEquals(Result.LIMIT, Contracts.take(f, "home", offers.indexOfFirst { !it.event }, t0))
        val c = f.deals[0]
        val n = c.needs[0]
        assertFalse(Contracts.canShip(f, c))
        f.stash[n.what] = n.amount / 2
        Contracts.ship(f, c, t0)
        assertEquals(n.amount / 2, n.got)
        assertFalse(f.stash.containsKey(n.what))
        f.stash[n.what] = n.left + 5
        val cash = f.cash
        Contracts.ship(f, c, t0)
        assertEquals(5, f.stash[n.what])
        assertFalse(c in f.deals)
        assertTrue(f.cash - cash > c.value)
        assertEquals(c.kind.rep, Contracts.rep(f, "home"))
        assertEquals(1L, f.stat(Stat.CONTRACTS))
        // A new day brings new offers.
        assertFalse(Contracts.offers(f, "home", t0 + 86_400_000L) === offers)
    }

    @Test fun storefrontSellsLabBudAndSpeedsResearch() {
        val f = farm()
        f.level = 30; f.cash = 1_000_000_000
        val (x, y) = (f.oy until f.oy + f.land).flatMap { b -> (f.ox until f.ox + f.land).map { it to b } }.first { (a, b) -> f.tile(a, b).kind == Kind.GRASS }
        assertEquals(Result.OK, f.buildHouse(x, y, WorkHouse.STORE))
        assertEquals(1, f.tile(x, y).lvl)
        val slow = Storefronts.researchMillis(f, Strains.hybridList.first())
        // Nothing to sell: the shelf stays empty and no sales pile up for later.
        val hr = 3_600_000L
        f.tick(t0); f.tick(t0 + hr)
        assertTrue(f.tile(x, y).progress < 1.0)
        val h = Strains.hybridList.first()
        f.stash.clear(); f.stash[h.id] = 100; f.stash["ditch"] = 50
        val cash = f.cash
        f.tick(t0 + 2 * hr)
        assertEquals(80, f.stash[h.id])
        assertEquals(50, f.stash["ditch"])
        assertEquals(Math.round(20 * h.price * f.priceMult() * Storefronts.premium(1)).toDouble(), (f.cash - cash).toDouble(), 2.0)
        assertEquals(20, f.stat(Stat.STORE_GRAMS))
        // Growing it: each step needs the level, and the top is a flagship.
        f.level = 10
        assertEquals(Result.LOCKED, Storefronts.upgrade(f, x, y))
        f.level = 80
        repeat(4) { assertEquals(Result.OK, Storefronts.upgrade(f, x, y)) }
        assertEquals(Result.LIMIT, Storefronts.upgrade(f, x, y))
        assertEquals("Flagship Store", Storefronts.name(f.tile(x, y).lvl))
        assertTrue(Storefronts.researchMillis(f, Strains.hybridList.first()) < slow)
        assertEquals(1.5, Storefronts.researchSpeed(f), 1e-9)
        val g = Farm(Random(1))
        Save.read(Save.write(f), g)
        assertEquals(5, g.site.tiles.first { Storefronts.isStore(it) }.lvl)
        assertEquals(Save.write(f), Save.write(g))
    }

    @Test fun contractsLapseDropAndSave() {
        val f = farm()
        f.level = 10
        Contracts.offers(f, "home", t0)
        f.rep["home"] = 20
        Contracts.take(f, "home", 0, t0)
        Contracts.take(f, "home", 0, t0)
        assertTrue(f.deals.none { it.event })
        val g = Farm(Random(1))
        Save.read(Save.write(f), g)
        assertEquals(2, g.deals.size)
        assertEquals(f.deals[0].until, g.deals[0].until)
        assertEquals(f.deals[0].needs.map { it.what }, g.deals[0].needs.map { it.what })
        assertEquals(2, g.offers["home"]!!.size)
        assertEquals(20, Contracts.rep(g, "home"))
        // Dropping costs half what running out does; neither goes below nothing.
        Contracts.drop(g, g.deals[0])
        assertEquals(20 - Contracts.LAPSE_REP / 2, Contracts.rep(g, "home"))
        val c = g.deals[0]
        c.value = 1000.0
        val cash = g.cash
        Contracts.expire(g, c.until - 1)
        assertEquals(1, g.deals.size)
        Contracts.expire(g, c.until)
        assertTrue(g.deals.isEmpty())
        assertEquals(cash + 1000, g.cash)
        assertEquals(0, Contracts.rep(g, "home"))
        assertEquals(1, g.lapsed)
    }

    @Test fun storyChainsWeeklyOrdersAndEvents() {
        val f = farm()
        f.level = 30
        fun fill(c: Contract) {
            for (n in c.needs) when {
                n.what.startsWith("lab:") -> f.stash[Strains.hybridList.first().id] = n.amount
                n.what.startsWith("class:") -> f.stash[Contracts.fitting(f, CupClass.valueOf(n.what.removePrefix("class:"))).first().id] = n.amount
                n.what.startsWith("goods:") -> f.goods[n.what.removePrefix("goods:") + ":ditch"] = n.amount
                else -> f.stash[n.what] = n.amount
            }
            Contracts.ship(f, c, t0)
            assertFalse(c in f.deals)
        }
        f.bred += Strains.hybridList.first().id
        // A stranger gets no story; a known face gets chapter one, which doesn't take a hand.
        assertTrue(Contracts.offers(f, "home", t0).none { it.chapter >= 0 })
        f.rep["home"] = Contracts.TIERS[1]
        val offers = Contracts.offers(f, "home", t0)
        assertEquals(0, offers[0].chapter)
        assertEquals(Result.OK, Contracts.take(f, "home", 0, t0))
        assertEquals(0, Contracts.held(f))
        // The next chapter waits for the next tier.
        fill(f.deals.single())
        assertEquals(1, Chains.done(f, "home"))
        assertTrue(Contracts.offers(f, "home", t0).none { it.chapter >= 0 })
        val sell = f.priceMult()
        for (i in 1 until Chains.STEPS) {
            f.rep["home"] = Contracts.TIERS[Chains.tierFor(i)]
            val k = Contracts.offers(f, "home", t0).indexOfFirst { it.chapter == i }
            assertEquals(Result.OK, Contracts.take(f, "home", k, t0))
            fill(f.deals.single { it.chapter == i })
        }
        assertEquals(Chains.STEPS, Chains.done(f, "home"))
        assertTrue(f.events.any { it is FarmEvent.Keepsake })
        assertEquals(sell * (1 + Keepsake.AMT), f.priceMult(), 1e-9)
        assertTrue(Contracts.offers(f, "home", t0).none { it.chapter >= 0 })
        assertEquals(Chains.STEPS.toLong(), f.stat(Stat.CHAPTERS))
        // Regulars get a weekly order, once a week, due when the week ends.
        f.rep["home"] = Contracts.TIERS[Contracts.WEEKLY_TIER]
        val day = 86_400_000L
        val w = Contracts.offers(f, "home", t0 + day).indexOfFirst { it.weekly }
        assertTrue(w >= 0)
        assertEquals(Result.OK, Contracts.take(f, "home", w, t0 + day))
        val weekly = f.deals.single { it.weekly }
        assertTrue(weekly.until > t0 + day && weekly.until <= t0 + day + 7 * day)
        assertEquals(0L, localDay(weekly.until) % 7)
        val nextDay = if (localDay(t0 + 2 * day) / 7 == localDay(t0 + day) / 7) t0 + 2 * day else t0 + day
        assertTrue(Contracts.offers(f, "home", nextDay).none { it.weekly })
        // Holiday events pay that holiday's tokens; only one event at a time.
        Holiday.force = Holiday.HALLOWEEN
        try {
            val e = Contracts.offers(f, "home", t0 + 9 * day).indexOfFirst { it.event }
            assertEquals(Holiday.HALLOWEEN, f.offers["home"]!![e].holiday)
            assertEquals(Result.OK, Contracts.take(f, "home", e, t0 + 9 * day))
            val ev = f.deals.single { it.event }
            assertFalse(Contracts.canTake(f, Contract(Deal.RUSH, "x", "home", emptyList(), 1.0, "event:SPRING")))
            fill(ev)
            assertTrue(f.tokens(Holiday.HALLOWEEN) > 0)
        } finally { Holiday.force = null }
        // It all saves.
        val g = Farm(Random(1))
        Save.read(Save.write(f), g)
        assertEquals(Chains.STEPS, Chains.done(g, "home"))
        assertTrue(g.deals.single().weekly)
        assertEquals(Save.write(f), Save.write(g))
    }
}
