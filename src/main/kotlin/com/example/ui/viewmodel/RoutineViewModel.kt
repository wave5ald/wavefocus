package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.db.AppDatabase
import com.example.data.model.BoardElement
import com.example.data.model.BoardItem
import com.example.data.model.CategoryItem
import com.example.data.model.DaySummary
import com.example.data.model.FocusPrize
import com.example.data.model.FocusSession
import com.example.data.model.NoteItem
import com.example.data.model.PrizeInventory
import com.example.data.model.RoutinePreset
import com.example.data.model.PresetTaskItem
import com.example.data.model.ScheduledPresetPlan
import com.example.data.model.RoutineTask
import com.example.util.CategoryLocalization
import com.example.data.model.ScheduledScript
import com.example.data.model.TaskCompletionLog
import com.example.data.model.TaskWithStatus
import com.example.data.model.social.BlockedUser
import com.example.data.model.social.FriendItem
import com.example.data.model.social.PrivacySettings
import com.example.data.model.social.RoutineGroup
import com.example.data.model.social.UserProfile
import com.example.data.repository.RoutineRepository
import com.example.data.security.CryptoManager
import com.example.data.sync.CloudSyncManager
import com.example.data.sync.SyncState
import com.example.service.FocusTimerService
import com.example.util.HijriCalendarHelper
import com.example.util.LocationWeather
import com.example.util.OfflineWeatherHelper
import com.example.util.WeatherService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth

enum class AppEdition(val id: String) {
    STANDARD("standard"),
    ISLAMIC("islamic")
}

enum class NavTab {
    ROUTINE,
    FOCUS,
    SCRIPTS,
    NOTES,
    ANALYTICS,
    CUSTOM_PAGE
}

data class CustomPageItem(
    val id: String,
    val title: String,
    val iconName: String = "Star",
    val description: String = "",
    val items: List<String> = emptyList()
)

enum class TimerMode(val title: String, val minutes: Int) {
    POMODORO("Focus", 25),
    DEEP_WORK("Focus", 50),
    SHORT_BREAK("Focus", 5),
    CUSTOM("Focus", 25)
}

