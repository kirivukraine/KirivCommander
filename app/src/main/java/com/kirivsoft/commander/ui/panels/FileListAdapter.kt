package com.kirivsoft.commander.ui.panels

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kirivsoft.commander.databinding.ItemFileBinding
import com.kirivsoft.commander.file.FileItem
import com.kirivsoft.commander.utils.FileDateFormatter
import com.kirivsoft.commander.utils.FileSizeFormatter

class FileListAdapter(
    private val onItemClick: (FileItem) -> Unit,
    private val onItemLongClick: (FileItem) -> Boolean
) : ListAdapter<FileItem, FileListAdapter.VH>(DIFF) {

    private val selected = mutableSetOf<String>()
    private var selectionMode = false

    inner class VH(private val b: ItemFileBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: FileItem) {
            b.fileName.text = item.name
            b.fileSize.text = if (item.isDirectory) "" else FileSizeFormatter.format(item.size)
            b.fileDate.text = FileDateFormatter.format(item.lastModified)
            b.fileIcon.setImageResource(item.iconRes)

            val isSel = item.path in selected
            b.root.isActivated = isSel
            b.checkBox.visibility = if (selectionMode) View.VISIBLE else View.GONE
            b.checkBox.isChecked  = isSel
            b.rootBadge.visibility = if (item.requiresRoot) View.VISIBLE else View.GONE

            b.root.setOnClickListener     { onItemClick(item) }
            b.root.setOnLongClickListener { onItemLongClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    fun toggleSelection(item: FileItem) {
        if (item.path in selected) selected.remove(item.path) else selected.add(item.path)
        selectionMode = selected.isNotEmpty()
        notifyDataSetChanged()
    }
    fun clearSelection() { selected.clear(); selectionMode = false; notifyDataSetChanged() }
    fun isInSelectionMode() = selectionMode
    fun getSelectedItems() = currentList.filter { it.path in selected }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<FileItem>() {
            override fun areItemsTheSame(a: FileItem, b: FileItem) = a.path == b.path
            override fun areContentsTheSame(a: FileItem, b: FileItem) = a == b
        }
    }
}
