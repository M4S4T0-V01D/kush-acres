package dev.m4s4t0.kushacres.art

import android.graphics.Bitmap
import dev.m4s4t0.kushacres.model.Farmer
import dev.m4s4t0.kushacres.model.Helper
import dev.m4s4t0.kushacres.model.Pet
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Shopkeepers in town. */
enum class Keeper { VIOLET, DEX, HANK, MONA, GUS, SHADE }

/** Little walking helpers on the farm and big shopkeeper portraits in town. */
object People {
    private val OUT = Sprites.OUT

    // ---- Little people on the farm ----

    const val P_W = 14
    const val P_H = 26
    /** Feet. */
    const val P_AX = 7
    const val P_AY = 25

    private val personCache = HashMap<String, Bitmap>()

    /** Each helper as a [Farmer] look, plus a prop or two drawn over the top. */
    private val helperLooks = mapOf(
        Helper.RUNNER to Farmer("", 1, 0, 1, 0, 2, 6, 1, 0),
        Helper.FETCHER to Farmer("", 4, 1, 0, 0, 1, 8, 0, 0),
        Helper.FARMHAND to Farmer("", 1, 0, 2, 1, 0, 1, 2, 1),
        Helper.COOK to Farmer("", 2, 0, 0, 0, 7, 6, 0, 2),
        Helper.LOOKOUT to Farmer("", 3, 0, 0, 2, 6, 6, 0, 4),
        Helper.MANAGER to Farmer("", 5, 4, 3, 0, 4, 6, 0, 3),
    )

    fun helper(h: Helper, frame: Int): Bitmap = personCache.getOrPut("h${h.ordinal}.$frame") {
        val px = figurePx(helperLooks.getValue(h), frame, outline = false)
        when (h) {
            // A backpack strap and a bag.
            Helper.RUNNER -> { px.rect(4, 12, 1, 6, 0xFF7A4E2A.toInt()); px.rect(0, 15, 2, 4, 0xFF7A4E2A.toInt()); px.set(0, 15, 0xFFA87A4A.toInt()) }
            // A cardboard box in both arms.
            Helper.FETCHER -> { px.rect(3, 15, 8, 4, 0xFFC89A62.toInt()); px.rect(3, 16, 8, 1, 0xFFA87A4A.toInt()); px.rect(6, 15, 2, 1, 0xFFE8D0A0.toInt()) }
            // Chef's toque and an apron.
            Helper.COOK -> {
                px.rect(4, 1, 6, 4, 0xFFF4F4F0.toInt()); px.rect(3, 0, 8, 2, 0xFFF4F4F0.toInt()); px.rect(4, 4, 6, 1, 0xFFD8D8D0.toInt())
                px.rect(4, 15, 6, 5, 0xFFE8E0D0.toInt()); px.rect(4, 16, 6, 1, 0xFFC8C0B0.toInt())
            }
            // A hood up over the shades.
            Helper.LOOKOUT -> { px.rect(3, 3, 8, 3, 0xFF2A2A34.toInt()); px.rect(3, 5, 1, 7, 0xFF2A2A34.toInt()); px.rect(10, 5, 1, 7, 0xFF2A2A34.toInt()) }
            // A clipboard.
            Helper.MANAGER -> { px.rect(10, 14, 3, 4, 0xFFC89A62.toInt()); px.rect(11, 15, 1, 2, 0xFFF4F4F0.toInt()); px.set(11, 13, 0xFF9AA0A8.toInt()) }
            Helper.FARMHAND -> {}
        }
        px.outline(OUT)
        px.bitmap()
    }

    /** Hired farm workers: hi-vis vests and hard hats, [v] picks the look. */
    fun worker(v: Int, frame: Int): Bitmap = personCache.getOrPut("w${Math.floorMod(v, 6)}.$frame") {
        val i = Math.floorMod(v, 6)
        val px = figurePx(Farmer("", intArrayOf(2, 4, 1, 3, 0, 5)[i], 0, i % 4, 0, intArrayOf(1, 6, 4, 9, 0, 11)[i], 1, 0, if (i == 3) 1 else 0), frame, outline = false)
        val vest = if (i % 2 == 0) 0xFFF08A2A.toInt() else 0xFFD8F03A.toInt()
        val hat = if (i < 3) 0xFFF2C440.toInt() else 0xFFF4F4F0.toInt()
        px.rect(3, 13, 3, 6, vest); px.rect(8, 13, 3, 6, vest); px.rect(3, 16, 8, 1, 0xFFE8ECF0.toInt())
        px.rect(4, 3, 6, 3, hat); px.rect(5, 2, 4, 1, hat); px.rect(3, 6, 8, 1, shade(hat, 0.8f)); px.set(5, 3, shade(hat, 1.2f))
        px.outline(OUT)
        px.bitmap()
    }

    /** Festival hippies: tie-dye, long hair and bandanas, [v] picks the look. */
    fun hippie(v: Int, frame: Int): Bitmap = personCache.getOrPut("p${Math.floorMod(v, 12)}.$frame") {
        val i = Math.floorMod(v, 12)
        val look = Farmer("", i % Farmer.SKINS.size, intArrayOf(1, 7, 4, 1, 6, 7)[i % 6], intArrayOf(1, 2, 3, 0, 4, 5, 6)[i % 7],
            if (i % 3 == 2) 5 else 4, (i * 5) % Farmer.CLOTH.size, intArrayOf(1, 8, 2, 11)[i % 4], intArrayOf(4, 0, 0, 6)[i % 4], intArrayOf(1, 4, 0, 3)[i % 4])
        val px = figurePx(look, frame, outline = false)
        // A peace sign or flower in hand.
        if (i % 2 == 0) { px.set(12, 17, 0xFFFF8AB8.toInt()); px.set(12, 16, 0xFFFFD23A.toInt()) }
        px.outline(OUT)
        px.bitmap()
    }

