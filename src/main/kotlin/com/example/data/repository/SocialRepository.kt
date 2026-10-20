package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.social.BlockedUser
import com.example.data.model.social.FriendItem
import com.example.data.model.social.FriendProgress
import com.example.data.model.social.GroupChatMessage
import com.example.data.model.social.RoutineGroup
import com.example.data.model.social.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

data class AuthSessionUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val isEmailVerified: Boolean = true,
    val photoUrl: String = "",
    val username: String = ""
)

class SocialRepository(
    private val context: Context? = null
) {
    private val authPrefs: SharedPreferences? = context?.getSharedPreferences("wave_auth_prefs", Context.MODE_PRIVATE)
    private val accountsPrefs: SharedPreferences? = context?.getSharedPreferences("wave_user_accounts", Context.MODE_PRIVATE)

    val cloudDataRepository: CloudDataRepository = CloudDataRepository()

    private val _activeSessionUser = MutableStateFlow<AuthSessionUser?>(
        AuthSessionUser("local_user", "", "Achiever", true, "", "")
    )
    val activeSessionUser: StateFlow<AuthSessionUser?> = _activeSessionUser.asStateFlow()

    val currentSessionUser: AuthSessionUser?
        get() = _activeSessionUser.value

    fun setSessionUser(sessionUser: AuthSessionUser) {
        _activeSessionUser.value = sessionUser
        authPrefs?.edit()
            ?.putString("user_uid", sessionUser.uid)
            ?.putString("user_email", sessionUser.email)
            ?.putString("user_name", sessionUser.displayName)
            ?.putString("user_photo", sessionUser.photoUrl)
            ?.apply()
    }

    fun getCurrentUserId(): String = "local_user"

    val authStateFlow: Flow<Any?> = flowOf(null)

    data class StoredAccount(
        val uid: String,
        val displayName: String,
        val username: String = "",
        val email: String = "",
        val photoUrl: String = ""
    )

    fun getStoredAccounts(): List<StoredAccount> = emptyList()
    fun getStoredAccount(email: String): StoredAccount? = null

    suspend fun updateUsername(newUsername: String, email: String = "", uid: String = ""): Result<String> {
        return Result.success(newUsername.trim())
    }

    fun getFriendsFlow(): Flow<List<FriendItem>> = flowOf(emptyList())
    fun getAllFriendsProgressFlow(): Flow<List<FriendProgress>> = flowOf(emptyList())
    fun getGroupMessagesFlow(groupId: String): Flow<List<GroupChatMessage>> = flowOf(emptyList())
    fun getUserGroupsFlow(): Flow<List<RoutineGroup>> = flowOf(emptyList())
    fun loadLocalChatMessages(groupId: String): List<GroupChatMessage> = emptyList()
}
