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

    /** Every keeper is drawn at this many pixels square. */
    const val BIG = 96
    private val portraitCache = HashMap<String, Bitmap>()

    /** [k]'s keeper in the town next to [site]: the home town has the originals, every other town its own look. */
    fun portrait(k: Keeper, site: String = "home"): Bitmap = portraitCache.getOrPut("${k.name}/$site") { portraitPx(k, site).bitmap() }

    private val SKINS = intArrayOf(0xFFF0C8A6.toInt(), 0xFFE0A07C.toInt(), 0xFFC08058.toInt(), 0xFFB07A58.toInt(), 0xFF94603C.toInt(), 0xFF6A4028.toInt())
    private val KEEPER_SKIN = mapOf(Keeper.VIOLET to 0xFFC08058.toInt(), Keeper.DEX to 0xFF94603C.toInt(), Keeper.HANK to 0xFFE0A07C.toInt(),
        Keeper.MONA to 0xFFF0C8A6.toInt(), Keeper.GUS to 0xFF6A4028.toInt(), Keeper.SHADE to 0xFFB07A58.toInt())

    /** Another town's keeper: their skin tone swapped, hair and clothes in other colours, sometimes facing the other way. */
    private fun restyle(fig: Px, k: Keeper, site: String) {
        var h = (site.hashCode() * 31 + k.ordinal) * 0x9E3779B1.toInt()
        h = h xor (h ushr 15)
        val skin = SKINS[Math.floorMod(h, SKINS.size)]
        val base = KEEPER_SKIN.getValue(k)
        val turn = 30f + Math.floorMod(h ushr 4, 300)
        val hsv = FloatArray(3)
        for (i in fig.p.indices) {
            val c = fig.p[i]
            if (c == 0) continue
            toHsv(c, hsv)
            val skinLike = hsv[0] in 8f..42f && hsv[1] in 0.2f..0.72f && hsv[2] > 0.3f
            fig.p[i] = when {
                skinLike -> {
                    // Keep the shading: scale the new tone by how far this pixel is from the drawn skin.
                    fun ch(sh: Int) = (((skin ushr sh) and 255) * ((c ushr sh) and 255) / ((base ushr sh) and 255).coerceAtLeast(1)).coerceIn(0, 255)
                    (c and 0xFF000000.toInt()) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
                }
                hsv[1] > 0.18f && hsv[2] > 0.12f -> { hsv[0] = (hsv[0] + turn) % 360f; fromHsv(hsv, c ushr 24) }
                else -> c
            }
        }
        if ((h ushr 12) and 1 == 1) for (y in 0 until fig.fh) for (x in 0 until fig.fw / 2) {
            val a = y * fig.fw + x; val b = y * fig.fw + fig.fw - 1 - x
            val t = fig.p[a]; fig.p[a] = fig.p[b]; fig.p[b] = t
        }
    }

    private fun toHsv(c: Int, out: FloatArray) {
        val r = (c shr 16 and 255) / 255f; val g = (c shr 8 and 255) / 255f; val b = (c and 255) / 255f
        val mx = maxOf(r, g, b); val mn = minOf(r, g, b); val d = mx - mn
        out[0] = when {
            d == 0f -> 0f
            mx == r -> 60f * (((g - b) / d) % 6f)
            mx == g -> 60f * ((b - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }.let { if (it < 0f) it + 360f else it }
        out[1] = if (mx == 0f) 0f else d / mx
        out[2] = mx
    }

    private fun fromHsv(hsv: FloatArray, alpha: Int): Int {
        val (hh, sat, v) = Triple(hsv[0], hsv[1], hsv[2])
        val cc = v * sat; val x = cc * (1 - kotlin.math.abs((hh / 60f) % 2f - 1)); val m = v - cc
        val (r, g, b) = when ((hh / 60f).toInt()) {
            0 -> Triple(cc, x, 0f); 1 -> Triple(x, cc, 0f); 2 -> Triple(0f, cc, x)
            3 -> Triple(0f, x, cc); 4 -> Triple(x, 0f, cc); else -> Triple(cc, 0f, x)
        }
        fun ch(f: Float) = ((f + m) * 255f).roundToInt().coerceIn(0, 255)
        return (alpha shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }

    /** The figure over its backdrop: a gradient with a soft light behind the head and something of their shop. */
    internal fun portraitPx(k: Keeper, site: String = "home"): Px {
        val n = BIG
        val fig = Px(n, n)
        val bg = when (k) {
            Keeper.VIOLET -> 0xFF7CC49A.toInt() to 0xFF1E4A34.toInt()
            Keeper.DEX -> 0xFF9A6A48.toInt() to 0xFF4A3024.toInt()
            Keeper.HANK -> 0xFFB89A70.toInt() to 0xFF6A5438.toInt()
            Keeper.MONA -> 0xFFEADCC4.toInt() to 0xFFB09A7C.toInt()
            Keeper.GUS -> 0xFF6AA8E0.toInt() to 0xFF2A5A98.toInt()
            Keeper.SHADE -> 0xFF34264A.toInt() to 0xFF100C18.toInt()
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
        if (site != "home") restyle(fig, k, site)
        val px = Px(n, n)
        val r = n * 0.42f
        for (y in 0 until n) for (x in 0 until n) {
            val dx = x - n / 2f; val dy = y - n * 0.38f
            val d = (dx * dx + dy * dy) / (r * r)
            val c = mix(bg.first, bg.second, y / n.toFloat())
            px.set(x, y, if (d < 1f) mix(c, 0xFFFFFFFF.toInt(), (1f - d) * 0.14f) else c)
        }
        backdrop(px, k)
        for (i in fig.p.indices) if (fig.p[i] != 0) px.p[i] = fig.p[i]
        return px
    }

    /** Something of each keeper's place behind them, kept dim so the figure stands out. */
    private fun backdrop(px: Px, k: Keeper) {
        val n = BIG
        fun dim(x: Int, y: Int, f: Float) = px.set(x, y, shade(px.get(x, y), f))
        fun glow(bx: Int, by: Int, br: Int, c: Int, a: Float) {
            for (y in by - br..by + br) for (x in bx - br..bx + br) {
                val d = ((x - bx) * (x - bx) + (y - by) * (y - by)).toFloat() / (br * br)
                if (d <= 1f) px.set(x, y, mix(px.get(x, y), c, a * (1f - d)))
            }
        }
        when (k) {
            Keeper.VIOLET -> {
                // Out-of-focus greenhouse lights and leaves hanging into the frame.
                for ((bx, by, br) in listOf(Triple(10, 14, 6), Triple(84, 22, 8), Triple(18, 52, 4), Triple(88, 60, 5), Triple(74, 6, 3), Triple(6, 80, 6))) {
                    for (y in by - br..by + br) for (x in bx - br..bx + br) {
                        val d = ((x - bx) * (x - bx) + (y - by) * (y - by)).toFloat() / (br * br)
                        if (d <= 1f) px.set(x, y, mix(px.get(x, y), 0xFFFFF4C8.toInt(), if (d > 0.7f) 0.3f else 0.18f))
                    }
                }
                Sprites.fanLeaf(px, 2, 0, PI / 3, 11, 0xFF2E6A48.toInt(), 0xFF3E7E58.toInt())
                Sprites.fanLeaf(px, 94, 2, PI * 2 / 3, 9, 0xFF2A6242.toInt(), 0xFF3A7652.toInt())
            }
            Keeper.DEX -> {
                // Back-alley brick, with a sprayed leaf.
                for (y in 0 until n) for (x in 0 until n) {
                    val row = y / 7
                    val mortar = y % 7 == 6 || (x + (row % 2) * 7) % 14 == 13
                    if (mortar) dim(x, y, 0.72f) else if ((x / 14 * 7 + row * 3) % 5 == 0) dim(x, y, 0.9f)
                }
                Sprites.fanLeaf(px, 84, 40, -PI / 2, 12, mix(px.get(84, 30), 0xFF5AC05A.toInt(), 0.45f), mix(px.get(84, 30), 0xFF8AE08A.toInt(), 0.45f))
            }
            Keeper.HANK -> {
                // Pegboard with tools hung on it.
                for (y in 0 until n) for (x in 0 until n) if (x % 6 == 3 && y % 6 == 3) dim(x, y, 0.6f)
                val t = shade(mix(0xFF6A6A72.toInt(), px.get(10, 20), 0.4f), 0.9f)
                for (k2 in 0..1) px.line(8 + k2, 8, 16 + k2, 34, t)
                px.disc(8, 7, 3, t); px.rect(7, 3, 3, 3, px.get(4, 4)); px.disc(17, 35, 2, t)
                px.rect(82, 10, 3, 26, shade(0xFF8A6A44.toInt(), 0.8f)); px.rect(76, 6, 15, 5, t); px.rect(88, 4, 3, 3, t)
            }
            Keeper.MONA -> {
                // An office window with the blinds half up.
                val frame = 0xFFF4EEE2.toInt()
                px.rect(62, 4, 32, 46, frame)
                for (y in 6..47) for (x in 64..91) {
                    val sky = mix(0xFF9CC8E8.toInt(), 0xFFD8ECF4.toInt(), (y - 6) / 42f)
                    px.set(x, y, if (y < 20 && y % 3 == 0) 0xFFE0D8C8.toInt() else if (y < 20) 0xFFEDE6DA.toInt() else sky)
                }
                px.rect(77, 20, 2, 28, frame); px.rect(64, 33, 28, 2, frame)
                px.poly(0xFF8AA87A.toInt(), 64, 48, 64, 42, 72, 38, 82, 41, 91, 37, 91, 48)
                px.rect(66, 24, 6, 5, 0xFFB86A4A.toInt()); px.poly(0xFF8A4A3A.toInt(), 65, 24, 69, 21, 73, 24)
                glow(10, 20, 14, 0xFFFFF8E8.toInt(), 0.25f)
            }
            Keeper.GUS -> {
                // Clouds over a work yard.
                for ((cx, cy, cr) in listOf(Triple(12, 16, 7), Triple(20, 13, 6), Triple(27, 17, 5), Triple(78, 30, 6), Triple(86, 27, 7), Triple(92, 31, 5))) {
                    oval(px, cx, cy, cr + 2, cr - 1, mix(px.get(cx, cy), 0xFFFFFFFF.toInt(), 0.7f))
                }
                for (y in 70 until n) for (x in 0 until n) px.set(x, y, mix(px.get(x, y), 0xFF7A8A6A.toInt(), 0.6f))
                px.poly(0xFFF08A2A.toInt(), 6, 74, 10, 60, 14, 74); px.rect(8, 66, 4, 2, 0xFFF4F4F0.toInt()); px.rect(4, 74, 12, 2, 0xFF3A3A3A.toInt())
            }
            Keeper.SHADE -> {
                // A buzzing neon sign down the alley.
                for (y in 0 until n) for (x in 0 until n) if (y % 7 == 6 || (x + (y / 7 % 2) * 7) % 14 == 13) dim(x, y, 0.8f)
                glow(16, 16, 20, 0xFFFF4AC8.toInt(), 0.35f)
                val neon = 0xFFFF8AE0.toInt()
                px.line(6, 8, 26, 8, neon); px.line(6, 22, 26, 22, neon); px.line(6, 8, 6, 22, neon); px.line(26, 8, 26, 22, neon)
                Sprites.fanLeaf(px, 16, 20, -PI / 2, 6, 0xFF6AF0B0.toInt(), 0xFFC8FFE8.toInt())
                glow(84, 70, 16, 0xFF4AC8FF.toInt(), 0.2f)
            }
        }
    }

    /** The bare shoulders line every portrait's clothes are cut from. */
    private val SHOULDERS = intArrayOf(2, 96, 4, 86, 11, 78, 26, 72, 40, 68, 56, 68, 70, 72, 85, 78, 92, 86, 94, 96)
    private fun mirrored(pts: IntArray) = IntArray(pts.size) { i -> if (i % 2 == 0) BIG - pts[i] else pts[i] }
    /** Lines through consecutive points, for inner contours the outline pass can't see. */
    private fun Px.trace(c: Int, vararg pts: Int) { for (i in 0 until pts.size / 2 - 1) line(pts[i * 2], pts[i * 2 + 1], pts[i * 2 + 2], pts[i * 2 + 3], c) }
    /** Blends [c] over what's drawn inside an ellipse, strongest in the middle. */
    private fun Px.tint(cx: Int, cy: Int, rx: Int, ry: Int, c: Int, a: Float) {
        for (y in cy - ry..cy + ry) for (x in cx - rx..cx + rx) {
            val d = (x - cx) * (x - cx) / (rx * rx + 0.5f) + (y - cy) * (y - cy) / (ry * ry + 0.5f)
            if (d <= 1f && get(x, y) != 0) set(x, y, mix(get(x, y), c, a * (1f - d * 0.6f)))
        }
    }
    /** Clothes over the shoulders, lit from the left. */
    private fun clothes(px: Px, c: Int) = px.poly(SHOULDERS) { x, y -> when { x > 70 -> shade(c, 0.8f); x < 22 && y < 88 -> shade(c, 1.15f); else -> c } }

    /** Neck, ears, a shaded face lit from the upper left, and the nose. [jaw] widens the chin. */
    private fun face(px: Px, skin: Int, jaw: Int = 0) {
        val l = shade(skin, 1.12f); val s = shade(skin, 0.86f); val d = shade(skin, 0.7f)
        px.poly(s, 41, 52, 55, 52, 56, 74, 40, 74)
        px.rect(41, 56, 15, 4, d)
        oval(px, 32, 41, 3, 6, s); oval(px, 33, 41, 1, 3, d)
        oval(px, 64, 41, 3, 6, shade(s, 0.92f)); oval(px, 63, 41, 1, 3, d)
        val h = jaw / 2
        val pts = intArrayOf(34, 24, 62, 24, 63, 36, 62 + h, 45, 59 + jaw, 51, 55 + jaw, 56, 51 + h, 59, 48, 60, 45 - h, 59, 41 - jaw, 56, 37 - jaw, 51, 34 - h, 45, 33, 36)
        px.poly(pts) { x, y ->
            when {
                x >= 59 || (x >= 54 && y >= 53) || y >= 58 -> s
                x <= 39 && y in 42..49 -> l
                else -> skin
            }
        }
        px.trace(d, 37 - jaw, 51, 41 - jaw, 56, 45 - h, 59, 48, 60, 51 + h, 59, 55 + jaw, 56, 59 + jaw, 51)
        px.tint(48, 30, 14, 3, s, 0.5f)
        px.line(50, 39, 51, 46, s); px.set(47, 45, l); px.set(48, 44, l)
        px.line(45, 48, 46, 48, d); px.line(50, 48, 51, 48, d); px.line(47, 49, 49, 49, s)
    }

    /** Both eyes, 10 px wide, top row at [y]; [lid] rows of upper lid come down for a sleepy look. */
    private fun eyes(px: Px, y: Int, skin: Int, iris: Int, lash: Int = 0xFF1E1418.toInt(), white: Int = 0xFFF6F2EE.toInt(), lid: Int = 0) {
        val rows = arrayOf(
            "..KKKKKK..",
            ".KWwIIIIwK",
            ".WWIPPHIW.",
            "..WIiPiIW.",
            "...ssss...",
        )
        val pal = mapOf(
            'K' to lash, 'W' to white, 'w' to shade(white, 0.86f), 'I' to iris, 'i' to mix(iris, 0xFFFFFFFF.toInt(), 0.3f),
            'P' to 0xFF100C0C.toInt(), 'H' to 0xFFFFFFFF.toInt(), 's' to shade(skin, 0.86f),
        )
        px.map(37, y, rows, pal); px.map(50, y, Array(rows.size) { rows[it].reversed() }, pal)
        for (k in 0 until lid) { px.line(38, y + k, 46, y + k, shade(skin, 0.92f)); px.line(50, y + k, 58, y + k, shade(skin, 0.92f)) }
        if (lid > 0) { px.line(38, y + lid, 46, y + lid, lash); px.line(50, y + lid, 58, y + lid, lash) }
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
        px.tint(48, 32, 15, 3, skinD, 0.35f); px.tint(44, 40, 4, 8, skinL, 0.12f)
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

    /** A rope of hair or cord from one point to another, [r] thick, banded every few pixels. */
    private fun Px.rope(x0: Int, y0: Int, x1: Int, y1: Int, r: Int, c: Int, band: Int) {
        val steps = maxOf(abs(x1 - x0), abs(y1 - y0), 1)
        for (i in 0..steps) {
            val x = x0 + (x1 - x0) * i / steps; val y = y0 + (y1 - y0) * i / steps
            disc(x, y, r, if (i % 4 == 0) band else c)
        }
    }

    /** Dex: dreadlocks under a big rasta tam, sleepy eyes, a full beard and a joint tucked behind his ear. */
    private fun dex(px: Px) {
        val skin = 0xFF94603C.toInt(); val dread = 0xFF3A2416.toInt(); val dreadL = 0xFF5A3A24.toInt()
        val hood = 0xFF3E7A3E.toInt()
        clothes(px, hood)
        // The hood bunched round his neck, drawstrings and a leaf on the chest.
        px.poly(shade(hood, 0.82f), 30, 74, 38, 66, 58, 66, 66, 74, 58, 80, 48, 82, 38, 80)
        px.poly(shade(hood, 0.62f), 38, 72, 58, 72, 54, 78, 42, 78)
        px.line(43, 78, 41, 90, 0xFFE8E0D0.toInt()); px.line(53, 78, 55, 90, 0xFFE8E0D0.toInt())
        px.rect(40, 90, 2, 2, 0xFFC8C0B0.toInt()); px.rect(55, 90, 2, 2, 0xFFC8C0B0.toInt())
        Sprites.fanLeaf(px, 64, 93, -PI / 2, 6, 0xFFF2C440.toInt(), 0xFFFFE890.toInt())
        // Locks falling past his shoulders, with a few beads.
        val locks = listOf(intArrayOf(31, 28, 22, 80), intArrayOf(28, 32, 14, 74), intArrayOf(34, 32, 30, 84), intArrayOf(26, 26, 10, 64))
        for (l in locks) {
            px.rope(l[0], l[1], l[2], l[3], 2, dread, dreadL)
            px.rope(BIG - l[0], l[1], BIG - l[2], l[3], 2, dread, dreadL)
        }
        for ((bx, by, bc) in listOf(Triple(23, 66, 0xFFE83A2A.toInt()), Triple(17, 60, 0xFFF2C440.toInt()), Triple(74, 70, 0xFF3AB84A.toInt()), Triple(80, 58, 0xFFF2C440.toInt()))) px.disc(bx, by, 1, bc)
        face(px, skin, jaw = 1)
        eyes(px, 37, skin, 0xFF3A2010.toInt(), white = 0xFFF0DCD4.toInt(), lid = 2)
        val brow = 0xFF24160E.toInt()
        px.rect(36, 34, 10, 2, brow); px.rect(50, 34, 10, 2, brow)
        // Beard round the jaw, moustache and a lazy grin.
        val beard = 0xFF2A1A10.toInt()
        px.poly(intArrayOf(33, 42, 36, 51, 40, 57, 45, 61, 48, 62, 51, 61, 56, 57, 60, 51, 63, 42, 60, 49, 56, 54, 52, 56, 44, 56, 40, 54, 36, 49)) { x, y -> if ((x + y) % 4 == 0) dreadL else beard }
        px.poly(beard, 42, 52, 46, 49, 50, 49, 54, 52, 52, 51, 44, 51)
        px.line(45, 53, 52, 53, 0xFF5A2A20.toInt()); px.set(53, 52, 0xFF5A2A20.toInt())
        px.line(46, 54, 51, 54, 0xFFB06A58.toInt())
        // Rasta tam: red, gold and green, with a ribbed brim.
        val r = 0xFFD83A2A.toInt(); val g = 0xFFF2C440.toInt(); val gr = 0xFF2E9A44.toInt()
        val tam = intArrayOf(26, 28, 22, 18, 26, 8, 36, 2, 48, 0, 62, 2, 72, 8, 76, 18, 72, 28)
        px.poly(tam) { x, y ->
            val c = when { y < 9 -> r; y < 17 -> g; else -> gr }
            when { x > 64 -> shade(c, 0.8f); x < 34 && y > 6 -> shade(c, 1.1f); (x + y / 3) % 7 == 0 -> shade(c, 0.9f); else -> c }
        }
        px.poly(intArrayOf(29, 32, 28, 25, 68, 25, 67, 32)) { x, _ -> if (x % 2 == 0) shade(gr, 0.7f) else shade(gr, 0.85f) }
        px.line(29, 25, 67, 25, shade(gr, 0.55f))
        // A joint behind his ear, still smoking.
        px.line(63, 33, 72, 28, 0xFFF4F0E4.toInt()); px.line(63, 34, 72, 29, 0xFFDAD4C4.toInt())
        px.set(73, 28, 0xFFFF7A2A.toInt()); px.set(73, 29, 0xFFC8401A.toInt())
        for ((sx, sy) in listOf(74 to 25, 75 to 22, 74 to 19, 76 to 16, 77 to 13)) px.set(sx, sy, 0xFFC8C8C8.toInt())
    }

    /** Hank: a red trucker cap, a white walrus moustache, flannel under denim overalls. */
    private fun hank(px: Px) {
        val skin = 0xFFE0A07C.toInt(); val red = 0xFFB0342C.toInt(); val denim = 0xFF3A5A8A.toInt()
        val gold = 0xFFF2C440.toInt(); val grey = 0xFFD8D4CC.toInt()
        px.poly(SHOULDERS) { x, y ->
            var c = red
            if (x % 10 < 2) c = shade(c, 0.7f)
            if (y % 10 < 2) c = shade(c, 0.72f)
            if ((x % 10 < 2) && (y % 10 < 2)) c = 0xFF2A1A1A.toInt()
            if (x > 70) shade(c, 0.82f) else c
        }
        px.poly(0xFFF4F0E8.toInt(), 40, 68, 56, 68, 52, 78, 44, 78)
        px.poly(shade(red, 1.15f), 36, 68, 44, 68, 46, 79, 38, 76)
        px.poly(shade(red, 0.9f), 60, 68, 52, 68, 50, 79, 58, 76)
        // Overalls: bib, pocket, straps and buckles.
        px.poly(intArrayOf(32, 96, 33, 84, 63, 84, 64, 96)) { x, _ -> if (x > 56) shade(denim, 0.8f) else denim }
        px.rect(42, 87, 12, 7, shade(denim, 0.85f)); px.line(42, 87, 53, 87, shade(denim, 1.3f))
        for (o in 0..3) { px.line(22 + o, 74, 33 + o, 86, denim); px.line(74 - o, 74, 63 - o, 86, shade(denim, 0.8f)) }
        px.rect(33, 84, 4, 3, gold); px.rect(60, 84, 4, 3, gold)
        face(px, skin, jaw = 1)
        // Weathered: squint, crow's feet, sideburns and bushy brows.
        eyes(px, 37, skin, 0xFF3A70B8.toInt(), lid = 1)
        val crease = shade(skin, 0.78f)
        px.line(33, 38, 35, 39, crease); px.line(33, 41, 35, 40, crease); px.line(63, 38, 61, 39, crease); px.line(63, 41, 61, 40, crease)
        px.line(40, 31, 56, 31, crease)
        px.rect(33, 28, 3, 16, grey); px.rect(61, 28, 3, 16, shade(grey, 0.85f))
        px.poly(grey, 36, 35, 38, 33, 46, 33, 46, 36, 41, 35); px.poly(grey, 60, 35, 58, 33, 50, 33, 50, 36, 55, 35)
        px.tint(39, 47, 3, 2, 0xFFE05A50.toInt(), 0.35f); px.tint(57, 47, 3, 2, 0xFFE05A50.toInt(), 0.35f)
        px.poly(intArrayOf(40, 50, 44, 48, 52, 48, 56, 50, 59, 56, 56, 57, 53, 53, 43, 53, 40, 57, 37, 56)) { x, y -> if ((x + y) % 3 == 0) 0xFFC8C4BC.toInt() else 0xFFF4F2EE.toInt() }
        px.line(45, 55, 51, 55, shade(skin, 0.68f))
        // Trucker cap with an H on the front.
        val cap = intArrayOf(31, 27, 32, 15, 38, 8, 48, 6, 58, 8, 64, 15, 65, 27)
        px.poly(cap) { x, y -> if ((x + y) % 2 == 0) shade(red, 0.85f) else red }
        px.poly(0xFFF4F0E8.toInt(), 38, 26, 38, 13, 43, 8, 53, 8, 58, 13, 58, 26)
        px.rect(44, 13, 2, 9, red); px.rect(51, 13, 2, 9, red); px.rect(46, 16, 5, 2, red)
        px.poly(intArrayOf(28, 26, 68, 26, 72, 31, 24, 31)) { x, _ -> if (x > 60) shade(red, 0.7f) else shade(red, 0.85f) }
        px.line(28, 26, 68, 26, shade(red, 0.6f)); px.set(48, 5, red)
        // A carpenter's pencil behind his ear.
        for (o in 0..1) px.line(62, 36 + o, 71, 31 + o, 0xFFF2C440.toInt())
        px.set(72, 31, 0xFF3A3A3A.toInt()); px.set(61, 37, 0xFFE89AA0.toInt())
    }

    /** Mona: a blonde bob, navy blazer, pearls and red lipstick. */
    private fun mona(px: Px) {
        val skin = 0xFFF0C8A6.toInt(); val hair = 0xFFE8C060.toInt(); val hairL = 0xFFFFE498.toInt(); val hairD = 0xFFB88A34.toInt()
        val navy = 0xFF2A3A6A.toInt()
        px.poly(hairD, 22, 62, 22, 40, 26, 22, 34, 10, 48, 5, 62, 10, 70, 22, 74, 40, 74, 62)
        clothes(px, navy)
        px.poly(0xFFF6F2EC.toInt(), 40, 68, 56, 68, 55, 82, 48, 92, 41, 82)
        px.poly(shade(navy, 1.3f), 34, 70, 41, 69, 48, 92, 38, 86, 32, 76)
        px.poly(shade(navy, 1.05f), 62, 70, 55, 69, 48, 92, 58, 86, 64, 76)
        px.trace(shade(navy, 0.6f), 41, 69, 48, 92, 55, 69)
        for (x in 40..56 step 2) { val t = (x - 48) / 8f; val y = 70 + (6 * (1 - t * t)).toInt(); px.set(x, y, 0xFFFFFFFF.toInt()); px.set(x + 1, y, 0xFFC8C0C8.toInt()) }
        px.rect(66, 82, 5, 6, 0xFFF2C440.toInt()); px.set(67, 83, 0xFFFFF0A0.toInt())
        face(px, skin)
        px.line(38, 36, 46, 36, mix(skin, 0xFF8A6AC0.toInt(), 0.45f)); px.line(50, 36, 58, 36, mix(skin, 0xFF8A6AC0.toInt(), 0.45f))
        eyes(px, 37, skin, 0xFF4A86C8.toInt())
        px.set(37, 37, 0xFF1E1418.toInt()); px.set(59, 37, 0xFF1E1418.toInt())
        val brow = 0xFF9A7030.toInt()
        px.line(37, 34, 40, 32, brow); px.line(41, 32, 45, 32, brow); px.line(59, 34, 56, 32, brow); px.line(55, 32, 51, 32, brow)
        px.tint(39, 48, 4, 2, 0xFFF06A86.toInt(), 0.3f); px.tint(57, 48, 4, 2, 0xFFF06A86.toInt(), 0.3f)
        px.rect(45, 52, 7, 1, 0xFFA82030.toInt()); px.set(48, 52, 0xFF801828.toInt())
        px.rect(46, 53, 5, 1, 0xFFD83A48.toInt()); px.set(47, 53, 0xFFF08A90.toInt())
        px.set(56, 51, 0xFF5A3020.toInt())
        // The bob, parted on the right and curling in at the jaw, with a shine on the crown.
        val front = intArrayOf(26, 62, 24, 44, 26, 28, 32, 14, 42, 7, 54, 7, 64, 12, 70, 24, 72, 44, 72, 62, 64, 62, 63, 48, 62, 34, 58, 27, 50, 25, 42, 28, 37, 34, 35, 46, 34, 62)
        px.poly(front) { x, y -> val c = when ((abs(x - 54) / 2 + y / 10) % 5) { 0 -> hairD; 2 -> hairL; else -> hair }; if (x > 64) shade(c, 0.86f) else c }
        px.trace(hairD, 34, 62, 35, 46, 37, 34, 42, 28, 50, 25, 58, 27, 62, 34, 63, 48, 64, 62)
        px.line(54, 8, 56, 25, hairD)
        for (y in 9..22) for (x in 28..62) {
            val dx = (x - 46) / 17f; val dy = (y - 24) / 15f
            val d = dx * dx + dy * dy
            if (d in 0.8f..0.92f && (x / 3) % 3 != 0 && px.get(x, y) != 0) px.set(x, y, if (d > 0.88f) hairL else 0xFFFFF4D0.toInt())
        }
    }

    /** Gus: short twists, a big grin and goatee, an open hi-vis vest and his clipboard. */
    private fun gus(px: Px) {
        val skin = 0xFF6A4028.toInt(); val vest = 0xFFF07A1A.toInt(); val strip = 0xFFE8E8E0.toInt()
        clothes(px, 0xFF7A7A82.toInt())
        px.poly(shade(0xFF7A7A82.toInt(), 0.8f), 40, 68, 56, 68, 52, 74, 44, 74)
        val left = intArrayOf(2, 96, 4, 86, 11, 78, 26, 72, 38, 69, 42, 96)
        val paint: (Int, Int) -> Int = { x, y -> if (y in 82..84 || y in 90..92) (if (x > 60) shade(strip, 0.85f) else strip) else if (x > 60) shade(vest, 0.82f) else vest }
        px.poly(left, paint); px.poly(mirrored(left), paint)
        px.trace(shade(vest, 0.6f), 38, 69, 42, 96); px.trace(shade(vest, 0.6f), 58, 69, 54, 96)
        px.rect(24, 80, 2, 6, 0xFF2A5AC8.toInt()); px.set(24, 79, 0xFFF4F4F0.toInt())
        face(px, skin, jaw = 1)
        eyes(px, 37, skin, 0xFF2A1A10.toInt())
        val brow = 0xFF140C08.toInt()
        px.line(37, 34, 45, 33, brow); px.line(37, 35, 45, 34, brow); px.line(51, 33, 59, 34, brow); px.line(51, 34, 59, 35, brow)
        // A big toothy grin and a goatee.
        px.poly(0xFFF8F4EC.toInt(), 41, 51, 55, 51, 53, 55, 48, 57, 43, 55)
        for (x in intArrayOf(44, 47, 50, 53)) px.set(x, 52, 0xFFC8C0B8.toInt())
        px.line(42, 54, 54, 54, 0xFFD0C8C0.toInt())
        px.trace(0xFF3A1A10.toInt(), 40, 50, 41, 51, 55, 51, 56, 50)
        px.trace(0xFF3A1A10.toInt(), 41, 51, 43, 55, 48, 57, 53, 55, 55, 51)
        px.set(39, 49, shade(skin, 0.78f)); px.set(57, 49, shade(skin, 0.78f))
        px.poly(0xFF140C08.toInt(), 45, 58, 51, 58, 50, 62, 46, 62)
        // Short twists on top with a fade at the sides.
        oval(px, 48, 20, 16, 9, 0xFF1A1210.toInt())
        px.rect(33, 22, 31, 4, 0xFF1A1210.toInt())
        for (y in 12..25 step 3) for (x in 33..63 step 3) if (px.get(x, y) == 0xFF1A1210.toInt()) { px.set(x, y, 0xFF3A2A20.toInt()); px.set(x + 1, y + 1, 0xFF2A1E18.toInt()) }
        px.rect(33, 26, 2, 12, mix(skin, 0xFF1A1210.toInt(), 0.6f)); px.rect(62, 26, 2, 12, mix(skin, 0xFF1A1210.toInt(), 0.6f))
        // The clipboard he's always holding.
        px.rect(66, 76, 20, 20, 0xFFA07040.toInt()); px.rect(68, 80, 16, 16, 0xFFF6F4EC.toInt())
        px.rect(72, 74, 8, 5, 0xFFB8B8C0.toInt()); px.rect(74, 73, 4, 2, 0xFF8A8A92.toInt())
        for (y in 83..93 step 3) px.line(70, y, 81, y, 0xFF9AA8C0.toInt())
        px.set(70, 83, 0xFF3AB84A.toInt()); px.set(70, 86, 0xFF3AB84A.toInt())
    }

    /** Shade: deep hood, wraparound shades, a smirk with a gold tooth and a chain. */
    private fun hooded(px: Px) {
        val skin = 0xFFB07A58.toInt(); val hood = 0xFF2C2A38.toInt(); val gold = 0xFFF2C440.toInt()
        clothes(px, hood)
        px.poly(intArrayOf(18, 96, 20, 62, 22, 38, 28, 20, 38, 8, 48, 5, 58, 8, 68, 20, 74, 38, 76, 62, 78, 96)) { x, _ -> if (x > 64) shade(hood, 0.8f) else if (x < 28) shade(hood, 1.15f) else hood }
        oval(px, 48, 44, 18, 27, 0xFF0C0A10.toInt())
        face(px, skin, jaw = 1)
        px.tint(48, 30, 18, 12, 0xFF0C0A10.toInt(), 0.65f)
        for (y in 50..61) for (x in 36..60) if ((x + y) % 3 == 0 && (x / 2 + y) % 2 == 0 && px.get(x, y) != 0 && px.get(x, y) != 0xFF0C0A10.toInt()) px.set(x, y, shade(px.get(x, y), 0.8f))
        // Hood rim shading the top of his face.
        px.poly(intArrayOf(29, 42, 30, 24, 38, 13, 48, 10, 58, 13, 66, 24, 67, 42, 63, 28, 56, 21, 48, 19, 40, 21, 33, 28)) { x, _ -> if (x > 58) shade(hood, 0.95f) else shade(hood, 1.35f) }
        // Wraparound shades, purple fading to cyan, with a glint.
        px.poly(intArrayOf(33, 36, 63, 36, 62, 42, 56, 44, 50, 41, 46, 41, 40, 44, 34, 42)) { x, y ->
            val c = mix(0xFF7A2AD0.toInt(), 0xFF2AC8F0.toInt(), (x - 33) / 30f)
            shade(c, 0.55f + (y - 36) * 0.06f)
        }
        px.line(33, 35, 63, 35, 0xFF141018.toInt())
        px.set(37, 37, 0xFFFFFFFF.toInt()); px.set(38, 38, 0xFFE0E0FF.toInt()); px.set(53, 37, 0xFFFFFFFF.toInt())
        // Smirk with a gold tooth.
        px.line(43, 52, 51, 52, 0xFF3A1A14.toInt()); px.line(52, 51, 54, 50, 0xFF3A1A14.toInt())
        px.rect(46, 53, 5, 1, 0xFF5A2A20.toInt()); px.set(49, 53, gold)
        // Drawstrings and a heavy chain.
        px.line(41, 66, 39, 86, 0xFFC8C8D0.toInt()); px.line(55, 66, 57, 86, 0xFFC8C8D0.toInt())
        px.rect(38, 86, 2, 3, 0xFF9A9AA8.toInt()); px.rect(57, 86, 2, 3, 0xFF9A9AA8.toInt())
        for (x in 34..62) { val t = (x - 48) / 14f; px.set(x, 72 + (10 * (1 - t * t)).toInt(), if (x % 2 == 0) gold else 0xFFB88A24.toInt()) }
        px.disc(48, 85, 3, gold); Sprites.fanLeaf(px, 48, 87, -PI / 2, 3, 0xFF3AB84A.toInt(), 0xFF8AE08A.toInt())
    }
}