    /** Your own farmer, as picked in the creator. */
    fun farmer(f: Farmer, frame: Int): Bitmap = personCache.getOrPut("f${f.encode()}.$frame") { figurePx(f, frame).bitmap() }

    /**
     * A person at [P_W]×[P_H]: shaded clothes lit from the left, a face with catchlights, and the hair, hat and face
     * picked in [look]. [frame] 1 is mid-stride.
     */
    internal fun figurePx(look: Farmer, frame: Int, outline: Boolean = true): Px {
        val px = Px(P_W, P_H)
        val skin = Farmer.SKINS[look.skin]; val skinS = shade(skin, 0.82f); val skinL = shade(skin, 1.08f)
        val hair = Farmer.HAIR_COLORS[look.hairColor]; val hairD = shade(hair, 0.7f); val hairL = mix(hair, 0xFFFFFFFF.toInt(), 0.3f)
        val shirt = Farmer.CLOTH[look.shirt]; val shirtD = shade(shirt, 0.72f); val shirtL = mix(shirt, 0xFFFFFFFF.toInt(), 0.22f)
        val pants = Farmer.CLOTH[look.pants]; val pantsD = shade(pants, 0.7f)
        val shoe = 0xFF2C2420.toInt()
        val outfit = Farmer.OUTFITS[look.outfit]; val hairStyle = Farmer.HAIRS[look.hair]
        val hat = Farmer.HATS[look.hat]; val face = Farmer.FACES[look.face]
        val dress = outfit == "Dress"
        val step = frame == 1

        // Hair that hangs behind the body.
        when (hairStyle) {
            "Long" -> px.rect(3, 6, 8, 11, hairD)
            "Afro" -> px.disc(7, 7, 5, hair)
            "Dreads" -> for (x in intArrayOf(3, 5, 8, 10)) px.rect(x, 7, 1, if (x == 3 || x == 10) 10 else 9, hairD)
            "Ponytail" -> { px.rect(10, 6, 2, 2, hair); px.rect(11, 8, 1, 6, hair); px.set(11, 13, hairD) }
        }
        // Legs (bare under a dress), then shoes; mid-stride one leg swings forward.
        val legC = if (dress) skinS else pants; val legD = if (dress) shade(skinS, 0.9f) else pantsD
        if (!step) {
            px.rect(4, 19, 3, 5, legC); px.rect(7, 19, 3, 5, legD)
            px.rect(4, 24, 3, 2, shoe); px.rect(7, 24, 3, 2, shoe)
        } else {
            px.rect(3, 19, 3, 4, legC); px.rect(8, 19, 3, 5, legD)
            px.rect(2, 23, 4, 2, shoe); px.rect(8, 24, 3, 2, shoe)
        }
        px.set(5, 25, shade(shoe, 1.6f))
        // The body, shaded: light down the left edge, dark down the right.
        fun cloth(x: Int, y: Int): Int = when (outfit) {
            "Flannel" -> if ((x + y) % 2 == 0 || y % 3 == 0) shirtD else shirt
            "Tie-dye" -> intArrayOf(0xFFFF5A5A.toInt(), 0xFFFFB03A.toInt(), 0xFFF4E04A.toInt(), 0xFF5AD06A.toInt(), 0xFF4AA0F0.toInt(), 0xFFB060E0.toInt())[
                Math.floorMod(((x - 7) * (x - 7) + (y - 15) * (y - 15)) / 3 + look.shirt, 6)]
            else -> when (x) { 3 -> shirtL; 10 -> shirtD; else -> shirt }
        }
        for (y in 12..18) for (x in 3..10) px.set(x, y, cloth(x, y))
        if (dress) {
            for (y in 19..21) for (x in 2 - (y - 19) / 2..11 + (y - 19) / 2) px.set(x, y, cloth(x, y))
            px.rect(3, 18, 8, 1, shirtD)
        } else px.rect(3, 18, 8, 1, shade(pants, 0.6f))
        when (outfit) {
            "Tee" -> { px.rect(6, 12, 2, 1, skinS) }
            "Overalls" -> {
                px.rect(5, 14, 4, 5, pants); px.set(5, 14, shade(pants, 1.15f))
                px.rect(4, 12, 1, 3, pants); px.rect(9, 12, 1, 3, pants)
                px.set(5, 14, 0xFFF2C440.toInt()); px.set(8, 14, 0xFFF2C440.toInt())
                px.rect(4, 18, 6, 1, pantsD)
            }
            "Hoodie" -> {
                px.rect(4, 11, 6, 1, shirtD); px.rect(5, 16, 4, 2, shirtD)
                px.set(6, 13, 0xFFF4F4F0.toInt()); px.set(7, 13, 0xFFF4F4F0.toInt()); px.set(6, 14, 0xFFE8E4DC.toInt()); px.set(7, 14, 0xFFE8E4DC.toInt())
            }
            "Flannel" -> { px.rect(6, 12, 2, 1, 0xFFE8E4DC.toInt()); px.rect(7, 13, 1, 5, shade(shirt, 0.55f)) }
            "Dress" -> { px.rect(6, 12, 2, 1, skinS); px.rect(3, 17, 8, 1, shirtL) }
        }
        // Arms swing opposite to the legs; hands in skin.
        val la = if (step) 13 else 12; val ra = if (step) 12 else 12
        px.rect(2, la, 1, 5, if (outfit == "Tie-dye") cloth(3, la) else shirtL); px.set(2, la + 5, skin)
        px.rect(11, ra, 1, 5, shirtD); px.set(11, ra + 5, skinS)
        if (outfit == "Tee" || outfit == "Dress") { px.rect(2, la + 3, 1, 2, skin); px.rect(11, ra + 3, 1, 2, skinS) }
        // Neck and head, rounded, lit from the left.
        px.rect(6, 11, 2, 1, skinS)
        px.rect(4, 4, 6, 7, skin); px.rect(5, 3, 4, 1, skin)
        px.rect(9, 4, 1, 7, skinS); px.clear(4, 10); px.clear(9, 10); px.set(5, 10, skinS); px.set(8, 10, skinS)
        px.set(4, 5, skinL)
        px.set(3, 7, skinS); px.set(10, 7, skinS)
        // Eyes with a fine catchlight, a rosy cheek, a small mouth.
        px.set(5, 7, 0xFF1E1610.toInt()); px.set(8, 7, 0xFF1E1610.toInt())
        px.dot(5 * S, 7 * S, 0xFFFFFFFF.toInt()); px.dot(8 * S, 7 * S, 0xFFFFFFFF.toInt())
        px.set(4, 8, mix(skin, 0xFFF06A86.toInt(), 0.3f)); px.set(9, 8, mix(skinS, 0xFFF06A86.toInt(), 0.3f))
        px.dot(6 * S + 1, 9 * S + 1, shade(skin, 0.55f)); px.dot(7 * S, 9 * S + 1, shade(skin, 0.55f)); px.dot(7 * S + 1, 9 * S + 1, shade(skin, 0.6f))
        // Facial hair and eyewear.
        when (face) {
            "Beard" -> { px.rect(4, 8, 6, 3, hair); px.rect(5, 11, 4, 1, hairD); px.rect(6, 9, 2, 1, shade(skin, 0.55f)); px.set(9, 8, hairD) }
            "Moustache" -> { px.rect(5, 8, 4, 1, hair); px.set(5, 9, hair); px.set(8, 9, hairD) }
            "Glasses" -> {
                for (cx in intArrayOf(5, 8)) {
                    for (d in 0 until S * 2) { px.dot(cx * S - 1 + d, 7 * S - 1, 0xFF3A2A20.toInt()); px.dot(cx * S - 1 + d, 8 * S, 0xFF3A2A20.toInt()) }
                    for (d in 0..S * 2) { px.dot(cx * S - 1, 7 * S - 1 + d, 0xFF3A2A20.toInt()); px.dot(cx * S + S, 7 * S - 1 + d, 0xFF3A2A20.toInt()) }
                }
                px.dot(6 * S, 7 * S, 0xFF3A2A20.toInt()); px.dot(6 * S + 1, 7 * S, 0xFF3A2A20.toInt()); px.dot(7 * S, 7 * S, 0xFF3A2A20.toInt()); px.dot(7 * S + 1, 7 * S, 0xFF3A2A20.toInt())
            }
            "Shades" -> { px.rect(4, 7, 6, 1, 0xFF14100E.toInt()); px.set(5, 7, 0xFF2A2A44.toInt()); px.set(8, 7, 0xFF2A2A44.toInt()); px.dot(5 * S, 7 * S, 0xFFB8D8FF.toInt()); px.dot(8 * S, 7 * S, 0xFFB8D8FF.toInt()) }
        }
        // Hair on top; a hat hides most of it.
        val hatted = hat != "None"
        when (hairStyle) {
            "Bald" -> px.dot(5 * S, 4 * S, skinL)
            "Mohawk" -> if (!hatted) { px.rect(6, 0, 2, 4, hair); px.set(6, 0, hairL); px.rect(4, 4, 1, 2, skinS); px.rect(9, 4, 1, 2, skinS) } else px.rect(4, 4, 6, 1, hairD)
            else -> {
                px.rect(4, 3, 6, 2, hair); px.rect(5, 2, 4, 1, hair); px.rect(4, 5, 1, 2, hair); px.rect(9, 5, 1, 3, hairD)
                px.set(5, 2, hairL); px.set(4, 3, hairL); px.rect(7, 4, 2, 1, hairD)
                when (hairStyle) {
                    "Long" -> { px.rect(3, 5, 1, 9, hair); px.rect(10, 5, 1, 9, hairD); px.set(3, 6, hairL) }
                    "Bun" -> if (!hatted) { px.disc(7, 1, 1, hair); px.set(6, 0, hairL) }
                    "Afro" -> { px.rect(3, 3, 8, 3, hair); px.set(4, 2, hairL); px.set(6, 1, hairL) }
                    "Dreads" -> { px.rect(3, 4, 1, 4, hair); px.rect(10, 4, 1, 4, hairD) }
                }
            }
        }
        val hc = when (hat) {
            "Straw hat" -> 0xFFE8C66A.toInt(); "Cowboy" -> 0xFF8A5A30.toInt(); "Witch" -> 0xFF3A2450.toInt()
            "Beanie" -> Farmer.CLOTH[(look.shirt + 4) % Farmer.CLOTH.size]; else -> pants
        }
        val hcD = shade(hc, 0.72f); val hcL = mix(hc, 0xFFFFFFFF.toInt(), 0.25f)
        when (hat) {
            "Cap" -> { px.rect(4, 2, 6, 3, hc); px.rect(5, 1, 4, 1, hc); px.set(5, 2, hcL); px.rect(8, 4, 4, 1, hcD) }
            "Straw hat" -> { px.rect(5, 1, 4, 3, hc); px.rect(5, 3, 4, 1, 0xFFC0402A.toInt()); px.rect(1, 4, 12, 1, hc); px.rect(2, 5, 10, 1, hcD); px.set(5, 1, hcL) }
            "Beanie" -> { px.rect(4, 1, 6, 4, hc); px.rect(5, 0, 4, 1, hc); px.rect(4, 4, 6, 1, hcD); px.set(7, 0, 0xFFF4F4F0.toInt()); px.set(5, 1, hcL) }
            "Bandana" -> { px.rect(4, 3, 6, 2, hc); px.rect(5, 2, 4, 1, hc); px.rect(10, 4, 1, 2, hcD); px.set(11, 5, hcD); px.set(5, 3, 0xFFF4F4F0.toInt()); px.set(8, 3, 0xFFF4F4F0.toInt()) }
            "Cowboy" -> { px.rect(4, 0, 6, 4, hc); px.clear(7, 0); px.rect(4, 3, 6, 1, hcD); px.rect(1, 4, 12, 1, hc); px.set(1, 3, hc); px.set(12, 3, hc); px.set(5, 1, hcL) }
            "Bucket" -> { px.rect(4, 1, 6, 4, hc); px.rect(3, 5, 8, 1, hcD); px.set(5, 2, hcL) }
            "Witch" -> {
                px.poly(hc, 4, 5, 6, -1, 8, -2, 7, 1, 10, 5)
                px.rect(1, 4, 12, 1, hc); px.rect(2, 5, 10, 1, hcD); px.rect(5, 3, 4, 1, 0xFFFF8A1E.toInt()); px.set(6, 3, 0xFFF2C440.toInt())
            }
        }
        if (outline) px.outline(OUT)
        return px
    }

