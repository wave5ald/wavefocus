package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryItem
import com.example.data.model.RoutinePreset
import com.example.data.model.RoutineTask
import com.example.data.model.ScheduledPresetPlan
import com.example.data.model.TaskWithStatus
import com.example.ui.components.RoutinePresetsSheet
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.HabitRose
import com.example.ui.theme.MindTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.AnalyticsData
import com.example.ui.viewmodel.NavTab
import com.example.ui.viewmodel.RoutineViewModel
import com.example.util.CategoryLocalization
import com.example.util.LocalAppLanguage
import com.example.util.appStrings
import com.example.util.formatLocalizedDate
import com.example.util.getLocalizedTimeSlot
import com.example.util.getLocalizedDayShort
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun getCategoryColor(categoryName: String, categories: List<CategoryItem>): Color {
    val canonical = CategoryLocalization.getCanonicalCategoryKey(categoryName)
    val found = categories.find {
        it.name.equals(categoryName, ignoreCase = true) ||
        it.name.equals(canonical, ignoreCase = true)
    }
    if (found != null) {
        return try {
            Color(android.graphics.Color.parseColor(found.colorHex))
        } catch (_: Exception) {
            SuccessGreen
        }
    }
    return when (canonical.lowercase()) {
        "health" -> SuccessGreen
        "focus" -> FocusIndigo
        "mind" -> MindTeal
        "habit" -> HabitRose
        else -> FlameOrange
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    viewModel: RoutineViewModel,
    tasks: List<TaskWithStatus>,
    selectedDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val isArabic = LocalAppLanguage.current == "ar"
    val strings = appStrings()
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<RoutineTask?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedTaskIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var categoryToEdit by remember { mutableStateOf<CategoryItem?>(null) }
    var showPresetsSheet by remember { mutableStateOf(false) }

    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allPresets by viewModel.allPresets.collectAsStateWithLifecycle()
    val scheduledPresets by viewModel.scheduledPresets.collectAsStateWithLifecycle()
    val analyticsData by viewModel.analyticsData.collectAsStateWithLifecycle()

    val isToday = selectedDate == LocalDate.now()
    val completedCount = tasks.count { it.isCompleted }
    val totalCount = tasks.size
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "progress")

    val scheduledForToday = scheduledPresets.find {
        !it.isApplied && it.startDateEpochDay == selectedDate.toEpochDay()
    }

    val filteredTasks = if (selectedCategoryFilter == "All") {
        tasks
    } else {
        val filterCanonical = CategoryLocalization.getCanonicalCategoryKey(selectedCategoryFilter)
        tasks.filter {
            it.task.category.equals(selectedCategoryFilter, ignoreCase = true) ||
            (CategoryLocalization.isMainCategory(it.task.category) &&
             CategoryLocalization.getCanonicalCategoryKey(it.task.category).equals(filterCanonical, ignoreCase = true))
        }
    }

    // Group tasks by Time of Day
    val timeOrder = listOf("Morning", "Afternoon", "Evening", "Anytime")
    val groupedTasks = filteredTasks.groupBy { it.task.timeOfDay }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp)
        ) {
            // Date Navigation Header
            item {
                DateNavigator(
                    selectedDate = selectedDate,
                    isToday = isToday,
                    isSelectionMode = isSelectionMode,
                    hasTasks = tasks.isNotEmpty(),
                    onToggleSelectionMode = {
                        isSelectionMode = !isSelectionMode
                        if (!isSelectionMode) selectedTaskIds = emptySet()
                    },
                    onPrevious = { viewModel.previousDay() },
                    onNext = { viewModel.nextDay() },
                    onToday = { viewModel.goToToday() }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Scheduled Routine Banner (if any preset is scheduled for this date)
            if (scheduledForToday != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .testTag("scheduled_preset_active_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = scheduledForToday.presetIcon, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${strings.scheduledFor}: ${scheduledForToday.presetName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = if (isArabic) "روتين مجدول للبدء اليوم (${scheduledForToday.taskCount} مهام)" else "Scheduled routine starting today (${scheduledForToday.taskCount} tasks)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Button(
                                onClick = { viewModel.applyScheduledPresetNow(scheduledForToday.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("apply_scheduled_preset_banner_button")
                            ) {
                                Text(strings.activateNow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Routine Presets Quick Bar
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPresetsSheet = true }
                        .testTag("open_routine_presets_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.routinePresets,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${allPresets.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (scheduledPresets.any { !it.isApplied }) "📅 ${strings.upcoming}" else strings.presets,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Multi-Select Action Banner
            if (isSelectionMode) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("multi_select_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isArabic) "${selectedTaskIds.size} ${strings.selectedCount}" else "${selectedTaskIds.size} ${strings.selectedCount}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TextButton(
                                    onClick = {
                                        selectedTaskIds = if (selectedTaskIds.size == filteredTasks.size) {
                                            emptySet()
                                        } else {
                                            filteredTasks.map { it.task.id }.toSet()
                                        }
                                    },
                                    modifier = Modifier.testTag("select_all_tasks_button")
                                ) {
                                    Text(
                                        text = if (selectedTaskIds.size == filteredTasks.size && filteredTasks.isNotEmpty()) strings.clear else strings.selectAll,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = {
                                        isSelectionMode = false
                                        selectedTaskIds = emptySet()
                                    },
                                    modifier = Modifier.testTag("cancel_selection_mode_button")
                                ) {
                                    Text(strings.done, fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Button(
                                    onClick = {
                                        viewModel.deleteSelectedTasks(selectedTaskIds)
                                        selectedTaskIds = emptySet()
                                        isSelectionMode = false
                                    },
                                    enabled = selectedTaskIds.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("delete_selected_tasks_button")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isArabic) "${strings.delete} (${selectedTaskIds.size})" else "Delete (${selectedTaskIds.size})", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Category Filter Row (Scrollable with custom categories & edit options)
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        val isAllSelected = selectedCategoryFilter == "All"
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = if (isAllSelected) 1.5.dp else 0.dp,
                                color = if (isAllSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            ),
                            modifier = Modifier
                                .clickable { selectedCategoryFilter = "All" }
                                .testTag("filter_all")
                        ) {
                            Text(
                                text = strings.all,
                                fontSize = 12.sp,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAllSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    items(allCategories) { cat ->
                        val isSelected = selectedCategoryFilter.equals(cat.name, ignoreCase = true) ||
                            (CategoryLocalization.isMainCategory(cat.name) &&
                             CategoryLocalization.isMainCategory(selectedCategoryFilter) &&
                             CategoryLocalization.getCanonicalCategoryKey(cat.name).equals(CategoryLocalization.getCanonicalCategoryKey(selectedCategoryFilter), ignoreCase = true))
                        val catColor = getCategoryColor(cat.name, allCategories)
                        val isDefault = CategoryLocalization.isMainCategory(cat.name)
                        val localizedName = CategoryLocalization.getLocalizedCategoryName(cat.name, isArabic)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) catColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) catColor else Color.Transparent
                            ),
                            modifier = Modifier
                                .clickable { selectedCategoryFilter = cat.name }
                                .testTag("filter_${cat.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = localizedName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // If category is custom or selected, allow editing
                                if (!isDefault || isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        if (isDefault) Icons.Default.Lock else Icons.Default.Edit,
                                        contentDescription = "Edit Category ${cat.name}",
                                        tint = if (isSelected) catColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { categoryToEdit = cat }
                                            .testTag("edit_category_icon_${cat.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }

                    // Manage / Edit Category button
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .clickable {
                                    val current = allCategories.find { it.name.equals(selectedCategoryFilter, true) }
                                        ?: allCategories.firstOrNull()
                                    categoryToEdit = current
                                }
                                .testTag("open_category_manager_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = strings.editCategory,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Empty state
            if (tasks.isEmpty()) {
                item {
                    EmptyRoutinesPlaceholder(onAdd = { showAddDialog = true })
                }
            } else {
                // Grouped tasks
                timeOrder.forEach { timeSlot ->
                    val slotTasks = groupedTasks[timeSlot]
                    if (!slotTasks.isNullOrEmpty()) {
                        item {
                            Text(
                                text = getLocalizedTimeSlot(timeSlot, isArabic).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }

                        items(slotTasks, key = { it.task.id }) { item ->
                            val isSelected = selectedTaskIds.contains(item.task.id)
                            TaskCard(
                                item = item,
                                allCategories = allCategories,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onSelectToggle = {
                                    selectedTaskIds = if (isSelected) {
                                        selectedTaskIds - item.task.id
                                    } else {
                                        selectedTaskIds + item.task.id
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedTaskIds = setOf(item.task.id)
                                    }
                                },
                                onToggle = { completed ->
                                    if (isSelectionMode) {
                                        selectedTaskIds = if (isSelected) {
                                            selectedTaskIds - item.task.id
                                        } else {
                                            selectedTaskIds + item.task.id
                                        }
                                    } else {
                                        viewModel.toggleTask(item.task.id, completed)
                                    }
                                },
                                onEdit = { taskToEdit = item.task },
                                onDelete = { viewModel.deleteTask(item.task) },
                                onStartFocus = {
                                    viewModel.setLinkedTask(item.task)
                                    viewModel.setTab(NavTab.FOCUS)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Daily Progress placed all the way down at the bottom
            item {
                Spacer(modifier = Modifier.height(14.dp))
                DailyProgressCard(
                    completedCount = completedCount,
                    totalCount = totalCount,
                    progress = animatedProgress,
                    isToday = isToday
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Floating Action Buttons (Presets & Add Task)
        if (!isSelectionMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Secondary FAB for Routine Presets & Forms
                FloatingActionButton(
                    onClick = { showPresetsSheet = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("routine_presets_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = strings.routinePresets,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Primary FAB to Add New Routine Task
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_routine_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Routine Task")
                }
            }
        }
    }

    // Routine Presets & Forms Sheet
    if (showPresetsSheet) {
        RoutinePresetsSheet(
            presets = allPresets,
            scheduledPlans = scheduledPresets,
            categories = allCategories,
            onDismiss = { showPresetsSheet = false },
            onActivatePreset = { preset, replaceExisting ->
                viewModel.activatePreset(preset, replaceExisting)
            },
            onSchedulePreset = { preset, startDate, replaceExisting, note ->
                viewModel.schedulePresetForDate(preset, startDate, replaceExisting, note)
            },
            onSaveCurrentAsPreset = { name, icon, colorHex, desc ->
                viewModel.saveCurrentTasksAsPreset(name, icon, colorHex, desc)
            },
            onCreatePreset = { preset ->
                viewModel.createOrUpdatePreset(preset)
            },
            onDeletePreset = { presetId ->
                viewModel.deletePreset(presetId)
            },
            onApplyScheduledNow = { planId ->
                viewModel.applyScheduledPresetNow(planId)
            },
            onCancelScheduled = { planId ->
                viewModel.cancelScheduledPreset(planId)
            },
            onShowMessage = { msg ->
                viewModel.showUserMessage(msg)
            }
        )
    }

    // Add Task Sheet
    if (showAddDialog) {
        TaskEditorSheet(
            initialTask = null,
            allCategories = allCategories,
            initialDate = selectedDate,
            onAddCategory = { name, colorHex -> viewModel.addCategory(name, colorHex) },
            onDismiss = { showAddDialog = false },
            onSave = { title, cat, timeOfDay, targetMinutes, days, repeatType, repeatIntervalDays, startEpochDay ->
                viewModel.addTask(
                    title = title,
                    category = cat,
                    timeOfDay = timeOfDay,
                    targetMinutes = targetMinutes,
                    daysOfWeek = days,
                    repeatType = repeatType,
                    repeatIntervalDays = repeatIntervalDays,
                    startEpochDay = startEpochDay
                )
                showAddDialog = false
            }
        )
    }

    // Edit Task Sheet
    taskToEdit?.let { task ->
        TaskEditorSheet(
            initialTask = task,
            allCategories = allCategories,
            initialDate = selectedDate,
            onAddCategory = { name, colorHex -> viewModel.addCategory(name, colorHex) },
            onDismiss = { taskToEdit = null },
            onSave = { title, cat, timeOfDay, targetMinutes, days, repeatType, repeatIntervalDays, startEpochDay ->
                viewModel.updateTask(
                    task.copy(
                        title = title,
                        category = cat,
                        timeOfDay = timeOfDay,
                        targetMinutes = targetMinutes,
                        daysOfWeek = days,
                        repeatType = repeatType,
                        repeatIntervalDays = repeatIntervalDays,
                        startEpochDay = startEpochDay
                    )
                )
                taskToEdit = null
            }
        )
    }

    // Edit Category Dialog
    categoryToEdit?.let { cat ->
        EditCategoryDialog(
            category = cat,
            onDismiss = { categoryToEdit = null },
            onSave = { newName, newColor ->
                viewModel.editCategory(cat.name, newName, newColor)
                if (selectedCategoryFilter.equals(cat.name, true)) {
                    selectedCategoryFilter = newName
                }
                categoryToEdit = null
            },
            onDelete = {
                viewModel.deleteCategory(cat.name)
                if (selectedCategoryFilter.equals(cat.name, true)) {
                    selectedCategoryFilter = "All"
                }
                categoryToEdit = null
            }
        )
    }
}

@Composable
fun DateNavigator(
    selectedDate: LocalDate,
    isToday: Boolean,
    isSelectionMode: Boolean = false,
    hasTasks: Boolean = true,
    onToggleSelectionMode: () -> Unit = {},
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val isArabic = LocalAppLanguage.current == "ar"
    val strings = appStrings()
    val dateText = formatLocalizedDate(selectedDate, isArabic)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.testTag("prev_date_button")
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = strings.prevDay,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onToday() }
        ) {
            Text(
                text = if (isToday) strings.today else dateText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (isToday) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasTasks) {
                IconButton(
                    onClick = onToggleSelectionMode,
                    modifier = Modifier.testTag("toggle_selection_mode_icon_button")
                ) {
                    Icon(
                        Icons.Default.Checklist,
                        contentDescription = if (isArabic) "تحديد المهام" else "Select Tasks",
                        tint = if (isSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.testTag("next_date_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = strings.nextDay,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DailyProgressCard(
    completedCount: Int,
    totalCount: Int,
    progress: Float,
    isToday: Boolean
) {
    val isArabic = LocalAppLanguage.current == "ar"
    val strings = appStrings()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("routine_daily_progress_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title & quick progress count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.dailyProgress,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isArabic) "$completedCount من $totalCount مكتمل (${(progress * 100).toInt()}%)" else "$completedCount of $totalCount done (${(progress * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SuccessGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = SuccessGreen,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Auto-reset / status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Default.Autorenew,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = SuccessGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isToday) (if (isArabic) "يتجدد يومياً" else "Resets daily") else (if (isArabic) "سجل سابق" else "History log"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = if (totalCount == 0) {
                        if (isArabic) "لا توجد مهام مجدولة" else "No tasks scheduled"
                    } else if (completedCount == totalCount) {
                        if (isArabic) "اكتملت جميع المهام! 🎉" else "All completed! 🎉"
                    } else {
                        if (isArabic) "متبقي ${totalCount - completedCount} مهام" else "${totalCount - completedCount} tasks remaining"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    item: TaskWithStatus,
    allCategories: List<CategoryItem>,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartFocus: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val categoryColor = getCategoryColor(item.task.category, allCategories)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
            else if (item.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isCompleted) 0.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onSelectToggle()
                    } else {
                        onToggle(!item.isCompleted)
                    }
                },
                onLongClick = onLongClick
            )
            .testTag("task_card_${item.task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectToggle() },
                    modifier = Modifier.testTag("task_select_checkbox_${item.task.id}")
                )
            } else {
                // Checkbox button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isCompleted) SuccessGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        .clickable { onToggle(!item.isCompleted) }
                        .testTag("checkbox_${item.task.id}")
                ) {
                    if (item.isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Badges
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Medium,
                    color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category chip with custom color
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = categoryColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val isArabic = LocalAppLanguage.current == "ar"
                            Text(
                                text = CategoryLocalization.getLocalizedCategoryName(item.task.category, isArabic),
                                color = categoryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (item.task.targetMinutes > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${item.task.targetMinutes}m",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val repeatLabel = when {
                        item.task.repeatType.equals("INTERVAL", ignoreCase = true) -> "Every ${item.task.repeatIntervalDays}d"
                        item.task.repeatType.equals("WEEKEND_ONCE", ignoreCase = true) -> "1 Weekend Day"
                        item.task.daysOfWeek == "6" -> "Sat Only"
                        item.task.daysOfWeek == "7" -> "Sun Only"
                        item.task.daysOfWeek == "6,7" || item.task.daysOfWeek == "7,6" -> "Weekends"
                        item.task.daysOfWeek == "1,2,3,4,5" -> "Weekdays"
                        item.task.daysOfWeek.split(",").size == 7 -> null
                        else -> null
                    }
                    if (repeatLabel != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Autorenew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = repeatLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Quick Focus Button
            IconButton(
                onClick = onStartFocus,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("focus_task_${item.task.id}")
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = "Focus on task",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Overflow Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

val CategoryExpandedColors = listOf(
    "#10B981", "#059669", "#14B8A6", "#0D9488",
    "#06B6D4", "#0284C7", "#3B82F6", "#2563EB",
    "#4F46E5", "#6366F1", "#8B5CF6", "#7C3AED",
    "#A855F7", "#D946EF", "#EC4899", "#F43F5E",
    "#E11D48", "#EF4444", "#DC2626", "#F97316",
    "#EA580C", "#F59E0B", "#D97706", "#EAB308",
    "#84CC16", "#65A30D", "#22C55E", "#16A34A",
    "#78716C", "#64748B", "#475569", "#334155"
)

@Composable
fun CategoryColorPickerGrid(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryExpandedColors.chunked(8).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                rowColors.forEach { hex ->
                    val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color(0xFF3B82F6) }
                    val isChosen = selectedColorHex.equals(hex, ignoreCase = true)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isChosen) 2.5.dp else 0.dp,
                                color = if (isChosen) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(hex) }
                    ) {
                        if (isChosen) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    initialTask: RoutineTask?,
    allCategories: List<CategoryItem>,
    initialDate: LocalDate = LocalDate.now(),
    initialRepeatType: String? = null,
    onAddCategory: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        category: String,
        timeOfDay: String,
        targetMinutes: Int,
        days: String,
        repeatType: String,
        repeatIntervalDays: Int,
        startEpochDay: Long
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(initialTask?.category ?: allCategories.firstOrNull()?.name ?: "Health") }
    var selectedTimeOfDay by remember { mutableStateOf(initialTask?.timeOfDay ?: "Morning") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    var specificDate by remember {
        mutableStateOf(
            if (initialTask != null && (initialTask.repeatType == "SPECIFIC_DAY" || initialTask.repeatType == "ONCE") && initialTask.startEpochDay > 0L) {
                LocalDate.ofEpochDay(initialTask.startEpochDay)
            } else {
                initialDate
            }
        )
    }

    // Recurrence state
    var repeatType by remember {
        mutableStateOf(
            when {
                initialRepeatType != null -> initialRepeatType
                initialTask?.repeatType == "SPECIFIC_DAY" || initialTask?.repeatType == "ONCE" -> "SPECIFIC_DAY"
                initialTask?.repeatType == "INTERVAL" -> "INTERVAL"
                initialTask?.repeatType == "WEEKEND_ONCE" -> "WEEKEND_ONCE"
                initialTask?.daysOfWeek == "6" || initialTask?.daysOfWeek == "7" -> "WEEKEND_ONCE"
                else -> "WEEKLY"
            }
        )
    }
    var repeatIntervalDays by remember { mutableIntStateOf(if ((initialTask?.repeatIntervalDays ?: 1) > 1) initialTask!!.repeatIntervalDays else 2) }
    var selectedDays by remember {
        mutableStateOf(
            initialTask?.daysOfWeek?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet()
                ?: setOf(1, 2, 3, 4, 5, 6, 7)
        )
    }

    val isArabic = LocalAppLanguage.current == "ar"
    val strings = appStrings()
    val timeOptions = listOf("Morning", "Afternoon", "Evening", "Anytime")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (initialTask == null) strings.newRoutineTask else strings.editRoutineTask,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(strings.routineTaskName) },
                placeholder = { Text(strings.routineTaskPlaceholder) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(strings.timeOfDay, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                timeOptions.chunked(2).forEach { rowSlots ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowSlots.forEach { slot ->
                            val isSelected = selectedTimeOfDay == slot
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTimeOfDay = slot }
                                    .testTag("time_slot_${slot.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = getLocalizedTimeSlot(slot, isArabic),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Schedule & Recurrence (Specific Day, Days of Week, Every 2/3 Days, 1 Weekend Day)
            Text(strings.scheduleAndRecurrence, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))

            // 4 Mode Tabs
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Mode 1: Specific Day (NEW BUTTON)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (repeatType == "SPECIFIC_DAY") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (repeatType == "SPECIFIC_DAY") MaterialTheme.colorScheme.primary else Color.Transparent),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { repeatType = "SPECIFIC_DAY" }
                            .testTag("mode_specific_day")
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = strings.specificDay,
                                fontSize = 11.sp,
                                fontWeight = if (repeatType == "SPECIFIC_DAY") FontWeight.Bold else FontWeight.Medium,
                                color = if (repeatType == "SPECIFIC_DAY") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Mode 2: Days of Week (Weekly Recurring)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (repeatType == "WEEKLY") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (repeatType == "WEEKLY") MaterialTheme.colorScheme.primary else Color.Transparent),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { repeatType = "WEEKLY" }
                            .testTag("mode_specific_days")
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = strings.weeklyDays,
                                fontSize = 11.sp,
                                fontWeight = if (repeatType == "WEEKLY") FontWeight.Bold else FontWeight.Medium,
                                color = if (repeatType == "WEEKLY") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Mode 3: Every 2 or 3 Days (Interval)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (repeatType == "INTERVAL") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (repeatType == "INTERVAL") MaterialTheme.colorScheme.primary else Color.Transparent),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { repeatType = "INTERVAL" }
                            .testTag("mode_interval_days")
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = strings.every23Days,
                                fontSize = 11.sp,
                                fontWeight = if (repeatType == "INTERVAL") FontWeight.Bold else FontWeight.Medium,
                                color = if (repeatType == "INTERVAL") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Mode 4: 1 Day in Weekend
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (repeatType == "WEEKEND_ONCE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (repeatType == "WEEKEND_ONCE") MaterialTheme.colorScheme.primary else Color.Transparent),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                repeatType = "WEEKEND_ONCE"
                                if (selectedDays != setOf(6) && selectedDays != setOf(7)) {
                                    selectedDays = setOf(6, 7)
                                }
                            }
                            .testTag("mode_weekend_once")
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = strings.weekendOnce,
                                fontSize = 11.sp,
                                fontWeight = if (repeatType == "WEEKEND_ONCE") FontWeight.Bold else FontWeight.Medium,
                                color = if (repeatType == "WEEKEND_ONCE") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (repeatType) {
                "SPECIFIC_DAY" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Task Scheduled Date:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = specificDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Quick date selector chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val today = LocalDate.now()
                            listOf(
                                "Today" to today,
                                "Tomorrow" to today.plusDays(1),
                                "In 2 Days" to today.plusDays(2),
                                "+1 Week" to today.plusWeeks(1)
                            ).forEach { (label, dateVal) ->
                                val isSelected = specificDate == dateVal
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { specificDate = dateVal }
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Date Stepper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { specificDate = specificDate.minusDays(1) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Prev Day", fontSize = 11.sp)
                            }
                            Text(
                                text = "Only appears on this day",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { specificDate = specificDate.plusDays(1) }) {
                                Text("Next Day", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                "INTERVAL" -> {
                    // One-tap quick presets: Every 2 Days, Every 3 Days, Every 4 Days, Every 5 Days
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            2 to "Every 2d",
                            3 to "Every 3d",
                            4 to "Every 4d",
                            5 to "Every 5d"
                        ).forEach { (days, label) ->
                            val isSelected = repeatIntervalDays == days
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { repeatIntervalDays = days }
                                    .testTag("interval_${days}_days")
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stepper for custom days
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Repeats every $repeatIntervalDays days starting today",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { if (repeatIntervalDays > 2) repeatIntervalDays-- },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "$repeatIntervalDays d",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = { if (repeatIntervalDays < 30) repeatIntervalDays++ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                "WEEKEND_ONCE" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isFlexible = selectedDays == setOf(6, 7)
                        val isSatOnly = selectedDays == setOf(6)
                        val isSunOnly = selectedDays == setOf(7)

                        // Option 1: Either Saturday or Sunday (1 day per weekend, carryover)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isFlexible) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (isFlexible) MaterialTheme.colorScheme.primary else Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDays = setOf(6, 7) }
                                .testTag("weekend_flexible_choice")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isFlexible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Either Saturday or Sunday (1 day per weekend)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isFlexible) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Do it once over the weekend. Doing it Saturday covers Sunday!",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Specific single weekend days: Saturday Only vs Sunday Only
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSatOnly) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, if (isSatOnly) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDays = setOf(6) }
                                    .testTag("weekend_sat_only_choice")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Saturday Only",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSatOnly) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Appears only Saturdays",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSunOnly) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, if (isSunOnly) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDays = setOf(7) }
                                    .testTag("weekend_sun_only_choice")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Sunday Only",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSunOnly) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Appears only Sundays",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Presets: "Every Day", "Weekdays", "Sat & Sun"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val presets = listOf(
                            (if (isArabic) "كل يوم" else "Every Day") to setOf(1, 2, 3, 4, 5, 6, 7),
                            (if (isArabic) "أيام العمل" else "Weekdays") to setOf(1, 2, 3, 4, 5),
                            (if (isArabic) "عطلة الأسبوع" else "Sat & Sun") to setOf(6, 7)
                        )
                        presets.forEach { (label, daysSet) ->
                            val isPresetActive = selectedDays == daysSet
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPresetActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, if (isPresetActive) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDays = daysSet }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isPresetActive) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isPresetActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Individual Day Pills (1=Mon ... 7=Sun)
                    val dayLabels = if (isArabic) {
                        listOf(1 to "إثن", 2 to "ثلا", 3 to "أرب", 4 to "خمي", 5 to "جمع", 6 to "سبت", 7 to "أحد")
                    } else {
                        listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        dayLabels.forEach { (dayInt, label) ->
                            val isDaySelected = selectedDays.contains(dayInt)
                            Surface(
                                shape = CircleShape,
                                color = if (isDaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable {
                                        selectedDays = if (isDaySelected) {
                                            if (selectedDays.size > 1) selectedDays - dayInt else selectedDays
                                        } else {
                                            selectedDays + dayInt
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (isArabic) label else label.take(1),
                                        fontSize = 10.sp,
                                        fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isDaySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Selection with "+ Add Category" button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.category, style = MaterialTheme.typography.labelMedium)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier
                        .clickable { showAddCategoryDialog = true }
                        .testTag("create_new_category_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = strings.newCategory,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(allCategories) { cat ->
                    val isSelected = selectedCategory.equals(cat.name, ignoreCase = true)
                    val catColor = getCategoryColor(cat.name, allCategories)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) catColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) catColor else Color.Transparent
                        ),
                        modifier = Modifier
                            .clickable { selectedCategory = cat.name }
                            .testTag("category_chip_${cat.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(catColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = CategoryLocalization.getLocalizedCategoryName(cat.name, isArabic),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(strings.cancel, fontSize = 13.sp)
                }

                androidx.compose.material3.Button(
                    onClick = {
                        val effectiveRepeatType: String
                        val formattedDays: String
                        if (repeatType == "WEEKEND_ONCE") {
                            if (selectedDays == setOf(6)) {
                                effectiveRepeatType = "WEEKLY"
                                formattedDays = "6"
                            } else if (selectedDays == setOf(7)) {
                                effectiveRepeatType = "WEEKLY"
                                formattedDays = "7"
                            } else {
                                effectiveRepeatType = "WEEKEND_ONCE"
                                formattedDays = "6,7"
                            }
                        } else {
                            effectiveRepeatType = repeatType
                            formattedDays = if (effectiveRepeatType == "SPECIFIC_DAY") specificDate.dayOfWeek.value.toString() else if (selectedDays.isEmpty()) "1,2,3,4,5,6,7" else selectedDays.sorted().joinToString(",")
                        }

                        val startEpoch = if (effectiveRepeatType == "SPECIFIC_DAY") {
                            specificDate.toEpochDay()
                        } else {
                            initialTask?.startEpochDay ?: LocalDate.now().toEpochDay()
                        }

                        onSave(
                            title,
                            selectedCategory,
                            selectedTimeOfDay,
                            initialTask?.targetMinutes ?: 0,
                            formattedDays,
                            effectiveRepeatType,
                            repeatIntervalDays,
                            startEpoch
                        )
                    },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_task_button")
                ) {
                    Text(if (initialTask == null) strings.saveRoutine else strings.updateRoutine, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Add Custom Category with Color Palette Dialog
    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf(CategoryExpandedColors[0]) }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text(strings.newCategory) },
            text = {
                Column {
                    Text(strings.categoryName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        singleLine = true,
                        placeholder = { Text("e.g. Work, Workout, Reading") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_category_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isArabic) "اختر لون التصنيف" else "Choose Category Color",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    CategoryColorPickerGrid(
                        selectedColorHex = selectedColorHex,
                        onColorSelected = { selectedColorHex = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            onAddCategory(newCatName.trim(), selectedColorHex)
                            selectedCategory = newCatName.trim()
                            showAddCategoryDialog = false
                        }
                    },
                    enabled = newCatName.isNotBlank(),
                    modifier = Modifier.testTag("confirm_add_category_button")
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun EmptyRoutinesPlaceholder(onAdd: () -> Unit) {
    val isArabic = LocalAppLanguage.current == "ar"
    val strings = appStrings()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isArabic) "لا توجد مهام روتينية مجدولة" else "No routine tasks scheduled",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isArabic) "أضف عادات وروتينات تتجدد تلقائياً كل يوم." else "Add habits and routines that automatically reset each day.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAdd) {
            Text(if (isArabic) "إنشاء مهمة روتين" else "Create Routine Task")
        }
    }
}

@Composable
fun EditCategoryDialog(
    category: CategoryItem,
    onDismiss: () -> Unit,
    onSave: (newName: String, newColorHex: String) -> Unit,
    onDelete: () -> Unit
) {
    val isArabic = LocalAppLanguage.current == "ar"
    val strings = appStrings()
    val isMain = CategoryLocalization.isMainCategory(category.name) || !category.isCustom
    val localizedName = CategoryLocalization.getLocalizedCategoryName(category.name, isArabic)
    var name by remember { mutableStateOf(if (isMain) localizedName else category.name) }
    var selectedColorHex by remember { mutableStateOf(category.colorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.editCategory)
            }
        },
        text = {
            Column {
                Text(
                    text = strings.categoryName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_category_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isArabic) "لون التصنيف (٣٢ خياراً مميزاً)" else "Category Color (32 Vibrant Options)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                CategoryColorPickerGrid(
                    selectedColorHex = selectedColorHex,
                    onColorSelected = { selectedColorHex = it }
                )
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("delete_category_button")
                ) {
                    Text(strings.delete)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isNotBlank()) {
                            val canonical = if (CategoryLocalization.isMainCategory(category.name) && trimmed.equals(localizedName, ignoreCase = true)) {
                                CategoryLocalization.getCanonicalCategoryKey(category.name)
                            } else {
                                trimmed
                            }
                            onSave(canonical, selectedColorHex)
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.testTag("save_category_button")
                ) {
                    Text(if (isArabic) "حفظ التغييرات" else "Save Changes")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