data class AnalyticsData(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val completionPast7Days: List<DaySummary> = emptyList(),
    val completionPast30Days: List<DaySummary> = emptyList(),
    val completionAllTime: List<DaySummary> = emptyList(),
    val totalFocusMinutesThisWeek: Int = 0,
    val totalFocusMinutesAllTime: Int = 0,
    val categoryDistribution: Map<String, Int> = emptyMap(),
    val pastDaysHistory: List<DaySummary> = emptyList(),
    val totalCoins: Int = 0,
    val totalCoinsEarned: Int = 0,
    val inventory: PrizeInventory = PrizeInventory()
) {
    val allTimeCompletedTasks: Int
        get() = completionAllTime.sumOf { it.completedTasks }

    val allTimeTotalTasks: Int
        get() = completionAllTime.sumOf { it.totalTasks }

    val allTimeCompletionPercentage: Float
        get() = if (allTimeTotalTasks > 0) (allTimeCompletedTasks.toFloat() / allTimeTotalTasks.toFloat()) * 100f else 0f
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RoutineViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("daily_routine_prefs", Context.MODE_PRIVATE)

    private val standardDatabase = AppDatabase.getDatabaseForUser(application, "local_user")
    val standardRepository: RoutineRepository = RoutineRepository(standardDatabase.routineDao())

    private val islamicDatabase = AppDatabase.getDatabaseForUser(application, "islamic_user")
    val islamicRepository: RoutineRepository = RoutineRepository(islamicDatabase.routineDao())

    private val _appEdition = MutableStateFlow(
        if (prefs.getString("active_app_edition", "standard") == "islamic") AppEdition.ISLAMIC else AppEdition.STANDARD
    )
    val appEdition: StateFlow<AppEdition> = _appEdition.asStateFlow()

    fun getActiveRepository(): RoutineRepository = if (_appEdition.value == AppEdition.ISLAMIC) islamicRepository else standardRepository
    val repository: RoutineRepository get() = getActiveRepository()

    // --- Profile State ---
    private val _isUserSignedIn = MutableStateFlow(true)
    val isUserSignedIn: StateFlow<Boolean> = _isUserSignedIn.asStateFlow()

    private val _isOnboarded = MutableStateFlow(true)
    val isOnboarded: StateFlow<Boolean> = _isOnboarded.asStateFlow()

    private val _needsUsernamePrompt = MutableStateFlow(false)
    val needsUsernamePrompt: StateFlow<Boolean> = _needsUsernamePrompt.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _userProfile = MutableStateFlow(
        UserProfile(
            uid = "local_user",
            displayName = prefs.getString("user_name", "Achiever") ?: "Achiever",
            email = prefs.getString("user_email", "") ?: "",
            photoUrl = prefs.getString("user_photo_url", "") ?: "",
            avatarEmoji = prefs.getString("user_avatar", "⚡") ?: "⚡",
            bio = prefs.getString("user_bio", "Building daily discipline & crushing goals") ?: "Building daily discipline & crushing goals",
            dailyGoal = prefs.getString("user_goal", "4 routines completed / day") ?: "4 routines completed / day",
            isEmailVerified = true
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Navigation & Messages
    private val _currentTab = MutableStateFlow(NavTab.ROUTINE)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _appLanguage = MutableStateFlow(
        prefs.getString("app_language", null)
            ?: if (java.util.Locale.getDefault().language == "ar") "ar" else "en"
    )
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _calendarMonth = MutableStateFlow(YearMonth.now())
    val calendarMonth: StateFlow<YearMonth> = _calendarMonth.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Custom Pages & Workspaces
    private val _customPages = MutableStateFlow(loadCustomPagesFromPrefs())
    val customPages: StateFlow<List<CustomPageItem>> = _customPages.asStateFlow()

    private val _activeCustomPage = MutableStateFlow<CustomPageItem?>(null)
    val activeCustomPage: StateFlow<CustomPageItem?> = _activeCustomPage.asStateFlow()

    // Economy & Coins
    private val _coinsBalance = MutableStateFlow(prefs.getInt("coins_balance", 0))
    val coinsBalance: StateFlow<Int> = _coinsBalance.asStateFlow()

    private val _totalCoinsEarned = MutableStateFlow(prefs.getInt("total_coins_earned", 0))
    val totalCoinsEarned: StateFlow<Int> = _totalCoinsEarned.asStateFlow()

    private val _inventory = MutableStateFlow(loadInventoryFromPrefs())
    val inventory: StateFlow<PrizeInventory> = _inventory.asStateFlow()

    // Categories
    private val _allCategories = MutableStateFlow(loadCategoriesFromPrefs())
    val allCategories: StateFlow<List<CategoryItem>> = _allCategories.asStateFlow()

    // Routine Presets & Scheduled Routine Forms
    private val _allPresets = MutableStateFlow(loadPresetsFromPrefs())
    val allPresets: StateFlow<List<RoutinePreset>> = _allPresets.asStateFlow()

    private val _scheduledPresets = MutableStateFlow(loadScheduledPresetsFromPrefs())
    val scheduledPresets: StateFlow<List<ScheduledPresetPlan>> = _scheduledPresets.asStateFlow()

    // Routine Tasks Flow
    val allActiveTasks: StateFlow<List<RoutineTask>> = _appEdition
        .flatMapLatest { edition ->
            val repo = if (edition == AppEdition.ISLAMIC) islamicRepository else standardRepository
            repo.allActiveTasks
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasksForSelectedDate: StateFlow<List<TaskWithStatus>> = combine(_selectedDate, _appEdition) { date, edition ->
        Pair(date, edition)
    }.flatMapLatest { (date, edition) ->
        val repo = if (edition == AppEdition.ISLAMIC) islamicRepository else standardRepository
        repo.getTasksWithStatusForDate(date)
    }.flowOn(Dispatchers.IO)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Focus Timer State
    private val _timerMode = MutableStateFlow(TimerMode.POMODORO)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    private val _timerTotalSeconds = MutableStateFlow(25 * 60)
    val timerTotalSeconds: StateFlow<Int> = _timerTotalSeconds.asStateFlow()

    private val _timerRemainingSeconds = MutableStateFlow(25 * 60)
    val timerRemainingSeconds: StateFlow<Int> = _timerRemainingSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _linkedRoutineTask = MutableStateFlow<RoutineTask?>(null)
    val linkedRoutineTask: StateFlow<RoutineTask?> = _linkedRoutineTask.asStateFlow()

    private var timerJob: Job? = null
    private var timerEndEpochMs: Long = 0L

    val todayFocusSessions: StateFlow<List<FocusSession>> = combine(_selectedDate, _appEdition) { date, edition ->
        Pair(date, edition)
    }.flatMapLatest { (date, edition) ->
        val repo = if (edition == AppEdition.ISLAMIC) islamicRepository else standardRepository
        repo.getFocusSessionsForDate(date)
    }.flowOn(Dispatchers.IO)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Scheduled Scripts & Notes
    val allScripts: StateFlow<List<ScheduledScript>> = _appEdition
        .flatMapLatest { edition ->
            val repo = if (edition == AppEdition.ISLAMIC) islamicRepository else standardRepository
            repo.allScripts
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scriptsForSelectedDate: StateFlow<List<ScheduledScript>> = combine(_selectedDate, _appEdition) { date, edition ->
        Pair(date, edition)
    }.flatMapLatest { (date, edition) ->
        val repo = if (edition == AppEdition.ISLAMIC) islamicRepository else standardRepository
        repo.getScriptsForDate(date)
    }.flowOn(Dispatchers.IO)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notes & Boards
    val allNotes: StateFlow<List<NoteItem>> = _appEdition
        .flatMapLatest { edition ->
            val repo = if (edition == AppEdition.ISLAMIC) islamicRepository else standardRepository
            repo.allNotes
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val standaloneNotes: StateFlow<List<NoteItem>> = _appEdition
        .flatMapLatest { edition ->
            val repo = if (edition == AppEdition.ISLAMIC) islamicRepository else standardRepository
            repo.standaloneNotes
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBoards: StateFlow<List<BoardItem>> = _appEdition
        .flatMapLatest { edition ->
            val repo = if (edition == AppEdition.ISLAMIC) islamicRepository else standardRepository
            repo.allBoards
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getNotesForBoard(boardId: Long) = getActiveRepository().getNotesForBoard(boardId)
    fun getElementsByBoard(boardId: Long) = getActiveRepository().getElementsByBoardId(boardId)

    // Analytics Engine
    val analyticsData: StateFlow<AnalyticsData> = _appEdition.flatMapLatest { edition ->
        val repo = if (edition == AppEdition.ISLAMIC) islamicRepository else standardRepository
        combine(
            repo.allTasks,
            repo.getAllLogs(),
            repo.allFocusSessions,
            _coinsBalance,
            _inventory
        ) { tasks, logs, focusSessions, coins, inv ->
            calculateAnalytics(tasks, logs, focusSessions, coins, inv)
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsData())

    // Weather
    private val _currentWeather = MutableStateFlow<LocationWeather?>(null)
    val currentWeather: StateFlow<LocationWeather?> = _currentWeather.asStateFlow()

    private val _isWeatherLoading = MutableStateFlow(false)
    val isWeatherLoading: StateFlow<Boolean> = _isWeatherLoading.asStateFlow()

    // Backward-compatibility stubs for clean compilation
    val friendsList: StateFlow<List<FriendItem>> = MutableStateFlow(emptyList<FriendItem>()).asStateFlow()
    val activeChatGroup: StateFlow<RoutineGroup?> = MutableStateFlow<RoutineGroup?>(null).asStateFlow()
    val blockedUsers: StateFlow<List<BlockedUser>> = MutableStateFlow(emptyList<BlockedUser>()).asStateFlow()
    val privacySettings: StateFlow<PrivacySettings> = MutableStateFlow(PrivacySettings()).asStateFlow()

    // Cloud Sync Manager (Multi-User Cloud Sync & Account Isolation)
    val cloudSyncManager = CloudSyncManager.getInstance(application)
    val syncState: StateFlow<SyncState> = cloudSyncManager.syncState
    val syncKey: StateFlow<String?> = cloudSyncManager.syncKey
    val syncUsername: StateFlow<String?> = cloudSyncManager.username
    val syncMessage: StateFlow<String> = cloudSyncManager.syncMessage
    val lastSyncTimestamp: StateFlow<Long> = cloudSyncManager.lastSyncTimestamp

    init {
        loadWeatherForCurrentLocation()
        syncTimerWithBackgroundService()
        checkAndApplyScheduledPresets(_selectedDate.value)
        viewModelScope.launch(Dispatchers.IO) {
            // Ensure the hijri/islamic database routine page starts completely empty
            if (!prefs.getBoolean("has_cleared_initial_islamic_tasks_v5", false)) {
                islamicRepository.dao.clearAllTasks()
                prefs.edit().putBoolean("has_cleared_initial_islamic_tasks_v5", true).apply()
            }
        }
        // Auto Cloud Sync on app startup if user has an active sync vault key
        if (cloudSyncManager.isConnected()) {
            viewModelScope.launch(Dispatchers.IO) {
                cloudSyncManager.performSync(repository)
            }
        }
    }

    // --- Profile & Photo Methods ---
    fun updateUserProfile(
        displayName: String,
        bio: String,
        avatarEmoji: String,
        dailyGoal: String,
        photoUrl: String? = null
    ) {
        val current = _userProfile.value
        val updated = current.copy(
            displayName = displayName.trim().ifBlank { current.displayName },
            bio = bio.trim().ifBlank { current.bio },
            avatarEmoji = avatarEmoji.ifBlank { current.avatarEmoji },
            dailyGoal = dailyGoal.trim().ifBlank { current.dailyGoal },
            photoUrl = photoUrl ?: current.photoUrl
        )
        _userProfile.value = updated
        prefs.edit()
            .putString("user_name", updated.displayName)
            .putString("user_bio", updated.bio)
            .putString("user_avatar", updated.avatarEmoji)
            .putString("user_goal", updated.dailyGoal)
            .putString("user_photo_url", updated.photoUrl)
            .apply()
        _userMessage.value = "Profile updated successfully!"
    }

    fun saveDevicePhotoAsProfile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profileImageFile = File(context.filesDir, "avatar_local_user.jpg")
                val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes()
                } ?: return@launch

                FileOutputStream(profileImageFile).use { output ->
                    output.write(bytes)
                }
                val savedPath = profileImageFile.absolutePath

                val current = _userProfile.value
                val updated = current.copy(photoUrl = savedPath)
                _userProfile.value = updated
                prefs.edit()
                    .putString("user_photo_url", savedPath)
                    .apply()

                _userMessage.value = "Profile photo updated!"
            } catch (e: Exception) {
                _userMessage.value = "Could not save photo: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun removeProfilePhoto() {
        val current = _userProfile.value
        val updated = current.copy(photoUrl = "")
        _userProfile.value = updated
        prefs.edit()
            .putString("user_photo_url", "")
            .apply()
        _userMessage.value = "Profile picture removed"
    }

    // --- Navigation & Controls ---
    fun setTab(tab: NavTab) {
        _currentTab.value = tab
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _calendarMonth.value = YearMonth.from(date)
        loadWeatherForCurrentLocation(date)
    }

    fun goToToday() {
        val today = LocalDate.now()
        _selectedDate.value = today
        _calendarMonth.value = YearMonth.from(today)
        loadWeatherForCurrentLocation(today)
    }

    fun previousDay() {
        val prev = _selectedDate.value.minusDays(1)
        _selectedDate.value = prev
        _calendarMonth.value = YearMonth.from(prev)
        loadWeatherForCurrentLocation(prev)
    }

    fun nextDay() {
        val next = _selectedDate.value.plusDays(1)
        _selectedDate.value = next
        _calendarMonth.value = YearMonth.from(next)
        loadWeatherForCurrentLocation(next)
    }

    fun nextCalendarMonth() {
        if (_appEdition.value == AppEdition.ISLAMIC) {
            val currHijriYear = HijriCalendarHelper.getHijriYear(_selectedDate.value)
            val currHijriMonth = HijriCalendarHelper.getHijriMonth(_selectedDate.value)
            var nextMonth = currHijriMonth + 1
            var nextYear = currHijriYear
            if (nextMonth > 12) {
                nextMonth = 1
                nextYear++
            }
            val newDate = HijriCalendarHelper.hijriToLocalDate(nextYear, nextMonth, 1)
            _selectedDate.value = newDate
            _calendarMonth.value = YearMonth.from(newDate)
            loadWeatherForCurrentLocation(newDate)
        } else {
            val newMonth = _calendarMonth.value.plusMonths(1)
            _calendarMonth.value = newMonth
            val newDate = if (_selectedDate.value.month == newMonth.month && _selectedDate.value.year == newMonth.year) {
                _selectedDate.value
            } else {
                newMonth.atDay(1)
            }
            _selectedDate.value = newDate
            loadWeatherForCurrentLocation(newDate)
        }
    }

    fun previousCalendarMonth() {
        if (_appEdition.value == AppEdition.ISLAMIC) {
            val currHijriYear = HijriCalendarHelper.getHijriYear(_selectedDate.value)
            val currHijriMonth = HijriCalendarHelper.getHijriMonth(_selectedDate.value)
            var prevMonth = currHijriMonth - 1
            var prevYear = currHijriYear
            if (prevMonth < 1) {
                prevMonth = 12
                prevYear--
            }
            val newDate = HijriCalendarHelper.hijriToLocalDate(prevYear, prevMonth, 1)
            _selectedDate.value = newDate
            _calendarMonth.value = YearMonth.from(newDate)
            loadWeatherForCurrentLocation(newDate)
        } else {
            val newMonth = _calendarMonth.value.minusMonths(1)
            _calendarMonth.value = newMonth
            val newDate = if (_selectedDate.value.month == newMonth.month && _selectedDate.value.year == newMonth.year) {
                _selectedDate.value
            } else {
                newMonth.atDay(1)
            }
            _selectedDate.value = newDate
            loadWeatherForCurrentLocation(newDate)
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit().putString("app_language", lang).apply()
        _userMessage.value = if (lang == "ar") "تم تفعيل اللغة العربية" else "English language enabled"
    }

    fun toggleLanguage() {
        val next = if (_appLanguage.value == "ar") "en" else "ar"
        setAppLanguage(next)
    }

    fun setAppEdition(edition: AppEdition) {
        if (_appEdition.value == edition) return
        _appEdition.value = edition
        prefs.edit().putString("active_app_edition", edition.id).apply()
        loadEditionData(edition)
        _userMessage.value = if (edition == AppEdition.ISLAMIC) {
            if (_appLanguage.value == "ar") "تم تفعيل التقويم الهجري 🌙" else "Switched to Hijri Calendar 🌙"
        } else {
            if (_appLanguage.value == "ar") "تمت العودة إلى التقويم الميلادي ⚡" else "Switched to Gregorian Calendar ⚡"
        }
    }

    fun toggleAppEdition() {
        val next = if (_appEdition.value == AppEdition.ISLAMIC) AppEdition.STANDARD else AppEdition.ISLAMIC
        setAppEdition(next)
    }

    private fun loadEditionData(edition: AppEdition) {
        _allCategories.value = loadCategoriesFromPrefs(edition)
        _allPresets.value = loadPresetsFromPrefs(edition)
        _scheduledPresets.value = loadScheduledPresetsFromPrefs(edition)
        _customPages.value = loadCustomPagesFromPrefs(edition)
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showUserMessage(message: String) {
        _userMessage.value = message
    }

    // --- Tasks Management ---
    fun addTask(
        title: String,
        category: String,
        timeOfDay: String,
        targetMinutes: Int,
        daysOfWeek: String = "1,2,3,4,5,6,7",
        repeatType: String = "WEEKLY",
        repeatIntervalDays: Int = 1,
        startEpochDay: Long = LocalDate.now().toEpochDay()
    ) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val task = RoutineTask(
                title = title.trim(),
                category = category,
                timeOfDay = timeOfDay,
                targetMinutes = targetMinutes,
                daysOfWeek = daysOfWeek,
                repeatType = repeatType,
                repeatIntervalDays = repeatIntervalDays,
                startEpochDay = startEpochDay,
                isActive = true,
                displayOrder = System.currentTimeMillis().toInt()
            )
            repository.insertTask(task)
            _userMessage.value = "Task added to daily routine"
            autoSyncCloud()
        }
    }

    fun updateTask(task: RoutineTask) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTask(task)
            _userMessage.value = "Routine task updated"
            autoSyncCloud()
        }
    }

    fun deleteTask(task: RoutineTask) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(task)
            _userMessage.value = "Routine task deleted"
            autoSyncCloud()
        }
    }

    fun deleteSelectedTasks(taskIds: Set<Long>) {
        if (taskIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTasksByIds(taskIds.toList())
            _userMessage.value = "Deleted ${taskIds.size} routine tasks"
            autoSyncCloud()
        }
    }

    fun toggleTask(taskId: Long, completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val date = _selectedDate.value
            repository.toggleTaskCompletion(taskId, date, completed)
            autoSyncCloud()
        }
    }

    fun removeAllTasksForDate(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            val epochDay = date.toEpochDay()
            val dayOfWeekInt = date.dayOfWeek.value
            val currentTasks = repository.dao.getAllTasksSync()
            val matchingTasks = currentTasks.filter { task ->
                RoutineRepository.isTaskScheduledForDate(task, date)
            }
            if (matchingTasks.isEmpty()) {
                _userMessage.value = "No tasks found for this date"
                return@launch
            }
            var count = 0
            for (task in matchingTasks) {
                if (task.repeatType.equals("SPECIFIC_DAY", ignoreCase = true) || task.repeatType.equals("ONCE", ignoreCase = true)) {
                    repository.deleteTask(task)
                    count++
                } else if (task.repeatType.equals("WEEKLY", ignoreCase = true)) {
                    val days = task.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableList()
                    days.remove(dayOfWeekInt)
                    if (days.isEmpty()) {
                        repository.deleteTask(task)
                    } else {
                        repository.updateTask(task.copy(daysOfWeek = days.joinToString(",")))
                    }
                    count++
                } else {
                    repository.deleteTask(task)
                    count++
                }
            }
            _userMessage.value = "Removed $count task(s) for ${date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))}"
        }
    }

    // --- Focus Timer Controls ---
    fun setTimerMode(mode: TimerMode, customMinutes: Int = 25) {
        pauseTimer()
        _timerMode.value = mode
        val mins = if (mode == TimerMode.CUSTOM) customMinutes else mode.minutes
        _timerTotalSeconds.value = mins * 60
        _timerRemainingSeconds.value = mins * 60
    }

    fun setTimerMinutes(minutes: Int) {
        pauseTimer()
        val totalSecs = (minutes * 60).coerceIn(60, 36000)
        _timerTotalSeconds.value = totalSecs
        _timerRemainingSeconds.value = totalSecs
    }

    fun setLinkedTask(task: RoutineTask?) {
        _linkedRoutineTask.value = task
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        timerEndEpochMs = System.currentTimeMillis() + (_timerRemainingSeconds.value * 1000L)
        val task = _linkedRoutineTask.value
        FocusTimerService.start(
            context = getApplication(),
            totalSeconds = _timerTotalSeconds.value,
            remainingSeconds = _timerRemainingSeconds.value,
            taskTitle = task?.title ?: "Deep Focus",
            taskId = task?.id,
            mode = _timerMode.value.title
        )
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value) {
                val now = System.currentTimeMillis()
                val remaining = ((timerEndEpochMs - now) / 1000L).coerceAtLeast(0L).toInt()
                _timerRemainingSeconds.value = remaining
                if (remaining <= 0) {
                    onTimerCompleted()
                    break
                }
                delay(500)
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        FocusTimerService.pause(getApplication())
    }

    fun resetTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        _timerRemainingSeconds.value = _timerTotalSeconds.value
        FocusTimerService.stop(getApplication())
    }

    fun syncTimerWithBackgroundService() {
        val timerPrefs = getApplication<Application>().getSharedPreferences(FocusTimerService.PREFS_NAME, Context.MODE_PRIVATE)
        val isRunning = timerPrefs.getBoolean(FocusTimerService.KEY_IS_RUNNING, false)
        if (isRunning) {
            val endTime = timerPrefs.getLong(FocusTimerService.KEY_END_TIME, 0L)
            val now = System.currentTimeMillis()
            if (endTime > now) {
                val remaining = ((endTime - now) / 1000L).toInt()
                val totalSecs = timerPrefs.getInt(FocusTimerService.KEY_TOTAL_SECONDS, remaining)
                _timerTotalSeconds.value = totalSecs
                _timerRemainingSeconds.value = remaining
                _isTimerRunning.value = true
                timerEndEpochMs = endTime
                timerJob?.cancel()
                timerJob = viewModelScope.launch {
                    while (_isTimerRunning.value) {
                        val curr = System.currentTimeMillis()
                        val rem = ((timerEndEpochMs - curr) / 1000L).coerceAtLeast(0L).toInt()
                        _timerRemainingSeconds.value = rem
                        if (rem <= 0) {
                            onTimerCompleted()
                            break
                        }
                        delay(500)
                    }
                }
            } else {
                _timerRemainingSeconds.value = 0
                _isTimerRunning.value = false
                onTimerCompleted()
            }
        }
    }

    fun addFiveMinutes() {
        adjustTimerSeconds(300)
    }

    fun subtractFiveMinutes() {
        adjustTimerSeconds(-300)
    }

    fun addOneMinute() {
        adjustTimerSeconds(60)
    }

    fun subtractOneMinute() {
        adjustTimerSeconds(-60)
    }

    fun adjustTimerSeconds(deltaSeconds: Int) {
        if (_isTimerRunning.value) {
            val newRemaining = (_timerRemainingSeconds.value + deltaSeconds).coerceAtLeast(10)
            _timerRemainingSeconds.value = newRemaining
            if (deltaSeconds > 0) {
                _timerTotalSeconds.value += deltaSeconds
            }
            timerEndEpochMs = System.currentTimeMillis() + (newRemaining * 1000L)
            val task = _linkedRoutineTask.value
            FocusTimerService.start(
                context = getApplication(),
                totalSeconds = _timerTotalSeconds.value,
                remainingSeconds = newRemaining,
                taskTitle = task?.title ?: "Deep Focus",
                taskId = task?.id,
                mode = _timerMode.value.title
            )
        } else {
            val newTotal = (_timerTotalSeconds.value + deltaSeconds).coerceIn(60, 36000)
            _timerTotalSeconds.value = newTotal
            _timerRemainingSeconds.value = newTotal
        }
    }

    private fun onTimerCompleted() {
        pauseTimer()
        val duration = _timerTotalSeconds.value
        val task = _linkedRoutineTask.value
        val modeTitle = "Focus"

        triggerVibration()

        viewModelScope.launch(Dispatchers.IO) {
            val session = FocusSession(
                taskId = task?.id,
                taskTitle = task?.title ?: "Deep Focus Session",
                durationSeconds = duration,
                mode = modeTitle,
                completedAt = System.currentTimeMillis(),
                dateEpochDay = LocalDate.now().toEpochDay()
            )
            repository.insertFocusSession(session)

            if (task != null) {
                repository.toggleTaskCompletion(task.id, LocalDate.now(), true)
            }
            _userMessage.value = "Focus session completed! Great work! 🎯"
            autoSyncCloud()
        }
        resetTimer()
    }

    private fun triggerVibration() {
        try {
            val app = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(longArrayOf(0, 400, 200, 400), -1)
            }
        } catch (_: Exception) {}
    }

    // --- Schedule, Goals & Quests ---
    fun saveDailyQuest(
        id: Long = 0,
        title: String,
        description: String = "",
        date: LocalDate,
        questType: String = "Daily Quest",
        emoji: String = "🎯",
        targetValue: String = "",
        xpReward: Int = 50
    ) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val script = ScheduledScript(
                id = id,
                title = title.trim(),
                encryptedContent = description.trim(),
                iv = "",
                startDateEpochDay = date.toEpochDay(),
                endDateEpochDay = date.toEpochDay(),
                category = "Quest",
                updatedAt = System.currentTimeMillis(),
                isEncrypted = false,
                isCompleted = false,
                targetValue = targetValue.trim(),
                xpReward = xpReward,
                questType = questType,
                emoji = emoji
            )
            if (id == 0L) {
                repository.insertScript(script)
                _userMessage.value = "🎯 Quest set for ${date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))}!"
            } else {
                repository.updateScript(script)
                _userMessage.value = "Quest updated"
            }
            autoSyncCloud()
        }
    }

    fun toggleQuestCompletion(id: Long, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getScriptById(id) ?: return@launch
            val updated = existing.copy(
                isCompleted = isCompleted,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateScript(updated)
            if (isCompleted) {
                triggerVibration()
                _userMessage.value = "🏆 Quest Complete!"
            }
            autoSyncCloud()
        }
    }

    fun saveScheduleNote(
        id: Long = 0,
        emoji: String = "📝",
        content: String,
        date: LocalDate
    ) {
        if (content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val script = ScheduledScript(
                id = id,
                title = emoji,
                encryptedContent = content.trim(),
                iv = "",
                startDateEpochDay = date.toEpochDay(),
                endDateEpochDay = date.toEpochDay(),
                category = "Note",
                updatedAt = System.currentTimeMillis(),
                isEncrypted = false,
                isCompleted = false,
                questType = "Note",
                emoji = emoji
            )
            if (id == 0L) {
                repository.insertScript(script)
                _userMessage.value = "Note added to schedule"
            } else {
                repository.updateScript(script)
                _userMessage.value = "Note updated"
            }
            autoSyncCloud()
        }
    }

    fun saveScript(
        id: Long = 0,
        title: String,
        content: String,
        startDate: LocalDate,
        endDate: LocalDate,
        category: String = "Daily Script",
        encrypt: Boolean = true
    ) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val (encryptedContent, iv) = if (encrypt) {
                CryptoManager.encrypt(content)
            } else {
                Pair(content, "")
            }

            val script = ScheduledScript(
                id = id,
                title = title.trim(),
                encryptedContent = encryptedContent,
                iv = iv,
                startDateEpochDay = startDate.toEpochDay(),
                endDateEpochDay = endDate.toEpochDay(),
                category = category,
                updatedAt = System.currentTimeMillis(),
                isEncrypted = encrypt,
                isCompleted = false
            )
            if (id == 0L) {
                repository.insertScript(script)
                _userMessage.value = "Schedule entry saved"
            } else {
                repository.updateScript(script)
                _userMessage.value = "Schedule entry updated"
            }
            autoSyncCloud()
        }
    }

    fun deleteScript(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteScript(id)
            _userMessage.value = "Item removed from schedule"
            autoSyncCloud()
        }
    }

    fun decryptScriptContent(script: ScheduledScript): String {
        return if (script.isEncrypted) {
            CryptoManager.decrypt(script.encryptedContent, script.iv)
        } else {
            script.encryptedContent
        }
    }

    // --- Notes & Boards ---
    fun saveNote(
        id: Long = 0,
        boardId: Long? = null,
        title: String,
        content: String,
        imageUri: String? = null,
        colorHex: String = "#1E293B",
        textColorHex: String = "#F8FAFC",
        posX: Float = 40f,
        posY: Float = 40f,
        pinned: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = NoteItem(
                id = id,
                boardId = boardId,
                title = title.trim(),
                content = content.trim(),
                imageUri = imageUri,
                colorHex = colorHex,
                textColorHex = textColorHex,
                posX = posX,
                posY = posY,
                pinned = pinned,
                updatedAt = System.currentTimeMillis()
            )
            if (id == 0L) {
                repository.insertNote(note)
                _userMessage.value = "Note created"
            } else {
                repository.updateNote(note)
                _userMessage.value = "Note updated"
            }
        }
    }

    fun deleteNoteById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNoteById(id)
            _userMessage.value = "Note deleted"
        }
    }

    fun deleteNotesByIds(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNotesByIds(ids.toList())
            _userMessage.value = "Deleted ${ids.size} notes"
        }
    }

    fun updateNotePosition(id: Long, posX: Float, posY: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = repository.getAllNotesSync().find { it.id == id } ?: return@launch
            repository.updateNote(note.copy(posX = posX, posY = posY))
        }
    }

    fun moveNotes(noteIds: Set<Long>, deltaX: Float, deltaY: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val notesMap = repository.getAllNotesSync().associateBy { it.id }
            noteIds.forEach { id ->
                val note = notesMap[id]
                if (note != null) {
                    val nx = (note.posX + deltaX).coerceAtLeast(0f)
                    val ny = (note.posY + deltaY).coerceAtLeast(0f)
                    repository.updateNote(note.copy(posX = nx, posY = ny))
                }
            }
        }
    }

    fun saveBoard(
        id: Long = 0,
        title: String,
        description: String = "",
        backgroundColorHex: String = "#0F172A"
    ) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val board = BoardItem(
                id = id,
                title = title.trim(),
                description = description.trim(),
                backgroundColorHex = backgroundColorHex,
                updatedAt = System.currentTimeMillis()
            )
            if (id == 0L) {
                repository.insertBoard(board)
                _userMessage.value = "Board created"
            } else {
                repository.updateBoard(board)
                _userMessage.value = "Board updated"
            }
        }
    }

    fun deleteBoardById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBoardById(id)
            _userMessage.value = "Board deleted"
        }
    }

    fun deleteBoardsByIds(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBoardsByIds(ids.toList())
            _userMessage.value = "Deleted ${ids.size} boards"
        }
    }

    fun deleteNotesAndBoards(noteIds: Set<Long>, boardIds: Set<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            if (noteIds.isNotEmpty()) {
                repository.deleteNotesByIds(noteIds.toList())
            }
            if (boardIds.isNotEmpty()) {
                repository.deleteBoardsByIds(boardIds.toList())
            }
            val total = noteIds.size + boardIds.size
            _userMessage.value = "Deleted $total items"
        }
    }

    fun saveBoardElement(
        id: Long = 0,
        boardId: Long,
        elementType: String,
        text: String = "",
        colorHex: String = "#3B82F6",
        textColorHex: String = "#FFFFFF",
        linkedNoteIds: String = "",
        imageUri: String? = null,
        posX: Float = 80f,
        posY: Float = 80f
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val elem = BoardElement(
                id = id,
                boardId = boardId,
                elementType = elementType,
                text = text.trim(),
                colorHex = colorHex,
                textColorHex = textColorHex,
                linkedNoteIds = linkedNoteIds,
                imageUri = imageUri,
                posX = posX,
                posY = posY
            )
            if (id == 0L) {
                repository.insertBoardElement(elem)
                _userMessage.value = "Added to board"
            } else {
                repository.updateBoardElement(elem)
                _userMessage.value = "Board element updated"
            }
        }
    }

    fun deleteBoardElementById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBoardElementById(id)
            _userMessage.value = "Item removed from board"
        }
    }

    fun moveBoardElements(elementIds: Set<Long>, deltaX: Float, deltaY: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val elemsMap = repository.getAllBoardElementsSync().associateBy { it.id }
            elementIds.forEach { id ->
                val elem = elemsMap[id]
                if (elem != null) {
                    val nx = (elem.posX + deltaX).coerceAtLeast(0f)
                    val ny = (elem.posY + deltaY).coerceAtLeast(0f)
                    repository.updateBoardElement(elem.copy(posX = nx, posY = ny))
                }
            }
        }
    }

    // --- Custom Workspaces / Pages ---
    fun openCustomPage(page: CustomPageItem) {
        _activeCustomPage.value = page
        setTab(NavTab.CUSTOM_PAGE)
    }

    fun createAndOpenNewPage(title: String, description: String = "", iconName: String = "Star") {
        val newPage = CustomPageItem(
            id = "page_${System.currentTimeMillis()}",
            title = title.ifBlank { "New Workspace" },
            iconName = iconName,
            description = description.ifBlank { "Custom workspace page" },
            items = emptyList()
        )
        val updated = _customPages.value + newPage
        _customPages.value = updated
        saveCustomPagesToPrefs(updated)
        openCustomPage(newPage)
        _userMessage.value = "Opened new page '${newPage.title}'!"
    }

    fun deleteCustomPage(pageId: String) {
        val updated = _customPages.value.filterNot { it.id == pageId }
        _customPages.value = updated
        saveCustomPagesToPrefs(updated)
        if (_activeCustomPage.value?.id == pageId) {
            _activeCustomPage.value = null
            setTab(NavTab.ROUTINE)
        }
    }

    fun addCustomPageGoal(pageId: String, text: String) {
        if (text.isBlank()) return
        val updated = _customPages.value.map { page ->
            if (page.id == pageId) {
                page.copy(items = page.items + text.trim())
            } else page
        }
        _customPages.value = updated
        saveCustomPagesToPrefs(updated)
        if (_activeCustomPage.value?.id == pageId) {
            _activeCustomPage.value = _customPages.value.firstOrNull { it.id == pageId }
        }
    }

    private fun loadCustomPagesFromPrefs(edition: AppEdition = _appEdition.value): List<CustomPageItem> {
        val prefKey = if (edition == AppEdition.ISLAMIC) "islamic_custom_pages_json" else "custom_pages_json"
        val standardDefaults = listOf(
            CustomPageItem(
                id = "goals_milestones",
                title = "Goals & Milestones",
                iconName = "Flag",
                description = "Track personal goals and milestones",
                items = emptyList()
            )
        )
        val raw = prefs.getString(prefKey, null) ?: return standardDefaults
        return try {
            val array = org.json.JSONArray(raw)
            val list = mutableListOf<CustomPageItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id")
                if (id == "islamic_goals") continue
                val itemsArr = obj.optJSONArray("items")
                val itemsList = mutableListOf<String>()
                if (itemsArr != null) {
                    for (j in 0 until itemsArr.length()) {
                        itemsList.add(itemsArr.getString(j))
                    }
                }
                list.add(
                    CustomPageItem(
                        id = id,
                        title = obj.optString("title"),
                        iconName = obj.optString("iconName"),
                        description = obj.optString("description"),
                        items = itemsList
                    )
                )
            }
            list.ifEmpty { standardDefaults }
        } catch (_: Exception) {
            standardDefaults
        }
    }

    private fun saveCustomPagesToPrefs(pages: List<CustomPageItem>) {
        try {
            val prefKey = if (_appEdition.value == AppEdition.ISLAMIC) "islamic_custom_pages_json" else "custom_pages_json"
            val array = org.json.JSONArray()
            for (p in pages) {
                val obj = org.json.JSONObject()
                obj.put("id", p.id)
                obj.put("title", p.title)
                obj.put("iconName", p.iconName)
                obj.put("description", p.description)
                val itemsArr = org.json.JSONArray()
                for (item in p.items) {
                    itemsArr.put(item)
                }
                obj.put("items", itemsArr)
                array.put(obj)
            }
            prefs.edit().putString(prefKey, array.toString()).apply()
        } catch (_: Exception) {}
    }

    // --- Categories ---
    fun addCategory(name: String, colorHex: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        if (_allCategories.value.any { it.name.equals(clean, ignoreCase = true) }) {
            _userMessage.value = "Category '$clean' already exists"
            return
        }
        val updated = _allCategories.value + CategoryItem(clean, colorHex, isCustom = true)
        _allCategories.value = updated
        saveCategoriesToPrefs(updated)
        _userMessage.value = "Created category '$clean'"
    }

    fun editCategory(oldName: String, newName: String, newColorHex: String) {
        val cleanNew = newName.trim()
        if (cleanNew.isBlank()) return
        val current = _allCategories.value
        if (!oldName.equals(cleanNew, ignoreCase = true) && current.any { it.name.equals(cleanNew, ignoreCase = true) }) {
            _userMessage.value = "Category '$cleanNew' already exists"
            return
        }
        val updated = current.map { cat ->
            if (cat.name.equals(oldName, ignoreCase = true)) {
                cat.copy(name = cleanNew, colorHex = newColorHex)
            } else {
                cat
            }
        }
        _allCategories.value = updated
        saveCategoriesToPrefs(updated)
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCategoryForTasks(oldName, cleanNew)
        }
        _userMessage.value = "Updated category '$cleanNew'"
    }

    fun deleteCategory(name: String) {
        if (CategoryLocalization.isMainCategory(name)) {
            _userMessage.value = "Cannot delete built-in category"
            return
        }
        val target = _allCategories.value.find { it.name.equals(name, ignoreCase = true) }
        if (target != null && !target.isCustom) {
            _userMessage.value = "Cannot delete built-in category"
            return
        }
        val updated = _allCategories.value.filterNot { it.name.equals(name, ignoreCase = true) }
        _allCategories.value = updated
        saveCategoriesToPrefs(updated)
        _userMessage.value = "Category '$name' removed"
    }

    private fun loadCategoriesFromPrefs(edition: AppEdition = _appEdition.value): List<CategoryItem> {
        val standardDefaults = listOf(
            CategoryItem("Health", "#10B981", false),
            CategoryItem("Focus", "#6366F1", false),
            CategoryItem("Mind", "#14B8A6", false),
            CategoryItem("Habit", "#EC4899", false),
            CategoryItem("Other", "#F97316", false)
        )
        val prefKey = if (edition == AppEdition.ISLAMIC) "islamic_categories_json" else "custom_categories_json"
        val raw = prefs.getString(prefKey, null) ?: return standardDefaults
        return try {
            val array = org.json.JSONArray(raw)
            val list = mutableListOf<CategoryItem>()
            val seenKeys = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.getString("name")
                // Filter out any previous religious category names so everything is same as standard
                if (name == "الصلاة والعبادات" || name == "القرآن الكريم" || name == "الأذكار اليومية" ||
                    name == "أعمال البر والصدقة" || name == "طلب العلم الشرعي" || name == "العادات والصحة"
                ) {
                    continue
                }
                val isCustom = obj.optBoolean("isCustom", !CategoryLocalization.isMainCategory(name))
                val colorHex = obj.optString("colorHex", "#6366F1")
                val cleanKey = name.trim().lowercase()
                if (cleanKey.isNotEmpty() && !seenKeys.contains(cleanKey)) {
                    list.add(
                        CategoryItem(
                            name = name,
                            colorHex = colorHex,
                            isCustom = isCustom
                        )
                    )
                    seenKeys.add(cleanKey)
                }
            }
            if (list.isEmpty()) standardDefaults else list
        } catch (_: Exception) {
            standardDefaults
        }
    }

    private fun saveCategoriesToPrefs(categories: List<CategoryItem>) {
        try {
            val prefKey = if (_appEdition.value == AppEdition.ISLAMIC) "islamic_categories_json" else "custom_categories_json"
            val array = org.json.JSONArray()
            for (c in categories) {
                val obj = org.json.JSONObject()
                obj.put("name", c.name)
                obj.put("colorHex", c.colorHex)
                obj.put("isCustom", c.isCustom)
                array.put(obj)
            }
            prefs.edit().putString(prefKey, array.toString()).apply()
        } catch (_: Exception) {}
    }

    // --- Routine Presets & Forms Engine ---
    fun activatePreset(preset: RoutinePreset, replaceExisting: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (replaceExisting) {
                repository.clearAllTasks()
            }
            val newTasks = preset.tasks.mapIndexed { index, item ->
                RoutineTask(
                    title = item.title,
                    category = item.category,
                    timeOfDay = item.timeOfDay,
                    targetMinutes = item.targetMinutes,
                    daysOfWeek = item.daysOfWeek,
                    repeatType = item.repeatType,
                    repeatIntervalDays = item.repeatIntervalDays,
                    isActive = true,
                    displayOrder = index
                )
            }
            repository.insertTasks(newTasks)
            _userMessage.value = "Activated preset '${preset.name}' (${newTasks.size} tasks)"
        }
    }

    fun schedulePresetForDate(preset: RoutinePreset, startDate: LocalDate, replaceExisting: Boolean, note: String = "") {
        val plan = ScheduledPresetPlan(
            presetId = preset.id,
            presetName = preset.name,
            presetIcon = preset.iconEmoji,
            presetColorHex = preset.colorHex,
            taskCount = preset.tasks.size,
            startDateEpochDay = startDate.toEpochDay(),
            note = note,
            replaceExisting = replaceExisting,
            isApplied = false
        )
        val updated = _scheduledPresets.value + plan
        _scheduledPresets.value = updated
        saveScheduledPresetsToPrefs(updated)
        _userMessage.value = "Scheduled '${preset.name}' for $startDate"
    }

    fun applyScheduledPresetNow(planId: String) {
        val plan = _scheduledPresets.value.find { it.id == planId } ?: return
        val preset = _allPresets.value.find { it.id == plan.presetId } ?: return
        activatePreset(preset, plan.replaceExisting)
        val updated = _scheduledPresets.value.map {
            if (it.id == planId) it.copy(isApplied = true) else it
        }
        _scheduledPresets.value = updated
        saveScheduledPresetsToPrefs(updated)
    }

    fun cancelScheduledPreset(planId: String) {
        val updated = _scheduledPresets.value.filterNot { it.id == planId }
        _scheduledPresets.value = updated
        saveScheduledPresetsToPrefs(updated)
        _userMessage.value = "Scheduled routine cancelled"
    }

    fun saveCurrentTasksAsPreset(presetName: String, iconEmoji: String, colorHex: String, description: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val activeTasks = repository.getAllTasksSync().filter { it.isActive }
            if (activeTasks.isEmpty()) {
                _userMessage.value = "No active tasks to save as preset"
                return@launch
            }
            val presetTasks = activeTasks.map { t ->
                PresetTaskItem(
                    title = t.title,
                    category = t.category,
                    timeOfDay = t.timeOfDay,
                    targetMinutes = t.targetMinutes,
                    daysOfWeek = t.daysOfWeek,
                    repeatType = t.repeatType,
                    repeatIntervalDays = t.repeatIntervalDays
                )
            }
            val newPreset = RoutinePreset(
                name = presetName.trim().ifBlank { "My Routine Preset" },
                description = description.trim(),
                iconEmoji = iconEmoji.ifBlank { "📋" },
                colorHex = colorHex.ifBlank { "#6366F1" },
                tasks = presetTasks,
                isBuiltIn = false
            )
            val updated = _allPresets.value + newPreset
            _allPresets.value = updated
            savePresetsToPrefs(updated)
            _userMessage.value = "Saved '${newPreset.name}' as preset (${presetTasks.size} tasks)"
        }
    }

    fun createOrUpdatePreset(preset: RoutinePreset) {
        val current = _allPresets.value
        val exists = current.any { it.id == preset.id }
        val updated = if (exists) {
            current.map { if (it.id == preset.id) preset else it }
        } else {
            current + preset
        }
        _allPresets.value = updated
        savePresetsToPrefs(updated)
        _userMessage.value = "Saved preset '${preset.name}'"
    }

    fun deletePreset(presetId: String) {
        val updated = _allPresets.value.filterNot { it.id == presetId }
        _allPresets.value = updated
        savePresetsToPrefs(updated)
        _userMessage.value = "Preset removed"
    }

    fun checkAndApplyScheduledPresets(currentDate: LocalDate) {
        val todayEpoch = currentDate.toEpochDay()
        val pendingPlans = _scheduledPresets.value.filter { !it.isApplied && it.startDateEpochDay <= todayEpoch }
        if (pendingPlans.isNotEmpty()) {
            val mostRecentPlan = pendingPlans.maxByOrNull { it.startDateEpochDay }
            mostRecentPlan?.let { plan ->
                val preset = _allPresets.value.find { it.id == plan.presetId }
                if (preset != null) {
                    activatePreset(preset, plan.replaceExisting)
                    val updated = _scheduledPresets.value.map {
                        if (it.id == plan.id) it.copy(isApplied = true) else it
                    }
                    _scheduledPresets.value = updated
                    saveScheduledPresetsToPrefs(updated)
                    _userMessage.value = "Scheduled routine '${preset.name}' activated for today!"
                }
            }
        }
    }

    private fun getDefaultCuratedPresets(): List<RoutinePreset> {
        return listOf(
            RoutinePreset(
                id = "preset_workday",
                name = "Workday Focus",
                description = "Focused workday productivity routine",
                iconEmoji = "💼",
                colorHex = "#6366F1",
                tasks = listOf(
                    PresetTaskItem("Deep Focus Block", "Focus", "Morning", 45, "1,2,3,4,5"),
                    PresetTaskItem("Inbox & Standup", "Focus", "Morning", 15, "1,2,3,4,5"),
                    PresetTaskItem("Lunch Walk & Hydrate", "Health", "Afternoon", 30, "1,2,3,4,5")
                )
            ),
            RoutinePreset(
                id = "preset_weekend",
                name = "Weekend Recharge",
                description = "Relaxation and self-care routine",
                iconEmoji = "🌿",
                colorHex = "#10B981",
                tasks = listOf(
                    PresetTaskItem("Outdoor Walk", "Health", "Morning", 45, "6,7"),
                    PresetTaskItem("Reading & Reflection", "Mind", "Evening", 30, "6,7")
                )
            ),
            RoutinePreset(
                id = "preset_morning",
                name = "Morning Kickstart",
                description = "Start your day with energy",
                iconEmoji = "☀️",
                colorHex = "#F97316",
                tasks = listOf(
                    PresetTaskItem("Drink Water & Stretch", "Health", "Morning", 10, "1,2,3,4,5,6,7"),
                    PresetTaskItem("Review Daily Goals", "Mind", "Morning", 10, "1,2,3,4,5,6,7")
                )
            ),
            RoutinePreset(
                id = "preset_evening",
                name = "Evening Wind-Down",
                description = "Unplug and prepare for restful sleep",
                iconEmoji = "🌙",
                colorHex = "#8B5CF6",
                tasks = listOf(
                    PresetTaskItem("Screen-free wind down", "Habit", "Evening", 20, "1,2,3,4,5,6,7"),
                    PresetTaskItem("Tomorrow's planning", "Mind", "Evening", 10, "1,2,3,4,5,6,7")
                )
            )
        )
    }

    private fun loadPresetsFromPrefs(edition: AppEdition = _appEdition.value): List<RoutinePreset> {
        val prefKey = if (edition == AppEdition.ISLAMIC) "islamic_custom_routine_presets_json" else "custom_routine_presets_json"
        val raw = prefs.getString(prefKey, null) ?: return if (edition == AppEdition.ISLAMIC) emptyList() else getDefaultCuratedPresets()
        return try {
            val array = org.json.JSONArray(raw)
            val list = mutableListOf<RoutinePreset>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                // Filter out any legacy built-in/example presets
                val isBuiltIn = obj.optBoolean("isBuiltIn", false)
                val id = obj.getString("id")
                if (isBuiltIn || id.startsWith("preset_")) continue

                val taskArray = obj.optJSONArray("tasks") ?: org.json.JSONArray()
                val taskList = mutableListOf<PresetTaskItem>()
                for (j in 0 until taskArray.length()) {
                    val tObj = taskArray.getJSONObject(j)
                    taskList.add(
                        PresetTaskItem(
                            title = tObj.getString("title"),
                            category = tObj.optString("category", "Health"),
                            timeOfDay = tObj.optString("timeOfDay", "Morning"),
                            targetMinutes = tObj.optInt("targetMinutes", 15),
                            daysOfWeek = tObj.optString("daysOfWeek", "1,2,3,4,5,6,7"),
                            repeatType = tObj.optString("repeatType", "WEEKLY"),
                            repeatIntervalDays = tObj.optInt("repeatIntervalDays", 1)
                        )
                    )
                }
                list.add(
                    RoutinePreset(
                        id = id,
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        iconEmoji = obj.optString("iconEmoji", "📋"),
                        colorHex = obj.optString("colorHex", "#6366F1"),
                        tasks = taskList,
                        isBuiltIn = false,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            if (edition == AppEdition.ISLAMIC) list else (if (list.isEmpty()) getDefaultCuratedPresets() else (getDefaultCuratedPresets() + list))
        } catch (_: Exception) {
            if (edition == AppEdition.ISLAMIC) emptyList() else getDefaultCuratedPresets()
        }
    }

    private fun savePresetsToPrefs(presets: List<RoutinePreset>) {
        try {
            val prefKey = if (_appEdition.value == AppEdition.ISLAMIC) "islamic_custom_routine_presets_json" else "custom_routine_presets_json"
            val array = org.json.JSONArray()
            for (p in presets) {
                val obj = org.json.JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                obj.put("description", p.description)
                obj.put("iconEmoji", p.iconEmoji)
                obj.put("colorHex", p.colorHex)
                obj.put("isBuiltIn", p.isBuiltIn)
                obj.put("createdAt", p.createdAt)
                val tArray = org.json.JSONArray()
                for (t in p.tasks) {
                    val tObj = org.json.JSONObject()
                    tObj.put("title", t.title)
                    tObj.put("category", t.category)
                    tObj.put("timeOfDay", t.timeOfDay)
                    tObj.put("targetMinutes", t.targetMinutes)
                    tObj.put("daysOfWeek", t.daysOfWeek)
                    tObj.put("repeatType", t.repeatType)
                    tObj.put("repeatIntervalDays", t.repeatIntervalDays)
                    tArray.put(tObj)
                }
                obj.put("tasks", tArray)
                array.put(obj)
            }
            prefs.edit().putString(prefKey, array.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadScheduledPresetsFromPrefs(edition: AppEdition = _appEdition.value): List<ScheduledPresetPlan> {
        val prefKey = if (edition == AppEdition.ISLAMIC) "islamic_scheduled_presets_json" else "scheduled_presets_json"
        val raw = prefs.getString(prefKey, null) ?: return emptyList()
        return try {
            val array = org.json.JSONArray(raw)
            val list = mutableListOf<ScheduledPresetPlan>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ScheduledPresetPlan(
                        id = obj.getString("id"),
                        presetId = obj.getString("presetId"),
                        presetName = obj.getString("presetName"),
                        presetIcon = obj.optString("presetIcon", if (edition == AppEdition.ISLAMIC) "🕌" else "📋"),
                        presetColorHex = obj.optString("presetColorHex", if (edition == AppEdition.ISLAMIC) "#0F766E" else "#6366F1"),
                        taskCount = obj.optInt("taskCount", 0),
                        startDateEpochDay = obj.getLong("startDateEpochDay"),
                        note = obj.optString("note", ""),
                        replaceExisting = obj.optBoolean("replaceExisting", true),
                        isApplied = obj.optBoolean("isApplied", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveScheduledPresetsToPrefs(plans: List<ScheduledPresetPlan>) {
        try {
            val prefKey = if (_appEdition.value == AppEdition.ISLAMIC) "islamic_scheduled_presets_json" else "scheduled_presets_json"
            val array = org.json.JSONArray()
            for (p in plans) {
                val obj = org.json.JSONObject()
                obj.put("id", p.id)
                obj.put("presetId", p.presetId)
                obj.put("presetName", p.presetName)
                obj.put("presetIcon", p.presetIcon)
                obj.put("presetColorHex", p.presetColorHex)
                obj.put("taskCount", p.taskCount)
                obj.put("startDateEpochDay", p.startDateEpochDay)
                obj.put("note", p.note)
                obj.put("replaceExisting", p.replaceExisting)
                obj.put("isApplied", p.isApplied)
                obj.put("createdAt", p.createdAt)
                array.put(obj)
            }
            prefs.edit().putString("scheduled_presets_json", array.toString()).apply()
        } catch (_: Exception) {}
    }

    // --- Economy & Inventory ---
    fun sellPrize(prize: FocusPrize, count: Int = 1) {
        val currentCount = _inventory.value.getCount(prize)
        if (currentCount <= 0) return
        val toSell = count.coerceAtMost(currentCount)
        val earnings = toSell * prize.sellPrice

        _inventory.value = when (prize) {
            FocusPrize.COPPER -> _inventory.value.copy(copper = _inventory.value.copper - toSell)
            FocusPrize.IRON -> _inventory.value.copy(iron = _inventory.value.iron - toSell)
            FocusPrize.GOLD -> _inventory.value.copy(gold = _inventory.value.gold - toSell)
            FocusPrize.DIAMOND -> _inventory.value.copy(diamond = _inventory.value.diamond - toSell)
            FocusPrize.EMERALD -> _inventory.value.copy(emerald = _inventory.value.emerald - toSell)
        }
        _coinsBalance.value += earnings
        saveEconomyToPrefs()
        _userMessage.value = "Sold $toSell ${prize.title} for +$earnings coins!"
    }

    fun sellAllPrizes() {
        val totalVal = _inventory.value.totalValue()
        if (totalVal <= 0) {
            _userMessage.value = "No prizes to sell"
            return
        }
        val itemsCount = _inventory.value.totalItems()
        _coinsBalance.value += totalVal
        _inventory.value = PrizeInventory()
        saveEconomyToPrefs()
        _userMessage.value = "Sold $itemsCount items for +$totalVal coins!"
    }

    private fun loadInventoryFromPrefs(): PrizeInventory {
        return PrizeInventory(
            copper = prefs.getInt("inv_copper", 0),
            iron = prefs.getInt("inv_iron", 0),
            gold = prefs.getInt("inv_gold", 0),
            diamond = prefs.getInt("inv_diamond", 0),
            emerald = prefs.getInt("inv_emerald", 0)
        )
    }

    private fun saveEconomyToPrefs() {
        prefs.edit()
            .putInt("coins_balance", _coinsBalance.value)
            .putInt("total_coins_earned", _totalCoinsEarned.value)
            .putInt("inv_copper", _inventory.value.copper)
            .putInt("inv_iron", _inventory.value.iron)
            .putInt("inv_gold", _inventory.value.gold)
            .putInt("inv_diamond", _inventory.value.diamond)
            .putInt("inv_emerald", _inventory.value.emerald)
            .apply()
    }

    // --- Weather ---
    fun loadWeatherForCurrentLocation(date: LocalDate = _selectedDate.value) {
        viewModelScope.launch {
            _isWeatherLoading.value = true
            val (savedCity, coords, isManual) = WeatherService.getSavedLocation(getApplication())
            if (isManual && coords != null && savedCity.isNotBlank()) {
                val w = WeatherService.fetchWeather(getApplication(), savedCity, coords.first, coords.second, date)
                _currentWeather.value = w
            } else {
                val detected = WeatherService.detectDeviceOrIpLocation(getApplication())
                if (detected != null) {
                    val w = WeatherService.fetchWeather(getApplication(), detected.first, detected.second, detected.third, date)
                    _currentWeather.value = w
                } else if (coords != null && savedCity.isNotBlank()) {
                    val w = WeatherService.fetchWeather(getApplication(), savedCity, coords.first, coords.second, date)
                    _currentWeather.value = w
                } else {
                    val offline = OfflineWeatherHelper.getWeatherForDate(date)
                    _currentWeather.value = LocationWeather(
                        cityName = "Tap to set location",
                        country = "",
                        latitude = 0.0,
                        longitude = 0.0,
                        condition = offline.condition,
                        conditionEmoji = offline.emoji,
                        tempCelsius = offline.tempCelsius,
                        tempFahrenheit = offline.tempFahrenheit,
                        highCelsius = offline.highCelsius,
                        lowCelsius = offline.lowCelsius,
                        humidity = 50,
                        windSpeedKmH = 10.0,
                        tip = offline.tip,
                        isRealTime = false,
                        lastUpdated = "Location needed"
                    )
                }
            }
            _isWeatherLoading.value = false
        }
    }

    fun refreshWeatherWithDeviceLocation() {
        viewModelScope.launch {
            _isWeatherLoading.value = true
            WeatherService.setManualLocationFlag(getApplication(), false)
            val detected = WeatherService.detectDeviceOrIpLocation(getApplication())
            if (detected != null) {
                WeatherService.saveLocation(getApplication(), detected.first, "", detected.second, detected.third, isManual = false)
                val w = WeatherService.fetchWeather(getApplication(), detected.first, detected.second, detected.third, _selectedDate.value)
                _currentWeather.value = w
                _userMessage.value = "Updated weather for ${detected.first}"
            } else {
                _userMessage.value = "Could not detect location. Tap to search your city manually!"
            }
            _isWeatherLoading.value = false
        }
    }

    fun resetToAutoLocation() {
        refreshWeatherWithDeviceLocation()
    }

    fun searchAndSetCity(cityName: String) {
        viewModelScope.launch {
            _isWeatherLoading.value = true
            val result = WeatherService.searchCityCoordinates(cityName)
            if (result != null) {
                WeatherService.saveLocation(getApplication(), result.first, "", result.second, result.third, isManual = true)
                val w = WeatherService.fetchWeather(getApplication(), result.first, result.second, result.third, _selectedDate.value)
                _currentWeather.value = w
                _userMessage.value = "Weather location set to ${result.first}"
            } else {
                _userMessage.value = "Could not find city '$cityName'. Try checking spelling."
            }
            _isWeatherLoading.value = false
        }
    }

    fun setCityFromCoordinates(displayName: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            _isWeatherLoading.value = true
            WeatherService.saveLocation(getApplication(), displayName, "", lat, lon, isManual = true)
            val w = WeatherService.fetchWeather(getApplication(), displayName, lat, lon, _selectedDate.value)
            _currentWeather.value = w
            _userMessage.value = "Weather location set to $displayName"
            _isWeatherLoading.value = false
        }
    }

    fun searchCitySuggestions(query: String, onResult: (List<com.example.util.CitySearchResult>) -> Unit) {
        viewModelScope.launch {
            val results = WeatherService.searchCities(query)
            onResult(results)
        }
    }

    // --- Backup & Restore ---
    fun exportBackup(encrypt: Boolean = false, onReady: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = BackupManager.createBackupJson(repository, encrypt)
                _userMessage.value = "Local backup generated successfully"
                onReady(json)
            } catch (e: Exception) {
                _userMessage.value = "Failed to create backup: ${e.localizedMessage}"
            }
        }
    }

    fun restoreBackup(backupText: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = BackupManager.restoreFromJson(repository, backupText)
            result.onSuccess { count ->
                _userMessage.value = "Restored $count items successfully!"
                onComplete(true)
            }.onFailure { error ->
                _userMessage.value = "Restore failed: Invalid backup format"
                onComplete(false)
            }
        }
    }

    fun exportTasksPack(onReady: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cats = _allCategories.value.map { it.name }
                val json = BackupManager.createTasksPackJson(repository, cats)
                _userMessage.value = "Tasks pack created"
                onReady(json)
            } catch (e: Exception) {
                _userMessage.value = "Failed to create tasks pack: ${e.localizedMessage}"
            }
        }
    }

    fun importTasksPack(rawJson: String, merge: Boolean, onComplete: (Boolean, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = BackupManager.importTasksPack(repository, rawJson, merge)
            result.onSuccess { (count, cats) ->
                val currentNames = _allCategories.value.map { it.name.lowercase() }.toSet()
                cats.forEach { catName ->
                    if (!currentNames.contains(catName.lowercase())) {
                        addCategory(catName, "#3B82F6")
                    }
                }
                _userMessage.value = if (merge) "Added $count shared tasks" else "Replaced routine with $count tasks"
                onComplete(true, count)
            }.onFailure {
                _userMessage.value = "Failed to import tasks pack: invalid format"
                onComplete(false, 0)
            }
        }
    }

    // --- Analytics Engine Calculation ---
    private fun calculateAnalytics(
        tasks: List<RoutineTask>,
        logs: List<TaskCompletionLog>,
        focusSessions: List<FocusSession>,
        coins: Int,
        inv: PrizeInventory
    ): AnalyticsData {
        val today = LocalDate.now()
        val activeTasks = tasks.filter { it.isActive }
        val logsByDate = logs.groupBy { it.dateEpochDay }
        val sessionsByDate = focusSessions.groupBy { it.dateEpochDay }

        val past7Days = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val epoch = date.toEpochDay()
            val dayOfWeek = date.dayOfWeek.value
            val scheduledForDay = activeTasks.filter { task ->
                val days = task.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                days.isEmpty() || days.contains(dayOfWeek)
            }
            val dayLogs = logsByDate[epoch].orEmpty().filter { it.isCompleted }
            val total = scheduledForDay.size
            val completed = dayLogs.size.coerceAtMost(total.coerceAtLeast(1))
            val pct = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f
            val focusMins = (sessionsByDate[epoch].orEmpty().sumOf { it.durationSeconds }) / 60

            DaySummary(
                epochDay = epoch,
                totalTasks = total,
                completedTasks = completed,
                completionPercentage = pct,
                focusMinutes = focusMins
            )
        }

        val past30Days = (29 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val epoch = date.toEpochDay()
            val dayOfWeek = date.dayOfWeek.value
            val scheduledForDay = activeTasks.filter { task ->
                val days = task.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                days.isEmpty() || days.contains(dayOfWeek)
            }
            val dayLogs = logsByDate[epoch].orEmpty().filter { it.isCompleted }
            val total = scheduledForDay.size
            val completed = dayLogs.size.coerceAtMost(total.coerceAtLeast(1))
            val pct = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f
            val focusMins = (sessionsByDate[epoch].orEmpty().sumOf { it.durationSeconds }) / 60

            DaySummary(
                epochDay = epoch,
                totalTasks = total,
                completedTasks = completed,
                completionPercentage = pct,
                focusMinutes = focusMins
            )
        }

        val allRecordedEpochs = (logs.filter { it.isCompleted }.map { it.dateEpochDay } +
                focusSessions.map { it.dateEpochDay } +
                listOf(today.toEpochDay())).distinct().sorted()
        val earliestEpoch = allRecordedEpochs.firstOrNull() ?: today.toEpochDay()
        val daysSpan = ((today.toEpochDay() - earliestEpoch + 1).toInt()).coerceIn(7, 180)

        val allTimeDays = (daysSpan - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val epoch = date.toEpochDay()
            val dayOfWeek = date.dayOfWeek.value
            val scheduledForDay = activeTasks.filter { task ->
                val days = task.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                days.isEmpty() || days.contains(dayOfWeek)
            }
            val dayLogs = logsByDate[epoch].orEmpty().filter { it.isCompleted }
            val total = scheduledForDay.size
            val completed = dayLogs.size.coerceAtMost(total.coerceAtLeast(1))
            val pct = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f
            val focusMins = (sessionsByDate[epoch].orEmpty().sumOf { it.durationSeconds }) / 60

            DaySummary(
                epochDay = epoch,
                totalTasks = total,
                completedTasks = completed,
                completionPercentage = pct,
                focusMinutes = focusMins
            )
        }

        var currentStreak = 0
        var bestStreak = 0
        var tempStreak = 0

        val checkEpoch = today.toEpochDay()
        val todayLogs = logsByDate[checkEpoch].orEmpty().filter { it.isCompleted }
        var dayPointer = if (todayLogs.isNotEmpty()) today else today.minusDays(1)

        while (true) {
            val epoch = dayPointer.toEpochDay()
            val dayLogs = logsByDate[epoch].orEmpty().filter { it.isCompleted }
            if (dayLogs.isNotEmpty()) {
                currentStreak++
                dayPointer = dayPointer.minusDays(1)
            } else {
                break
            }
            if (currentStreak > 365) break
        }

        val sortedDistinctDays = logs.filter { it.isCompleted }
            .map { it.dateEpochDay }
            .distinct()
            .sorted()

        if (sortedDistinctDays.isNotEmpty()) {
            tempStreak = 1
            bestStreak = 1
            for (i in 1 until sortedDistinctDays.size) {
                if (sortedDistinctDays[i] == sortedDistinctDays[i - 1] + 1) {
                    tempStreak++
                    if (tempStreak > bestStreak) bestStreak = tempStreak
                } else {
                    tempStreak = 1
                }
            }
        }
        if (currentStreak > bestStreak) bestStreak = currentStreak

        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1).toEpochDay()
        val totalFocusMinutesThisWeek = focusSessions
            .filter { it.dateEpochDay >= startOfWeek }
            .sumOf { it.durationSeconds } / 60
        val totalFocusMinutesAllTime = focusSessions.sumOf { it.durationSeconds } / 60

        val taskMap = tasks.associateBy { it.id }
        val categoryDistribution = mutableMapOf<String, Int>()
        for (log in logs.filter { it.isCompleted }) {
            val task = taskMap[log.taskId]
            val cat = task?.category ?: "Habit"
            categoryDistribution[cat] = (categoryDistribution[cat] ?: 0) + 1
        }

        return AnalyticsData(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            completionPast7Days = past7Days,
            completionPast30Days = past30Days,
            completionAllTime = allTimeDays,
            totalFocusMinutesThisWeek = totalFocusMinutesThisWeek,
            totalFocusMinutesAllTime = totalFocusMinutesAllTime,
            categoryDistribution = categoryDistribution,
            pastDaysHistory = past30Days.filter { it.totalTasks > 0 || it.focusMinutes > 0 }.reversed(),
            totalCoins = coins,
            totalCoinsEarned = _totalCoinsEarned.value,
            inventory = inv
        )
    }

    // --- Multi-User Cloud Sync & Account System ---
    fun autoSyncCloud() {
        if (cloudSyncManager.isConnected()) {
            viewModelScope.launch(Dispatchers.IO) {
                cloudSyncManager.performSync(repository)
            }
        }
    }

    fun loginWithSyncKey(key: String, display: String = "") {
        cloudSyncManager.loginWithSyncKey(key, display)
        triggerCloudSync()
    }

    fun loginWithUsernameAndPin(username: String, pin: String) {
        cloudSyncManager.loginWithUsernameAndPin(username, pin)
        triggerCloudSync()
    }

    fun disconnectCloudSync() {
        cloudSyncManager.disconnect()
        _userMessage.value = "Disconnected from cloud sync"
    }

    fun triggerCloudSync() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = cloudSyncManager.performSync(repository)
            result.onSuccess {
                _userMessage.value = "Cloud Sync successful! All devices up to date."
            }.onFailure { e ->
                _userMessage.value = "Sync error: ${e.localizedMessage ?: "Connection issue"}"
            }
        }
    }

    fun generateRandomSyncKey(): String {
        return CloudSyncManager.generateRandomSyncKey()
    }

    fun exportCloudBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = cloudSyncManager.exportBackupJson(repository)
                onReady(json)
            } catch (e: Exception) {
                _userMessage.value = "Export failed: ${e.localizedMessage}"
            }
        }
    }

    fun importCloudBackupJson(jsonString: String, onComplete: (Boolean, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = cloudSyncManager.importBackupJson(jsonString, repository)
            result.onSuccess { count ->
                _userMessage.value = "Successfully imported $count items from backup"
                onComplete(true, count)
                autoSyncCloud()
            }.onFailure { e ->
                _userMessage.value = "Import failed: ${e.localizedMessage}"
                onComplete(false, 0)
            }
        }
    }
}
