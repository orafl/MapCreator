package com.rafl.mapcr

import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.control.ColorPicker
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import javafx.scene.image.Image
import javafx.scene.layout.Region
import javafx.scene.layout.TilePane
import javafx.stage.Stage

fun tilePane(k: Int, o: Orientation = Orientation.HORIZONTAL) = TilePane(o)
    .apply {
        if (o == Orientation.VERTICAL) {
            maxHeight = Region.USE_PREF_SIZE
            prefRows = k
        }
        else {
            maxWidth = Region.USE_PREF_SIZE
            prefColumns = k
        }
    }

fun numberOnlyTextField(limit: Int) = object : TextField() {
    override fun paste() {}
}.numberOnly(limit)

fun TextInputControl.numberOnly(limit: Int) = apply {
    textProperty().addListener { _, previous, new ->
        if (!new.matches(Regex("\\d*"))) {
            text = new.replace("[^\\d]".toRegex(), "")
        }
        if (text.isNotEmpty() && text.toInt() > limit) text = previous
    }
}

val ColorPicker.rgb: Int get()
= value.run {
        val r = (red * 255).toInt()
        val g = (green * 255).toInt()
        val b = (blue * 255).toInt()
        var rgb: Int = r
        rgb = (rgb shl 8) + g
        rgb = (rgb shl 8) + b
        rgb
    }

fun Stage.addApplicationIcon() {
    icons.add(Image(
        Any::class.java.getResourceAsStream("/icon.png"))
    )
}