    // ---- Festival tents ----

    const val TENT_W = 24
    const val TENT_H = 20
    const val TENT_AX = 12
    const val TENT_AY = 18

    /** A striped festival tent with a pennant; [v] picks the colours. */
    fun tent(v: Int): Bitmap = personCache.getOrPut("tent${Math.floorMod(v, 6)}") {
        val i = Math.floorMod(v, 6)
        val px = Px(TENT_W, TENT_H)
        val a = intArrayOf(0xFFE84A5A.toInt(), 0xFF4AA0F0.toInt(), 0xFFF2A030.toInt(), 0xFF8A5AD0.toInt(), 0xFF3EB86A.toInt(), 0xFFF06AB0.toInt())[i]
        val b = intArrayOf(0xFFF4E8D0.toInt(), 0xFFF2D04A.toInt(), 0xFF7A3AA0.toInt(), 0xFFF4E8D0.toInt(), 0xFFF2D04A.toInt(), 0xFF4AC0D0.toInt())[i]
        // Body: stripes running down from the peak, the right side in shade.
        px.poly(intArrayOf(12, 3, 1, 18, 23, 18)) { x, _ ->
            val c = if (Math.floorMod(x - 12, 4) < 2) a else b
            if (x >= 12) shade(c, 0.78f) else c
        }
        // An open door flap, dark inside.
        px.poly(0xFF2A1E1A.toInt(), 12, 9, 9, 18, 15, 18)
        px.line(12, 9, 9, 18, shade(a, 1.15f))
        // Pole and pennant.
        px.rect(12, 0, 1, 4, 0xFF6A4A2A.toInt())
        px.poly(b, 13, 0, 17, 1, 13, 2)
        // Pegs and ropes.
        px.line(1, 18, 0, 19, 0xFF8A6A4A.toInt()); px.line(23, 18, 23, 19, 0xFF8A6A4A.toInt())
        px.outline(OUT)
        px.bitmap()
    }

