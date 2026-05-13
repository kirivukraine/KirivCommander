package com.kirivsoft.commander.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kirivsoft.commander.R
import com.kirivsoft.commander.databinding.ActivityMainBinding
import com.kirivsoft.commander.file.FileOperationsManager
import com.kirivsoft.commander.root.RootAccessManager
import com.kirivsoft.commander.ui.dialogs.NewFolderDialog
import com.kirivsoft.commander.ui.dialogs.SearchDialog
import com.kirivsoft.commander.ui.panels.FilePanelFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fileOps: FileOperationsManager
    private val rootMgr = RootAccessManager()

    private lateinit var leftPanel: FilePanelFragment
    private lateinit var rightPanel: FilePanelFragment

    private val activePanel: FilePanelFragment
        get() = if (binding.panelIndicator.selectedTabPosition == 0) leftPanel else rightPanel

    private val storagePermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* refresh after returning */ leftPanel.refresh(); rightPanel.refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        fileOps = FileOperationsManager(this)

        val startPath = Environment.getExternalStorageDirectory().absolutePath

        if (savedInstanceState == null) {
            leftPanel  = FilePanelFragment.newInstance(startPath)
            rightPanel = FilePanelFragment.newInstance(startPath)
            supportFragmentManager.beginTransaction()
                .replace(R.id.containerLeft,  leftPanel,  "LEFT")
                .replace(R.id.containerRight, rightPanel, "RIGHT")
                .commit()
        } else {
            leftPanel  = supportFragmentManager.findFragmentByTag("LEFT")  as FilePanelFragment
            rightPanel = supportFragmentManager.findFragmentByTag("RIGHT") as FilePanelFragment
        }

        setupActionButtons()
        checkPermissions()

        lifecycleScope.launch {
            val hasRoot = rootMgr.checkRoot()
            binding.rootBadge.text = if (hasRoot) "ROOT" else "USER"
        }
    }

    private fun setupActionButtons() {
        binding.btnCopy.setOnClickListener {
            val src = activePanel.getSelectedFiles()
            if (src.isEmpty()) return@setOnClickListener
            lifecycleScope.launch {
                fileOps.copyFiles(src, getOppositePanel().currentPath) {
                    activePanel.refresh(); getOppositePanel().refresh()
                }
            }
        }
        binding.btnMove.setOnClickListener {
            val src = activePanel.getSelectedFiles()
            if (src.isEmpty()) return@setOnClickListener
            lifecycleScope.launch {
                fileOps.moveFiles(src, getOppositePanel().currentPath) {
                    activePanel.refresh(); getOppositePanel().refresh()
                }
            }
        }
        binding.btnDelete.setOnClickListener {
            val files = activePanel.getSelectedFiles()
            if (files.isEmpty()) return@setOnClickListener
            MaterialAlertDialogBuilder(this)
                .setTitle("Видалити ${files.size} файл(ів)?")
                .setMessage(files.joinToString("\n") { it.name })
                .setPositiveButton("Видалити") { _, _ ->
                    lifecycleScope.launch {
                        fileOps.deleteFiles(files) { activePanel.refresh() }
                    }
                }
                .setNegativeButton("Скасувати", null)
                .show()
        }
        binding.btnNewFolder.setOnClickListener {
            NewFolderDialog(activePanel.currentPath) { activePanel.refresh() }
                .show(supportFragmentManager, "NewFolder")
        }
        binding.btnSearch.setOnClickListener {
            SearchDialog.newInstance(activePanel.currentPath)
                .show(supportFragmentManager, "Search")
        }
    }

    private fun getOppositePanel() =
        if (activePanel === leftPanel) rightPanel else leftPanel

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_sort        -> { activePanel.showSortDialog(); true }
        R.id.menu_show_hidden -> { activePanel.toggleHidden(); true }
        R.id.menu_root        -> { lifecycleScope.launch { rootMgr.checkRoot(); activePanel.refresh() }; true }
        R.id.menu_settings    -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
        else                  -> super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!activePanel.navigateUp()) finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                storagePermLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                )
            }
        } else {
            val perms = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val missing = perms.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 100)
        }
    }
}
