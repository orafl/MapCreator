package com.rafl.mapcr

import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Pos
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.TilePane
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.awt.image.ImageProducer
import java.awt.image.RGBImageFilter
import kotlin.math.max

class Tileset(var srcImage: BufferedImage?, val tileSize: Int,
              val transparency1: Int, val transparency2: Int) {

    var width = -1; private set
    var height = -1; private set

    class Cell(val view: BorderPane) : Selectable {

        override fun onSelect() {
            view.style = "-fx-border-color: yellow; -fx-border-style: solid;"
        }

        override fun unselect() {
            view.style = "-fx-border-color: black; -fx-border-style: solid;"
        }
    }

    private val cells = ArrayList<Cell>()

    val selector = Selector<Cell>()

    fun createTileset() {
        val tileSrc = applyTransparency(srcImage ?: return)
        val tilesAcross = tileSrc.width / tileSize
        val tileset = tilePane(tilesAcross)
        tileset.alignment = Pos.CENTER
        width = tileSrc.width / tileSize
        height = tileSrc.height / tileSize

        for (y in 0 until tileSrc.height step tileSize) {
            for (x in 0 until tileSrc.width step tileSize) {
                val b = BorderPane().apply {
                    center = ImageView(
                        SwingFXUtils.toFXImage(
                            tileSrc.getSubimage(x, y, tileSize, tileSize), null
                        )
                    )
                    tileCell(x / tileSize, y / tileSize)
                }
                tileset.children.add(b)
                cells.add(Cell(b).also { it.unselect() })
            }
        }
        component = tileset
        selector.maxWidth = max(width, 1)
    }

    var component: TilePane? = null
    set(value) {
        if (value == null) {
            cells.clear()
        }
        field = value
    }

    private fun applyTransparency(img: BufferedImage) : BufferedImage {
        val ip: ImageProducer = FilteredImageSource(img.source,
            object : RGBImageFilter() {
                override fun filterRGB(x: Int, y: Int, rgb: Int): Int {
                    val value = 16777216 + rgb
                    if (value == transparency1 || value == transparency2) {
                        return 0x00FFFFFF and rgb
                    }
                    return rgb
                }
            }
        )
        val copy = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
        copy.createGraphics().apply {
            drawImage(Toolkit.getDefaultToolkit().createImage(ip), 0, 0, null)
            dispose()
        }
        return copy
    }

    private fun BorderPane.tileCell(x: Int, y: Int) {
        setOnDragDetected { startFullDrag() }

        fun select() {
            selector.applySelection(x, y, ::cellAt)
            val c = cellAt(x, y) ?: return
            c.onSelect()
            selector.select(c)
        }

        fun refresh() {
            selector.refresh()
            select()
        }

        setOnMousePressed { refresh() }
        setOnMouseDragEntered { select() }
    }

    fun cellAt(x: Int, y: Int): Cell? {
        return if (width == -1) null else cells[x + y*width]
    }

    fun cellAt(i: Int): Cell = cells[i]
}