    // ---- Pets ----

    const val PET_W = 16
    const val PET_H = 14
    const val PET_AX = 8
    const val PET_AY = 13

    /** A holiday pet: four-legged with a tail, or a bird; [frame] 1 is mid-step. Faces right. */
    fun pet(p: Pet, frame: Int): Bitmap = personCache.getOrPut("pet${p.ordinal}.$frame") { petPx(p, frame).bitmap() }

    internal fun petPx(p: Pet, frame: Int): Px {
        val px = Px(PET_W, PET_H)
        val b = p.body; val bd = shade(b, 0.75f); val bl = mix(b, 0xFFFFFFFF.toInt(), 0.2f); val be = p.belly
        val eye = if (p == Pet.CAT) 0xFFF2E040.toInt() else 0xFF14100E.toInt()
        val step = frame == 1
        if (p.bird) {
            // Legs, a round body, a wing, head and beak.
            val leg = 0xFFE0A03A.toInt()
            px.rect(6, 11, 1, 2, leg); px.rect(if (step) 9 else 8, 11, 1, 2, leg)
            if (p == Pet.TURKEY) {
                // A fan of tail feathers behind.
                for ((i, c) in intArrayOf(0xFFC03A2A.toInt(), 0xFFE0903A.toInt(), 0xFF8A4A20.toInt(), 0xFFE0C04A.toInt(), 0xFFC03A2A.toInt()).withIndex())
                    px.poly(c, 6, 9, 1 + i * 2, 1 + if (i in 1..3) 0 else 2, 3 + i * 2, 1 + if (i in 1..3) 0 else 2)
            }
            px.disc(7, 8, 3, b); px.rect(4, 8, 7, 3, b); px.rect(5, 10, 5, 1, be)
            px.rect(5, 7, 4, 2, bd); px.set(5, 7, bl)
            val hx = 11; val hy = 4
            px.rect(hx - 1, hy + 2, 2, 3, if (p == Pet.EAGLE) be else b)
            px.disc(hx, hy, 1, if (p == Pet.EAGLE) be else 0xFF8AA0C0.toInt())
            px.rect(hx + 2, hy, 2, 1, 0xFFF2C440.toInt()); px.set(hx + 2, hy + 1, 0xFFE0A03A.toInt())
            if (p == Pet.TURKEY) { px.set(hx + 1, hy + 2, 0xFFE03A3A.toInt()); px.set(hx + 1, hy + 3, 0xFFE03A3A.toInt()) }
            px.set(hx + 1, hy, eye)
        } else {
            val small = p == Pet.PUG || p == Pet.BUNNY
            // Legs: back pair and front pair, swapping mid-step.
            val legY = 10; val legH = 3
            px.rect(4, legY, 1, legH - (if (step) 1 else 0), bd); px.rect(if (step) 6 else 5, legY, 1, legH, b)
            px.rect(10, legY, 1, legH - (if (step) 0 else 1), bd); px.rect(if (step) 12 else 11, legY, 1, legH, b)
            // Body.
            px.rect(3, 6, 10, 4, b); px.rect(4, 5, 8, 1, b); px.rect(4, 9, 8, 1, be); px.rect(4, 6, 6, 1, bl)
            // Tail.
            when (p) {
                Pet.CAT -> { px.rect(2, 3, 1, 4, b); px.set(1, 2, b); px.set(2, 2, b) }
                Pet.FOX -> { px.poly(b, 3, 6, 0, 4, 0, 8, 3, 9); px.set(0, 6, 0xFFF4F4F0.toInt()); px.set(0, 5, 0xFFF4F4F0.toInt()) }
                Pet.BUNNY -> px.disc(2, 6, 1, 0xFFFFFFFF.toInt())
                Pet.POODLE -> { px.set(2, 4, b); px.disc(2, 3, 1, bl) }
                Pet.REINDEER -> px.set(2, 6, be)
                Pet.SLOTH -> {}
                else -> px.set(2, 5, b)
            }
            // Head and ears.
            val hx = 12; val hy = if (small) 5 else 4
            px.disc(hx, hy, 2, b); px.set(hx + 2, hy + 1, be); px.set(hx + 3, hy + 1, if (p == Pet.REINDEER) 0xFFE03A3A.toInt() else be)
            when (p) {
                Pet.CAT, Pet.FOX -> { px.set(hx - 1, hy - 3, b); px.set(hx + 1, hy - 3, b); px.set(hx - 1, hy - 2, bd); px.set(hx + 1, hy - 2, bd) }
                Pet.BUNNY -> { px.rect(hx - 1, hy - 5, 1, 4, b); px.rect(hx + 1, hy - 5, 1, 4, b); px.set(hx + 1, hy - 4, be) }
                Pet.REINDEER -> { val a = 0xFFC8A070.toInt(); px.line(hx - 1, hy - 2, hx - 2, hy - 4, a); px.line(hx + 1, hy - 2, hx + 2, hy - 4, a); px.set(hx - 3, hy - 4, a); px.set(hx + 3, hy - 4, a) }
                Pet.PUG -> { px.set(hx - 2, hy - 1, be); px.set(hx + 2, hy - 1, be); px.rect(hx, hy, 3, 2, be) }
                Pet.POODLE -> { px.disc(hx, hy - 2, 1, bl); px.disc(hx - 2, hy, 1, bl) }
                Pet.SLOTH -> { px.rect(hx - 1, hy, 3, 1, be); px.set(hx - 1, hy, 0xFF3A2A20.toInt()) }
                else -> {}
            }
            px.set(hx + 1, hy - 1, eye)
            if (p == Pet.CAT) px.dot((hx + 1) * S, (hy - 1) * S, 0xFF14100E.toInt())
        }
        px.outline(OUT)
        return px
    }

