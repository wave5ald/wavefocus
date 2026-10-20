package com.example

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.desktop.platform.DesktopApp
import com.example.desktop.platform.ToastBus
import com.example.ui.components.MorePagesSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BackupSettingsDialog
import com.example.ui.screens.CustomPageScreen
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.NotesAndBoardsScreen
import com.example.ui.screens.RoutineScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppEdition
import com.example.ui.viewmodel.NavTab
import com.example.ui.viewmodel.RoutineViewModel
import com.example.util.LocalAppLanguage
import com.example.util.appStrings
import kotlinx.coroutines.delay

private const val APP_TITLE = "Wave Focus"

fun main() = application {
    val windowState = rememberWindowState(width = 1000.dp, height = 820.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = APP_TITLE,
        state = windowState,
        icon = painterResource("wave_focus_icon.png")
    ) {
        WaveFocusDesktopApp()
    }
}

@Composable
private fun WaveFocusDesktopApp() {
    val viewModel = remember { RoutineViewModel(DesktopApp.context) }
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val layoutDirection = if (appLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppLanguage provides appLanguage
    ) {
        MyApplicationTheme(themeMode = themeMode) {
            Box(modifier = Modifier.fillMaxSize()) {
                DailyRoutineApp(viewModel = viewModel, themeMode = themeMode)
                ToastOverlay()
            }
        }
    }
}

