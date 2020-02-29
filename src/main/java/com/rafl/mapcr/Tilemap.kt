package com.rafl.mapcr

import javafx.animation.AnimationTimer
import javafx.scene.Node
import javafx.scene.canvas.Canvas
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import java.awt.Point
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

class Tilemap(val tileSize: Int, val width: Int, val height: Int) {

    private var scale = 1.0
    private fun newLayer() = Canvas(width.toDouble() * tileSize, height.toDouble() * tileSize)
    private val darkness = newLayer()
    val view = Pane(darkness)
    private var isCtrlDown = false
    private val tilesets = HashMap<String, Tileset>()
    private var tilesetKey: String? = null
    private val t = ArrayList<String>()

    val tilesetsUsed: List<String> get() = t

    fun setTileset(name: String, tileset: Tileset) {
        if (tilesets[name] != tileset)tilesets[name] = tileset
        if (name !in t) t.add(name)
        this.tilesetKey = name
    }

    fun setNoTileset() { tilesetKey = null }

    private class MapCell : Selectable {
        override fun onSelect() {}
        override fun unselect() {}
    }

    private val placement = Selector<MapCell>(width).apply {origin = Point(0, 0)}

    private var current = -1
    private val layers = CopyOnWriteArrayList<Layer>()

    private val animationTimer = object : AnimationTimer() {
        override fun handle(now: Long) {
            layers.forEach { it.drawOn(this@Tilemap) }
            if (isCtrlDown)
                layers.lastOrNull()?.view?.graphicsContext2D?.apply {
                    stroke = Color.RED
                    val o = placement.origin ?: return
                    val d = placement.destiny ?: return
                    val k = tileSize*scale
                    val xx = min(d.x, o.x).toDouble() * k
                    val yy = min(d.y, o.y).toDouble() * k
                    val dx = abs(d.x - o.x).toDouble() * k
                    val dy = abs(d.y - o.y).toDouble() * k
                    strokeRect(xx, yy, dx+2*k, dy+2*k)
                }
        }
    }

    fun createLayer() {
        val layer = Layer(this)
        layers.add(layer)
        view.children.add(layer.view)
        layer.view.toFront()
    }

    init {
        darkness.graphicsContext2D.apply {
            fill = Color.BLACK
            fillRect(0.0, 0.0, darkness.width, darkness.height)
        }
        mapControls()
        animationTimer.start()
    }

    class Layer(val view: Canvas, val contents: Array<ImageView?>)  {
        val realContents = IntArray(contents.size) { -1 }

        constructor(tilemap: Tilemap) : this(
            tilemap.newLayer(),
            Array(tilemap.width * tilemap.height) { ImageView() }
        )

        fun drawOn(tilemap: Tilemap, clear: Boolean = false) {
            view.toFront()
            val g = view.graphicsContext2D
            g.clearRect(0.0, 0.0, view.width, view.height)

            if (!clear) for (y in 0 until tilemap.height)
                for (x in 0 until tilemap.width) {
                    val view = contents[x + y * tilemap.width] ?: continue
                    val img = view.image
                    val mapx = x * tilemap.tileSize * tilemap.scale
                    val mapy = y * tilemap.tileSize * tilemap.scale
                    g.drawImage(img, mapx, mapy,
                        tilemap.tileSize * tilemap.scale,
                        tilemap.tileSize * tilemap.scale) }
        }
    }

    fun getLayer(i: Int): Layer? = layers[i]

    fun setLayer(i: Int) {
        current  = if (layers.isNotEmpty() && i in 0 until layers.size) i else -1
    }

    fun moveUp(i: Int) { if (layers.isNotEmpty()) layers.moveUp(i) }

    fun moveDown(i: Int) { if (layers.isNotEmpty()) layers.moveDown(i) }

    fun removeAt(i: Int) {
        val l = layers.removeAt(i)
        l.drawOn(this, true)
        current = if (layers.size == 0) -1 else 0
    }