    // ---- Portraits ----

    const val SIZE = 48
    /** Violet is drawn at twice the detail of the others. */
    const val BIG = 96
    private val portraitCache = HashMap<Keeper, Bitmap>()

    fun portrait(k: Keeper): Bitmap = portraitCache.getOrPut(k) { portraitPx(k).bitmap() }

    /** The figure over its backdrop: a gradient with a soft light behind the head. */
    internal fun portraitPx(k: Keeper): Px {
        val n = if (k == Keeper.VIOLET) BIG else SIZE
        val fig = Px(n, n)
        val bg = when (k) {
            Keeper.VIOLET -> 0xFF7CC49A.toInt() to 0xFF1E4A34.toInt()
            Keeper.DEX -> 0xFF8A6A3A.toInt() to 0xFF4A3420.toInt()
            Keeper.HANK -> 0xFF6A7A8A.toInt() to 0xFF3A4450.toInt()
            Keeper.MONA -> 0xFFE8D8C0.toInt() to 0xFFA89878.toInt()
            Keeper.GUS -> 0xFF4A7AB8.toInt() to 0xFF24406A.toInt()
            Keeper.SHADE -> 0xFF3A2A50.toInt() to 0xFF120E1A.toInt()
        }
        when (k) {
            Keeper.VIOLET -> violet(fig)
            Keeper.DEX -> dex(fig)
            Keeper.HANK -> hank(fig)
            Keeper.MONA -> mona(fig)
            Keeper.GUS -> gus(fig)
            Keeper.SHADE -> hooded(fig)
        }
        fig.outline(OUT)
        val px = Px(n, n)
        val r = n * 0.42f
        for (y in 0 until n) for (x in 0 until n) {
            val dx = x - n / 2f; val dy = y - n * 0.38f
            val d = (dx * dx + dy * dy) / (r * r)
            val c = mix(bg.first, bg.second, y / n.toFloat())
            px.set(x, y, if (d < 1f) mix(c, 0xFFFFFFFF.toInt(), (1f - d) * 0.14f) else c)
        }
        // Out-of-focus greenhouse lights behind Violet.
        if (k == Keeper.VIOLET) for ((bx, by, br) in listOf(Triple(10, 14, 6), Triple(84, 22, 8), Triple(18, 52, 4), Triple(88, 60, 5), Triple(74, 6, 3), Triple(6, 80, 6))) {
            for (y in by - br..by + br) for (x in bx - br..bx + br) {
                val d = ((x - bx) * (x - bx) + (y - by) * (y - by)).toFloat() / (br * br)
                if (d <= 1f) px.set(x, y, mix(px.get(x, y), 0xFFFFF4C8.toInt(), if (d > 0.7f) 0.3f else 0.18f))
            }
        }
        for (i in fig.p.indices) if (fig.p[i] != 0) px.p[i] = fig.p[i]
        return px
    }

    private fun torso(px: Px, c: Int, w: Int = 17) = px.poly(c, 24 - w, 48, 24 - w, 41, 24 - w + 5, 35, 24 + w - 5, 35, 24 + w, 41, 24 + w, 48)

