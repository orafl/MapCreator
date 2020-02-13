package com.rafl.mapcr

import java.awt.image.BufferedImage
import java.io.File
import java.io.FileWriter
import java.nio.file.Paths

fun loadTilesets(editor: Editor) {
    Any::class.java.getResourceAsStream("/tilesets.txt").bufferedReader().useLines {
        var i = 0
        var pi = 0
        var name = ""
        var tileSize = 0
        var transp1 = 0
        var transp2 = 0
        var imgWidth = 0
        var imgHeight = 0
        var img: BufferedImage? = null
        for (line in it.iterator()) {
            if (line.isEmpty()) {
                if (i <= 6) continue
                editor.bindTileset(name, Tileset(img, tileSize, transp1, transp2))
                i = 0; pi = 0; img = null; continue
            }
            when (i) {
                0 -> name = line
                1 -> tileSize = line.toInt()
                2 -> transp1 = line.toInt()
                3 -> transp2 = line.toInt()
                4 -> imgWidth = line.toInt()
                5 -> imgHeight = line.toInt()
                else -> {
                    if (img == null) img = BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB)
                    val tokens = line.split(" ").map(String::toInt)
                    img.setRGB(tokens[0], tokens[1], tokens[2])
                    pi++
                }
            }
            i++
        }
    }
}

fun saveTileset(name: String, tileset: Tileset) {
    FileWriter(
        File(
            Any::class.java.getResource("/tilesets.txt").toURI()), true).use {
        val img = tileset.srcImage ?: return
        it.write("$name\n")
        it.write("${tileset.tileSize}\n")
        it.write("${tileset.transparency1}\n")
        it.write("${tileset.transparency2}\n")
        it.write("${img.width}\n")
        it.write("${img.height}\n")

        for (y in 0 until img.height)
            for (x in 0 until img.width) {
                it.write("$x $y ${img.getRGB(x, y)}\n")
            }
        it.write("\n")
    }
}

fun delete(tileset: String) {
    val fileUri = Any::class.java.getResource("/tilesets.txt").toURI()
    val filePath = Paths.get(fileUri)
    val temp = filePath.resolveSibling("temp.txt").toFile()
    val toWrite = FileWriter(temp, false)
    val file = filePath.toFile()

    file.bufferedReader().useLines {
        var i = 0
        val iter = it.iterator()

        while (iter.hasNext()) {
            val line = iter.next()
            if (line.isEmpty()) {
                toWrite.append("$line\n")
                if (i <= 6) continue
                i = 0; continue
            }
            if (i == 0) {
                if (tileset == line) {
                    repeat(3) { iter.next() }
                    val w = iter.next().toInt()
                    val h = iter.next().toInt()
                    repeat(w * h) { iter.next() }
                    continue
                }
            }
            toWrite.append("$line\n")
            i++
        }
    }
    toWrite.close()
    file.delete()
    temp.renameTo(file)
}

fun update(name: String, tileset: Tileset) {
    delete(name); saveTileset(name, tileset)
}