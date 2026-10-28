package dev.m4s4t0.kushacres.model

import java.util.Calendar

/** A pet that wanders the farm; bought once from its holiday's shop and kept for good. */
enum class Pet(val title: String, val holiday: String, val body: Int, val belly: Int, val bird: Boolean = false) {
    CAT("Black cat", "halloween", 0xFF1A1420.toInt(), 0xFF2E2434.toInt()),
    REINDEER("Reindeer", "christmas", 0xFF8A5A34.toInt(), 0xFFD8B48A.toInt()),
    PUG("Party pug", "newyear", 0xFFD8B080.toInt(), 0xFF3A2A20.toInt()),
    POODLE("Pink poodle", "valentine", 0xFFFFA8D0.toInt(), 0xFFFFE0F0.toInt()),
    FOX("Lucky fox", "stpatrick", 0xFFE0702A.toInt(), 0xFFF4E6D0.toInt()),
    SLOTH("Chill sloth", "fourtwenty", 0xFF8A7A5A.toInt(), 0xFFC4B490.toInt()),
    BUNNY("Bunny", "easter", 0xFFF4F0F4.toInt(), 0xFFFFC0D8.toInt()),
    EAGLE("Bald eagle", "july4", 0xFF4A3220.toInt(), 0xFFF4F4F0.toInt(), bird = true),
    TURKEY("Turkey", "thanksgiving", 0xFF6A4228.toInt(), 0xFFC03A2A.toInt(), bird = true);
}

/**
 * The main holidays. While one is on, sales, orders, dailies, challenges and some events pay out its token,
 * and the holiday shop sells its farm, strains, decor and pet for them. Everything bought is kept for good.
 */
enum class Holiday(
    val id: String, val title: String, val token: String, val tokens: String,
    val main: Int, val accent: Int, val greeting: String,
) {
    NEWYEAR("newyear", "New Year's", "confetti", "Confetti", 0xFFFFD23A.toInt(), 0xFF3A3AA0.toInt(), "Happy New Year! Pop some bottles."),
    VALENTINE("valentine", "Valentine's", "heart", "Hearts", 0xFFFF4A7A.toInt(), 0xFFFFC0D8.toInt(), "Love is in the air. So is smoke."),
    STPATRICK("stpatrick", "St Patrick's", "clover", "Clovers", 0xFF3AC04A.toInt(), 0xFFFFD23A.toInt(), "Top o' the mornin'. Find the gold."),
    FOURTWENTY("fourtwenty", "4/20", "nug", "Nugs", 0xFF7AE03A.toInt(), 0xFF8A4AE0.toInt(), "It's the high holiday. Blaze up."),
    EASTER("easter", "Easter", "egg", "Eggs", 0xFFC0A0FF.toInt(), 0xFFFFE07A.toInt(), "Eggs are hidden all over the county."),
    JULY4("july4", "Fourth of July", "spark", "Sparks", 0xFFE03A3A.toInt(), 0xFF3A6AE0.toInt(), "Fireworks tonight. Freedom grows here."),
    HALLOWEEN("halloween", "Halloween", "candy corn", "Candy corn", 0xFFFF8A1E.toInt(), 0xFFA43AFF.toInt(), "Spooky season! Trick or treat."),
    THANKSGIVING("thanksgiving", "Thanksgiving", "acorn", "Acorns", 0xFFE0903A.toInt(), 0xFF8A4A20.toInt(), "Give thanks and pass the gravy."),
    CHRISTMAS("christmas", "Christmas", "candy cane", "Candy canes", 0xFFE03A3A.toInt(), 0xFF3AA04A.toInt(), "Ho ho ho! The elves are on shift.");

    val strains get() = Strains.holidays.filter { it.holiday == id }
    val decor get() = Decor.entries.filter { it.holiday == id }
    val pet get() = Pet.entries.first { it.holiday == id }
    val site get() = sites.first()
    /** Its farms: the first, then the dearer, bigger estate. */
    val sites get() = Sites.holidays.filter { it.holiday == id }
    /** The hat sold in its shop for the farmer to wear. */
    val hat get() = Farmer.HOLIDAY_HATS[ordinal]

    /** Whether the holiday is on at [now] (epoch ms, local calendar). */
    fun active(now: Long): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = now }
        val y = c.get(Calendar.YEAR)
        val doy = c.get(Calendar.DAY_OF_YEAR)
        fun day(m: Int, d: Int) = Calendar.getInstance().apply { clear(); set(y, m - 1, d) }.get(Calendar.DAY_OF_YEAR)
        fun around(m: Int, d: Int, before: Int, after: Int) = day(m, d).let { doy in it - before..it + after }
        return when (this) {
            NEWYEAR -> doy <= day(1, 3) || doy >= day(12, 30)
            VALENTINE -> around(2, 14, 4, 1)
            STPATRICK -> around(3, 17, 3, 1)
            FOURTWENTY -> around(4, 20, 3, 1)
            EASTER -> easter(y).let { (m, d) -> around(m, d, 4, 1) }
            JULY4 -> around(7, 4, 3, 1)
            HALLOWEEN -> doy in day(10, 20)..day(11, 1)
            THANKSGIVING -> around(11, thanksgiving(y), 3, 3)
            CHRISTMAS -> around(12, 25, 7, 1)
        }
    }

    companion object {
        /** Set by tests (and nothing else) to pretend a holiday is on. */
        var force: Holiday? = null
        private val byId = entries.associateBy { it.id }
        operator fun get(id: String?) = id?.let { byId[it] }

        /** The holiday on at [now], if any. */
        fun at(now: Long): Holiday? = force ?: entries.firstOrNull { it.active(now) }

        /** Easter Sunday of year [y] as (month, day), by the anonymous Gregorian algorithm. */
        fun easter(y: Int): Pair<Int, Int> {
            val a = y % 19; val b = y / 100; val c = y % 100; val d = b / 4; val e = b % 4
            val f = (b + 8) / 25; val g = (b - f + 1) / 3; val h = (19 * a + b - d - g + 15) % 30
            val i = c / 4; val k = c % 4; val l = (32 + 2 * e + 2 * i - h - k) % 7
            val m = (a + 11 * h + 22 * l) / 451
            val month = (h + l - 7 * m + 114) / 31; val day = (h + l - 7 * m + 114) % 31 + 1
            return month to day
        }

        /** Day of November that's the fourth Thursday in year [y]. */
        fun thanksgiving(y: Int): Int {
            val c = Calendar.getInstance().apply { clear(); set(y, Calendar.NOVEMBER, 1) }
            val first = (Calendar.THURSDAY - c.get(Calendar.DAY_OF_WEEK) + 7) % 7 + 1
            return first + 21
        }

        // Shop prices in tokens.
        const val FARM = 500L
        const val QUICK = 100L
        const val LONG = 200L
        const val DECOR = 70L
        const val PET = 150L
        const val ESTATE = 1200L
        const val HAT = 120L
    }
}
