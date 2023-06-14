package io.nuvalence.cx.tools.shared

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Recursively zips a directory
 *
 * @param inputPath path to the source directory
 * @param outputPath path to the zip file (.zip added if not part of the file name)
 */
fun zipDirectory(inputPath: String, outputPath: String) {
    val inputDirectory = File(inputPath)
    val outputZipFile = File(if (outputPath.endsWith(".zip")) outputPath else "$outputPath.zip")
    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile))).use { zos ->
        inputDirectory.walkTopDown().forEach { file ->
            val zipFileName = file.absolutePath.removePrefix(inputDirectory.absolutePath).removePrefix("/")
            val entry = ZipEntry( "$zipFileName${(if (file.isDirectory) "/" else "" )}")
            zos.putNextEntry(entry)
            if (file.isFile) {
                file.inputStream().use { fis -> fis.copyTo(zos) }
            }
        }
    }
}