    private fun mapControls() {
        view.setOnDragDetected { view.startFullDrag() }
        fun x(x: Double) = (x / (tileSize * scale)).toInt()
        fun y(y: Double) = (y / (tileSize * scale)).toInt()

        view.setOnMousePressed { e ->
            val x = x(e.x) ; val y = y(e.y)
            if (x >= width || y >= height) return@setOnMousePressed
            placement.origin = Point(x, y)

            putTile(e, x + y * width)
        }

        view.setOnMouseDragOver { e ->
            val x = x(e.x) ; val y = y(e.y)
            if (x >= width || y >= height) return@setOnMouseDragOver

            isCtrlDown = e.isControlDown
            if (e.isControlDown) {
                placement.destiny = Point(x, y)
            } else {
                putTile(e, x + y * width)
            }
        }

        view.setOnMouseDragExited { e ->
            if (placement.destiny != null) {
                val o = placement.origin ?: return@setOnMouseDragExited
                val (sw, sh) = selectionDimensions() ?: return@setOnMouseDragExited
                placement.scanSelection { xi, yi ->
                    if (xi % sw == 0 && yi % sh == 0) {
                        val x = o.x + xi
                        val y = o.y + yi
                        putTile(e, x + y * width)
                    }
                }
                placement.destiny = null
            }
        }
    }

    private fun putTile(e: MouseEvent, index: Int) {
        if (current < 0) return
        fun getOffset(): Int {
            var offset = 0
            for (name in t) {
                if (tilesetKey == name) break
                val v = tilesets[name] ?: continue
                if (v.width < 0 || v.height < 0) continue
                offset += v.width * v.height
            }
            return offset
        }

        val currentLayerContents = layers[current].contents
        if (e.isSecondaryButtonDown) {
            currentLayerContents[index]?.image = null
        } else {
            val tileset = tilesets[tilesetKey ?: return]
            val o = tileset?.selector?.origin ?: return
            var x = index%width
            var y = index/height

            if(tileset.selector.selectionSize() == 1) {
                val mapi = x + y*width
                val view = tileset.cellAt(o.x, o.y
                )?.view?.children?.get(0) as? ImageView ?: return
                currentLayerContents[mapi]?.image = view.image
                layers[current].realContents[mapi] =
                    o.x + o.y * tileset.width + getOffset()
                return
            }
            val (sw, sh) = selectionDimensions() ?: return
            val op = placement.origin ?: return
            val deltaX = sw - ((op.x - x)%sw)
            x -= (deltaX - if (deltaX >= sw) sw else 0)

            val deltaY = sh - ((op.y - y)%sh)
            y -= (deltaY - if (deltaY >= sh) sh else 0)

            tileset.selector.scanSelection { xi, yi ->
                if (tileset.width < 0) return
                val mapx = x + xi
                val mapy = y + yi
                if (mapx >= width || mapx < 0 || mapx >= width || mapx < 0)
                    return@scanSelection
                val mapi = mapx + mapy*width
                if (mapi >= 0 && mapi < currentLayerContents.size) {
                    val cell = tileset.cellAt(o.x + xi, o.y + yi)
                    val view = cell?.view?.children?.get(0) as? ImageView
                    currentLayerContents[mapi]?.image = view?.image
                    layers[current].realContents[mapi] = (o.x + xi) +
                            (o.y + yi) * tileset.width + getOffset()
                }
            }
        }
    }

    private fun selectionDimensions() : Pair<Int, Int>? {
        val tileset = tilesets[tilesetKey ?: return null]
        val o = tileset?.selector?.origin ?: return null
        val h = ArrayList<Int>()
        val v = ArrayList<Int>()
        tileset.selector.scanSelection { xi, yi ->
            val w = tileset.width
            val c: Node? = tileset.cellAt(o.x + xi, o.y + yi)?.view ?: return@scanSelection
            val i = tileset.component?.children?.indexOf(c) ?: return null
            h += i%w; v += i/w
        }
        //Selection width and height
        val sw = ((h.max()?:0) - (h.min()?:0)) + 1
        val sh = ((v.max()?:0) - (v.min()?:0)) + 1
        return sw to sh
    }

    fun zoom(n: Node) {
        n.addEventFilter(ScrollEvent.SCROLL) {
            if (it.isControlDown) {
                scale += 0.10*it.deltaY.sign
                scale = min(max(0.25, scale), 4.0)

                val canvasWidth = width * tileSize * scale
                val canvasHeight = height * tileSize * scale

                layers.forEach { layer ->
                    layer.view.prefWidth(canvasWidth)
                    layer.view.prefHeight(canvasHeight)
                }

                view.setPrefSize(canvasWidth, canvasHeight)
                darkness.prefWidth(canvasWidth)
                darkness.prefHeight(canvasHeight)

                it.consume()
            }
        }
    }

    fun undo() {}
}