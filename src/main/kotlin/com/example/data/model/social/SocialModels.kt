package com.example.data.model.social

data class UserProfile(
    val uid: String = "local_user",
    val username: String = "",
    val displayName: String = "Achiever",
    val email: String = "",
    val photoUrl: String = "",
    val avatarEmoji: String = "⚡",
    val bio: String = "Focusing on daily routines & consistency",
    val dailyGoal: String = "4 routines completed / day",
    val routinesSharedCount: Int = 0,
    val totalFocusMinutes: Long = 0L,
    val streakDays: Int = 0,
    val totalHistoryCompletedTasks: Int = 0,
    val isEmailVerified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class FriendItem(
    val email: String = "",
    val username: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val status: String = "active",
    val addedAt: Long = System.currentTimeMillis()
)

data class FriendProgress(
    val email: String = "",
    val username: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val streakDays: Int = 1,
    val historyCompletedTasks: Int = 0,
    val currentRoutineName: String = "Daily Routine",
    val statusText: String = "In Progress",
    val cheerCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class RoutineGroup(
    val id: String = "group_1",
    val name: String = "",
    val description: String = "",
    val iconEmoji: String = "🎯",
    val memberCount: Int = 1,
    val isDirectMessage: Boolean = false,
    val friendEmail: String = "",
    val friendUsername: String = "",
    val friendPhotoUrl: String = "",
    val isGhostChat: Boolean = false,
    val ghostRecipientUsername: String = ""
)

data class GroupChatMessage(
    val id: String = "",
    val groupId: String = "",
    val senderEmail: String = "",
    val senderUsername: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class BlockedUser(
    val username: String = "",
    val email: String = "",
    val name: String = "",
    val blockedAt: Long = System.currentTimeMillis()
)

data class PrivacySettings(
    val allowFriendRequests: Boolean = true,
    val allowMessageInvites: Boolean = true,
    val allowGhostMessages: Boolean = true
)
