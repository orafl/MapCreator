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
import kotlin.math.sign

class Tilemap(var tileset: Tileset, val tileSize: Int,
              private val mapWidth: Int, private val mapHeight: Int) {

    private fun newLayer() = Canvas(mapWidth.toDouble() * tileSize, mapHeight.toDouble() * tileSize)

    private val darkness = newLayer()
    val view = Pane(darkness)

    private var current = -1
    private val layers = CopyOnWriteArrayList<Layer>()

    private val animationTimer = object : AnimationTimer() {
        override fun handle(now: Long) {
            layers.forEach { it.drawOn(this@Tilemap) }
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

    private class Layer(val view: Canvas, val contents: Array<ImageView>)  {
        constructor(tilemap: Tilemap) : this(
            tilemap.newLayer(),
            Array(tilemap.mapWidth * tilemap.mapHeight) { ImageView() }
        )

        fun drawOn(tilemap: Tilemap, clear: Boolean = false) {
            view.toFront()
            val g = view.graphicsContext2D
            g.clearRect(0.0, 0.0, view.width, view.height)

            if (!clear) for (y in 0 until tilemap.mapHeight)
                for (x in 0 until tilemap.mapWidth) {
                    val i = x + y * tilemap.mapWidth
                    val img = contents[i].image ?: continue
                    val mapx = x * tilemap.tileSize.toDouble()
                    val mapy = y * tilemap.tileSize.toDouble()
                    g.drawImage(img, mapx, mapy)
                }
        }
    }

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

    private var originallyPlaced = Point(0, 0)

    private fun putTile(e: MouseEvent, index: Int) {
        if (current < 0) return
        val currentLayerContents = layers[current].contents
        if (e.isAltDown) {
            currentLayerContents[index].image = null
        } else {
            if (e.isPrimaryButtonDown) {
                val component = tileset.component ?: return
                val o = tileset.origin ?: return
                var x = index%mapWidth
                var y = index/mapHeight

                if(tileset.selectedCells.size == 1) {
                    val cell = tileset.cellAt(o.x, o.y) ?: return
                    val i = cell.children[0] as ImageView
                    val mapi = x + y*mapWidth
                    currentLayerContents[mapi].image = i.image
                    return
                }

                val points = tileset.selectedCells.map {
                    val i = component.children.indexOf(it)
                    (i%tileset.width!!) to (i/tileset.width!!)
                }

                //Selection width and height
                val px = points.map { it.first }
                val py = points.map { it.second }

                val sw = ((px.max()?:0) - (px.min()?:0)) + 1
                val sh = ((py.max()?:0) - (py.min()?:0)) + 1

                val deltaX = sw - ((originallyPlaced.x - x)%sw)
                x -= (deltaX - if (deltaX >= sw) sw else 0)

                val deltaY = sh - ((originallyPlaced.y - y)%sh)
                y -= (deltaY - if (deltaY >= sh) sh else 0)

                tileset.scanSelection { xi, yi ->
                    val cell = tileset.cellAt(o.x + xi, o.y + yi) ?: return
                    val i = cell.children[0] as ImageView
                    val mapx = x + xi
                    val mapy = y + yi
                    if (mapx >= mapWidth || mapx < 0 || mapx >= mapWidth || mapx < 0)
                        return@scanSelection
                    val mapi = mapx + mapy*mapWidth
                    if (mapi >= 0 && mapi < currentLayerContents.size) {
                        currentLayerContents[mapi].image = i.image
                    }
                }
            }
        }
    }

    private fun mapControls() {
        view.setOnDragDetected { view.startFullDrag() }

        view.setOnMousePressed { e ->
            val x = e.x.toInt() / tileSize
            val y = e.y.toInt() / tileSize
            val index = x + y * mapWidth
            originallyPlaced = Point(index%mapWidth, index/mapHeight)
            putTile(e, index)
        }

        view.setOnMouseDragOver { e ->
            val x = e.x.toInt() / tileSize
            val y = e.y.toInt() / tileSize
            val index = x + y * mapWidth
            putTile(e, index)
        }
    }

    fun zoom(n: Node) {
        n.addEventFilter(ScrollEvent.SCROLL) {
            if (it.isControlDown) {
                val scale = 1 + it.deltaY.sign * 0.10
               /* layers[current].contents.forEach { imageView ->
                    imageView.apply {
                        val t = tileSize.toDouble()
                        fitWidth = min(max(t/4, fitWidth * scale), t * 4)
                        fitHeight = min(max(t/4, fitHeight * scale), t * 4)
                    }
                }*/
                it.consume()
            }
        }
    }

    fun undo() {}
}