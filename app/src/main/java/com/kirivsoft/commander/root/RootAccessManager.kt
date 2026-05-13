package com.kirivsoft.commander.root

import com.kirivsoft.commander.file.FileItem
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import com.topjohnwu.superuser.io.SuFileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RootAccessManager {

    private var rootChecked = false
    private var _hasRoot = false

    suspend fun checkRoot(): Boolean = withContext(Dispatchers.IO) {
        if (!rootChecked) {
            _hasRoot = Shell.getShell().isRoot
            rootChecked = true
        }
        _hasRoot
    }

    fun hasRoot(): Boolean = _hasRoot

    suspend fun listDirectory(path: String, showHidden: Boolean): List<FileItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                (SuFile(path).listFiles() ?: emptyArray()).mapNotNull { f ->
                    if (!showHidden && f.name.startsWith(".")) return@mapNotNull null
                    FileItem(
                        path = f.absolutePath,
                        name = f.name,
                        size = if (f.isFile) f.length() else 0L,
                        lastModified = f.lastModified(),
                        isDirectory = f.isDirectory,
                        isHidden = f.name.startsWith("."),
                        requiresRoot = true,
                        permissions = getPermissions(f.absolutePath)
                    )
                }
            }.getOrDefault(emptyList())
        }

    suspend fun copy(srcPath: String, destPath: String): Boolean = shell("cp -r '$srcPath' '$destPath'")
    suspend fun move(srcPath: String, destPath: String): Boolean = shell("mv '$srcPath' '$destPath'")
    suspend fun delete(path: String): Boolean = shell("rm -rf '$path'")
    suspend fun mkdir(path: String): Boolean = shell("mkdir -p '$path'")
    suspend fun rename(srcPath: String, destPath: String): Boolean = shell("mv '$srcPath' '$destPath'")
    suspend fun chmod(path: String, mode: String): Boolean = shell("chmod $mode '$path'")
    suspend fun chown(path: String, owner: String): Boolean = shell("chown $owner '$path'")

    suspend fun readFileAsText(path: String): String = withContext(Dispatchers.IO) {
        SuFileInputStream.open(SuFile(path)).use { it.bufferedReader().readText() }
    }

    suspend fun writeFile(path: String, content: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            SuFileOutputStream.open(SuFile(path)).use { it.write(content.toByteArray()) }
            true
        }.getOrDefault(false)
    }

    suspend fun exec(command: String): Pair<Boolean, List<String>> = withContext(Dispatchers.IO) {
        val result = Shell.cmd(command).exec()
        Pair(result.isSuccess, result.out)
    }

    private fun getPermissions(path: String): String =
        runCatching {
            Shell.cmd("stat -c '%A %U %G' '$path'").exec().out.firstOrNull() ?: ""
        }.getOrDefault("")

    private suspend fun shell(cmd: String): Boolean = withContext(Dispatchers.IO) {
        Shell.cmd(cmd).exec().isSuccess
    }
}
