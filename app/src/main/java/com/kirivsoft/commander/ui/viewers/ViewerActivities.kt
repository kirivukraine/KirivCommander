package com.kirivsoft.commander.ui.viewers

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import coil.load
import com.github.chrisbanes.photoview.PhotoView
import com.kirivsoft.commander.databinding.ActivityImageViewerBinding
import com.kirivsoft.commander.databinding.ActivityPdfViewerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ─── ImageViewer ─────────────────────────────────────────────────────────────
class ImageViewerActivity : AppCompatActivity() {
    private lateinit var b: ActivityImageViewerBinding

    companion object {
        fun open(context: Context, path: String, all: List<String>) =
            context.startActivity(Intent(context, ImageViewerActivity::class.java)
                .putExtra("path", path)
                .putStringArrayListExtra("all", ArrayList(all)))
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(b.root)

        val path = intent.getStringExtra("path") ?: run { finish(); return }
        val all  = intent.getStringArrayListExtra("all") ?: arrayListOf(path)
        val start = all.indexOf(path).coerceAtLeast(0)
        title = File(path).name

        b.pager.adapter = ImagePagerAdapter(this, all)
        b.pager.setCurrentItem(start, false)
        b.pager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                title = File(all[position]).name
                b.counter.text = "${position + 1} / ${all.size}"
            }
        })
        b.counter.text = "${start + 1} / ${all.size}"
    }
}

class ImagePagerAdapter(activity: FragmentActivity, private val paths: List<String>) :
    FragmentStateAdapter(activity) {
    override fun getItemCount() = paths.size
    override fun createFragment(position: Int) = ImagePageFragment.newInstance(paths[position])
}

class ImagePageFragment : Fragment() {
    companion object {
        fun newInstance(path: String) = ImagePageFragment().apply {
            arguments = Bundle().also { it.putString("path", path) }
        }
    }
    private var path = ""
    override fun onCreate(s: Bundle?) { super.onCreate(s); path = arguments?.getString("path") ?: "" }
    override fun onCreateView(i: android.view.LayoutInflater, c: android.view.ViewGroup?, s: Bundle?) =
        PhotoView(requireContext()).also { it.load(File(path)) { crossfade(true) } }
}

// ─── PdfViewer ───────────────────────────────────────────────────────────────
class PdfViewerActivity : AppCompatActivity() {
    private lateinit var b: ActivityPdfViewerBinding
    private var renderer: PdfRenderer? = null
    private var currentPage = 0

    companion object {
        fun open(context: Context, path: String) =
            context.startActivity(Intent(context, PdfViewerActivity::class.java).putExtra("path", path))
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(b.root)

        val path = intent.getStringExtra("path") ?: run { finish(); return }
        title = File(path).name

        runCatching {
            renderer = PdfRenderer(ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY))
            renderPage(0)
        }.onFailure {
            Toast.makeText(this, "Не вдалось відкрити PDF", Toast.LENGTH_LONG).show(); finish()
        }

        b.btnPrev.setOnClickListener { if (currentPage > 0) renderPage(--currentPage) }
        b.btnNext.setOnClickListener {
            renderer?.let { if (currentPage < it.pageCount - 1) renderPage(++currentPage) }
        }
    }

    private fun renderPage(index: Int) {
        val r = renderer ?: return
        lifecycleScope.launch {
            val bmp = withContext(Dispatchers.Default) {
                val page = r.openPage(index)
                android.graphics.Bitmap.createBitmap(page.width * 2, page.height * 2,
                    android.graphics.Bitmap.Config.ARGB_8888).also {
                    page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                }
            }
            b.pdfImage.load(bmp)
            currentPage = index
            b.pageCounter.text = "${index + 1} / ${r.pageCount}"
        }
    }

    override fun onDestroy() { renderer?.close(); super.onDestroy() }
}

// ─── ApkInstallHelper ────────────────────────────────────────────────────────
object ApkInstallHelper {
    fun install(context: Context, apkPath: String) {
        val file = File(apkPath)
        val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        else Uri.fromFile(file)

        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun openWith(context: Context, path: String) {
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
        context.startActivity(Intent.createChooser(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Відкрити за допомогою..."
        ))
    }
}
