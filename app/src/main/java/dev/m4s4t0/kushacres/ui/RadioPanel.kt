package dev.m4s4t0.kushacres.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.kushacres.Store
import dev.m4s4t0.kushacres.audio.Audio
import dev.m4s4t0.kushacres.audio.Radio
import dev.m4s4t0.kushacres.audio.Style

private val Lofi = Color(0xFFC9A2FF)

private fun styleColor(s: Style) = when (s) {
    Style.REGGAE -> Leaf; Style.DUB -> Sky; Style.LOFI -> Lofi; Style.HOUSE -> Gold; Style.ITALO -> Color(0xFFFF7EC8)
    Style.SYNTHPOP -> Color(0xFF6FE0E0); Style.DARKWAVE -> Color(0xFFA08CB8); Style.JUNGLE -> Color(0xFFFF9A4D)
    Style.FUSION -> Color(0xFFFFD27A); Style.CHIP -> Color(0xFF9BE15D)
}

/** The radio: what's on now, the two stations, and every track, the locked ones greyed with how to get them. */
@Composable
internal fun RadioPanel(ui: UiState) {
    val f = Store.farm
    Store.rev
    val area = f.site.def.id
    val now = Radio.pick(area, f, System.currentTimeMillis())
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().frame(WoodIn, WoodEdge).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            T("♪", 22.sp, Gold)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                T("Now playing", 10.sp, Dim, bold = false)
                T(Radio[now]?.name ?: now, 15.sp, Cream)
            }
            T("${Radio.owned(f).size}/${Radio.tracks.size}", 11.sp, Dim)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            for ((id, label) in listOf(Radio.LOCAL to "Local tunes", Radio.SHUFFLE to "Shuffle")) {
                Btn(label, Modifier.weight(1f), color = if (Audio.station == id) Leaf else WoodEdge) { Audio.changeStation(id) }
                Spacer(Modifier.width(6.dp))
            }
        }
        T(if (Audio.station == Radio.LOCAL) "Every farm plays its own tune" else if (Audio.station == Radio.SHUFFLE) "A new track from your collection every few minutes"
            else "Playing one track everywhere", 10.sp, Dim, bold = false)
        if (Audio.music == 0f) T("Music is turned off in Settings", 10.sp, Bad)
        Spacer(Modifier.height(8.dp))
        for (t in Radio.tracks) {
            val has = Radio.has(f, t.id)
            val on = Audio.station == t.id
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp).frame(if (on) Color(0xFF3A4A22) else WoodIn, if (on) Leaf else WoodEdge)
                    .clickable(enabled = has) { Audio.changeStation(if (on) Radio.LOCAL else t.id) }.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                T(if (on) "▶" else if (has) "♪" else "•", 13.sp, if (on) Leaf else if (has) Cream else Dim)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    T(t.name, 13.sp, if (has) Cream else Dim)
                    if (!has) T(t.how, 10.sp, Dim, bold = false)
                }
                T(Radio.styleName(t.style), 10.sp, styleColor(t.style).let { if (has) it else Dim })
            }
        }
    }
}
