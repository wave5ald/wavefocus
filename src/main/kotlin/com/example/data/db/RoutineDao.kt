package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FocusSession
import com.example.data.model.RoutineTask
import com.example.data.model.ScheduledScript
import com.example.data.model.TaskCompletionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    // --- Routine Tasks ---
    @Query("SELECT * FROM routine_tasks WHERE isActive = 1 ORDER BY displayOrder ASC, id ASC")
    fun getAllActiveTasks(): Flow<List<RoutineTask>>

    @Query("SELECT * FROM routine_tasks ORDER BY displayOrder ASC, id ASC")
    fun getAllTasks(): Flow<List<RoutineTask>>

    @Query("SELECT * FROM routine_tasks")
    suspend fun getAllTasksSync(): List<RoutineTask>

    @Query("SELECT * FROM routine_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): RoutineTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: RoutineTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<RoutineTask>)

    @Update
    suspend fun updateTask(task: RoutineTask)

    @Delete
    suspend fun deleteTask(task: RoutineTask)

    @Query("DELETE FROM routine_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM routine_tasks WHERE id IN (:ids)")
    suspend fun deleteTasksByIds(ids: List<Long>)

    @Query("UPDATE routine_tasks SET category = :newCategory WHERE category = :oldCategory")
    suspend fun updateCategoryForTasks(oldCategory: String, newCategory: String)

    @Query("DELETE FROM routine_tasks")
    suspend fun clearAllTasks()

    // --- Completion Logs ---
    @Query("SELECT * FROM task_completion_logs WHERE dateEpochDay = :epochDay")
    fun getLogsForDate(epochDay: Long): Flow<List<TaskCompletionLog>>

    @Query("SELECT * FROM task_completion_logs WHERE taskId = :taskId")
    fun getLogsForTask(taskId: Long): Flow<List<TaskCompletionLog>>

    @Query("SELECT * FROM task_completion_logs WHERE dateEpochDay BETWEEN :startEpoch AND :endEpoch ORDER BY dateEpochDay ASC")
    fun getLogsBetweenDates(startEpoch: Long, endEpoch: Long): Flow<List<TaskCompletionLog>>

    @Query("SELECT * FROM task_completion_logs ORDER BY dateEpochDay DESC")
    fun getAllLogs(): Flow<List<TaskCompletionLog>>

    @Query("SELECT * FROM task_completion_logs")
    suspend fun getAllLogsSync(): List<TaskCompletionLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLog(log: TaskCompletionLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<TaskCompletionLog>)

    @Query("DELETE FROM task_completion_logs WHERE taskId = :taskId AND dateEpochDay = :dateEpochDay")
    suspend fun deleteLog(taskId: Long, dateEpochDay: Long)

    @Query("DELETE FROM task_completion_logs")
    suspend fun clearAllLogs()

    // --- Focus Sessions ---
    @Query("SELECT * FROM focus_sessions ORDER BY completedAt DESC")
    fun getAllFocusSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions")
    suspend fun getAllFocusSessionsSync(): List<FocusSession>

    @Query("SELECT * FROM focus_sessions WHERE dateEpochDay = :epochDay ORDER BY completedAt DESC")
    fun getFocusSessionsForDate(epochDay: Long): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE dateEpochDay BETWEEN :startEpoch AND :endEpoch ORDER BY completedAt DESC")
    fun getFocusSessionsBetweenDates(startEpoch: Long, endEpoch: Long): Flow<List<FocusSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSessions(sessions: List<FocusSession>)

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    suspend fun deleteFocusSession(id: Long)

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAllFocusSessions()

    // --- Scheduled Scripts & Writings ---
    @Query("SELECT * FROM scheduled_scripts ORDER BY startDateEpochDay ASC, updatedAt DESC")
    fun getAllScripts(): Flow<List<ScheduledScript>>

    @Query("SELECT * FROM scheduled_scripts")
    suspend fun getAllScriptsSync(): List<ScheduledScript>

    @Query("SELECT * FROM scheduled_scripts WHERE :epochDay >= startDateEpochDay AND :epochDay <= endDateEpochDay ORDER BY updatedAt DESC")
    fun getScriptsForDate(epochDay: Long): Flow<List<ScheduledScript>>

    @Query("SELECT * FROM scheduled_scripts WHERE id = :id LIMIT 1")
    suspend fun getScriptById(id: Long): ScheduledScript?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ScheduledScript): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScripts(scripts: List<ScheduledScript>)

    @Update
    suspend fun updateScript(script: ScheduledScript)

    @Query("DELETE FROM scheduled_scripts WHERE id = :id")
    suspend fun deleteScript(id: Long)

    @Query("DELETE FROM scheduled_scripts")
    suspend fun clearAllScripts()

    // --- Notes ---
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<com.example.data.model.NoteItem>>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSync(): List<com.example.data.model.NoteItem>

    @Query("SELECT * FROM notes WHERE boardId IS NULL ORDER BY pinned DESC, updatedAt DESC")
    fun getStandaloneNotes(): Flow<List<com.example.data.model.NoteItem>>

    @Query("SELECT * FROM notes WHERE boardId = :boardId ORDER BY pinned DESC, updatedAt DESC")
    fun getNotesForBoard(boardId: Long): Flow<List<com.example.data.model.NoteItem>>

    @Query("SELECT * FROM notes WHERE boardId = :boardId")
    suspend fun getNotesForBoardSync(boardId: Long): List<com.example.data.model.NoteItem>

    @Query("SELECT * FROM notes WHERE id IN (:ids)")
    fun getNotesByIds(ids: List<Long>): Flow<List<com.example.data.model.NoteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: com.example.data.model.NoteItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<com.example.data.model.NoteItem>)

    @Update
    suspend fun updateNote(note: com.example.data.model.NoteItem)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteNotesByIds(ids: List<Long>)

    @Query("DELETE FROM notes WHERE boardId = :boardId")
    suspend fun deleteNotesByBoardId(boardId: Long)

    // --- Boards ---
    @Query("SELECT * FROM boards ORDER BY updatedAt DESC")
    fun getAllBoards(): Flow<List<com.example.data.model.BoardItem>>

    @Query("SELECT * FROM boards")
    suspend fun getAllBoardsSync(): List<com.example.data.model.BoardItem>

    @Query("SELECT * FROM boards WHERE id = :id LIMIT 1")
    fun getBoardById(id: Long): Flow<com.example.data.model.BoardItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoard(board: com.example.data.model.BoardItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoards(boards: List<com.example.data.model.BoardItem>)

    @Update
    suspend fun updateBoard(board: com.example.data.model.BoardItem)

    @Query("DELETE FROM boards WHERE id = :id")
    suspend fun deleteBoardById(id: Long)

    @Query("DELETE FROM boards WHERE id IN (:ids)")
    suspend fun deleteBoardsByIds(ids: List<Long>)

    // --- Board Elements (Buttons, Text Blocks, Images) ---
    @Query("SELECT * FROM board_elements WHERE boardId = :boardId ORDER BY id ASC")
    fun getElementsByBoardId(boardId: Long): Flow<List<com.example.data.model.BoardElement>>

    @Query("SELECT * FROM board_elements WHERE boardId = :boardId")
    suspend fun getElementsByBoardIdSync(boardId: Long): List<com.example.data.model.BoardElement>

    @Query("SELECT * FROM board_elements")
    suspend fun getAllBoardElementsSync(): List<com.example.data.model.BoardElement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoardElement(element: com.example.data.model.BoardElement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoardElements(elements: List<com.example.data.model.BoardElement>)

    @Update
    suspend fun updateBoardElement(element: com.example.data.model.BoardElement)

    @Query("DELETE FROM board_elements WHERE id = :id")
    suspend fun deleteBoardElementById(id: Long)

    @Query("DELETE FROM board_elements WHERE id IN (:ids)")
    suspend fun deleteBoardElementsByIds(ids: List<Long>)

    @Query("DELETE FROM board_elements WHERE boardId = :boardId")
    suspend fun deleteBoardElementsByBoardId(boardId: Long)
}
