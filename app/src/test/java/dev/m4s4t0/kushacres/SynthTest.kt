package dev.m4s4t0.kushacres

import dev.m4s4t0.kushacres.audio.Amb
import dev.m4s4t0.kushacres.audio.Ambience
import dev.m4s4t0.kushacres.audio.Sfx
import dev.m4s4t0.kushacres.audio.Synth
import dev.m4s4t0.kushacres.audio.Tunes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SynthTest {
    private fun check(name: String, x: FloatArray) {
        assertTrue("$name is empty", x.isNotEmpty())
        assertTrue("$name has NaN", x.none { it.isNaN() })
        val peak = x.maxOf { kotlin.math.abs(it) }
        assertTrue("$name peak $peak", peak in 0.2f..0.85f)
        // Written out so they can be listened to: app/build/sounds/*.wav
        File("build/sounds").apply { mkdirs() }.resolve("$name.wav").writeBytes(Synth.wav(x))
    }

    @Test fun effects() { for (s in Sfx.entries) check(s.name.lowercase(), Synth.sfx(s)) }

    @Test fun ambience() { for (a in Amb.entries) check("amb-" + a.name.lowercase(), Ambience.loop(a).also { assertEquals(Synth.RATE * 16, it.size) }) }

    @Test fun music() {
        for (id in Tunes.areas + "somewhere-new") {
            val t = Tunes.of(id)
            val m = Synth.music(t)
            assertEquals(16 * 4 * 60.0 / t.bpm, m.size / Synth.RATE.toDouble(), 0.01)
            check("music-$id", m)
            assertTrue(kotlin.math.abs(m.first()) < 0.01f && kotlin.math.abs(m.last()) < 0.01f)
        }
    }

    @Test fun everyAreaHasItsOwnTune() {
        val areas = listOf("home", "town") + (dev.m4s4t0.kushacres.model.Sites.all + dev.m4s4t0.kushacres.model.Sites.holidays + dev.m4s4t0.kushacres.model.Sites.premium).map { it.id }
        for (a in areas) assertTrue("no tune for $a", a in Tunes.areas)
        val sounds = Tunes.areas.map { Tunes.of(it).let { t -> listOf(t.root, t.bpm, t.minor, t.prog.toList(), t.lead) } }
        assertEquals(sounds.size, sounds.toSet().size)
    }

    @Test fun wavHeader() {
        val w = Synth.wav(FloatArray(10))
        assertEquals("RIFF", String(w, 0, 4)); assertEquals(64, w.size)
    }
}
