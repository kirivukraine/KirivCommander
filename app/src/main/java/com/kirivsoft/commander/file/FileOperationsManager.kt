package com.kirivsoft.commander.file

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.kirivsoft.commander.root.RootAccessManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File
import java.security.MessageDigest

class FileOperationsManager(private val context: Context) {

    private val root = RootAccessManager()
    private val handler = Handler(Looper.getMainLooper())

    suspend fun copyFiles(sources: List<FileItem>, destDir: String, onComplete: () -> Unit) =
        withContext(Dispatchers.IO) {
            sources.forEach { item ->
                runCatching {
                    val dest = File(destDir, item.name)
                    if (item.isDirectory) copyDirectory(File(item.path), dest)
                    else copyFile(File(item.path), dest)
                }.onFailure {
                    if (root.hasRoot()) root.copy(item.path, "$destDir/${item.name}")
                    else showError("Помилка копіювання: ${item.name}")
                }
            }
            withContext(Dispatchers.Main) { onComplete() }
        }

    suspend fun moveFiles(sources: List<FileItem>, destDir: String, onComplete: () -> Unit) =
        withContext(Dispatchers.IO) {
            sources.forEach { item ->
                val src = File(item.path)
                val dest = File(destDir, item.name)
                runCatching {
                    if (!src.renameTo(dest)) {
                        if (item.isDirectory) copyDirectory(src, dest) else copyFile(src, dest)
                        src.deleteRecursively()
                    }
                }.onFailure {
                    if (root.hasRoot()) root.move(item.path, "$destDir/${item.name}")
                    else showError("Помилка переміщення: ${item.name}")
                }
            }
            withContext(Dispatchers.Main) { onComplete() }
        }

    suspend fun deleteFiles(sources: List<FileItem>, onComplete: () -> Unit) =
        withContext(Dispatchers.IO) {
            sources.forEach { item ->
                val f = File(item.path)
                if (!f.deleteRecursively() && root.hasRoot()) root.delete(item.path)
            }
            withContext(Dispatchers.Main) { onComplete() }
        }

    suspend fun createDirectory(parentPath: String, name: String): Boolean =
        withContext(Dispatchers.IO) {
            File(parentPath, name).mkdirs() || (root.hasRoot() && root.mkdir("$parentPath/$name"))
        }

    suspend fun rename(item: FileItem, newName: String): Boolean =
        withContext(Dispatchers.IO) {
            val src = File(item.path)
            val dest = File(src.parent!!, newName)
            src.renameTo(dest) || (root.hasRoot() && root.rename(item.path, dest.absolutePath))
        }

    suspend fun zipFiles(sources: List<FileItem>, destZipPath: String) =
        withContext(Dispatchers.IO) {
            ZipArchiveOutputStream(File(destZipPath).outputStream()).use { zos ->
                sources.forEach { addToZip(zos, File(it.path), File(it.path).parent ?: "") }
            }
        }

    suspend fun unzipFile(zipPath: String, destDir: String) =
        withContext(Dispatchers.IO) {
            ZipFile(zipPath).use { zip ->
                zip.entries.asSequence().forEach { entry ->
                    val outFile = File(destDir, entry.name)
                    if (entry.isDirectory) outFile.mkdirs()
                    else {
                        outFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { it.copyTo(outFile.outputStream()) }
                    }
                }
            }
        }

    suspend fun calcHash(path: String, algorithm: String = "MD5"): String =
        withContext(Dispatchers.IO) {
            val digest = MessageDigest.getInstance(algorithm)
            File(path).inputStream().use { stream ->
                val buf = ByteArray(8192)
                var read: Int
                while (stream.read(buf).also { read = it } != -1) digest.update(buf, 0, read)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }

    private fun copyFile(src: File, dest: File) {
        src.inputStream().use { input -> dest.outputStream().use { input.copyTo(it, 1024 * 1024) } }
        dest.setLastModified(src.lastModified())
    }

    private fun copyDirectory(src: File, dest: File) {
        dest.mkdirs()
        src.listFiles()?.forEach { child ->
            if (child.isDirectory) copyDirectory(child, File(dest, child.name))
            else copyFile(child, File(dest, child.name))
        }
    }

    private fun addToZip(zos: ZipArchiveOutputStream, file: File, baseDir: String) {
        val entryName = file.absolutePath.removePrefix(baseDir).trimStart('/')
        if (file.isDirectory) {
            zos.putArchiveEntry(ZipArchiveEntry(entryName + "/")); zos.closeArchiveEntry()
            file.listFiles()?.forEach { addToZip(zos, it, baseDir) }
        } else {
            zos.putArchiveEntry(ZipArchiveEntry(file, entryName))
            file.inputStream().use { it.copyTo(zos) }
            zos.closeArchiveEntry()
        }
    }

    private fun showError(msg: String) {
        handler.post { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    }
}
