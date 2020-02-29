package com.rafl.mapcr

import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.controlsfx.control.textfield.TextFields

fun mapNames(): List<String> {
    return Any::class.java.getResourceAsStream("/mapnames.txt").bufferedReader().
        useLines { ls -> ls.asSequence().filter { it.isNotBlank() }.toList() }
}

fun horizontalPush() = Pane().apply { HBox.setHgrow(this, Priority.ALWAYS) }

fun startMenu(stage: Stage?, scene: Scene): VBox {
    val mapName = TextField()
    val width = numberOnlyTextField(256).apply { text = "32" }
    val height = numberOnlyTextField(256).apply { text = "32" }
    width.prefWidth = 64.0
    height.prefWidth = 64.0
    val mapNames = mapNames()

    val maps = TextField()
    TextFields.bindAutoCompletion(maps, mapNames)

    val new = Button("New")
    new.setOnAction {
        if (mapName.text.isEmpty()
            || width.text.isEmpty() || height.text.isEmpty() ) {
            Alert(Alert.AlertType.WARNING, "Please fill all fields.").showAndWait()
            return@setOnAction
        }
        if (mapName.text in mapNames) {
            Alert(Alert.AlertType.WARNING, "Such map already exists.").showAndWait()
            return@setOnAction
        }
        stage?.hide()
        stage?.width = 1024.0*1.5
        stage?.height = 720.0*1.5
        stage?.title = mapName.text
        val editor = Editor(
            mapName.text, width.text.toInt(), height.text.toInt())
        loadTilesets(editor)
        editor.start()
        stage?.setOnCloseRequest(editor::askSave)
        scene.accelerators.putAll(editor.commands)
        scene.root = editor.view
        stage?.show()
    }

    val load = Button("Load")
    load.setOnAction {
        if (maps.text !in mapNames) {
            Alert(Alert.AlertType.WARNING, "No such map is saved").showAndWait()
            return@setOnAction
        }
        val editor = readMap(maps.text) ?: return@setOnAction
        stage?.hide()
        stage?.width = 1024.0*1.5
        stage?.height = 720.0*1.5
        stage?.title = mapName.text
        editor.start()
        stage?.setOnCloseRequest(editor::askSave)
        scene.accelerators.putAll(editor.commands)
        scene.root = editor.view
        stage?.show()
    }

    return VBox(
        Label("TileMaker"),
        HBox(
            Label("Open:"), maps, horizontalPush(), load
        ).apply { spacing = 5.0 },
        VBox(
            HBox(
                Label("New:"), mapName, new
            ).apply { spacing = 15.0 },
            HBox(
                Label("Width:"), width,
                HBox(Label("Height:"), height).apply { spacing = 10.0 },
                horizontalPush()
            ).apply { spacing = 15.0 }
        ).apply { spacing = 10.0 }
    ).apply {
        spacing = 20.0
        padding = Insets(10.0, 10.0, 10.0, 10.0)
    }
}