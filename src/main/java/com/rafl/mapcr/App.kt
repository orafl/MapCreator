package com.rafl.mapcr

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage

class App : Application() {
    companion object {
        var primaryStage: Stage? = null
        @JvmStatic fun main(args: Array<String>) {
            launch(App::class.java)
        }

        fun subWindow(scene: Scene) : Stage {
            val dialog = Stage()
            dialog.addApplicationIcon()
            dialog.initModality(Modality.APPLICATION_MODAL)
            dialog.initOwner(primaryStage)
            dialog.scene = scene
            dialog.isResizable = false
            dialog.show()
            return dialog
        }
    }

    override fun start(stage: Stage?) {

        primaryStage = stage

        val prompt = VBox().apply {
            spacing = 20.0
            val width = numberOnlyTextField(256).apply { text = "32" }
            val height = numberOnlyTextField(256).apply { text = "32" }
            children.addAll(
                Label("Tile Map"),
                HBox(
                    HBox( Label("Width"), width).apply { spacing = 5.0 },
                    HBox( Label("Height"), height)
                ),
                Button("Create").apply {
                    setOnAction {
                        stage?.hide()
                        stage?.width = 1024.0*1.5
                        stage?.height = 720.0*1.5
                        val editor = Editor(
                            32, width.text.toInt(), height.text.toInt())
                        scene.accelerators.putAll(editor.commands)
                        scene.root = editor.view
                        stage?.show()
                    }
                }
            )
        }
        val scene = Scene(prompt)
        primaryStage?.addApplicationIcon()
        stage?.scene = scene
        stage?.show()
    }
}