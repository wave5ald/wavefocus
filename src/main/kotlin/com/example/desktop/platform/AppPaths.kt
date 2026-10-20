package com.example.desktop.platform

import java.io.File

/**
 * Central file locations for the desktop build.
 * Everything lives under %APPDATA%\WaveFocus so the app is fully self contained.
 */
object AppPaths {

    private val root: File by lazy {
        val base = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }?.let { File(it) }
            ?: File(System.getProperty("user.home"), ".wavefocus")
        File(base, "WaveFocus").apply { mkdirs() }
    }

    val rootDir: File get() = root
    val filesDir: File get() = File(root, "files").apply { mkdirs() }
    val cacheDir: File get() = File(root, "cache").apply { mkdirs() }
    val prefsDir: File get() = File(root, "prefs").apply { mkdirs() }
    val databasesDir: File get() = File(root, "databases").apply { mkdirs() }
    val backupsDir: File get() = File(root, "backups").apply { mkdirs() }
}
