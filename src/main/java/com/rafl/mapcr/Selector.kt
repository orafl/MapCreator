package com.rafl.mapcr

import java.awt.Point
import kotlin.math.sign

interface Selectable {
    fun onSelect()
    fun unselect()
}

class Selector<T: Selectable>(var maxWidth: Int = 1) {
    var origin: Point? = null
    var destiny: Point? = null
    private val selected = mutableSetOf<T>()

    fun selectionSize() = selected.size

    inline fun scanSelection(f: (Int, Int) -> Unit) {
        val o = origin ?: return
        val d = destiny ?: return Unit.also { f (o.x, o.y) }
        val dx = d.x - o.x; val dy = d.y - o.y
        var yy = 0

        if (dy == 0) {
            var xx = 0
            while (xx != dx + dx.sign) {
                f(xx, yy)
                xx += dx.sign
            }
        }
        else while (yy != dy + dy.sign) {
            if (dx == 0) f(0, yy)
            else {
                var xx = 0
                while (xx != dx + dx.sign) {
                    f(xx, yy)
                    xx += dx.sign
                }
            }
            yy += dy.sign
        }
    }

    fun select(t: T) { selected += t }
    fun isSelected(t: T) = t !in selected

    inline fun applySelection(startX: Int, startY: Int, at: (Int, Int) -> T?) {
        val o = origin
        if (o == null) {
            origin = Point(startX, startY)
        } else {
            if (isSelected(at(startX, startY) ?: return)) {
                destiny = Point(startX, startY)
                scanSelection { xi, yi ->
                    val s = at(o.x + xi, o.y + yi) ?: return@scanSelection
                    select(s)
                    s.onSelect()
                }
            }
        }
    }

    fun refresh() {
        origin = null
        destiny = null
        selected.forEach { it.unselect() }
        selected.clear()
    }
}