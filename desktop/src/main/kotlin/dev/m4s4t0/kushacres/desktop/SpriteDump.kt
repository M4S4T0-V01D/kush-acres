package dev.m4s4t0.kushacres.desktop

import dev.m4s4t0.kushacres.art.DecorArt
import dev.m4s4t0.kushacres.art.People
import dev.m4s4t0.kushacres.model.Decor
import dev.m4s4t0.kushacres.art.Px
import dev.m4s4t0.kushacres.model.Farmer
import dev.m4s4t0.kushacres.model.Helper
import dev.m4s4t0.kushacres.model.Pet
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** A developer aid: writes the people and pet sprites to one PNG sheet (set KUSH_SPRITES to the file to write). */
internal fun dumpSprites(path: String) {
    val sheet = ArrayList<Px>()
    for (h in Helper.entries) for (fr in 0..1) sheet += People.figurePx(Farmer.random(kotlin.random.Random(h.ordinal)), fr)
    for (i in 0 until 12) sheet += People.figurePx(Farmer("", i % 6, i % Farmer.HAIRS.size, i % 9, i % Farmer.OUTFITS.size, i, (i + 3) % 12, i % Farmer.HATS.size, i % Farmer.FACES.size), i % 2)
    for (p in Pet.entries) for (fr in 0..1) sheet += People.petPx(p, fr)
    val cell = 56; val cols = 11; val scale = 2
    val rows = (sheet.size + cols - 1) / cols
    val img = BufferedImage(cols * cell * scale, rows * cell * scale, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics(); g.color = java.awt.Color(0x6A, 0x9A, 0x4A); g.fillRect(0, 0, img.width, img.height); g.dispose()
    sheet.forEachIndexed { n, px ->
        val ox = (n % cols) * cell * scale; val oy = (n / cols) * cell * scale
        for (y in 0 until px.fh) for (x in 0 until px.fw) {
            val c = px.p[y * px.fw + x]
            if (c ushr 24 == 0) continue
            for (j in 0 until scale) for (i in 0 until scale) img.setRGB(ox + x * scale + i, oy + y * scale + j, c)
        }
    }
    ImageIO.write(img, "png", File(path))
    // Decor on its own sheet next to it.
    val decor = Decor.entries.map { DecorArt.sprite(it) }
    val dc = 72; val dcols = 10
    val dimg = BufferedImage(dcols * dc * scale, (decor.size + dcols - 1) / dcols * dc * scale, BufferedImage.TYPE_INT_ARGB)
    dimg.createGraphics().apply { color = java.awt.Color(0x6A, 0x9A, 0x4A); fillRect(0, 0, dimg.width, dimg.height); dispose() }
    decor.forEachIndexed { n, b ->
        val ox = (n % dcols) * dc * scale; val oy = (n / dcols) * dc * scale
        for (y in 0 until b.height) for (x in 0 until b.width) {
            val c = b.getPixel(x, y)
            if (c ushr 24 == 0) continue
            for (j in 0 until scale) for (i in 0 until scale) dimg.setRGB(ox + x * scale + i, oy + y * scale + j, c)
        }
    }
    ImageIO.write(dimg, "png", File(path.removeSuffix(".png") + "-decor.png"))
    // And a share card for a fresh farm with a few plants in.
    val now = System.currentTimeMillis()
    val f = dev.m4s4t0.kushacres.model.Farm(kotlin.random.Random(5)).apply { newGame(now); cash = 1000L }
    for (y in f.oy until f.oy + f.land) for (x in f.ox until f.ox + f.land) if (f.tile(x, y).kind == dev.m4s4t0.kushacres.model.Kind.PLOT)
        f.plant(x, y, dev.m4s4t0.kushacres.model.Strains.all.first().id, now)
    val card = dev.m4s4t0.kushacres.ui.ShareCard.render(f)
    val cimg = BufferedImage(card.width, card.height, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until card.height) for (x in 0 until card.width) cimg.setRGB(x, y, card.getPixel(x, y))
    ImageIO.write(cimg, "png", File(path.removeSuffix(".png") + "-card.png"))
}
