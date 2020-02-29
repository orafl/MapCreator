package com.rafl.mapcr

import javafx.animation.AnimationTimer
import javafx.collections.ListChangeListener
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.TextFieldListCell
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.*
import javafx.scene.text.Font
import javafx.stage.FileChooser
import javafx.stage.WindowEvent
import javafx.util.Callback
import javafx.util.StringConverter
import org.controlsfx.control.textfield.TextFields
import java.io.File
import java.io.FileWriter
import java.net.MalformedURLException
import java.net.URL
import javax.imageio.ImageIO

class Editor(mapName: String, mapWidth: Int, mapHeight: Int)
{
    var mapName = mapName; private set
    private val tilemapTileSize = 32
    val view = VBox()
    private val emptyTileset = Tileset(null, tilemapTileSize, -1, -1)

    val tilemap = Tilemap(tilemapTileSize, mapWidth, mapHeight)
    private var tileset = emptyTileset

    private val tilesetNames = TabPane()
    private val selectionArea = ScrollPane()
    private val tilesets = mutableMapOf<String, Tileset>()
    private var scrollLayers = {}
    private var moveLayerUp = {}
    private var moveLayerDown = {}
    private var deleteLayer = {}
    var layers = ListView<String>()

    fun getTilesets() : Map<String, Tileset> = tilesets