    /** Neck, ears, head and eyes. Brows and mouth are up to each character. */
    private fun head(px: Px, skin: Int, iris: Int = 0xFF2A1A10.toInt()) {
        val sd = shade(skin, 0.84f)
        px.rect(20, 27, 8, 9, sd)
        px.disc(15, 21, 2, sd); px.disc(33, 21, 2, sd)
        oval(px, 24, 20, 9, 11, sd); oval(px, 23, 19, 8, 10, skin)
        px.rect(19, 20, 3, 2, 0xFFF4F4F0.toInt()); px.rect(26, 20, 3, 2, 0xFFF4F4F0.toInt())
        px.rect(20, 20, 2, 2, iris); px.rect(27, 20, 2, 2, iris)
        px.set(20, 20, shade(iris, 2.2f)); px.set(27, 20, shade(iris, 2.2f))
        px.set(24, 23, shade(skin, 0.72f)); px.set(24, 24, shade(skin, 0.72f)); px.set(23, 24, shade(skin, 0.8f))
    }

    /** Violet at [BIG] pixels: long purple hair with side-swept bangs, green eyes, a flower and leaf in her hair, a gold necklace. */
    private fun violet(px: Px) {
        val skin = 0xFFC08058.toInt(); val skinL = 0xFFD89A72.toInt(); val skinS = 0xFFA0623F.toInt(); val skinD = 0xFF7E4A30.toInt()
        val hair = 0xFF8E52D0.toInt(); val hairL = 0xFFB98CF2.toInt(); val hairHi = 0xFFE4CCFF.toInt()
        val hairD = 0xFF62349E.toInt(); val hairDD = 0xFF40206E.toInt()
        val gold = 0xFFF2C440.toInt(); val goldD = 0xFFB88A24.toInt()
        val tank = 0xFF26202E.toInt(); val tankL = 0xFF40364E.toInt()
        fun mirror(pts: IntArray) = IntArray(pts.size) { i -> if (i % 2 == 0) BIG - pts[i] else pts[i] }
        /** Lines through consecutive points, for inner contours the outline pass can't see. */
        fun edge(c: Int, vararg pts: Int) { for (i in 0 until pts.size / 2 - 1) px.line(pts[i * 2], pts[i * 2 + 1], pts[i * 2 + 2], pts[i * 2 + 3], c) }
        val strands: (Int, Int) -> Int = { x, y ->
            val c = when ((abs(x - 48) + y / 9) % 5) { 0 -> hairD; 2 -> hairL; else -> hair }
            if (y > 76) shade(c, 0.88f) else c
        }

        // Hair falling behind her head and shoulders.
        val back = intArrayOf(30, 20, 38, 9, 48, 6, 58, 9, 66, 20, 70, 36, 72, 56, 76, 72, 80, 86, 70, 90, 26, 90, 16, 86, 20, 72, 24, 56, 26, 36)
        px.poly(back) { x, y -> if ((abs(x - 48) + y / 9) % 5 == 0) hairDD else hairD }
        // Neck, shoulders and collarbones, lit from the left.
        px.poly(skinS, 42, 52, 54, 52, 55, 70, 41, 70)
        px.poly(intArrayOf(4, 96, 6, 86, 12, 79, 26, 73, 40, 69, 56, 69, 70, 73, 84, 79, 90, 86, 92, 96)) { x, y -> if (x > 72 || (y < 73 && x > 54)) skinS else skin }
        px.rect(42, 56, 13, 5, skinD)
        px.line(34, 76, 43, 78, skinS); px.line(53, 78, 62, 76, skinS)
        px.line(12, 81, 22, 76, skinL)
        // Black tank top with a leaf on the front.
        px.poly(tank, 18, 96, 19, 87, 24, 81, 34, 85, 48, 87, 62, 85, 72, 81, 77, 87, 78, 96)
        val strap = intArrayOf(23, 82, 28, 72, 31, 72, 27, 83)
        px.poly(tank, *strap); px.poly(tank, *mirror(strap))
        px.line(25, 79, 28, 73, tankL); px.line(21, 88, 30, 91, tankL)
        Sprites.fanLeaf(px, 48, 94, -PI / 2, 7, 0xFF5CC05C.toInt(), 0xFF9AF09A.toInt())
        // Gold chain with a little leaf pendant.
        for (x in 39..57) { val t = (x - 48) / 9f; px.set(x, 70 + (7 * (1 - t * t) + 0.5f).toInt(), if (x % 2 == 0) gold else goldD) }
        px.disc(48, 79, 1, gold); px.set(48, 79, 0xFF6AD06A.toInt()); px.set(47, 78, 0xFFFFF0A0.toInt())

        // The crown of her head, then her face.
        oval(px, 48, 26, 17, 19, hair)
        val face = intArrayOf(34, 24, 62, 24, 63, 36, 62, 44, 59, 50, 55, 55, 51, 58, 48, 59, 45, 58, 41, 55, 37, 50, 34, 44, 33, 36)
        px.poly(face) { x, y ->
            when {
                y <= 30 -> skinS
                x >= 59 || (x >= 54 && y >= 53) -> skinS
                x in 37..41 && y in 43..46 -> skinL
                else -> skin
            }
        }
        edge(skinD, 37, 50, 41, 55, 45, 58, 48, 59, 51, 58, 55, 55, 59, 50)
        // Big green eyes with lashes, a winged liner and two catchlights.
        val eye = arrayOf(
            "K...kKKKk..",
            ".KKKKKKKKKk",
            "..KwIIIIIw.",
            "..WWHHIIIW.",
            "...WHiPPiW.",
            "...WiiPPiW.",
            "....jiiHj..",
            ".....LLL...",
        )
        val pal = mapOf(
            'K' to 0xFF241228.toInt(), 'k' to 0xFF4A2840.toInt(), 'W' to 0xFFF8F2F0.toInt(), 'w' to 0xFFD8C8D4.toInt(),
            'I' to 0xFF1E5A3C.toInt(), 'i' to 0xFF3A9E64.toInt(), 'j' to 0xFF86DCA0.toInt(), 'P' to 0xFF0E2016.toInt(),
            'H' to 0xFFFFFFFF.toInt(), 'L' to 0xFF7A4050.toInt(),
        )
        val lid = mix(skin, 0xFF9A5AC8.toInt(), 0.3f)
        px.line(40, 36, 44, 36, lid); px.line(56, 36, 52, 36, lid)
        px.map(36, 37, eye, pal); px.map(50, 37, Array(eye.size) { eye[it].reversed() }, pal)
        val brow = 0xFF4A2458.toInt()
        px.line(37, 34, 39, 33, brow); px.line(40, 32, 44, 32, brow); px.set(45, 33, brow)
        px.line(59, 34, 57, 33, brow); px.line(56, 32, 52, 32, brow); px.set(51, 33, brow)
        // Rosy cheeks, a small nose and a soft smile.
        for (cx in intArrayOf(39, 57)) for (y in 47..49) for (x in cx - 3..cx + 3) {
            val d = (x - cx) * (x - cx) / 10f + (y - 48) * (y - 48) / 2f
            if (d <= 1f) px.set(x, y, mix(px.get(x, y), 0xFFF06A86.toInt(), 0.32f))
        }
        px.set(47, 45, skinL); px.set(49, 47, skinS); px.set(48, 48, skinD)
        val lipD = 0xFFA03A52.toInt(); val lip = 0xFFD8687E.toInt()
        px.set(44, 51, skinS); px.set(52, 51, skinS)
        px.rect(45, 52, 7, 1, lipD); px.set(48, 52, shade(lipD, 0.85f))
        px.rect(46, 53, 5, 1, lip); px.set(47, 53, 0xFFF4A4B4.toInt())

        // Locks framing her face and falling over her shoulders.
        val lock = intArrayOf(33, 24, 30, 38, 29, 54, 27, 68, 24, 82, 21, 93, 26, 91, 31, 82, 34, 68, 37, 56, 37, 48, 35, 40, 36, 32, 38, 26)
        px.poly(lock, strands); px.poly(mirror(lock), strands)
        val inner = intArrayOf(38, 26, 36, 32, 35, 40, 37, 48, 37, 56, 34, 68, 31, 82, 26, 91)
        edge(hairDD, *inner); edge(hairDD, *mirror(inner))
        // Side-swept bangs from a part on her left, with a shine across the crown.
        val bangs = intArrayOf(30, 30, 30, 20, 35, 12, 43, 8, 53, 8, 61, 12, 66, 20, 66, 32, 64, 38,
            62, 31, 60, 33, 57, 28, 54, 31, 51, 27, 47, 31, 44, 27, 41, 31, 39, 28, 36, 33, 34, 28, 32, 36)
        px.poly(bangs) { x, y ->
            when (((x - 43) * 4 / maxOf(y - 4, 1) + 100) % 4) { 0 -> hairD; 2 -> hairL; else -> hair }
        }
        edge(hairDD, 64, 38, 62, 31, 60, 33, 57, 28, 54, 31, 51, 27, 47, 31, 44, 27, 41, 31, 39, 28, 36, 33, 34, 28, 32, 36)
        px.line(43, 8, 41, 13, hairDD)
        for (y in 9..20) for (x in 32..64) {
            val dx = (x - 48) / 16f; val dy = (y - 22) / 11f
            val d = dx * dx + dy * dy
            if (d in 0.8f..1f && (x / 3) % 3 != 0) px.set(x, y, if (d > 0.9f) hairL else hairHi)
        }
        // A pink flower tucked behind a fan leaf.
        Sprites.fanLeaf(px, 32, 19, -2.3, 7, 0xFF4FA03C.toInt(), 0xFF8AD86A.toInt())
        val pet = 0xFFFF8AB8.toInt()
        for (i in 0 until 5) {
            val a = i * 2 * PI / 5 - PI / 2
            px.disc(36 + (cos(a) * 3).roundToInt(), 15 + (sin(a) * 3).roundToInt(), 2, if (i % 2 == 0) pet else shade(pet, 0.88f))
        }
        px.disc(36, 15, 1, gold); px.set(36, 15, 0xFFFFF0A0.toInt())
    }

