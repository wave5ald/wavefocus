package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.FocusSession
import com.example.data.model.NoteItem
import com.example.data.model.RoutineTask
import com.example.data.model.ScheduledScript
import com.example.data.model.TaskCompletionLog
import com.example.data.repository.RoutineRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class SyncState {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE,
    ERROR
}

/**
 * CloudSyncManager:
 * Provides Multi-User Account isolation, frictionless Sync Key / PIN authentication,
 * local-first auto-sync with smart conflict resolution (Last-Write-Wins for tasks/quests,
 * Append-Only for history logs and focus sessions), and manual JSON backup/restore.
 */
class CloudSyncManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("wave_focus_sync_prefs", Context.MODE_PRIVATE)
    private val appContext: Context = context.applicationContext

    private val _syncKey = MutableStateFlow<String?>(prefs.getString(KEY_SYNC_KEY, null))
    val syncKey: StateFlow<String?> = _syncKey.asStateFlow()

    private val _username = MutableStateFlow<String?>(prefs.getString(KEY_USERNAME, null))
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncMessage = MutableStateFlow("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(prefs.getLong(KEY_LAST_SYNC, 0L))
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    companion object {
        private const val TAG = "CloudSyncManager"
        private const val KEY_SYNC_KEY = "cloud_sync_key"
        private const val KEY_USERNAME = "cloud_username"
        private const val KEY_LAST_SYNC = "cloud_last_sync_timestamp"

        @Volatile
        private var instance: CloudSyncManager? = null

        fun getInstance(context: Context): CloudSyncManager {
            return instance ?: synchronized(this) {
                instance ?: CloudSyncManager(context.applicationContext).also { instance = it }
            }
        }

        fun generateRandomSyncKey(): String {
            val raw = UUID.randomUUID().toString().replace("-", "").uppercase()
            return "WAVE-${raw.substring(0, 4)}-${raw.substring(4, 8)}-${raw.substring(8, 12)}"
        }

        fun deriveSyncKeyFromCredentials(username: String, pin: String): String {
            val normalized = "${username.trim().lowercase()}:$pin"
            val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
            val hex = digest.joinToString("") { "%02x".format(it) }.uppercase()
            return "USER-${hex.substring(0, 4)}-${hex.substring(4, 8)}-${hex.substring(8, 12)}"
        }
    }

    private fun getFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance(com.example.desktop.platform.CloudConfig.FIRESTORE_DATABASE_ID)

    fun isConnected(): Boolean = !_syncKey.value.isNullOrBlank()

    fun loginWithSyncKey(key: String, userDisplay: String = "") {
        val cleanKey = key.trim().uppercase()
        if (cleanKey.isBlank()) return
        _syncKey.value = cleanKey
        val name = if (userDisplay.isNotBlank()) userDisplay.trim() else "User-${cleanKey.takeLast(4)}"
        _username.value = name

        prefs.edit()
            .putString(KEY_SYNC_KEY, cleanKey)
            .putString(KEY_USERNAME, name)
            .apply()

        _syncMessage.value = "Connected to vault $cleanKey"
    }

    fun loginWithUsernameAndPin(usernameStr: String, pin: String) {
        val cleanUser = usernameStr.trim()
        val derivedKey = deriveSyncKeyFromCredentials(cleanUser, pin.trim())
        loginWithSyncKey(derivedKey, cleanUser)
    }

    fun disconnect() {
        _syncKey.value = null
        _username.value = null
        _syncState.value = SyncState.IDLE
        _syncMessage.value = "Disconnected from cloud sync"
        prefs.edit()
            .remove(KEY_SYNC_KEY)
            .remove(KEY_USERNAME)
            .apply()
    }

    /**
     * Executes 2-way merge sync with the isolated cloud vault:
     * - Last-Write-Wins on tasks, routines, and scheduled scripts/quests based on updatedAt timestamp.
     * - Append-Only on completion history logs and focus sessions.
     */
    suspend fun performSync(repository: RoutineRepository): Result<String> = withContext(Dispatchers.IO) {
        val activeKey = _syncKey.value
        if (activeKey.isNullOrBlank()) {
            _syncState.value = SyncState.IDLE
            return@withContext Result.failure(IllegalStateException("No sync key configured"))
        }

        _syncState.value = SyncState.SYNCING
        _syncMessage.value = "Syncing with cloud vault..."

        try {
            val firestore = getFirestore()
            val vaultRef = firestore.collection("sync_vaults").document(activeKey)

            // --- 1. SYNC TASKS (Last-Write-Wins per item) ---
            val localTasks = repository.getAllTasksSync()
            val remoteTasksSnap = vaultRef.collection("tasks").get().awaitTask()
            val remoteTasksMap = mutableMapOf<String, Map<String, Any?>>()
            for (doc in remoteTasksSnap.documents) {
                doc.data?.let { remoteTasksMap[doc.id] = it }
            }

            // Push newer local tasks or missing tasks to cloud
            for (task in localTasks) {
                val docId = "task_${task.id}"
                val remoteData = remoteTasksMap[docId]
                val remoteUpdatedAt = (remoteData?.get("updatedAt") as? Number)?.toLong() ?: 0L

                if (remoteData == null || task.updatedAt >= remoteUpdatedAt) {
                    val map = hashMapOf(
                        "id" to task.id,
                        "title" to task.title,
                        "category" to task.category,
                        "timeOfDay" to task.timeOfDay,
                        "targetMinutes" to task.targetMinutes,
                        "daysOfWeek" to task.daysOfWeek,
                        "repeatType" to task.repeatType,
                        "repeatIntervalDays" to task.repeatIntervalDays,
                        "startEpochDay" to task.startEpochDay,
                        "isActive" to task.isActive,
                        "createdAt" to task.createdAt,
                        "updatedAt" to task.updatedAt,
                        "displayOrder" to task.displayOrder
                    )
                    vaultRef.collection("tasks").document(docId).set(map, SetOptions.merge()).awaitTask()
                }
            }

            // Pull newer remote tasks into local database
            for ((docId, rData) in remoteTasksMap) {
                val remoteTitle = rData["title"] as? String ?: continue
                val remoteUpdatedAt = (rData["updatedAt"] as? Number)?.toLong() ?: 0L
                val remoteId = (rData["id"] as? Number)?.toLong() ?: 0L

                val localMatch = localTasks.find { "task_${it.id}" == docId || it.title.equals(remoteTitle, ignoreCase = true) }
                if (localMatch == null) {
                    // Item exists in cloud but not locally -> insert locally
                    val newTask = RoutineTask(
                        id = 0L,
                        title = remoteTitle,
                        category = rData["category"] as? String ?: "Health",
                        timeOfDay = rData["timeOfDay"] as? String ?: "Morning",
                        targetMinutes = (rData["targetMinutes"] as? Number)?.toInt() ?: 15,
                        daysOfWeek = rData["daysOfWeek"] as? String ?: "1,2,3,4,5,6,7",
                        repeatType = rData["repeatType"] as? String ?: "WEEKLY",
                        repeatIntervalDays = (rData["repeatIntervalDays"] as? Number)?.toInt() ?: 1,
                        startEpochDay = (rData["startEpochDay"] as? Number)?.toLong() ?: 0L,
                        isActive = rData["isActive"] as? Boolean ?: true,
                        createdAt = (rData["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = remoteUpdatedAt,
                        displayOrder = (rData["displayOrder"] as? Number)?.toInt() ?: 0
                    )
                    repository.insertTask(newTask)
                } else if (remoteUpdatedAt > localMatch.updatedAt) {
                    // Remote has newer edits -> update local
                    val updatedTask = localMatch.copy(
                        title = remoteTitle,
                        category = rData["category"] as? String ?: localMatch.category,
                        timeOfDay = rData["timeOfDay"] as? String ?: localMatch.timeOfDay,
                        targetMinutes = (rData["targetMinutes"] as? Number)?.toInt() ?: localMatch.targetMinutes,
                        daysOfWeek = rData["daysOfWeek"] as? String ?: localMatch.daysOfWeek,
                        repeatType = rData["repeatType"] as? String ?: localMatch.repeatType,
                        repeatIntervalDays = (rData["repeatIntervalDays"] as? Number)?.toInt() ?: localMatch.repeatIntervalDays,
                        startEpochDay = (rData["startEpochDay"] as? Number)?.toLong() ?: localMatch.startEpochDay,
                        isActive = rData["isActive"] as? Boolean ?: localMatch.isActive,
                        updatedAt = remoteUpdatedAt,
                        displayOrder = (rData["displayOrder"] as? Number)?.toInt() ?: localMatch.displayOrder
                    )
                    repository.updateTask(updatedTask)
                }
            }

            // --- 2. SYNC SCHEDULED SCRIPTS / QUESTS (Last-Write-Wins per item) ---
            val localScripts = repository.getAllScriptsSync()
            val remoteScriptsSnap = vaultRef.collection("scripts").get().awaitTask()
            val remoteScriptsMap = mutableMapOf<String, Map<String, Any?>>()
            for (doc in remoteScriptsSnap.documents) {
                doc.data?.let { remoteScriptsMap[doc.id] = it }
            }

            for (script in localScripts) {
                val docId = "script_${script.id}"
                val rData = remoteScriptsMap[docId]
                val remoteUpdatedAt = (rData?.get("updatedAt") as? Number)?.toLong() ?: 0L

                if (rData == null || script.updatedAt >= remoteUpdatedAt) {
                    val map = hashMapOf(
                        "id" to script.id,
                        "title" to script.title,
                        "encryptedContent" to script.encryptedContent,
                        "iv" to script.iv,
                        "startDateEpochDay" to script.startDateEpochDay,
                        "endDateEpochDay" to script.endDateEpochDay,
                        "category" to script.category,
                        "updatedAt" to script.updatedAt,
                        "isEncrypted" to script.isEncrypted,
                        "isCompleted" to script.isCompleted,
                        "targetValue" to script.targetValue,
                        "xpReward" to script.xpReward,
                        "questType" to script.questType,
                        "emoji" to script.emoji
                    )
                    vaultRef.collection("scripts").document(docId).set(map, SetOptions.merge()).awaitTask()
                }
            }

            for ((docId, rData) in remoteScriptsMap) {
                val remoteTitle = rData["title"] as? String ?: continue
                val remoteUpdatedAt = (rData["updatedAt"] as? Number)?.toLong() ?: 0L
                val localMatch = localScripts.find { "script_${it.id}" == docId }

                if (localMatch == null) {
                    val newScript = ScheduledScript(
                        id = 0L,
                        title = remoteTitle,
                        encryptedContent = rData["encryptedContent"] as? String ?: "",
                        iv = rData["iv"] as? String ?: "",
                        startDateEpochDay = (rData["startDateEpochDay"] as? Number)?.toLong() ?: 0L,
                        endDateEpochDay = (rData["endDateEpochDay"] as? Number)?.toLong() ?: 0L,
                        category = rData["category"] as? String ?: "Quest",
                        updatedAt = remoteUpdatedAt,
                        isEncrypted = rData["isEncrypted"] as? Boolean ?: false,
                        isCompleted = rData["isCompleted"] as? Boolean ?: false,
                        targetValue = rData["targetValue"] as? String ?: "",
                        xpReward = (rData["xpReward"] as? Number)?.toInt() ?: 50,
                        questType = rData["questType"] as? String ?: "Daily Quest",
                        emoji = rData["emoji"] as? String ?: "🎯"
                    )
                    repository.insertScript(newScript)
                } else if (remoteUpdatedAt > localMatch.updatedAt) {
                    val updated = localMatch.copy(
                        title = remoteTitle,
                        encryptedContent = rData["encryptedContent"] as? String ?: localMatch.encryptedContent,
                        iv = rData["iv"] as? String ?: localMatch.iv,
                        startDateEpochDay = (rData["startDateEpochDay"] as? Number)?.toLong() ?: localMatch.startDateEpochDay,
                        endDateEpochDay = (rData["endDateEpochDay"] as? Number)?.toLong() ?: localMatch.endDateEpochDay,
                        category = rData["category"] as? String ?: localMatch.category,
                        updatedAt = remoteUpdatedAt,
                        isEncrypted = rData["isEncrypted"] as? Boolean ?: localMatch.isEncrypted,
                        isCompleted = rData["isCompleted"] as? Boolean ?: localMatch.isCompleted,
                        targetValue = rData["targetValue"] as? String ?: localMatch.targetValue,
                        xpReward = (rData["xpReward"] as? Number)?.toInt() ?: localMatch.xpReward,
                        questType = rData["questType"] as? String ?: localMatch.questType,
                        emoji = rData["emoji"] as? String ?: localMatch.emoji
                    )
                    repository.updateScript(updated)
                }
            }

            // --- 3. SYNC HISTORY LOGS (Append-Only Strategy) ---
            val localLogs = repository.getAllLogsSync()
            val remoteLogsSnap = vaultRef.collection("task_logs").get().awaitTask()
            val remoteLogsMap = mutableMapOf<String, Map<String, Any?>>()
            for (doc in remoteLogsSnap.documents) {
                doc.data?.let { remoteLogsMap[doc.id] = it }
            }

            for (log in localLogs) {
                val docId = "log_${log.taskId}_${log.dateEpochDay}"
                val rData = remoteLogsMap[docId]
                if (rData == null) {
                    val map = hashMapOf(
                        "taskId" to log.taskId,
                        "dateEpochDay" to log.dateEpochDay,
                        "isCompleted" to log.isCompleted,
                        "completedAt" to log.completedAt
                    )
                    vaultRef.collection("task_logs").document(docId).set(map, SetOptions.merge()).awaitTask()
                }
            }

            for ((docId, rData) in remoteLogsMap) {
                val taskId = (rData["taskId"] as? Number)?.toLong() ?: continue
                val dateEpoch = (rData["dateEpochDay"] as? Number)?.toLong() ?: continue
                val isCompleted = rData["isCompleted"] as? Boolean ?: true
                val completedAt = (rData["completedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()

                val existsLocally = localLogs.any { it.taskId == taskId && it.dateEpochDay == dateEpoch }
                if (!existsLocally) {
                    repository.toggleTaskCompletion(taskId, java.time.LocalDate.ofEpochDay(dateEpoch), isCompleted)
                }
            }

            // --- 4. SYNC FOCUS SESSIONS (Append-Only Strategy) ---
            val localSessions = repository.getAllFocusSessionsSync()
            val remoteSessionsSnap = vaultRef.collection("focus_sessions").get().awaitTask()
            val remoteSessionsMap = mutableMapOf<String, Map<String, Any?>>()
            for (doc in remoteSessionsSnap.documents) {
                doc.data?.let { remoteSessionsMap[doc.id] = it }
            }

            for (session in localSessions) {
                val docId = "session_${session.completedAt}_${session.durationSeconds}"
                if (!remoteSessionsMap.containsKey(docId)) {
                    val map = hashMapOf(
                        "taskId" to session.taskId,
                        "taskTitle" to session.taskTitle,
                        "durationSeconds" to session.durationSeconds,
                        "mode" to session.mode,
                        "completedAt" to session.completedAt,
                        "dateEpochDay" to session.dateEpochDay
                    )
                    vaultRef.collection("focus_sessions").document(docId).set(map, SetOptions.merge()).awaitTask()
                }
            }

            for ((docId, rData) in remoteSessionsMap) {
                val completedAt = (rData["completedAt"] as? Number)?.toLong() ?: continue
                val duration = (rData["durationSeconds"] as? Number)?.toInt() ?: continue
                val exists = localSessions.any { it.completedAt == completedAt && it.durationSeconds == duration }
                if (!exists) {
                    val newSession = FocusSession(
                        id = 0L,
                        taskId = (rData["taskId"] as? Number)?.toLong(),
                        taskTitle = rData["taskTitle"] as? String ?: "",
                        durationSeconds = duration,
                        mode = rData["mode"] as? String ?: "Pomodoro",
                        completedAt = completedAt,
                        dateEpochDay = (rData["dateEpochDay"] as? Number)?.toLong() ?: java.time.LocalDate.now().toEpochDay()
                    )
                    repository.insertFocusSession(newSession)
                }
            }

            // --- 5. VAULT METADATA UPDATE ---
            val now = System.currentTimeMillis()
            vaultRef.set(
                mapOf(
                    "syncKey" to activeKey,
                    "lastSyncTimestamp" to now,
                    "devicePlatform" to com.example.desktop.platform.CloudConfig.DEVICE_PLATFORM,
                    "version" to 2
                ),
                SetOptions.merge()
            ).awaitTask()

            _lastSyncTimestamp.value = now
            prefs.edit().putLong(KEY_LAST_SYNC, now).apply()

            _syncState.value = SyncState.SYNCED
            _syncMessage.value = "Synced successfully with cloud vault"
            Result.success("Sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}", e)
            _syncState.value = SyncState.ERROR
            _syncMessage.value = "Sync failed: ${e.localizedMessage ?: "Network or permission error"}"
            Result.failure(e)
        }
    }

    /**
     * Manual Backup export: generates a clean JSON snapshot.
     */
    suspend fun exportBackupJson(repository: RoutineRepository): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "WaveFocus")
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("syncKey", _syncKey.value ?: "")
        root.put("username", _username.value ?: "")

        val tasksArray = JSONArray()
        for (t in repository.getAllTasksSync()) {
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
            obj.put("createdAt", t.createdAt)
            obj.put("updatedAt", t.updatedAt)
            tasksArray.put(obj)
        }
        root.put("tasks", tasksArray)

        val scriptsArray = JSONArray()
        for (s in repository.getAllScriptsSync()) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("title", s.title)
            obj.put("category", s.category)
            obj.put("startDateEpochDay", s.startDateEpochDay)
            obj.put("endDateEpochDay", s.endDateEpochDay)
            obj.put("isCompleted", s.isCompleted)
            obj.put("targetValue", s.targetValue)
            obj.put("xpReward", s.xpReward)
            obj.put("emoji", s.emoji)
            obj.put("updatedAt", s.updatedAt)
            scriptsArray.put(obj)
        }
        root.put("scripts", scriptsArray)

        val logsArray = JSONArray()
        for (l in repository.getAllLogsSync()) {
            val obj = JSONObject()
            obj.put("taskId", l.taskId)
            obj.put("dateEpochDay", l.dateEpochDay)
            obj.put("isCompleted", l.isCompleted)
            obj.put("completedAt", l.completedAt)
            logsArray.put(obj)
        }
        root.put("logs", logsArray)

        val sessionsArray = JSONArray()
        for (f in repository.getAllFocusSessionsSync()) {
            val obj = JSONObject()
            obj.put("taskId", f.taskId ?: 0L)
            obj.put("taskTitle", f.taskTitle)
            obj.put("durationSeconds", f.durationSeconds)
            obj.put("mode", f.mode)
            obj.put("completedAt", f.completedAt)
            obj.put("dateEpochDay", f.dateEpochDay)
            sessionsArray.put(obj)
        }
        root.put("focusSessions", sessionsArray)

        root.toString(2)
    }

    /**
     * Manual Backup import: parses JSON and imports items with smart merging.
     */
    suspend fun importBackupJson(jsonString: String, repository: RoutineRepository): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            var count = 0

            // Restore tasks
            val tasksArr = root.optJSONArray("tasks")
            if (tasksArr != null) {
                val list = mutableListOf<RoutineTask>()
                for (i in 0 until tasksArr.length()) {
                    val obj = tasksArr.getJSONObject(i)
                    list.add(
                        RoutineTask(
                            id = 0L,
                            title = obj.getString("title"),
                            category = obj.optString("category", "Health"),
                            timeOfDay = obj.optString("timeOfDay", "Morning"),
                            targetMinutes = obj.optInt("targetMinutes", 15),
                            daysOfWeek = obj.optString("daysOfWeek", "1,2,3,4,5,6,7"),
                            repeatType = obj.optString("repeatType", "WEEKLY"),
                            repeatIntervalDays = obj.optInt("repeatIntervalDays", 1),
                            startEpochDay = obj.optLong("startEpochDay", 0L),
                            isActive = obj.optBoolean("isActive", true),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                    count++
                }
                repository.insertTasks(list)
            }

            // Restore scripts
            val scriptsArr = root.optJSONArray("scripts")
            if (scriptsArr != null) {
                for (i in 0 until scriptsArr.length()) {
                    val obj = scriptsArr.getJSONObject(i)
                    repository.insertScript(
                        ScheduledScript(
                            id = 0L,
                            title = obj.getString("title"),
                            category = obj.optString("category", "Quest"),
                            startDateEpochDay = obj.optLong("startDateEpochDay", java.time.LocalDate.now().toEpochDay()),
                            endDateEpochDay = obj.optLong("endDateEpochDay", java.time.LocalDate.now().toEpochDay()),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            targetValue = obj.optString("targetValue", ""),
                            xpReward = obj.optInt("xpReward", 50),
                            emoji = obj.optString("emoji", "🎯"),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                    count++
                }
            }

            // Restore logs
            val logsArr = root.optJSONArray("logs")
            if (logsArr != null) {
                val logsList = mutableListOf<TaskCompletionLog>()
                for (i in 0 until logsArr.length()) {
                    val obj = logsArr.getJSONObject(i)
                    logsList.add(
                        TaskCompletionLog(
                            id = 0L,
                            taskId = obj.getLong("taskId"),
                            dateEpochDay = obj.getLong("dateEpochDay"),
                            isCompleted = obj.getBoolean("isCompleted"),
                            completedAt = obj.optLong("completedAt", System.currentTimeMillis())
                        )
                    )
                    count++
                }
                repository.restoreDatabase(
                    tasks = emptyList(),
                    logs = logsList,
                    focusSessions = emptyList(),
                    scripts = emptyList()
                )
            }

            // Restore focus sessions
            val sessionsArr = root.optJSONArray("focusSessions")
            if (sessionsArr != null) {
                for (i in 0 until sessionsArr.length()) {
                    val obj = sessionsArr.getJSONObject(i)
                    repository.insertFocusSession(
                        FocusSession(
                            id = 0L,
                            taskId = if (obj.optLong("taskId", 0L) > 0) obj.getLong("taskId") else null,
                            taskTitle = obj.optString("taskTitle", ""),
                            durationSeconds = obj.optInt("durationSeconds", 1500),
                            mode = obj.optString("mode", "Pomodoro"),
                            completedAt = obj.optLong("completedAt", System.currentTimeMillis()),
                            dateEpochDay = obj.optLong("dateEpochDay", java.time.LocalDate.now().toEpochDay())
                        )
                    )
                    count++
                }
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private suspend fun <T> Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
