package com.example.data.backup

import com.example.data.model.BoardElement
import com.example.data.model.BoardItem
import com.example.data.model.FocusSession
import com.example.data.model.NoteItem
import com.example.data.model.RoutineTask
import com.example.data.model.ScheduledScript
import com.example.data.model.TaskCompletionLog
import com.example.data.repository.RoutineRepository
import com.example.data.security.CryptoManager
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    // --- 1. FULL APP DATA BACKUP ---
    suspend fun createBackupJson(repository: RoutineRepository, encryptExport: Boolean = false): String {
        val tasks = repository.getAllTasksSync()
        val logs = repository.getAllLogsSync()
        val focusSessions = repository.getAllFocusSessionsSync()
        val scripts = repository.getAllScriptsSync()
        val notes = repository.getAllNotesSync()
        val boards = repository.getAllBoardsSync()
        val elements = repository.getAllBoardElementsSync()

        val root = JSONObject()
        root.put("type", "WAVE_FOCUS_FULL_BACKUP")
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())
        root.put("app", "WaveFocus")

        // Tasks
        val tasksArray = JSONArray()
        for (t in tasks) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("category", t.category)
            obj.put("timeOfDay", t.timeOfDay)
            obj.put("targetMinutes", t.targetMinutes)
            obj.put("daysOfWeek", t.daysOfWeek)
            obj.put("repeatType", t.repeatType)
            obj.put("repeatIntervalDays", t.repeatIntervalDays)
            obj.put("startEpochDay", t.startEpochDay)
            obj.put("isActive", t.isActive)
            obj.put("displayOrder", t.displayOrder)
            tasksArray.put(obj)
        }
        root.put("tasks", tasksArray)

        // Logs
        val logsArray = JSONArray()
        for (l in logs) {
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("taskId", l.taskId)
            obj.put("dateEpochDay", l.dateEpochDay)
            obj.put("isCompleted", l.isCompleted)
            obj.put("completedAt", l.completedAt)
            logsArray.put(obj)
        }
        root.put("logs", logsArray)

        // Focus Sessions
        val focusArray = JSONArray()
        for (f in focusSessions) {
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("taskId", f.taskId ?: JSONObject.NULL)
            obj.put("taskTitle", f.taskTitle)
            obj.put("durationSeconds", f.durationSeconds)
            obj.put("mode", f.mode)
            obj.put("completedAt", f.completedAt)
            obj.put("dateEpochDay", f.dateEpochDay)
            focusArray.put(obj)
        }
        root.put("focusSessions", focusArray)

        // Scripts & Calendar Quests/Goals
        val scriptsArray = JSONArray()
        for (s in scripts) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("title", s.title)
            obj.put("encryptedContent", s.encryptedContent)
            obj.put("iv", s.iv)
            obj.put("startDateEpochDay", s.startDateEpochDay)
            obj.put("endDateEpochDay", s.endDateEpochDay)
            obj.put("category", s.category)
            obj.put("updatedAt", s.updatedAt)
            obj.put("isEncrypted", s.isEncrypted)
            obj.put("isCompleted", s.isCompleted)
            obj.put("targetValue", s.targetValue)
            obj.put("xpReward", s.xpReward)
            obj.put("questType", s.questType)
            obj.put("emoji", s.emoji)
            scriptsArray.put(obj)
        }
        root.put("scripts", scriptsArray)

        // Notes
        val notesArray = JSONArray()
        for (n in notes) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("boardId", n.boardId ?: JSONObject.NULL)
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("imageUri", n.imageUri ?: JSONObject.NULL)
            obj.put("colorHex", n.colorHex)
            obj.put("textColorHex", n.textColorHex)
            obj.put("posX", n.posX.toDouble())
            obj.put("posY", n.posY.toDouble())
            obj.put("pinned", n.pinned)
            obj.put("createdAt", n.createdAt)
            obj.put("updatedAt", n.updatedAt)
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

        // Boards
        val boardsArray = JSONArray()
        for (b in boards) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("title", b.title)
            obj.put("description", b.description)
            obj.put("backgroundColorHex", b.backgroundColorHex)
            obj.put("createdAt", b.createdAt)
            obj.put("updatedAt", b.updatedAt)
            boardsArray.put(obj)
        }
        root.put("boards", boardsArray)

        // Board Elements
        val elementsArray = JSONArray()
        for (e in elements) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("boardId", e.boardId)
            obj.put("elementType", e.elementType)
            obj.put("text", e.text)
            obj.put("colorHex", e.colorHex)
            obj.put("textColorHex", e.textColorHex)
            obj.put("linkedNoteIds", e.linkedNoteIds)
            obj.put("imageUri", e.imageUri ?: JSONObject.NULL)
            obj.put("posX", e.posX.toDouble())
            obj.put("posY", e.posY.toDouble())
            obj.put("createdAt", e.createdAt)
            elementsArray.put(obj)
        }
        root.put("boardElements", elementsArray)

        val jsonString = root.toString(2)
        if (encryptExport) {
            val (cipher, iv) = CryptoManager.encrypt(jsonString)
            val encObj = JSONObject()
            encObj.put("encryptedBackup", true)
            encObj.put("cipher", cipher)
            encObj.put("iv", iv)
            return encObj.toString(2)
        }
        return jsonString
    }

    suspend fun restoreFromJson(repository: RoutineRepository, rawText: String): Result<Int> {
        return try {
            var content = rawText.trim()
            val parsedFirst = JSONObject(content)
            if (parsedFirst.optBoolean("encryptedBackup", false)) {
                val cipher = parsedFirst.getString("cipher")
                val iv = parsedFirst.getString("iv")
                content = CryptoManager.decrypt(cipher, iv)
            }

            val root = JSONObject(content)
            val tasksArray = root.optJSONArray("tasks") ?: JSONArray()
            val logsArray = root.optJSONArray("logs") ?: JSONArray()
            val focusArray = root.optJSONArray("focusSessions") ?: JSONArray()
            val scriptsArray = root.optJSONArray("scripts") ?: JSONArray()
            val notesArray = root.optJSONArray("notes") ?: JSONArray()
            val boardsArray = root.optJSONArray("boards") ?: JSONArray()
            val elementsArray = root.optJSONArray("boardElements") ?: JSONArray()

            val tasks = mutableListOf<RoutineTask>()
            for (i in 0 until tasksArray.length()) {
                val o = tasksArray.getJSONObject(i)
                tasks.add(
                    RoutineTask(
                        id = o.optLong("id", 0L),
                        title = o.getString("title"),
                        category = o.optString("category", "Health"),
                        timeOfDay = o.optString("timeOfDay", "Morning"),
                        targetMinutes = o.optInt("targetMinutes", 0),
                        daysOfWeek = o.optString("daysOfWeek", "1,2,3,4,5,6,7"),
                        repeatType = o.optString("repeatType", "WEEKLY"),
                        repeatIntervalDays = o.optInt("repeatIntervalDays", 1),
                        startEpochDay = o.optLong("startEpochDay", 0L),
                        isActive = o.optBoolean("isActive", true),
                        displayOrder = o.optInt("displayOrder", i)
                    )
                )
            }

            val logs = mutableListOf<TaskCompletionLog>()
            for (i in 0 until logsArray.length()) {
                val o = logsArray.getJSONObject(i)
                logs.add(
                    TaskCompletionLog(
                        id = o.optLong("id", 0L),
                        taskId = o.getLong("taskId"),
                        dateEpochDay = o.getLong("dateEpochDay"),
                        isCompleted = o.getBoolean("isCompleted"),
                        completedAt = o.optLong("completedAt", System.currentTimeMillis())
                    )
                )
            }

            val focusSessions = mutableListOf<FocusSession>()
            for (i in 0 until focusArray.length()) {
                val o = focusArray.getJSONObject(i)
                val taskId = if (o.isNull("taskId")) null else o.optLong("taskId")
                focusSessions.add(
                    FocusSession(
                        id = o.optLong("id", 0L),
                        taskId = taskId,
                        taskTitle = o.optString("taskTitle", ""),
                        durationSeconds = o.getInt("durationSeconds"),
                        mode = o.optString("mode", "Pomodoro"),
                        completedAt = o.optLong("completedAt", System.currentTimeMillis()),
                        dateEpochDay = o.getLong("dateEpochDay")
                    )
                )
            }

            val scripts = mutableListOf<ScheduledScript>()
            for (i in 0 until scriptsArray.length()) {
                val o = scriptsArray.getJSONObject(i)
                scripts.add(
                    ScheduledScript(
                        id = o.optLong("id", 0L),
                        title = o.getString("title"),
                        encryptedContent = o.optString("encryptedContent", ""),
                        iv = o.optString("iv", ""),
                        startDateEpochDay = o.getLong("startDateEpochDay"),
                        endDateEpochDay = o.optLong("endDateEpochDay", o.getLong("startDateEpochDay")),
                        category = o.optString("category", "Quest"),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                        isEncrypted = o.optBoolean("isEncrypted", false),
                        isCompleted = o.optBoolean("isCompleted", false),
                        targetValue = o.optString("targetValue", ""),
                        xpReward = o.optInt("xpReward", 50),
                        questType = o.optString("questType", "Daily Quest"),
                        emoji = o.optString("emoji", "🎯")
                    )
                )
            }

            val notes = mutableListOf<NoteItem>()
            for (i in 0 until notesArray.length()) {
                val o = notesArray.getJSONObject(i)
                val boardId = if (o.isNull("boardId")) null else o.optLong("boardId")
                notes.add(
                    NoteItem(
                        id = o.optLong("id", 0L),
                        boardId = boardId,
                        title = o.optString("title", ""),
                        content = o.optString("content", ""),
                        imageUri = if (o.isNull("imageUri")) null else o.optString("imageUri"),
                        colorHex = o.optString("colorHex", "#1E293B"),
                        textColorHex = o.optString("textColorHex", "#F8FAFC"),
                        posX = o.optDouble("posX", 40.0).toFloat(),
                        posY = o.optDouble("posY", 40.0).toFloat(),
                        pinned = o.optBoolean("pinned", false),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val boards = mutableListOf<BoardItem>()
            for (i in 0 until boardsArray.length()) {
                val o = boardsArray.getJSONObject(i)
                boards.add(
                    BoardItem(
                        id = o.optLong("id", 0L),
                        title = o.getString("title"),
                        description = o.optString("description", ""),
                        backgroundColorHex = o.optString("backgroundColorHex", "#0F172A"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val elements = mutableListOf<BoardElement>()
            for (i in 0 until elementsArray.length()) {
                val o = elementsArray.getJSONObject(i)
                elements.add(
                    BoardElement(
                        id = o.optLong("id", 0L),
                        boardId = o.getLong("boardId"),
                        elementType = o.optString("elementType", "BUTTON"),
                        text = o.optString("text", ""),
                        colorHex = o.optString("colorHex", "#3B82F6"),
                        textColorHex = o.optString("textColorHex", "#FFFFFF"),
                        linkedNoteIds = o.optString("linkedNoteIds", ""),
                        imageUri = if (o.isNull("imageUri")) null else o.optString("imageUri"),
                        posX = o.optDouble("posX", 100.0).toFloat(),
                        posY = o.optDouble("posY", 100.0).toFloat(),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            repository.restoreDatabase(tasks, logs, focusSessions, scripts, notes, boards, elements)
            val totalItems = tasks.size + logs.size + focusSessions.size + scripts.size + notes.size + boards.size + elements.size
            Result.success(totalItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- 2. TASKS & CATEGORIES ONLY (SHARE WITH OTHERS) ---
    suspend fun createTasksPackJson(repository: RoutineRepository, categories: List<String> = emptyList()): String {
        val tasks = repository.getAllTasksSync()
        val root = JSONObject()
        root.put("type", "WAVE_FOCUS_TASKS_PACK")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("description", "Routine tasks and categories to share with friends")

        // Categories
        val catsArray = JSONArray()
        val allCatNames = if (categories.isNotEmpty()) {
            categories
        } else {
            tasks.map { it.category }.distinct()
        }
        allCatNames.forEach { catsArray.put(it) }
        root.put("categories", catsArray)

        // Tasks
        val tasksArray = JSONArray()
        for (t in tasks) {
            val obj = JSONObject()
            obj.put("title", t.title)
            obj.put("category", t.category)
            obj.put("timeOfDay", t.timeOfDay)
            obj.put("targetMinutes", t.targetMinutes)
            obj.put("daysOfWeek", t.daysOfWeek)
            obj.put("repeatType", t.repeatType)
            obj.put("repeatIntervalDays", t.repeatIntervalDays)
            tasksArray.put(obj)
        }
        root.put("tasks", tasksArray)

        return root.toString(2)
    }

    suspend fun importTasksPack(repository: RoutineRepository, rawText: String, merge: Boolean): Result<Pair<Int, List<String>>> {
        return try {
            val root = JSONObject(rawText.trim())
            val tasksArray = root.optJSONArray("tasks") ?: JSONArray()
            val catsArray = root.optJSONArray("categories") ?: JSONArray()

            val categories = mutableListOf<String>()
            for (i in 0 until catsArray.length()) {
                categories.add(catsArray.getString(i))
            }

            val tasks = mutableListOf<RoutineTask>()
            for (i in 0 until tasksArray.length()) {
                val o = tasksArray.getJSONObject(i)
                tasks.add(
                    RoutineTask(
                        title = o.getString("title"),
                        category = o.optString("category", "Health"),
                        timeOfDay = o.optString("timeOfDay", "Morning"),
                        targetMinutes = o.optInt("targetMinutes", 0),
                        daysOfWeek = o.optString("daysOfWeek", "1,2,3,4,5,6,7"),
                        repeatType = o.optString("repeatType", "WEEKLY"),
                        repeatIntervalDays = o.optInt("repeatIntervalDays", 1),
                        startEpochDay = System.currentTimeMillis() / (24 * 60 * 60 * 1000L),
                        isActive = true,
                        displayOrder = i
                    )
                )
            }

            repository.importTasksOnly(tasks, merge)
            Result.success(Pair(tasks.size, categories))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
