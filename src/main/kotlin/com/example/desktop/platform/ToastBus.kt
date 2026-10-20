package com.example.desktop.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop replacement for Android's Toast. Messages are surfaced by the main window as snackbars.
 */
object ToastBus {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun show(text: CharSequence?) {
        if (text.isNullOrBlank()) return
        _message.value = text.toString()
    }

    fun consume() {
        _message.value = null
    }
}