    val commands = mapOf(
        KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN)
                to Runnable { tilemap.undo() },
        KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN)
                to Runnable { moveLayerUp() },
        KeyCodeCombination(KeyCode.W,  KeyCombination.SHORTCUT_DOWN)
                to Runnable { moveLayerDown() },
        KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN)
                to Runnable { scrollLayers()},
        KeyCodeCombination(KeyCode.DELETE) to Runnable { deleteLayer()})

    fun start() {
        tabControl()

        val map = ScrollPane(tilemap.view).apply(tilemap::zoom)
        map.setPrefSize(tilemap.view.prefWidth, tilemap.view.prefHeight)

        val anchor = AnchorPane()

        AnchorPane.setTopAnchor(tilesetNames, 5.0)
        AnchorPane.setLeftAnchor(tilesetNames, 5.0)
        AnchorPane.setRightAnchor(tilesetNames, 5.0)
        anchor.children.addAll(tilesetNames)

        val gridPane = GridPane()
        val layerPane = layerPane()

        gridPane.add(VBox(5.0, anchor, selectionArea), 0, 0)
        gridPane.add(map, 1, 0)
        gridPane.add(VBox(layerPane), 2, 0)
        gridPane.columnConstraints.addAll(
            ColumnConstraints(tilemap.tileSize * 8.0),
            ColumnConstraints().apply {
                hgrow = Priority.ALWAYS
            },
            ColumnConstraints()
        )

        val save = Menu("Save")
        save.setOnAction { saveMap() }
        val load = Menu("Close")
        load.setOnAction { closeEditor() }
        val delete = Menu("Delete")
        delete.setOnAction { askDeleteMap() }

        val menuTileset = Menu("Tilesets...")
        menuTileset.setOnAction { addTileset() }

        val menubar = MenuBar(
            Menu("File").apply {
                items.addAll(save, load,delete)
            },
            Menu("Tile").apply {
                items.addAll(menuTileset)
            }
        )
        view.children.addAll(menubar, gridPane)

        layerPane.prefHeight = 540.0
    }

    private fun saveMap() {
        var init = false
        Any::class.java.getResourceAsStream("/mapnames.txt").bufferedReader()
            .useLines { for (line in it) init = line == mapName }
        if (!init) {
            FileWriter(File(Any::class.java.getResource("/mapnames.txt").toURI()), true)
                .use { it.write("$mapName\n") }
        }

        saveTilemap(this)
    }

    private fun closeEditor() {
        val closeAndSave = ButtonType("Close and save")
        val alert = Alert(Alert.AlertType.WARNING, "Close current map?",
            closeAndSave, ButtonType("Just close"), ButtonType.CANCEL
        )
        alert.showAndWait()
        if (alert.result == ButtonType.CANCEL) return
        val stage = App.primaryStage ?: return
        val scene = stage.scene ?: return
        if (alert.result == closeAndSave) saveMap()
        scene.root = startMenu(stage, scene)
        stage.width = App.initialWidth
        stage.height = App.initialHeight
        stage.centerOnScreen()
    }

    private fun askDeleteMap() {
        val alert = Alert(Alert.AlertType.WARNING, "Delete this map?",
            ButtonType.YES, ButtonType.NO, ButtonType.CANCEL
        )
        alert.showAndWait()
        if (alert.result == ButtonType.YES) {
            deleteMap(this.mapName)

            overwriteFile("/mapnames.txt") { lines, toWrite ->
                lines.forEach {
                    if (it != this.mapName) toWrite.write("$it\n")
                }
            }

            val stage = App.primaryStage ?: return
            val scene = stage.scene ?: return
            scene.root = startMenu(stage, scene)
            stage.width = App.initialWidth
            stage.height = App.initialHeight
            stage.centerOnScreen()
        }
    }

    fun askSave(e: WindowEvent) {
        val alert = Alert(Alert.AlertType.WARNING, "Save before quiting?",
            ButtonType.YES, ButtonType.NO, ButtonType.CANCEL
        )
        alert.showAndWait()
        if (alert.result == ButtonType.CANCEL) e.consume()
        else if (alert.result == ButtonType.YES) saveMap()
    }

    private fun tabControl() {
        tilesetNames.selectionModel
            .selectedItemProperty().addListener { _, _, new: Tab? ->
                if (new == null) {
                    selectionArea.content = emptyTileset.component
                    tileset = emptyTileset
                    tilemap.setNoTileset()
                } else {
                    changeSelection(new.text)
                }
            }
    }

    private fun changeSelection(name: String) {
        val newTileset = tilesets[name] ?: return
        tileset.component = null
        tileset = newTileset
        tilemap.setTileset(name, newTileset)
        tileset.createTileset()
        selectionArea.content = tileset.component
    }

    fun refresh() {
        tilesetNames.selectionModel.also {
            if (!it.isEmpty) {
                changeSelection(tilesets.keys.first())
            }
        }
    }

    private fun layerPane(): VBox {
        layers.prefHeight = 100.0
        val layerPane = VBox(10.0,
            HBox(Label("Layers").apply { font = Font.font(20.0) })
                .apply { alignment = Pos.CENTER }, layers)

        layers.selectionModel.selectionMode = SelectionMode.SINGLE
        layers.selectionModel.selectedIndices.addListener(ListChangeListener{
            for (i in it.list) { tilemap.setLayer(i) }
        })

        scrollLayers = {
            if (layers.items.isNotEmpty()) {
                layers.selectionModel.selectedIndices.forEach { i ->
                    val ii = (i + 1) % layers.items.size
                    layers.selectionModel.select(ii)
                }
            }
        }

        moveLayerUp = {
            if (layers.items.isNotEmpty()) {
                layers.selectionModel.selectedIndices.forEach { i ->
                    tilemap.moveUp(i)
                    layers.selectionModel.select(layers.items.moveUp(i))
                }
            }
        }

        moveLayerDown = {
            if (layers.items.isNotEmpty()) {
                layers.selectionModel.selectedIndices.forEach { i ->
                    tilemap.moveDown(i)
                    layers.selectionModel.select(layers.items.moveDown(i))
                }
            }
        }

        deleteLayer = {
            if (layers.items.isNotEmpty()) {
                layers.selectionModel.selectedIndices.forEach { i ->
                    val v = layers.items[i]
                    val a = Alert(Alert.AlertType.CONFIRMATION, "Delete layer \"$v\"?")
                    a.showAndWait()
                    if (a.result == ButtonType.OK) {
                        layers.items.removeAt(i)
                        tilemap.removeAt(i)
                    }
                }
            }
        }

        layers.isEditable = true
        layers.cellFactory = Callback<ListView<String?>?, ListCell<String?>?> {
            object: TextFieldListCell<String?>(object : StringConverter<String?>() {
                override fun toString(obj: String?) = obj
                override fun fromString(string: String?) = string
            }) {
            }
        }

        layers.contextMenu = ContextMenu().apply {
            items.add(MenuItem("New").also {
                it.setOnAction {
                    tilemap.createLayer()

                    val c = "Layer" + (layers.items.size+1)
                    layers.items.add(layers.items.size, c)
                    layers.scrollTo(layers.items.lastIndex)

                    object : AnimationTimer() {
                        var frameCount = 0
                        override fun handle(now: Long) {
                            frameCount++
                            if (frameCount > 1) {
                                layers.edit(layers.items.lastIndex)
                                stop()
                            }
                        }
                    }.start()
                }
            })
        }
        return layerPane
    }

    fun bindTileset(name: String, tileset: Tileset) {
        tilesets[name] = tileset
        tilesetNames.tabs.add(Tab(name))
    }

    private fun addTileset() {
        val fileUrl = TextField().apply {
            isDisable = true
        }
        val getFile = Button("Load")
        val save = Button("Save")
        val cancel = Button("Cancel")
        val delete = Button("Delete")
        val tileSize = numberOnlyTextField(256).apply { prefWidth = 48.0 }
        val transparency1 = ColorPicker()
        val transparency2 = ColorPicker()

        transparency1.setOnAction {
            transparency2.value = transparency1.value
        }

        val name = TextField()
        TextFields.bindAutoCompletion(name, tilesets.keys)

        val root = VBox().apply {
            spacing = 10.0
            children.addAll(
                HBox(Label("Tileset: "), name),
                VBox(
                    HBox (
                        HBox(Label("Source:"), fileUrl, getFile),
                        HBox( Label("Tile size: "), tileSize)
                    ).apply { spacing = 20.0 },
                    HBox(
                        Label("Transparency: "),
                        transparency1, transparency2
                    ).apply { spacing = 10.0 }
                ).apply { spacing = 7.0 },
                HBox(cancel, delete, save).apply { spacing = 15.0 }
            )
        }
        root.padding = Insets(20.0, 20.0, 20.0, 20.0)

        val scene = Scene(root)
        val stage = App.subWindow(scene)

        fileUrl.prefWidth = fileUrl.width + fileUrl.width/6
        name.prefWidth = name.width + 2*getFile.width + tileSize.width + 55
        save.prefWidth = save.width*2.5
        cancel.prefWidth = cancel.width*2.5
        delete.prefWidth = delete.width*2.5

        getFile.setOnAction {
            val file = FileChooser().showOpenDialog(stage)
            fileUrl.text = file?.toURI()?.toURL()?.toString() ?: ""
        }

        cancel.setOnAction { stage.close() }

        delete.setOnAction {
            if (name.text.isBlank() || name.text !in tilesets.keys)
                Alert(Alert.AlertType.WARNING, "No tileset to delete.").showAndWait()
            else {
                delete(name.text)
                tilesets.remove(name.text)
                val index = tilesetNames.tabs.indices.firstOrNull {
                    tilesetNames.tabs[it].text == name.text
                } ?: -1
                if (index != -1) tilesetNames.tabs.removeAt(index)
                stage.close()
            }
        }

        save.setOnAction {
            try {
                if (name.text.isBlank()) throw IllegalArgumentException()
                var update = false

                if (name.text in tilesets.keys) {
                    val replace = ButtonType("Update")
                    val open = ButtonType("Just open")
                    val c = ButtonType("Cancel")
                    val a = Alert(Alert.AlertType.WARNING,
                        "Tileset \"${name.text}\" already exists.", replace, open, c)
                    a.showAndWait()
                    if (a.result == c) return@setOnAction
                    if (a.result == open) {
                        val dup = tilesetNames.tabs.firstOrNull {
                            it.text == name.text
                        }
                        if (dup != null) tilesetNames.selectionModel.select(dup)
                        else bindTileset(name.text, tilesets[name.text]!!)
                        stage.close()
                        return@setOnAction
                    }
                    update = a.result == replace
                }

                val img = if (update && fileUrl.text.isEmpty()) {
                    tilesets[name.text]!!.srcImage
                } else {
                    if (fileUrl.text.isEmpty()) throw MalformedURLException()
                    val url = URL(fileUrl.text)
                    val ext = url.file.split(".").last()
                    if (ext !in setOf("PNG", "png", "gif", "jpg"))
                        throw MalformedURLException()
                    ImageIO.read(url)
                }

                if (tileSize.text.isBlank())
                    if (update)
                        tileSize.text = tilesets[name.text]!!.tileSize.toString()
                    else throw IllegalArgumentException()

                val tileset = Tileset(img, tileSize.text.toInt(),
                    transparency1.rgb, transparency2.rgb)

                stage.close()

                if (update) {
                    update(name.text, tileset)
                    val dup = tilesetNames.tabs.firstOrNull { it.text == name.text }
                    if (dup != null) tilesetNames.tabs.remove(dup)
                }
                else saveTileset(name.text, tileset)

                bindTileset(name.text, tileset)
            } catch (e: MalformedURLException) {
                Alert(Alert.AlertType.WARNING, "Please specify an image file.").showAndWait()
            } catch (e: IllegalArgumentException) {
                Alert(Alert.AlertType.WARNING, "Please fill all fields.").showAndWait()
            }
        }
    }
}