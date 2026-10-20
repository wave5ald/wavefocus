package androidx.activity.compose

import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Desktop returns a tiny replacement for androidx.activity's activity-result launchers.
 * File based contracts open native Windows dialogs; permission requests always succeed.
 */
class ManagedActivityResultLauncher<I, O> internal constructor(
    private val block: (I) -> O,
    private val onResult: (O) -> Unit
) {
    fun launch(input: I) {
        val result = try {
            block(input)
        } catch (_: Exception) {
            null
        }
        @Suppress("UNCHECKED_CAST")
        onResult(result as O)
    }

    fun unregister() = Unit
}

@Composable
fun <I, O> rememberLauncherForActivityResult(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit
): ManagedActivityResultLauncher<I, O> {
    val callback = rememberUpdatedState(onResult)
    return remember(contract) {
        ManagedActivityResultLauncher({ input -> contract.execute(input) }) { output ->
            callback.value(output)
        }
    }
}

/** Android's system back button does not exist on desktop; navigation back is handled in-app. */
@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) = Unit
