package com.kirivsoft.commander.ui.panels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kirivsoft.commander.R
import com.kirivsoft.commander.databinding.FragmentFilePanelBinding
import com.kirivsoft.commander.file.FileItem
import com.kirivsoft.commander.file.FileListLoader
import com.kirivsoft.commander.file.SortOrder
import com.kirivsoft.commander.root.RootAccessManager
import com.kirivsoft.commander.ui.viewers.ApkInstallHelper
import com.kirivsoft.commander.ui.viewers.ImageViewerActivity
import com.kirivsoft.commander.ui.viewers.MediaPlayerActivity
import com.kirivsoft.commander.ui.viewers.PdfViewerActivity
import com.kirivsoft.commander.ui.viewers.TextEditorActivity
import kotlinx.coroutines.launch
import java.io.File

class FilePanelFragment : Fragment() {

    private var _b: FragmentFilePanelBinding? = null
    private val b get() = _b!!

    private lateinit var adapter: FileListAdapter
    private val rootMgr = RootAccessManager()
    private val loader = FileListLoader()

    var currentPath: String = "/"
        private set

    private var sortOrder = SortOrder.NAME_ASC
    private var showHidden = false
    private val history = ArrayDeque<String>()

    companion object {
        fun newInstance(path: String) = FilePanelFragment().apply {
            arguments = Bundle().also { it.putString("path", path) }
        }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        currentPath = arguments?.getString("path") ?: "/sdcard"
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentFilePanelBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = FileListAdapter(
            onItemClick      = { handleClick(it) },
            onItemLongClick  = { adapter.toggleSelection(it); true }
        )
        b.recycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FilePanelFragment.adapter
            setHasFixedSize(true)
        }
        b.swipeRefresh.setOnRefreshListener { refresh() }
        navigateTo(currentPath)
    }

    fun navigateTo(path: String) {
        if (currentPath != path) history.addLast(currentPath)
        currentPath = path
        b.pathText.text = path
        loadFiles()
    }

    fun navigateUp(): Boolean {
        if (history.isNotEmpty()) {
            currentPath = history.removeLast()
            b.pathText.text = currentPath
            loadFiles()
            return true
        }
        val parent = File(currentPath).parent ?: return false
        if (parent == currentPath) return false
        navigateTo(parent)
        return true
    }

    fun refresh() = loadFiles()

    private fun loadFiles() {
        b.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val items = loader.loadDirectory(currentPath, sortOrder, showHidden, rootMgr)
            b.progressBar.visibility = View.GONE
            b.swipeRefresh.isRefreshing = false
            adapter.submitList(items)
            b.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun handleClick(item: FileItem) {
        if (adapter.isInSelectionMode()) { adapter.toggleSelection(item); return }
        if (item.isDirectory) { navigateTo(item.path); return }
        when {
            item.isText()  -> TextEditorActivity.open(requireContext(), item.path)
            item.isImage() -> ImageViewerActivity.open(requireContext(), item.path,
                adapter.currentList.filter { it.isImage() }.map { it.path })
            item.isVideo() || item.isAudio() -> MediaPlayerActivity.open(requireContext(), item.path)
            item.isPdf()   -> PdfViewerActivity.open(requireContext(), item.path)
            item.isApk()   -> ApkInstallHelper.install(requireContext(), item.path)
            else           -> ApkInstallHelper.openWith(requireContext(), item.path)
        }
    }

    fun getSelectedFiles() = adapter.getSelectedItems()
    fun clearSelection()   = adapter.clearSelection()

    fun showSortDialog() {
        val labels = arrayOf("Ім'я А→Я","Ім'я Я→А","Дата нові","Дата старі","Розмір ↑","Розмір ↓","Тип")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Сортування")
            .setSingleChoiceItems(labels, sortOrder.ordinal) { d, i ->
                sortOrder = SortOrder.values()[i]; loadFiles(); d.dismiss()
            }.show()
    }

    fun toggleHidden() {
        showHidden = !showHidden
        Toast.makeText(context, if (showHidden) "Приховані: видно" else "Приховані: сховано", Toast.LENGTH_SHORT).show()
        loadFiles()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
