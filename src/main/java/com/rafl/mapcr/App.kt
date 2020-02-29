package com.rafl.mapcr

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.HBox
import javafx.stage.Modality
import javafx.stage.Stage

class App : Application() {
    companion object {
        var primaryStage: Stage? = null
        var initialWidth = Double.NaN; private set
        var initialHeight = Double.NaN; private set
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
        primaryStage?.addApplicationIcon()
        val scene = Scene(HBox())
        val root = startMenu(primaryStage, scene)
        scene.root = root
        stage?.scene = scene
        stage?.setOnShown {
            if (initialWidth.isNaN()) {
                initialWidth = stage.width
                initialHeight = stage.height
            }
        }
        stage?.show()
    }
}