package com.example.desktop.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Native open/save dialogs used to replace Android's Storage Access Framework pickers
 * (ActivityResultContracts.GetContent / CreateDocument).
 */
object FileDialogs {

    fun openFile(title: String, defaultNameHint: String? = null): File? {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        defaultNameHint?.let { dialog.file = it }
        dialog.isVisible = true
        val dir = dialog.directory ?: return null
        val name = dialog.file ?: return null
        return File(dir, name)
    }

    fun saveFile(title: String, suggestedName: String): File? {
        val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
        dialog.file = suggestedName
        dialog.isVisible = true
        val dir = dialog.directory ?: return null
        val name = dialog.file ?: return null
        val file = File(dir, name)
        return if (file.extension.isEmpty() && suggestedName.contains('.')) {
            File(dir, "$name.${suggestedName.substringAfterLast('.')}")
        } else file
    }
}
