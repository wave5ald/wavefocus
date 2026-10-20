package androidx.compose.ui.platform

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.desktop.platform.DesktopApp

/**
 * Compose Desktop does not ship LocalContext, but the app sources (written for Android) rely on it.
 * A process-wide desktop context is provided so no screen code has to change.
 */
val LocalContext = staticCompositionLocalOf<Context> { DesktopApp.context }
