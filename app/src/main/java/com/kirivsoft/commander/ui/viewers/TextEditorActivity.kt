package com.kirivsoft.commander.ui.viewers

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kirivsoft.commander.R
import com.kirivsoft.commander.databinding.ActivityTextEditorBinding
import com.kirivsoft.commander.root.RootAccessManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TextEditorActivity : AppCompatActivity() {

    private lateinit var b: ActivityTextEditorBinding
    private val rootMgr = RootAccessManager()
    private var filePath = ""
    private var isModified = false
    private var isReadOnly = false
    private var searchPositions = listOf<Int>()
    private var searchIndex = 0
    private var searchQuery = ""

    companion object {
        fun open(context: Context, path: String) =
            context.startActivity(Intent(context, TextEditorActivity::class.java).putExtra("path", path))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTextEditorBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        filePath = intent.getStringExtra("path") ?: run { finish(); return }
        isReadOnly = !File(filePath).canWrite()
        title = File(filePath).name

        b.editor.typeface = Typeface.MONOSPACE
        b.editor.textSize = 14f
        if (isReadOnly) { b.editor.isEnabled = false; b.readOnlyBanner.visibility = View.VISIBLE }

        b.editor.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { isModified = true; updateStats() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        b.btnFindPrev.setOnClickListener { navigateSearch(-1) }
        b.btnFindNext.setOnClickListener { navigateSearch(1) }
        b.btnCloseSearch.setOnClickListener { b.searchBar.visibility = View.GONE }
        b.searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { doSearch(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadFile()
    }

    private fun loadFile() {
        b.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val content = withContext(Dispatchers.IO) {
                runCatching { File(filePath).readText() }
                    .getOrElse { if (rootMgr.hasRoot()) rootMgr.readFileAsText(filePath) else "" }
            }
            b.progressBar.visibility = View.GONE
            b.editor.setText(content)
            updateStats()
            isModified = false
        }
    }

    private fun saveFile() {
        if (isReadOnly) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Файл лише для читання")
                .setPositiveButton("Root") { _, _ ->
                    lifecycleScope.launch {
                        val ok = rootMgr.writeFile(filePath, b.editor.text.toString())
                        Toast.makeText(this@TextEditorActivity,
                            if (ok) "Збережено (root)" else "Помилка", Toast.LENGTH_SHORT).show()
                        if (ok) isModified = false
                    }
                }
                .setNegativeButton("Скасувати", null).show()
            return
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { File(filePath).writeText(b.editor.text.toString()) }
            isModified = false
            Toast.makeText(this@TextEditorActivity, "Збережено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun doSearch(q: String) {
        searchQuery = q
        if (q.isEmpty()) { searchPositions = emptyList(); b.searchCount.text = ""; return }
        val text = b.editor.text.toString().lowercase()
        val positions = mutableListOf<Int>()
        var idx = text.indexOf(q.lowercase())
        while (idx >= 0) { positions.add(idx); idx = text.indexOf(q.lowercase(), idx + 1) }
        searchPositions = positions; searchIndex = 0
        b.searchCount.text = "${positions.size} знайдено"
        if (positions.isNotEmpty()) highlight(positions[0])
    }

    private fun navigateSearch(dir: Int) {
        if (searchPositions.isEmpty()) return
        searchIndex = (searchIndex + dir + searchPositions.size) % searchPositions.size
        highlight(searchPositions[searchIndex])
        b.searchCount.text = "${searchIndex + 1}/${searchPositions.size}"
    }

    private fun highlight(start: Int) {
        b.editor.setSelection(start, (start + searchQuery.length).coerceAtMost(b.editor.text.length))
    }

    private fun updateStats() {
        val lines = (b.editor.text?.count { it == '\n' } ?: 0) + 1
        b.statusBar.text = "Рядків: $lines  |  UTF-8"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.editor_menu, menu); return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> { onBackPressedDispatcher.onBackPressed(); true }
        R.id.menu_save    -> { saveFile(); true }
        R.id.menu_find    -> { b.searchBar.visibility = View.VISIBLE; b.searchInput.requestFocus(); true }
        else              -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (isModified) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Незбережені зміни")
                .setPositiveButton("Зберегти") { _, _ -> saveFile() }
                .setNegativeButton("Закрити") { _, _ -> super.onBackPressed() }
                .setNeutralButton("Скасувати", null).show()
        } else super.onBackPressed()
    }
}
