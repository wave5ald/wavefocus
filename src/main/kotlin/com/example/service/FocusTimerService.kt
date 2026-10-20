package com.example.service

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.FocusSession
import com.example.desktop.platform.ToastBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Desktop counterpart of the Android foreground service.
 *
 * The countdown state lives in the same SharedPreferences keys as on Android, so the ViewModel's
 * "resume timer from background" logic keeps working. A lightweight coroutine ticker watches the
 * end time, records the completed focus session and notifies the UI.
 */
object FocusTimerService {

    const val PREFS_NAME = "focus_timer_prefs"
    const val KEY_END_TIME = "target_end_time"
    const val KEY_TOTAL_SECONDS = "total_seconds"
    const val KEY_IS_RUNNING = "is_running"
    const val KEY_TASK_TITLE = "task_title"
    const val KEY_TASK_ID = "task_id"
    const val KEY_MODE = "timer_mode"
    const val KEY_SAVED_REMAINING = "saved_remaining_seconds"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null

    fun start(
        context: Context,
        totalSeconds: Int,
        remainingSeconds: Int,
        taskTitle: String,
        taskId: Long?,
        mode: String
    ) {
        val endTime = System.currentTimeMillis() + (remainingSeconds * 1000L)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_END_TIME, endTime)
            .putInt(KEY_TOTAL_SECONDS, totalSeconds)
            .putBoolean(KEY_IS_RUNNING, true)
            .putString(KEY_TASK_TITLE, taskTitle)
            .putLong(KEY_TASK_ID, taskId ?: -1L)
            .putString(KEY_MODE, mode)
            .apply()
        startTicker(context)
    }

    fun pause(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val endTime = prefs.getLong(KEY_END_TIME, 0L)
        val remaining = ((endTime - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()
        prefs.edit()
            .putBoolean(KEY_IS_RUNNING, false)
            .putLong(KEY_END_TIME, 0L)
            .putInt(KEY_SAVED_REMAINING, remaining)
            .apply()
        tickerJob?.cancel()
    }

    fun stop(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        tickerJob?.cancel()
    }

    private fun startTicker(context: Context) {
        tickerJob?.cancel()
        val appContext = context.applicationContext
        tickerJob = scope.launch {
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            while (isActive) {
                if (!prefs.getBoolean(KEY_IS_RUNNING, false)) break
                val endTime = prefs.getLong(KEY_END_TIME, 0L)
                val remaining = ((endTime - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()
                if (remaining <= 0) {
                    // The in-app timer may already have completed and cleared the state.
                    if (prefs.getBoolean(KEY_IS_RUNNING, false)) {
                        onTimerFinished(appContext)
                    }
                    break
                }
                delay(1000)
            }
        }
    }

    private fun onTimerFinished(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val totalSecs = prefs.getInt(KEY_TOTAL_SECONDS, 25 * 60)
        val taskId = prefs.getLong(KEY_TASK_ID, -1L)
        val mode = prefs.getString(KEY_MODE, "Focus") ?: "Focus"
        val taskTitle = prefs.getString(KEY_TASK_TITLE, "Deep Focus") ?: "Deep Focus"
        prefs.edit().clear().apply()

        scope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                db.routineDao().insertFocusSession(
                    FocusSession(
                        taskId = if (taskId > 0) taskId else null,
                        taskTitle = taskTitle,
                        durationSeconds = totalSecs,
                        mode = mode,
                        completedAt = System.currentTimeMillis(),
                        dateEpochDay = LocalDate.now().toEpochDay()
                    )
                )
            } catch (_: Exception) {
            }
        }

        ToastBus.show("Focus session complete! Great job on \"$taskTitle\".")
    }
}
