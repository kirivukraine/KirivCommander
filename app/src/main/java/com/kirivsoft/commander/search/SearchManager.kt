package com.kirivsoft.commander.search

import com.kirivsoft.commander.file.FileItem
import com.kirivsoft.commander.root.RootAccessManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.util.regex.Pattern
import kotlin.coroutines.coroutineContext

data class SearchQuery(
    val text: String,
    val rootPath: String,
    val searchInContent: Boolean = false,
    val useRegex: Boolean = false,
    val caseSensitive: Boolean = false,
    val fileTypes: Set<String> = emptySet(),
    val minSizeBytes: Long = 0,
    val maxSizeBytes: Long = Long.MAX_VALUE,
    val modifiedAfterMs: Long = 0
)

class SearchManager(private val rootMgr: RootAccessManager) {

    fun search(query: SearchQuery): Flow<FileItem> = flow {
        val pattern: Pattern? = if (query.useRegex) {
            val flags = if (!query.caseSensitive) Pattern.CASE_INSENSITIVE else 0
            runCatching { Pattern.compile(query.text, flags) }.getOrNull()
        } else null
        val nameQuery = if (query.caseSensitive) query.text else query.text.lowercase()

        fun matches(name: String): Boolean {
            val n = if (query.caseSensitive) name else name.lowercase()
            return if (pattern != null) pattern.matcher(n).find() else n.contains(nameQuery)
        }

        suspend fun traverse(dir: File) {
            if (!coroutineContext.isActive) return
            val children: List<File> = if (dir.canRead()) {
                dir.listFiles()?.toList() ?: emptyList()
            } else if (rootMgr.hasRoot()) {
                rootMgr.listDirectory(dir.absolutePath, true).map { File(it.path) }
            } else emptyList()

            for (child in children) {
                if (!coroutineContext.isActive) return
                if (query.fileTypes.isNotEmpty() && !child.isDirectory) {
                    if (child.extension.lowercase() !in query.fileTypes) continue
                }
                if (!child.isDirectory) {
                    if (child.length() < query.minSizeBytes || child.length() > query.maxSizeBytes) continue
                }
                if (child.lastModified() < query.modifiedAfterMs) continue

                if (matches(child.name)) {
                    emit(FileItem(child.absolutePath, child.name,
                        if (child.isFile) child.length() else 0L,
                        child.lastModified(), child.isDirectory, child.name.startsWith(".")))
                }
                if (query.searchInContent && child.isFile && child.length() < 10_000_000) {
                    runCatching {
                        val content = if (query.caseSensitive) child.readText()
                                      else child.readText().lowercase()
                        if (content.contains(nameQuery) && !matches(child.name)) {
                            emit(FileItem(child.absolutePath, "[вміст] ${child.name}",
                                child.length(), child.lastModified(), false, child.name.startsWith(".")))
                        }
                    }
                }
                if (child.isDirectory) traverse(child)
            }
        }
        traverse(File(query.rootPath))
    }.flowOn(Dispatchers.IO)
}
