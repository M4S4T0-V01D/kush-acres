package dev.m4s4t0.kushacres.model

/** The player's own farmer: a name and a look, picked when a game starts and restyled later for a fee. */
data class Farmer(
    val name: String = "Farmer",
    val skin: Int = 1,
    val hair: Int = 0,
    val hairColor: Int = 1,
    val outfit: Int = 1,
    val shirt: Int = 0,
    val pants: Int = 0,
    val hat: Int = 2,
    val face: Int = 0,
    // Everything below came later; saves from before load with these defaults.
    val body: Int = 2,
    val eyes: Int = 0,
    val eyeColor: Int = 0,
    val brows: Int = 0,
    val lashes: Int = 0,
    val lips: Int = 0,
    val blush: Int = 1,
    val freckles: Int = 0,
    val mole: Int = 0,
    val shadow: Int = 0,
    val bottom: Int = 0,
    val shoes: Int = 0,
    val shoeColor: Int = 6,
    /** One bit per [EXTRAS] entry. */
    val extras: Int = 0,
) {
    fun encode() = listOf(skin, hair, hairColor, outfit, shirt, pants, hat, face,
        body, eyes, eyeColor, brows, lashes, lips, blush, freckles, mole, shadow, bottom, shoes, shoeColor, extras).joinToString(",")

    fun has(extra: Int) = extras and (1 shl extra) != 0
    fun toggle(extra: Int) = copy(extras = extras xor (1 shl extra))

    companion object {
        // Lists only ever grow at the end, so saved indices keep meaning the same thing.
        val SKINS = intArrayOf(0xFFFFE0C4.toInt(), 0xFFF0C090.toInt(), 0xFFD89A68.toInt(), 0xFFB07448.toInt(), 0xFF8A5432.toInt(), 0xFF5E3820.toInt(),
            0xFFFFEEE2.toInt(), 0xFFE8B888.toInt(), 0xFFC68A5A.toInt(), 0xFF46281A.toInt())
        val HAIR_COLORS = intArrayOf(0xFF1E1610.toInt(), 0xFF5A3A20.toInt(), 0xFFA0602A.toInt(), 0xFFE8C860.toInt(), 0xFFC0402A.toInt(),
            0xFFB8B8B8.toInt(), 0xFF7A3AE0.toInt(), 0xFF3AC07A.toInt(), 0xFFFF6AA0.toInt(),
            0xFFF6ECCE.toInt(), 0xFF8A2E1E.toInt(), 0xFFE8946A.toInt(), 0xFF3A6AE0.toInt(), 0xFF2AB0B0.toInt(), 0xFFC4A6F2.toInt(), 0xFF3A2A4A.toInt())
        val CLOTH = intArrayOf(0xFFC03A3A.toInt(), 0xFF3A6AC0.toInt(), 0xFF3A9A4A.toInt(), 0xFFE0B03A.toInt(), 0xFF8A4AC0.toInt(),
            0xFFE07A2A.toInt(), 0xFF2A2A30.toInt(), 0xFFE8E4DC.toInt(), 0xFF6A4A30.toInt(), 0xFF2AA0A0.toInt(), 0xFFFF7AB0.toInt(), 0xFF6A7A3A.toInt(),
            0xFFF4A8C0.toInt(), 0xFFA8D4F2.toInt(), 0xFFB8E8A8.toInt(), 0xFFF4E2A0.toInt(), 0xFFC6A8E8.toInt(), 0xFF8A1E3A.toInt(),
            0xFF1E2A5A.toInt(), 0xFF2E5230.toInt(), 0xFF9A9AA4.toInt(), 0xFFE2C4A0.toInt(), 0xFFFF6A4A.toInt(), 0xFF4ACCBC.toInt())
        val HAIRS = listOf("Short", "Long", "Bun", "Mohawk", "Afro", "Bald", "Ponytail", "Dreads",
            "Pixie", "Bob", "Wavy", "Pigtails", "Braids", "Space buns", "Curls", "Side swept", "Undercut", "Mullet", "Top knot", "Bangs",
            "Beehive", "Buzz", "Mermaid", "Shag")
        val OUTFITS = listOf("Tee", "Overalls", "Hoodie", "Flannel", "Tie-dye", "Dress",
            "Tank top", "Crop top", "Sundress", "Blouse", "Sweater", "Denim jacket", "Leather jacket", "Kimono", "Polo", "Lab coat",
            "Halter", "Peasant top", "Cardigan", "Jersey", "Puffer", "Off-shoulder")
        /** Outfits that cover the legs themselves, so the bottoms don't show. */
        val DRESSES = setOf("Dress", "Sundress")
        val BOTTOMS = listOf("Jeans", "Shorts", "Skirt", "Mini skirt", "Cargo pants", "Leggings")
        val SHOES = listOf("Boots", "Sneakers", "Sandals", "Cowboy boots", "Wellies")
        /** Hats sold in each holiday's shop, in [Holiday] order; worn only once bought. */
        val HOLIDAY_HATS = listOf("Party hat", "Heart band", "Leprechaun", "Leaf crown", "Bunny ears", "Uncle Sam", "Pumpkin", "Pilgrim", "Santa")
        /** Free hats added after the holiday ones, so the holiday ones keep their saved indices. */
        val MORE_HATS = listOf("Sun hat", "Cowgirl", "Beret", "Headscarf", "Visor")
        val HATS = listOf("None", "Cap", "Straw hat", "Beanie", "Bandana", "Cowboy", "Bucket", "Witch") + HOLIDAY_HATS + MORE_HATS
        val BASE_HATS = HATS.size - HOLIDAY_HATS.size - MORE_HATS.size
        /** Hats anyone can wear without buying them. */
        fun freeHat(i: Int) = i < BASE_HATS || i >= BASE_HATS + HOLIDAY_HATS.size
        val FACES = listOf("Plain", "Beard", "Moustache", "Glasses", "Shades", "Round specs", "Heart shades", "Goatee", "Stubble")
        val BODIES = listOf("Masculine", "Feminine", "Neutral")
        val EYES = listOf("Round", "Almond", "Big", "Sleepy", "Cat-eye")
        val EYE_COLORS = intArrayOf(0xFF5A3A22.toInt(), 0xFF2A1A14.toInt(), 0xFF8A6A2A.toInt(), 0xFF3A8A4A.toInt(), 0xFF3A7AD0.toInt(),
            0xFF7A8A9A.toInt(), 0xFF8A4AC0.toInt(), 0xFFD09A2A.toInt())
        val BROWS = listOf("Soft", "Thin", "Bold", "Arched")
        /** Lip colours; 0 is the skin's own. */
        val LIPS = intArrayOf(0, 0xFFE87A9A.toInt(), 0xFFD02A3A.toInt(), 0xFF9A2A4A.toInt(), 0xFFF2866A.toInt(), 0xFFC8907A.toInt(), 0xFF6A2A4A.toInt(), 0xFF2A1A20.toInt())
        /** Eyeshadow colours; 0 is none. */
        val SHADOWS = intArrayOf(0, 0xFFB088E0.toInt(), 0xFF4AB8C0.toInt(), 0xFFE0B04A.toInt(), 0xFFF08AB0.toInt(), 0xFF4A4050.toInt(), 0xFF6AC07A.toInt())
        val EXTRAS = listOf("Earrings", "Flower", "Flower crown", "Necklace", "Nose ring", "Bracelets", "Tattoos", "Gloves")
        val NAMES = listOf("Mary Jane", "Buddy", "Sage", "Willow", "Rusty", "Juniper", "Clay", "Indica", "Skye", "Dale", "Fern", "Roach")

        /** Restyling costs this much cash once the game has begun (or [RESTYLE_BUCKS] Pot Bucks). */
        fun restyleCost(level: Int) = nice(500.0 + 150.0 * Levels.tier(level))
        const val RESTYLE_BUCKS = 5L
        const val NAME_MAX = 18

        fun decode(name: String, s: String?): Farmer {
            val v = s?.split(",")?.mapNotNull { it.toIntOrNull() } ?: return Farmer(name)
            if (v.size < 8) return Farmer(name)
            val d = Farmer()
            fun at(i: Int, n: Int, def: Int) = v.getOrElse(i) { def }.mod(n)
            return Farmer(name, v[0].mod(SKINS.size), v[1].mod(HAIRS.size), v[2].mod(HAIR_COLORS.size), v[3].mod(OUTFITS.size),
                v[4].mod(CLOTH.size), v[5].mod(CLOTH.size), v[6].mod(HATS.size), v[7].mod(FACES.size),
                at(8, BODIES.size, d.body), at(9, EYES.size, d.eyes), at(10, EYE_COLORS.size, d.eyeColor), at(11, BROWS.size, d.brows),
                at(12, 2, d.lashes), at(13, LIPS.size, d.lips), at(14, 2, d.blush), at(15, 2, d.freckles), at(16, 2, d.mole),
                at(17, SHADOWS.size, d.shadow), at(18, BOTTOMS.size, d.bottom), at(19, SHOES.size, d.shoes), at(20, CLOTH.size, d.shoeColor),
                v.getOrElse(21) { 0 } and ((1 shl EXTRAS.size) - 1))
        }

        private fun look(
            body: Int, skin: Int, hair: String, hairColor: Int, outfit: String, shirt: Int, bottom: String, pants: Int, hat: String,
            shoes: String, shoeColor: Int, face: String = "Plain", eyes: String = "Round", eyeColor: Int = 0, lips: Int = 0, shadow: Int = 0,
            freckles: Int = 0, brows: String = "Soft", extras: List<String> = emptyList(),
        ) = Farmer(
            "Farmer", skin, HAIRS.indexOf(hair), hairColor, OUTFITS.indexOf(outfit), shirt, pants, HATS.indexOf(hat), FACES.indexOf(face),
            body = body, eyes = EYES.indexOf(eyes), eyeColor = eyeColor, brows = BROWS.indexOf(brows), lashes = if (body == 1) 1 else 0,
            lips = lips, freckles = freckles, shadow = shadow, bottom = BOTTOMS.indexOf(bottom), shoes = SHOES.indexOf(shoes), shoeColor = shoeColor,
            extras = extras.fold(0) { m, e -> m or (1 shl EXTRAS.indexOf(e)) },
        )

        /** Ready-made looks to start from in the creator. */
        val PRESETS: List<Pair<String, Farmer>> by lazy {
            listOf(
                "Mary Jane" to look(1, 1, "Wavy", 4, "Sundress", 14, "Skirt", 2, "Sun hat", "Sandals", 8, eyes = "Almond", eyeColor = 3, lips = 1, freckles = 1, extras = listOf("Flower", "Earrings")),
                "Willow" to look(1, 6, "Mermaid", 14, "Peasant top", 7, "Skirt", 16, "None", "Sandals", 8, eyes = "Big", eyeColor = 4, lips = 4, shadow = 1, extras = listOf("Flower crown", "Necklace", "Bracelets")),
                "Daisy" to look(1, 3, "Pigtails", 3, "Crop top", 12, "Shorts", 1, "None", "Cowboy boots", 8, eyes = "Cat-eye", eyeColor = 4, lips = 2, brows = "Arched", extras = listOf("Earrings")),
                "Skye" to look(1, 5, "Curls", 0, "Off-shoulder", 23, "Leggings", 6, "None", "Sneakers", 7, eyes = "Almond", eyeColor = 2, lips = 3, shadow = 3, extras = listOf("Earrings", "Nose ring")),
                "Cowgirl" to look(1, 2, "Braids", 2, "Flannel", 0, "Jeans", 18, "Cowgirl", "Cowboy boots", 8, eyeColor = 3, lips = 4, freckles = 1, extras = listOf("Earrings")),
                "Hippie" to look(2, 7, "Long", 1, "Tie-dye", 3, "Jeans", 1, "Headscarf", "Sandals", 8, face = "Round specs", eyes = "Sleepy", extras = listOf("Necklace", "Bracelets")),
                "Grower" to look(0, 3, "Short", 1, "Overalls", 2, "Jeans", 1, "Straw hat", "Wellies", 2, face = "Beard", brows = "Bold", extras = listOf("Gloves")),
                "Punk" to look(2, 0, "Mohawk", 8, "Leather jacket", 0, "Leggings", 6, "None", "Boots", 6, face = "Shades", lips = 7, shadow = 5, extras = listOf("Earrings", "Nose ring", "Tattoos")),
            )
        }

        private val FREE_HATS get() = HATS.indices.filter { freeHat(it) && HATS[it] != "Witch" }

        /** A look picked at random; [body] keeps one frame (and gives it fitting styles), or null for any. */
        fun random(rnd: kotlin.random.Random = kotlin.random.Random, body: Int? = null): Farmer {
            val b = body ?: rnd.nextInt(BODIES.size)
            val fem = b == 1
            val femHair = listOf(1, 2, 6, 9, 10, 11, 12, 13, 14, 15, 18, 19, 20, 22, 23)
            val face = if (fem) listOf(0, 0, 0, 3, 4, 5, 6).random(rnd) else rnd.nextInt(FACES.size)
            return Farmer(
                NAMES.random(rnd), rnd.nextInt(SKINS.size), if (fem) femHair.random(rnd) else rnd.nextInt(HAIRS.size), rnd.nextInt(HAIR_COLORS.size),
                rnd.nextInt(OUTFITS.size), rnd.nextInt(CLOTH.size), rnd.nextInt(CLOTH.size), if (rnd.nextInt(3) == 0) 0 else FREE_HATS.random(rnd), face,
                body = b, eyes = rnd.nextInt(EYES.size), eyeColor = rnd.nextInt(EYE_COLORS.size), brows = rnd.nextInt(BROWS.size),
                lashes = if (fem) 1 else 0, lips = if (fem) rnd.nextInt(LIPS.size) else 0, blush = rnd.nextInt(2), freckles = if (rnd.nextInt(4) == 0) 1 else 0,
                mole = if (rnd.nextInt(6) == 0) 1 else 0, shadow = if (fem && rnd.nextBoolean()) rnd.nextInt(SHADOWS.size) else 0,
                bottom = if (fem) rnd.nextInt(BOTTOMS.size) else listOf(0, 1, 4).random(rnd), shoes = rnd.nextInt(SHOES.size),
                shoeColor = rnd.nextInt(CLOTH.size), extras = rnd.nextInt(1 shl EXTRAS.size) and rnd.nextInt(1 shl EXTRAS.size) and rnd.nextInt(1 shl EXTRAS.size),
            )
        }
    }
}
