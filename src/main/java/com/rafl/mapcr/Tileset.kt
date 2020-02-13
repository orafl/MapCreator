package com.rafl.mapcr

import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Pos
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.TilePane
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.awt.image.ImageProducer
import java.awt.image.RGBImageFilter
import java.net.URL
import javax.imageio.ImageIO
import kotlin.math.sign
import kotlin.properties.Delegates

class Tileset(var srcImage: BufferedImage?, val tileSize: Int,
              val transparency1: Int, val transparency2: Int) {
    private val cellDefaultStyle = "-fx-border-color: black; -fx-border-style: solid;"
    private val cellSelectedStyle = "-fx-border-color: yellow; -fx-border-style: solid;"
    val width get() = component?.prefColumns

    var origin: Point? = null
    var destiny: Point? = null
    val selectedCells = mutableSetOf<BorderPane>()

    /*constructor(srcUrl: URL, tileSize: Int,
                transparency1: Int, transparency2: Int)
            : this(ImageIO.read(srcUrl), tileSize, transparency1, transparency2)*/

    fun createTileset() {
        val tileSrc = applyTransparency(srcImage ?: return)
        val tilesAcross = tileSrc.width / tileSize
        val tileset = tilePane(tilesAcross)
        tileset.alignment = Pos.CENTER

        for (y in 0 until tileSrc.height step tileSize)
            for (x in 0 until tileSrc.width step tileSize) {
                tileset.children.add(
                    BorderPane().apply {
                        style = cellDefaultStyle
                        center = ImageView(
                            SwingFXUtils.toFXImage(
                                tileSrc.getSubimage(x, y, tileSize, tileSize), null
                            )
                        )
                        tileCell(x/tileSize, y/tileSize)
                    }
                )
            }
        component = tileset
    }

    var component: TilePane? = null

    private fun applyTransparency(img: BufferedImage) : BufferedImage {
        val ip: ImageProducer = FilteredImageSource(img.source,
            object : RGBImageFilter() {
                override fun filterRGB(x: Int, y: Int, rgb: Int): Int {
                    val value = 16777216 + rgb + 1
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

    private fun BorderPane.tileCell(x: Int, y: Int) {
        setOnDragDetected { startFullDrag() }

        fun select() {
            val o = origin
            if (o == null) {
                origin = Point(x, y)
            } else {
                val d = Point(x, y)
                if (cellAt(d.x, d.y) !in selectedCells) {
                    destiny = d
                    scanSelection { xi, yi ->
                        val c = cellAt(o.x + xi, o.y + yi) ?: return
                        c.style = cellSelectedStyle
                        selectedCells += c
                    }
                }
            }
            style = cellSelectedStyle
            selectedCells += this
        }

        fun refresh() {
            origin = null
            destiny = null
            selectedCells.forEach { it.style = cellDefaultStyle }
            selectedCells.clear()
            select()
        }

        setOnMousePressed { refresh() }
        setOnMouseDragEntered { select() }
    }

    fun cellAt(x: Int, y: Int): BorderPane? {
        return (component?.children?.get(x + y*(width ?: return null)) as BorderPane?)
    }

}
