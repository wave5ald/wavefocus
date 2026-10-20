package com.example.data.repository

import com.example.data.db.RoutineDao
import com.example.data.model.BoardElement
import com.example.data.model.BoardItem
import com.example.data.model.FocusSession
import com.example.data.model.NoteItem
import com.example.data.model.RoutineTask
import com.example.data.model.ScheduledScript
import com.example.data.model.TaskCompletionLog
import com.example.data.model.TaskWithStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class RoutineRepository(val dao: RoutineDao) {

    val allActiveTasks: Flow<List<RoutineTask>> = dao.getAllActiveTasks()
    val allTasks: Flow<List<RoutineTask>> = dao.getAllTasks()
    val allScripts: Flow<List<ScheduledScript>> = dao.getAllScripts()
    val allFocusSessions: Flow<List<FocusSession>> = dao.getAllFocusSessions()

    val allNotes: Flow<List<NoteItem>> = dao.getAllNotes()
    val standaloneNotes: Flow<List<NoteItem>> = dao.getStandaloneNotes()
    val allBoards: Flow<List<BoardItem>> = dao.getAllBoards()

    companion object {
        fun isTaskScheduledForDate(task: RoutineTask, date: LocalDate): Boolean {
            val epochDay = date.toEpochDay()
            if (task.repeatType.equals("SPECIFIC_DAY", ignoreCase = true) || task.repeatType.equals("ONCE", ignoreCase = true)) {
                return epochDay == task.startEpochDay
            } else if (task.repeatType.equals("INTERVAL", ignoreCase = true) && task.repeatIntervalDays > 1) {
                val anchor = if (task.startEpochDay > 0L) task.startEpochDay else (task.createdAt / (24 * 60 * 60 * 1000L))
                val diff = epochDay - anchor
                return diff >= 0 && (diff % task.repeatIntervalDays == 0L)
            } else if (task.repeatType.equals("WEEKEND_ONCE", ignoreCase = true)) {
                val dayOfWeekInt = date.dayOfWeek.value // 6 (Sat) or 7 (Sun)
                return dayOfWeekInt == 6 || dayOfWeekInt == 7
            } else {
                val dayOfWeekInt = date.dayOfWeek.value // 1 (Mon) to 7 (Sun)
                val allowedDays = task.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                return allowedDays.isEmpty() || allowedDays.contains(dayOfWeekInt)
            }
        }
    }

    fun getTasksWithStatusForDate(date: LocalDate): Flow<List<TaskWithStatus>> {
        val epochDay = date.toEpochDay()
        
        return combine(dao.getAllActiveTasks(), dao.getLogsForDate(epochDay), dao.getAllLogs()) { tasks, logs, allLogs ->
            val logMap = logs.associateBy { it.taskId }
            tasks
                .filter { task -> isTaskScheduledForDate(task, date) }
                .map { task ->
                    val todayLog = logMap[task.id]
                    var isCompleted = todayLog?.isCompleted == true
                    var completedAt = todayLog?.completedAt

                    // If task is set for 1 day in the weekend (flexible) and today is Sunday,
                    // check if completed yesterday (Saturday)
                    if (!isCompleted && task.repeatType.equals("WEEKEND_ONCE", ignoreCase = true) && date.dayOfWeek.value == 7) {
                        val satEpoch = epochDay - 1
                        val satLog = allLogs.find { it.taskId == task.id && it.dateEpochDay == satEpoch && it.isCompleted }
                        if (satLog != null) {
                            isCompleted = true
                            completedAt = satLog.completedAt
                        }
                    }

                    TaskWithStatus(
                        task = task,
                        isCompleted = isCompleted,
                        completedAt = completedAt
                    )
                }
        }
    }

    suspend fun toggleTaskCompletion(taskId: Long, date: LocalDate, completed: Boolean) {
        val epochDay = date.toEpochDay()
        dao.insertOrUpdateLog(
            TaskCompletionLog(
                taskId = taskId,
                dateEpochDay = epochDay,
                isCompleted = completed,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getTaskById(id: Long): RoutineTask? = dao.getTaskById(id)
    suspend fun insertTask(task: RoutineTask): Long = dao.insertTask(task)
    suspend fun insertTasks(tasks: List<RoutineTask>) = dao.insertTasks(tasks)
    suspend fun updateTask(task: RoutineTask) = dao.updateTask(task)
    suspend fun deleteTask(task: RoutineTask) = dao.deleteTask(task)
    suspend fun deleteTaskById(id: Long) = dao.deleteTaskById(id)
    suspend fun deleteTasksByIds(ids: List<Long>) = dao.deleteTasksByIds(ids)
    suspend fun clearAllTasks() = dao.clearAllTasks()
    suspend fun updateCategoryForTasks(oldCat: String, newCat: String) = dao.updateCategoryForTasks(oldCat, newCat)

    fun getLogsBetween(startEpoch: Long, endEpoch: Long): Flow<List<TaskCompletionLog>> =
        dao.getLogsBetweenDates(startEpoch, endEpoch)

    fun getAllLogs(): Flow<List<TaskCompletionLog>> = dao.getAllLogs()

    // Focus Sessions
    fun getFocusSessionsForDate(date: LocalDate): Flow<List<FocusSession>> =
        dao.getFocusSessionsForDate(date.toEpochDay())

    fun getFocusSessionsBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<FocusSession>> =
        dao.getFocusSessionsBetweenDates(startDate.toEpochDay(), endDate.toEpochDay())

    suspend fun insertFocusSession(session: FocusSession): Long = dao.insertFocusSession(session)
    suspend fun deleteFocusSession(id: Long) = dao.deleteFocusSession(id)

    // Scheduled Scripts & Writings
    fun getScriptsForDate(date: LocalDate): Flow<List<ScheduledScript>> =
        dao.getScriptsForDate(date.toEpochDay())

    suspend fun getScriptById(id: Long): ScheduledScript? = dao.getScriptById(id)
    suspend fun insertScript(script: ScheduledScript): Long = dao.insertScript(script)
    suspend fun updateScript(script: ScheduledScript) = dao.updateScript(script)
    suspend fun deleteScript(id: Long) = dao.deleteScript(id)

    // --- Notes & Boards ---
    fun getNotesForBoard(boardId: Long): Flow<List<NoteItem>> = dao.getNotesForBoard(boardId)
    fun getNotesByIds(ids: List<Long>): Flow<List<NoteItem>> = dao.getNotesByIds(ids)
    suspend fun insertNote(note: NoteItem): Long = dao.insertNote(note)
    suspend fun insertNotes(notes: List<NoteItem>) = dao.insertNotes(notes)
    suspend fun updateNote(note: NoteItem) = dao.updateNote(note)
    suspend fun deleteNoteById(id: Long) = dao.deleteNoteById(id)
    suspend fun deleteNotesByIds(ids: List<Long>) = dao.deleteNotesByIds(ids)

    fun getBoardById(id: Long): Flow<BoardItem?> = dao.getBoardById(id)
    suspend fun insertBoard(board: BoardItem): Long = dao.insertBoard(board)
    suspend fun insertBoards(boards: List<BoardItem>) = dao.insertBoards(boards)
    suspend fun updateBoard(board: BoardItem) = dao.updateBoard(board)
    suspend fun deleteBoardById(id: Long) {
        dao.deleteBoardById(id)
        dao.deleteNotesByBoardId(id)
        dao.deleteBoardElementsByBoardId(id)
    }
    suspend fun deleteBoardsByIds(ids: List<Long>) {
        dao.deleteBoardsByIds(ids)
        ids.forEach {
            dao.deleteNotesByBoardId(it)
            dao.deleteBoardElementsByBoardId(it)
        }
    }

    fun getElementsByBoardId(boardId: Long): Flow<List<BoardElement>> = dao.getElementsByBoardId(boardId)
    suspend fun insertBoardElement(element: BoardElement): Long = dao.insertBoardElement(element)
    suspend fun insertBoardElements(elements: List<BoardElement>) = dao.insertBoardElements(elements)
    suspend fun updateBoardElement(element: BoardElement) = dao.updateBoardElement(element)
    suspend fun deleteBoardElementById(id: Long) = dao.deleteBoardElementById(id)
    suspend fun deleteBoardElementsByIds(ids: List<Long>) = dao.deleteBoardElementsByIds(ids)

    // Backup & Restore
    suspend fun getAllTasksSync() = dao.getAllTasksSync()
    suspend fun getAllLogsSync() = dao.getAllLogsSync()
    suspend fun getAllFocusSessionsSync() = dao.getAllFocusSessionsSync()
    suspend fun getAllScriptsSync() = dao.getAllScriptsSync()
    suspend fun getAllNotesSync() = dao.getAllNotesSync()
    suspend fun getAllBoardsSync() = dao.getAllBoardsSync()
    suspend fun getAllBoardElementsSync() = dao.getAllBoardElementsSync()

    suspend fun restoreDatabase(
        tasks: List<RoutineTask>,
        logs: List<TaskCompletionLog>,
        focusSessions: List<FocusSession>,
        scripts: List<ScheduledScript>,
        notes: List<NoteItem> = emptyList(),
        boards: List<BoardItem> = emptyList(),
        boardElements: List<BoardElement> = emptyList()
    ) {
        dao.clearAllTasks()
        dao.clearAllLogs()
        dao.clearAllFocusSessions()
        dao.clearAllScripts()

        if (tasks.isNotEmpty()) dao.insertTasks(tasks)
        if (logs.isNotEmpty()) dao.insertLogs(logs)
        if (focusSessions.isNotEmpty()) dao.insertFocusSessions(focusSessions)
        if (scripts.isNotEmpty()) dao.insertScripts(scripts)
        if (notes.isNotEmpty()) dao.insertNotes(notes)
        if (boards.isNotEmpty()) dao.insertBoards(boards)
        if (boardElements.isNotEmpty()) dao.insertBoardElements(boardElements)
    }

    suspend fun importTasksOnly(tasks: List<RoutineTask>, merge: Boolean) {
        if (!merge) {
            dao.clearAllTasks()
        }
        if (tasks.isNotEmpty()) {
            val tasksToInsert = if (merge) {
                // reset IDs to 0 so Room generates new primary keys
                tasks.map { it.copy(id = 0L) }
            } else {
                tasks
            }
            dao.insertTasks(tasksToInsert)
        }
    }
}

