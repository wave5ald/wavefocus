package com.example.data.repository

import com.example.data.model.BoardElement
import com.example.data.model.BoardItem
import com.example.data.model.FocusSession
import com.example.data.model.NoteItem
import com.example.data.model.RoutineTask
import com.example.data.model.ScheduledScript
import com.example.data.model.TaskCompletionLog
import com.example.data.model.social.UserProfile

data class UserPreferencesPayload(
    val coinsBalance: Int = 0,
    val totalCoinsEarned: Int = 0,
    val themeMode: String = "system",
    val customPagesJson: String = "",
    val inventoryCopper: Int = 0,
    val inventoryIron: Int = 0,
    val inventoryGold: Int = 0,
    val inventoryDiamond: Int = 0,
    val inventoryEmerald: Int = 0,
    val blockedUsersJson: String = "",
    val customStickersJson: String = "",
    val privacyAllowFriendRequests: Boolean = true,
    val privacyAllowMessageInvites: Boolean = true,
    val privacyAllowGhostMessages: Boolean = true
)

class CloudDataRepository {
    suspend fun claimOrUpdateUsername(rawUsername: String, uid: String, email: String, displayName: String): Result<String> {
        return Result.success(rawUsername.trim().lowercase())
    }

    suspend fun isUsernameAvailable(rawUsername: String, currentUid: String): Boolean = true

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = Result.success(Unit)

    suspend fun fetchUserProfile(uid: String): UserProfile? = null

    suspend fun syncTaskToCloud(uid: String, task: RoutineTask) {}
    suspend fun deleteTaskFromCloud(uid: String, taskId: Long) {}
    suspend fun fetchAllTasksFromCloud(uid: String): List<RoutineTask> = emptyList()
    suspend fun syncAllTasksToCloud(uid: String, tasks: List<RoutineTask>) {}

    suspend fun syncLogToCloud(uid: String, log: TaskCompletionLog) {}
    suspend fun deleteLogFromCloud(uid: String, taskId: Long, dateEpochDay: Long) {}
    suspend fun fetchAllLogsFromCloud(uid: String): List<TaskCompletionLog> = emptyList()
    suspend fun syncAllLogsToCloud(uid: String, logs: List<TaskCompletionLog>) {}

    suspend fun syncFocusSessionToCloud(uid: String, session: FocusSession) {}
    suspend fun fetchAllFocusSessionsFromCloud(uid: String): List<FocusSession> = emptyList()
    suspend fun syncAllFocusSessionsToCloud(uid: String, sessions: List<FocusSession>) {}

    suspend fun syncScriptToCloud(uid: String, script: ScheduledScript) {}
    suspend fun deleteScriptFromCloud(uid: String, scriptId: Long) {}
    suspend fun fetchAllScriptsFromCloud(uid: String): List<ScheduledScript> = emptyList()
    suspend fun syncAllScriptsToCloud(uid: String, scripts: List<ScheduledScript>) {}

    suspend fun syncNoteToCloud(uid: String, note: NoteItem) {}
    suspend fun deleteNoteFromCloud(uid: String, noteId: Long) {}
    suspend fun syncBoardToCloud(uid: String, board: BoardItem, elements: List<BoardElement> = emptyList()) {}
    suspend fun deleteBoardFromCloud(uid: String, boardId: Long) {}
    suspend fun fetchAllNotesFromCloud(uid: String): List<NoteItem> = emptyList()
    suspend fun fetchAllBoardsFromCloud(uid: String): Pair<List<BoardItem>, List<BoardElement>> = emptyList<BoardItem>() to emptyList()

    suspend fun syncPreferencesToCloud(uid: String, prefs: UserPreferencesPayload) {}
    suspend fun fetchPreferencesFromCloud(uid: String): UserPreferencesPayload? = null
}
