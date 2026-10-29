package dev.m4s4t0.kushacres.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.max

/** Where a scroller is: the thumb's share of the track, how far along it is (0..1), and its whole length and view in px. */
private class Span(val frac: Float, val pos: Float, val content: Float, val view: Float)

private val GUTTER = 7.dp
private val BAR = 3.dp
/** How wide a strip along the bar takes a drag, for a mouse or a thumb. */
private val GRAB = 16.dp

/** A column that scrolls, with a [scrollbar] on its right. */
@Composable
fun Modifier.vScroll(state: ScrollState = rememberScrollState()): Modifier = scrollbar(state).verticalScroll(state)

/** A row that scrolls, with a [scrollbar] along its bottom. */
@Composable
fun Modifier.hScroll(state: ScrollState = rememberScrollState()): Modifier = scrollbar(state, vertical = false).horizontalScroll(state)

/**
 * A tiny accent-coloured scrollbar on the right edge (or the bottom with [vertical] off) of a scroller, in a gutter of its own so
 * it never covers the content. Put it before the scroll modifier (or on a lazy list's modifier). It hides when everything fits;
 * the strip along it can be dragged, and on a horizontal one an ordinary mouse wheel scrolls sideways.
 */
fun Modifier.scrollbar(state: ScrollState, vertical: Boolean = true): Modifier = bar(state, vertical) {
    val view = state.viewportSize.toFloat()
    if (state.maxValue <= 0 || state.maxValue == Int.MAX_VALUE || view <= 0) null
    else Span(view / (view + state.maxValue), state.value / state.maxValue.toFloat(), view + state.maxValue, view)
}

fun Modifier.scrollbar(state: LazyListState, vertical: Boolean = true): Modifier = bar(state, vertical) {
    val info = state.layoutInfo
    val seen = info.visibleItemsInfo
    val view = (if (vertical) info.viewportSize.height else info.viewportSize.width).toFloat() - info.beforeContentPadding - info.afterContentPadding
    if (seen.isEmpty() || view <= 0) return@bar null
    val step = seen.sumOf { it.size }.toFloat() / seen.size + info.mainAxisItemSpacing
    span(info.totalItemsCount * step - info.mainAxisItemSpacing, view, state.firstVisibleItemIndex * step + state.firstVisibleItemScrollOffset)
}

fun Modifier.scrollbar(state: LazyGridState, vertical: Boolean = true): Modifier = bar(state, vertical) {
    val info = state.layoutInfo
    val seen = info.visibleItemsInfo
    val view = (if (vertical) info.viewportSize.height else info.viewportSize.width).toFloat() - info.beforeContentPadding - info.afterContentPadding
    if (seen.isEmpty() || view <= 0) return@bar null
    val lines = max(1, seen.map { if (vertical) it.column else it.row }.distinct().size)
    val step = seen.maxOf { if (vertical) it.size.height else it.size.width } + info.mainAxisItemSpacing.toFloat()
    val first = seen.minOf { if (vertical) it.row else it.column }
    val top = seen.first { (if (vertical) it.row else it.column) == first }
    val off = -(if (vertical) top.offset.y else top.offset.x).toFloat()
    span(ceil(info.totalItemsCount / lines.toFloat()) * step - info.mainAxisItemSpacing, view, first * step + off)
}

private fun span(content: Float, view: Float, scrolled: Float): Span? {
    if (content <= view + 1) return null
    return Span(view / content, (scrolled / (content - view)).coerceIn(0f, 1f), content, view)
}

private fun Modifier.bar(state: ScrollableState, vertical: Boolean, measure: () -> Span?): Modifier = this
    .drawWithContent {
        drawContent()
        val s = measure() ?: return@drawWithContent
        val track = if (vertical) size.height else size.width
        val len = (track * s.frac).coerceIn(14.dp.toPx(), track)
        val at = (track - len) * s.pos
        val w = BAR.toPx(); val inset = (GUTTER.toPx() - w) / 2
        val r = CornerRadius(w / 2)
        if (vertical) {
            val x = size.width - inset - w
            drawRoundRect(Gold.copy(alpha = 0.15f), Offset(x, 0f), Size(w, track), r)
            drawRoundRect(Gold.copy(alpha = 0.9f), Offset(x, at), Size(w, len), r)
        } else {
            val y = size.height - inset - w
            drawRoundRect(Gold.copy(alpha = 0.15f), Offset(0f, y), Size(track, w), r)
            drawRoundRect(Gold.copy(alpha = 0.9f), Offset(at, y), Size(len, w), r)
        }
    }
    .pointerInput(state, vertical) {
        awaitPointerEventScope {
            var dragging = false
            while (true) {
                // Seen on the way down, before the scroller under it, so a drag on the strip moves the bar and not the list.
                val e = awaitPointerEvent(PointerEventPass.Initial)
                val c = e.changes.firstOrNull() ?: continue
                if (e.type == PointerEventType.Scroll && !vertical && c.scrollDelta.x == 0f && c.scrollDelta.y != 0f) {
                    state.dispatchRawDelta(c.scrollDelta.y * 48.dp.toPx())
                    c.consume(); continue
                }
                val edge = if (vertical) size.width - c.position.x else size.height - c.position.y
                if (c.pressed && !c.previousPressed) dragging = edge <= GRAB.toPx() && measure() != null
                if (!c.pressed) { dragging = false; continue }
                if (!dragging) continue
                val s = measure() ?: continue
                val track = (if (vertical) size.height else size.width).toFloat()
                val len = (track * s.frac).coerceIn(14.dp.toPx(), track)
                val moved = if (vertical) c.position.y - c.previousPosition.y else c.position.x - c.previousPosition.x
                if (moved != 0f && track > len) state.dispatchRawDelta(moved * (s.content - s.view) / (track - len))
                c.consume()
            }
        }
    }
    .padding(end = if (vertical) GUTTER else 0.dp, bottom = if (vertical) 0.dp else GUTTER)

/** A [LazyColumn] with a [scrollbar]. */
@Composable
fun BarColumn(modifier: Modifier = Modifier, verticalArrangement: Arrangement.Vertical = Arrangement.Top, content: LazyListScope.() -> Unit) {
    val s = rememberLazyListState()
    LazyColumn(modifier.scrollbar(s), s, verticalArrangement = verticalArrangement, content = content)
}

/** A [LazyRow] with a [scrollbar] along the bottom. */
@Composable
fun BarRow(modifier: Modifier = Modifier, horizontalArrangement: Arrangement.Horizontal = Arrangement.Start, content: LazyListScope.() -> Unit) {
    val s = rememberLazyListState()
    LazyRow(modifier.scrollbar(s, vertical = false), s, horizontalArrangement = horizontalArrangement, content = content)
}

/** A [LazyVerticalGrid] with a [scrollbar]. */
@Composable
fun BarGrid(
    columns: GridCells, modifier: Modifier = Modifier, horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top, content: LazyGridScope.() -> Unit,
) {
    val s = rememberLazyGridState()
    LazyVerticalGrid(columns, modifier.scrollbar(s), s, verticalArrangement = verticalArrangement, horizontalArrangement = horizontalArrangement, content = content)
}
