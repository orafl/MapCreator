package com.rafl.mapcr

import javafx.scene.image.ImageView
import java.io.File
import java.io.FileWriter
import java.nio.file.Paths

fun saveTilemap(editor: Editor) {
    deleteMap(editor.mapName)
    FileWriter(File(Any::class.java.getResource("/tilemaps.txt").toURI()), true).use {
        it.write("${editor.mapName}\n")
        it.write("${editor.tilemap.width}\n")
        it.write("${editor.tilemap.height}\n\n")

        val tilesets = editor.tilemap.tilesetsUsed
        for (k in tilesets) { it.write("$k\n") }
        it.write("\n")

        val layers = editor.layers.items
        for (i in layers.indices) {
            val l = editor.tilemap.getLayer(i)?: break
            it.write(layers[i])
            for (tile in l.realContents) it.write(",$tile")
            it.write("\n")
        }

        it.write("\n")
    }
}

fun deleteMap(name: String) {
    overwriteFile("/tilemaps.txt") { lines, toWrite ->
        var newLines = 0
        var foundName = false

        for (line in lines) {
            if (line == name) foundName = true
            if (foundName) { if (line.isBlank() || line.isEmpty()) newLines++ }
            else toWrite.append("$line\n")
            if (newLines == 3) foundName = false
        }
    }
}

fun readMap(name: String): Editor? {
    var editor : Editor? = null
    val tilesets = ArrayList<String>()
    val layers = ArrayList<IntArray>()
    val orderedLayerNames = ArrayList<String>()
    Any::class.java.getResourceAsStream("/tilemaps.txt").bufferedReader().useLines {
        var inTilesets = false
        var inLayers = false
        var newLines = 0
        var index = -1
        var mapName = ""
        var width = 0
        var height = 0

        for (line in it) {
            if (editor == null && index == 3) editor = Editor(mapName, width, height)
            if (line.isBlank() || line.isEmpty()) {
                newLines++
                inTilesets = newLines == 1
                inLayers = newLines == 2
                if (newLines == 3) break
                continue
            }

            if (line == name) index++
            if (index > -1) {
                when (index) {
                    0 -> mapName = line
                    1 -> width = line.toInt()
                    2 -> height = line.toInt()
                }

                if(editor != null) {
                    if (inTilesets) {
                        tilesets.add(line)
                    } else if (inLayers) {
                        val layer = line.split(',')
                        var layerName = ""
                        val theLayer = IntArray(width*height)
                        for (i in layer.indices) {
                            if (i == 0) {
                                layerName = layer[i]
                            } else {
                                theLayer[i-1] = layer[i].toInt()
                            }
                        }
                        orderedLayerNames += layerName
                        layers += theLayer
                    }
                }
                index++
            }
        }
    }
    val e = editor ?: return null
    //Populate
    loadTilesets(e)
    val ts = tilesets.map {
        e.getTilesets()[it]?.also { t ->
            t.createTileset()
            e.tilemap.setTileset(it, t)
        }
    }

    orderedLayerNames.forEach {
        e.layers.items.add(it)
        e.tilemap.createLayer()
    }

    for (li in layers.indices) {
        val theLayer = e.tilemap.getLayer(li) ?: continue
        val layer = layers[li]
        for (pos in layer.indices) {
            val i = layer[pos]
            if (i == -1) continue
            var a = 0
            var tileset: Tileset? = null
            for (t in ts) {
                if (t == null || t.width < 0) continue
                val ap = a
                a += t.width * t.height
                if (i >= a) continue
                tileset = t
                a = ap
                break
            }
            if (tileset == null) continue
            theLayer.contents[pos]?.image =
                (tileset.cellAt(i - a).view.children[0] as ImageView).image
            theLayer.realContents[pos] = i
        }
    }
    e.refresh()
    return e
}