@Composable
private fun ToastOverlay() {
    val message by ToastBus.message.collectAsStateWithLifecycle()
    val current = message ?: return

    LaunchedEffect(current) {
        delay(2800)
        ToastBus.consume()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = MaterialTheme.colorScheme.inverseSurface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .padding(bottom = 92.dp)
                .widthIn(max = 560.dp)
        ) {
            Text(
                text = current,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyRoutineApp(
    viewModel: RoutineViewModel,
    themeMode: String
) {
    val strings = appStrings()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val tasks by viewModel.tasksForSelectedDate.collectAsStateWithLifecycle()
    val activeTasks by viewModel.allActiveTasks.collectAsStateWithLifecycle()

    val timerMode by viewModel.timerMode.collectAsStateWithLifecycle()
    val totalSeconds by viewModel.timerTotalSeconds.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.timerRemainingSeconds.collectAsStateWithLifecycle()
    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val linkedTask by viewModel.linkedRoutineTask.collectAsStateWithLifecycle()
    val todayFocusSessions by viewModel.todayFocusSessions.collectAsStateWithLifecycle()

    val calendarMonth by viewModel.calendarMonth.collectAsStateWithLifecycle()
    val scriptsForDate by viewModel.scriptsForSelectedDate.collectAsStateWithLifecycle()
    val allScripts by viewModel.allScripts.collectAsStateWithLifecycle()

    val analyticsData by viewModel.analyticsData.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val activeCustomPage by viewModel.activeCustomPage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMorePagesSheet by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentTab) {
                            NavTab.ROUTINE -> strings.routine
                            NavTab.FOCUS -> strings.focus
                            NavTab.SCRIPTS -> strings.schedule
                            NavTab.NOTES -> strings.notes
                            NavTab.ANALYTICS -> strings.progress
                            NavTab.CUSTOM_PAGE -> activeCustomPage?.title?.take(12) ?: strings.workspace
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    val appEdition by viewModel.appEdition.collectAsStateWithLifecycle()
                    val currentLang by viewModel.appLanguage.collectAsStateWithLifecycle()
                    val isArabic = currentLang == "ar"

                    // Dedicated button beside Arabic language: Opens Hijri version with Hijri calendar and separate app data
                    Surface(
                        onClick = { viewModel.toggleAppEdition() },
                        shape = CircleShape,
                        color = if (appEdition == AppEdition.ISLAMIC) {
                            Color(0xFF0F766E).copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        },
                        border = if (appEdition == AppEdition.ISLAMIC) {
                            BorderStroke(1.2.dp, Color(0xFF0F766E))
                        } else null,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("top_hijri_version_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (appEdition == AppEdition.ISLAMIC) Icons.Default.CalendarMonth else Icons.Default.Mosque,
                                contentDescription = if (appEdition == AppEdition.ISLAMIC) strings.switchToStandardEdition else strings.switchToIslamicEdition,
                                tint = if (appEdition == AppEdition.ISLAMIC) Color(0xFF0F766E) else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (appEdition == AppEdition.ISLAMIC) {
                                    if (isArabic) "ميلادي 📅" else "Gregorian 📅"
                                } else {
                                    if (isArabic) "هجري 🌙" else "Hijri 🌙"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (appEdition == AppEdition.ISLAMIC) Color(0xFF0F766E) else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Quick language toggle button (العربية / English)
                    Surface(
                        onClick = { viewModel.toggleLanguage() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("top_language_toggle_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = if (currentLang == "ar") "Switch to English" else "التبديل إلى العربية",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (currentLang == "ar") "English" else "العربية",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Button with three lines at top right for More Pages, Settings, Theme
                    IconButton(
                        onClick = { showMorePagesSheet = true },
                        modifier = Modifier
                            .size(38.dp)
                            .padding(end = 4.dp)
                            .testTag("top_right_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = strings.morePages,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == NavTab.ROUTINE,
                    onClick = { viewModel.setTab(NavTab.ROUTINE) },
                    icon = {
                        Icon(
                            if (currentTab == NavTab.ROUTINE) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = strings.routine
                        )
                    },
                    label = { Text(strings.routine) },
                    modifier = Modifier.testTag("nav_tab_routine")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.FOCUS,
                    onClick = { viewModel.setTab(NavTab.FOCUS) },
                    icon = {
                        Icon(
                            if (currentTab == NavTab.FOCUS) Icons.Default.Timer else Icons.Outlined.Timer,
                            contentDescription = strings.focus
                        )
                    },
                    label = { Text(strings.focus) },
                    modifier = Modifier.testTag("nav_tab_focus")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.SCRIPTS,
                    onClick = { viewModel.setTab(NavTab.SCRIPTS) },
                    icon = {
                        Icon(
                            if (currentTab == NavTab.SCRIPTS) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = strings.schedule
                        )
                    },
                    label = { Text(strings.schedule) },
                    modifier = Modifier.testTag("nav_tab_schedule")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.NOTES,
                    onClick = { viewModel.setTab(NavTab.NOTES) },
                    icon = {
                        Icon(
                            if (currentTab == NavTab.NOTES) Icons.Default.Description else Icons.Outlined.Description,
                            contentDescription = strings.notes
                        )
                    },
                    label = { Text(strings.notes) },
                    modifier = Modifier.testTag("nav_tab_notes")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "tabCrossfade") { tab ->
                when (tab) {
                    NavTab.ROUTINE -> RoutineScreen(
                        viewModel = viewModel,
                        tasks = tasks,
                        selectedDate = selectedDate
                    )
                    NavTab.FOCUS -> FocusTimerScreen(
                        viewModel = viewModel,
                        timerMode = timerMode,
                        totalSeconds = totalSeconds,
                        remainingSeconds = remainingSeconds,
                        isRunning = isTimerRunning,
                        linkedTask = linkedTask,
                        activeTasks = activeTasks,
                        todaySessions = todayFocusSessions
                    )
                    NavTab.SCRIPTS -> ScheduleScreen(
                        viewModel = viewModel,
                        selectedDate = selectedDate,
                        calendarMonth = calendarMonth,
                        scriptsForDate = scriptsForDate,
                        allScripts = allScripts,
                        tasks = tasks,
                        allActiveTasks = activeTasks
                    )
                    NavTab.NOTES -> NotesAndBoardsScreen(
                        viewModel = viewModel
                    )
                    NavTab.ANALYTICS -> AnalyticsScreen(
                        viewModel = viewModel,
                        analyticsData = analyticsData
                    )
                    NavTab.CUSTOM_PAGE -> CustomPageScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showMorePagesSheet) {
        MorePagesSheet(
            viewModel = viewModel,
            currentTab = currentTab,
            onOpenSettings = { showSettingsDialog = true },
            onDismiss = { showMorePagesSheet = false }
        )
    }

    if (showSettingsDialog) {
        BackupSettingsDialog(
            viewModel = viewModel,
            currentThemeMode = themeMode,
            onDismiss = { showSettingsDialog = false }
        )
    }
}
