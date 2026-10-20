package androidx.lifecycle.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle

/**
 * Desktop counterpart of LifecycleEventEffect. The desktop window is always "resumed" while visible,
 * so ON_RESUME observers run once when first composed (and whenever the key changes is not required here).
 */
@Composable
fun LifecycleEventEffect(event: Lifecycle.Event, onEvent: () -> Unit) {
    if (event != Lifecycle.Event.ON_RESUME) return
    val callback by rememberUpdatedState(onEvent)
    LaunchedEffect(Unit) { callback() }
}