    private fun dex(px: Px) {
        val skin = 0xFFC88A5A.toInt()
        val hood = 0xFF3A7A3A.toInt()
        torso(px, hood)
        px.poly(shade(hood, 0.8f), 17, 35, 31, 35, 28, 40, 20, 40)
        px.line(21, 38, 21, 45, 0xFFF4F4F0.toInt()); px.line(27, 38, 27, 45, 0xFFF4F4F0.toInt())
        head(px, skin)
        px.line(19, 20, 21, 20, shade(skin, 0.7f)); px.line(26, 20, 28, 20, shade(skin, 0.7f))
        val beard = 0xFF5A3A22.toInt()
        px.poly(beard, 15, 21, 17, 27, 21, 31, 27, 31, 31, 27, 33, 21, 33, 25, 29, 33, 19, 33, 15, 25)
        px.poly(beard, 20, 25, 28, 25, 28, 27, 20, 27)
        px.line(21, 28, 27, 28, 0xFF8A3A3A.toInt())
        px.line(19, 17, 22, 17, beard); px.line(26, 17, 29, 17, beard)
        val st = intArrayOf(0xFFD8484A.toInt(), 0xFFF2D24A.toInt(), 0xFF3F9A3A.toInt())
        for (y in 4..15) for (x in 14..34) {
            val dx = (x - 24) / 10.5f; val dy = (y - 14) / 10f
            if (dx * dx + dy * dy <= 1f) px.set(x, y, if (y >= 13) shade(st[2], 0.8f) else st[(y - 4) / 3 % 3])
        }
        px.disc(24, 3, 2, st[0])
    }

