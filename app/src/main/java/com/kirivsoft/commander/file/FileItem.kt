package com.kirivsoft.commander.file

import android.os.Parcelable
import com.kirivsoft.commander.R
import kotlinx.parcelize.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Parcelize
data class FileItem(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val isHidden: Boolean,
    val requiresRoot: Boolean = false,
    val permissions: String = ""
) : Parcelable {

    val extension: String get() = if (isDirectory) "" else File(path).extension.lowercase()

    val iconRes: Int get() = when {
        isDirectory -> R.drawable.ic_folder
        isImage()   -> R.drawable.ic_image
        isVideo()   -> R.drawable.ic_video
        isAudio()   -> R.drawable.ic_audio
        isPdf()     -> R.drawable.ic_pdf
        isText()    -> R.drawable.ic_text
        isApk()     -> R.drawable.ic_apk
        isArchive() -> R.drawable.ic_archive
        else        -> R.drawable.ic_file
    }

    fun isImage()   = extension in setOf("jpg","jpeg","png","gif","webp","bmp","heic","avif","svg")
    fun isVideo()   = extension in setOf("mp4","mkv","avi","mov","webm","3gp","ts","m4v","flv")
    fun isAudio()   = extension in setOf("mp3","flac","ogg","aac","wav","opus","m4a","wma")
    fun isPdf()     = extension == "pdf"
    fun isText()    = extension in setOf("txt","log","xml","json","yaml","yml","toml","ini","kt",
                                         "java","py","js","ts","html","css","md","sh","c","cpp",
                                         "h","rs","go","cs","php","rb","gradle","properties")
    fun isApk()     = extension == "apk"
    fun isArchive() = extension in setOf("zip","rar","7z","tar","gz","bz2","xz","zst")
}

enum class SortOrder { NAME_ASC, NAME_DESC, DATE_NEW, DATE_OLD, SIZE_ASC, SIZE_DESC, TYPE }

class FileListLoader {
    suspend fun loadDirectory(
        path: String,
        sort: SortOrder,
        showHidden: Boolean,
        rootMgr: com.kirivsoft.commander.root.RootAccessManager
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(path)
        val items: List<FileItem> = if (dir.canRead()) {
            (dir.listFiles() ?: emptyArray()).mapNotNull { f ->
                if (!showHidden && f.isHidden) return@mapNotNull null
                FileItem(
                    path = f.absolutePath,
                    name = f.name,
                    size = if (f.isFile) f.length() else 0L,
                    lastModified = f.lastModified(),
                    isDirectory = f.isDirectory,
                    isHidden = f.isHidden
                )
            }
        } else if (rootMgr.hasRoot()) {
            rootMgr.listDirectory(path, showHidden)
        } else {
            emptyList()
        }

        when (sort) {
            SortOrder.NAME_ASC  -> items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            SortOrder.NAME_DESC -> items.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenByDescending { it.name.lowercase() })
            SortOrder.DATE_NEW  -> items.sortedWith(compareBy({ !it.isDirectory }, { -it.lastModified }))
            SortOrder.DATE_OLD  -> items.sortedWith(compareBy({ !it.isDirectory }, { it.lastModified }))
            SortOrder.SIZE_ASC  -> items.sortedWith(compareBy({ !it.isDirectory }, { it.size }))
            SortOrder.SIZE_DESC -> items.sortedWith(compareBy({ !it.isDirectory }, { -it.size }))
            SortOrder.TYPE      -> items.sortedWith(compareBy({ !it.isDirectory }, { it.extension }, { it.name.lowercase() }))
        }
    }
}
