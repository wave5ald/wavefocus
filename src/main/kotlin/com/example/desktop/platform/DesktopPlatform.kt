package com.example.desktop.platform

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File

/** Small helpers shared by the android.* / androidx.* compatibility classes. */
object DesktopPlatform {

    fun clipboardText(): String? = try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
            clipboard.getData(DataFlavor.stringFlavor) as? String
        } else null
    } catch (_: Exception) {
        null
    }

    fun setClipboardText(text: String) {
        try {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        } catch (_: Exception) {
        }
    }

    fun openInExplorer(file: File) {
        try {
            val target = if (file.isDirectory) file else file.parentFile ?: file
            java.awt.Desktop.getDesktop().open(target)
        } catch (_: Exception) {
            try {
                Runtime.getRuntime().exec(arrayOf("explorer.exe", "/select,", file.absolutePath))
            } catch (_: Exception) {
            }
        }
    }

    fun browse(url: String) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI(url))
        } catch (_: Exception) {
        }
    }
}
