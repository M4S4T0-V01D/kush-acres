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
) {
    fun encode() = listOf(skin, hair, hairColor, outfit, shirt, pants, hat, face).joinToString(",")

    companion object {
        val SKINS = intArrayOf(0xFFFFE0C4.toInt(), 0xFFF0C090.toInt(), 0xFFD89A68.toInt(), 0xFFB07448.toInt(), 0xFF8A5432.toInt(), 0xFF5E3820.toInt())
        val HAIR_COLORS = intArrayOf(0xFF1E1610.toInt(), 0xFF5A3A20.toInt(), 0xFFA0602A.toInt(), 0xFFE8C860.toInt(), 0xFFC0402A.toInt(),
            0xFFB8B8B8.toInt(), 0xFF7A3AE0.toInt(), 0xFF3AC07A.toInt(), 0xFFFF6AA0.toInt())
        val CLOTH = intArrayOf(0xFFC03A3A.toInt(), 0xFF3A6AC0.toInt(), 0xFF3A9A4A.toInt(), 0xFFE0B03A.toInt(), 0xFF8A4AC0.toInt(),
            0xFFE07A2A.toInt(), 0xFF2A2A30.toInt(), 0xFFE8E4DC.toInt(), 0xFF6A4A30.toInt(), 0xFF2AA0A0.toInt(), 0xFFFF7AB0.toInt(), 0xFF6A7A3A.toInt())
        val HAIRS = listOf("Short", "Long", "Bun", "Mohawk", "Afro", "Bald", "Ponytail", "Dreads")
        val OUTFITS = listOf("Tee", "Overalls", "Hoodie", "Flannel", "Tie-dye", "Dress")
        val HATS = listOf("None", "Cap", "Straw hat", "Beanie", "Bandana", "Cowboy", "Bucket", "Witch")
        val FACES = listOf("Plain", "Beard", "Moustache", "Glasses", "Shades")
        val NAMES = listOf("Mary Jane", "Buddy", "Sage", "Willow", "Rusty", "Juniper", "Clay", "Indica", "Skye", "Dale", "Fern", "Roach")

        /** Restyling costs this much cash once the game has begun (or [RESTYLE_BUCKS] Pot Bucks). */
        fun restyleCost(level: Int) = nice(500.0 + 150.0 * level)
        const val RESTYLE_BUCKS = 5L
        const val NAME_MAX = 18

        fun decode(name: String, s: String?): Farmer {
            val v = s?.split(",")?.mapNotNull { it.toIntOrNull() } ?: return Farmer(name)
            if (v.size < 8) return Farmer(name)
            return Farmer(name, v[0].mod(SKINS.size), v[1].mod(HAIRS.size), v[2].mod(HAIR_COLORS.size), v[3].mod(OUTFITS.size),
                v[4].mod(CLOTH.size), v[5].mod(CLOTH.size), v[6].mod(HATS.size), v[7].mod(FACES.size))
        }

        fun random(rnd: kotlin.random.Random = kotlin.random.Random) = Farmer(
            NAMES.random(rnd), rnd.nextInt(SKINS.size), rnd.nextInt(HAIRS.size), rnd.nextInt(HAIR_COLORS.size), rnd.nextInt(OUTFITS.size),
            rnd.nextInt(CLOTH.size), rnd.nextInt(CLOTH.size), rnd.nextInt(HATS.size - 1), rnd.nextInt(FACES.size))
    }
}