    private fun hank(px: Px) {
        val skin = 0xFFF0C090.toInt()
        px.poly(intArrayOf(7, 48, 7, 41, 12, 35, 36, 35, 41, 41, 41, 48)) { x, y -> if ((x / 3 + y / 3) % 2 == 0) 0xFFB83A3A.toInt() else 0xFF5A1E22.toInt() }
        px.rect(15, 38, 3, 10, 0xFF3E5E9A.toInt()); px.rect(30, 38, 3, 10, 0xFF3E5E9A.toInt()); px.set(16, 41, 0xFFF2C440.toInt()); px.set(31, 41, 0xFFF2C440.toInt())
        head(px, skin, 0xFF3A5A8A.toInt())
        val grey = 0xFFC8C8C8.toInt()
        px.rect(15, 15, 2, 6, grey); px.rect(31, 15, 2, 6, grey)
        px.line(19, 18, 22, 18, grey); px.line(26, 18, 29, 18, grey)
        px.poly(grey, 19, 26, 24, 25, 29, 26, 30, 28, 24, 27, 18, 28)
        px.line(22, 29, 26, 29, shade(skin, 0.6f))
        val cap = 0xFFC83A32.toInt()
        for (y in 5..14) for (x in 14..34) {
            val dx = (x - 24) / 10f; val dy = (y - 14) / 9f
            if (dx * dx + dy * dy <= 1f) px.set(x, y, if (x in 19..29 && y in 7..13) 0xFFF4F4F0.toInt() else cap)
        }
        px.rect(22, 9, 1, 4, cap); px.rect(26, 9, 1, 4, cap); px.rect(23, 11, 3, 1, cap)
        px.poly(shade(cap, 0.8f), 14, 14, 36, 14, 39, 16, 15, 16)
    }

    private fun mona(px: Px) {
        val skin = 0xFFF4D0B0.toInt(); val hair = 0xFFF2D27A.toInt(); val hairD = 0xFFC8A04A.toInt()
        oval(px, 24, 20, 13, 13, hairD); px.rect(11, 20, 26, 11, hairD)
        val navy = 0xFF2A3A6A.toInt()
        torso(px, navy)
        px.poly(0xFFF4F4F0.toInt(), 19, 35, 29, 35, 24, 45)
        px.line(19, 35, 23, 46, shade(navy, 1.4f)); px.line(29, 35, 25, 46, shade(navy, 1.4f))
        for (i in 0..6) px.set(20 + i * 4 / 3, 36 + if (i in 2..4) 2 else if (i in 1..5) 1 else 0, 0xFFF8F4EC.toInt())
        head(px, skin, 0xFF3A6A9A.toInt())
        px.line(19, 17, 22, 17, hairD); px.line(26, 17, 29, 17, hairD)
        px.line(18, 19, 22, 19, OUT); px.line(26, 19, 30, 19, OUT)
        px.rect(22, 27, 5, 1, 0xFFD03A4A.toInt()); px.rect(23, 28, 3, 1, 0xFFA82A3A.toInt())
        val s: (Int, Int) -> Int = { x, _ -> if (x % 4 == 0) hairD else hair }
        px.poly(intArrayOf(13, 10, 24, 7, 35, 10, 36, 30, 32, 31, 32, 16, 26, 13, 18, 16, 16, 31, 12, 30), s)
        px.set(15, 25, 0xFFF8F4EC.toInt()); px.set(33, 25, 0xFFF8F4EC.toInt())
    }

    private fun gus(px: Px) {
        val skin = 0xFF5A3A26.toInt()
        torso(px, 0xFF8A8A92.toInt())
        val vest = 0xFFF08A2A.toInt()
        px.poly(vest, 8, 48, 8, 41, 13, 36, 19, 36, 21, 48); px.poly(vest, 40, 48, 40, 41, 35, 36, 29, 36, 27, 48)
        px.rect(8, 43, 13, 2, 0xFFE8ECF0.toInt()); px.rect(27, 43, 13, 2, 0xFFE8ECF0.toInt())
        head(px, skin)
        px.line(19, 17, 22, 17, OUT); px.line(26, 17, 29, 17, OUT)
        px.poly(0xFFF4F4F0.toInt(), 20, 26, 28, 26, 26, 29, 22, 29); px.line(20, 26, 28, 26, 0xFF6A2A2A.toInt())
        for (y in 7..13) for (x in 15..33) {
            val dx = (x - 24) / 9f; val dy = (y - 14) / 7f
            if (dx * dx + dy * dy <= 1f) px.set(x, y, if ((x + y) % 3 == 0) 0xFF2A2420.toInt() else 0xFF14100E.toInt())
        }
        px.rect(41, 36, 6, 9, 0xFFC89A62.toInt()); px.rect(42, 37, 4, 6, 0xFFF4F4F0.toInt()); px.rect(43, 35, 2, 2, 0xFF9AA0A8.toInt())
    }

    private fun hooded(px: Px) {
        val hood = 0xFF2A2A34.toInt(); val hoodL = 0xFF3E3E4C.toInt()
        px.poly(hood, 4, 48, 6, 40, 12, 35, 36, 35, 42, 40, 44, 48)
        oval(px, 24, 20, 14, 16, hood); oval(px, 22, 18, 11, 13, hoodL)
        val skin = 0xFF6A5040.toInt()
        oval(px, 24, 23, 8, 9, 0xFF1A161E.toInt())
        oval(px, 24, 26, 7, 6, skin)
        px.rect(16, 20, 17, 4, 0xFF0E0C12.toInt()); px.rect(18, 21, 3, 1, 0xFF6A8AB8.toInt()); px.rect(27, 21, 3, 1, 0xFF6A8AB8.toInt())
        px.line(20, 28, 28, 27, 0xFF2A1A18.toInt()); px.set(25, 28, 0xFFF2C440.toInt()); px.set(23, 28, 0xFFF4F4F0.toInt())
        px.line(24, 36, 24, 48, 0xFF1A1A22.toInt())
        px.rect(30, 40, 4, 2, 0xFF9AA0A8.toInt()); px.set(31, 39, 0xFFF2C440.toInt())
    }
}
