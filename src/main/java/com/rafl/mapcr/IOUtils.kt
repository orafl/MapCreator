package com.rafl.mapcr

import java.io.FileWriter
import java.nio.file.Paths

fun overwriteFile(fileName: String, overwrite: (Sequence<String>, FileWriter) -> Unit) {
    val fileUri = Any::class.java.getResource(fileName).toURI()
    val filePath = Paths.get(fileUri)
    val temp = filePath.resolveSibling("temp.txt").toFile()
    val toWrite = FileWriter(temp, false)
    val file = filePath.toFile()

    file.bufferedReader().useLines { overwrite(it, toWrite) }
    toWrite.close()
    file.delete()
    temp.renameTo(file)
}