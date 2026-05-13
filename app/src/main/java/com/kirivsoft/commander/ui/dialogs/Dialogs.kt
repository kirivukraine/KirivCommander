package com.kirivsoft.commander.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kirivsoft.commander.R
import com.kirivsoft.commander.databinding.DialogSearchBinding
import com.kirivsoft.commander.file.FileItem
import com.kirivsoft.commander.root.RootAccessManager
import com.kirivsoft.commander.search.SearchManager
import com.kirivsoft.commander.search.SearchQuery
import com.kirivsoft.commander.ui.panels.FileListAdapter
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// ─── NewFolderDialog ─────────────────────────────────────────────────────────
class NewFolderDialog(
    private val parentPath: String,
    private val onCreated: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val input = EditText(requireContext()).apply {
            hint = "Назва папки"
            setPadding(48, 24, 48, 0)
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Нова папка")
            .setView(input)
            .setPositiveButton("Створити") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    java.io.File(parentPath, name).mkdirs()
                    onCreated()
                }
            }
            .setNegativeButton("Скасувати", null)
            .create()
    }
}

// ─── SearchDialog ─────────────────────────────────────────────────────────────
class SearchDialog : DialogFragment() {

    private var _b: DialogSearchBinding? = null
    private val b get() = _b!!
    private lateinit var adapter: FileListAdapter
    private val rootMgr = RootAccessManager()
    private val searchMgr by lazy { SearchManager(rootMgr) }
    private var searchJob: Job? = null

    companion object {
        fun newInstance(path: String) = SearchDialog().apply {
            arguments = Bundle().also { it.putString("path", path) }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _b = DialogSearchBinding.inflate(layoutInflater)

        adapter = FileListAdapter(
            onItemClick = { dismiss() },
            onItemLongClick = { false }
        )
        b.resultsList.layoutManager = LinearLayoutManager(requireContext())
        b.resultsList.adapter = adapter

        b.btnSearch.setOnClickListener { startSearch() }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Пошук файлів")
            .setView(b.root)
            .setNegativeButton("Закрити") { _, _ -> searchJob?.cancel() }
            .create()
    }

    private fun startSearch() {
        val query = b.searchInput.text.toString().trim()
        if (query.isEmpty()) return
        val rootPath = arguments?.getString("path") ?: "/"
        searchJob?.cancel()
        adapter.submitList(emptyList())
        b.searchStatus.text = "Пошук..."

        val results = mutableListOf<FileItem>()
        searchJob = lifecycleScope.launch {
            searchMgr.search(
                SearchQuery(
                    text = query,
                    rootPath = rootPath,
                    searchInContent = b.chkContent.isChecked,
                    useRegex = b.chkRegex.isChecked,
                    caseSensitive = b.chkCase.isChecked
                )
            ).collect { item ->
                results.add(item)
                adapter.submitList(results.toList())
                b.searchStatus.text = "Знайдено: ${results.size}"
            }
            b.searchStatus.text = "Готово: ${results.size} результатів"
        }
    }

    override fun onDestroyView() { super.onDestroyView(); searchJob?.cancel(); _b = null }
}
