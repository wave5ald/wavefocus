package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryItem
import com.example.data.model.RoutineTask
import com.example.data.model.ScheduledScript
import com.example.data.model.TaskWithStatus
import com.example.data.repository.RoutineRepository
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.HabitRose
import com.example.ui.theme.MindTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.AppEdition
import com.example.ui.viewmodel.RoutineViewModel
import com.example.util.HijriCalendarHelper
import com.example.util.LocalAppLanguage
import com.example.util.appStrings
import com.example.util.formatLocalizedDate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: RoutineViewModel,
    selectedDate: LocalDate,
    calendarMonth: YearMonth,
    scriptsForDate: List<ScheduledScript>,
    allScripts: List<ScheduledScript>,
    tasks: List<TaskWithStatus> = emptyList(),
    allActiveTasks: List<RoutineTask> = emptyList(),
    modifier: Modifier = Modifier
) {
    val isArabic = LocalAppLanguage.current == "ar"
    val strings = appStrings()
    var showEditorSheet by remember { mutableStateOf(false) }
    var editorInitialTab by remember { mutableIntStateOf(0) } // 0 = Goal/Quest, 1 = Note
    var editingScript by remember { mutableStateOf<ScheduledScript?>(null) }
    var scheduleFilterTab by remember { mutableStateOf("All") }
    var showTaskEditorForDate by remember { mutableStateOf(false) }
    var showClearTasksDialog by remember { mutableStateOf(false) }

    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val appEdition by viewModel.appEdition.collectAsStateWithLifecycle()
    val isHijri = appEdition == AppEdition.ISLAMIC

    // Separate quests & goals from notes
    val questsForDate = remember(scriptsForDate) {
        scriptsForDate.filter { it.category != "Note" }
    }
    val notesForDate = remember(scriptsForDate) {
        scriptsForDate.filter { it.category == "Note" }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
        ) {
            // Month Header
            item {
                ScheduleMonthHeader(
                    currentMonth = calendarMonth,
                    selectedDate = selectedDate,
                    isHijri = isHijri,
                    onPrevMonth = { viewModel.previousCalendarMonth() },
                    onNextMonth = { viewModel.nextCalendarMonth() }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Interactive Calendar Grid with Goals/Quests/Routines indicators
            item {
                if (isHijri) {
                    HijriCalendarGrid(
                        selectedDate = selectedDate,
                        allScripts = allScripts,
                        allActiveTasks = allActiveTasks,
                        onDateSelected = { viewModel.selectDate(it) }
                    )
                } else {
                    InteractiveCalendarGrid(
                        yearMonth = calendarMonth,
                        selectedDate = selectedDate,
                        allScripts = allScripts,
                        allActiveTasks = allActiveTasks,
                        onDateSelected = { viewModel.selectDate(it) }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Day Header with Today Jump, Quest Summary & Action Buttons
            item {
                val dateLabel = if (isHijri) {
                    HijriCalendarHelper.formatFullHijri(selectedDate, isArabic)
                } else {
                    formatLocalizedDate(selectedDate, isArabic)
                }
                val isToday = (selectedDate == LocalDate.now())
                val completedQuestsCount = questsForDate.count { it.isCompleted }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (isToday) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = strings.today,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (!isToday) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { viewModel.goToToday() }
                                    .testTag("jump_to_today_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Today,
                                        contentDescription = strings.today,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = strings.today,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Summary statistics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isArabic) "${tasks.size} روتينات" else "${tasks.size} routines",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FocusIndigo.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (isArabic) "${questsForDate.size} مهمات ($completedQuestsCount مكتمل)" else "${questsForDate.size} quests ($completedQuestsCount done)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FocusIndigo,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isArabic) "${notesForDate.size} ملاحظات" else "${notesForDate.size} notes",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Action Buttons Row (LazyRow with generous spacing, NO vertical text wrapping)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Primary Set Quest / Goal Button
                        item {
                            Button(
                                onClick = {
                                    editingScript = null
                                    editorInitialTab = 0
                                    showEditorSheet = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FocusIndigo),
                                modifier = Modifier.testTag("add_quest_button")
                            ) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = strings.setQuest,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // 2. Set Task for This Day Button
                        item {
                            Button(
                                onClick = { showTaskEditorForDate = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier.testTag("add_day_task_button")
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.taskForDay,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // 3. Add Note Button (Fixed: letters NEVER wrap top-to-bottom!)
                        item {
                            FilledTonalButton(
                                onClick = {
                                    editingScript = null
                                    editorInitialTab = 1
                                    showEditorSheet = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .widthIn(min = 88.dp)
                                    .testTag("add_note_button")
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.note,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // 4. Remove All Tasks for this Day (if tasks exist)
                        if (tasks.isNotEmpty()) {
                            item {
                                OutlinedButton(
                                    onClick = { showClearTasksDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    modifier = Modifier.testTag("clear_day_tasks_button")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = strings.clearDayTasks,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Filter Tabs Row: All, Quests & Goals, Routines, Notes
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf(
                        "All" to (tasks.size + questsForDate.size + notesForDate.size),
                        "Quests" to questsForDate.size,
                        "Routines" to tasks.size,
                        "Notes" to notesForDate.size
                    )
                    filterOptions.forEach { (rawKey, count) ->
                        val isSelected = (scheduleFilterTab == rawKey)
                        val displayTitle = when (rawKey) {
                            "All" -> strings.all
                            "Quests" -> strings.quests
                            "Routines" -> strings.routine
                            "Notes" -> strings.notes
                            else -> rawKey
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { scheduleFilterTab = rawKey }
                                .testTag("schedule_filter_$rawKey")
                        ) {
                            Text(
                                text = "$displayTitle ($count)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Both Empty State
            if (tasks.isEmpty() && scriptsForDate.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = FocusIndigo,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isArabic) "لا توجد أهداف أو مهمات لهذا اليوم" else "No goals or quests set for this day",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isArabic) "اضغط على 'تحديد مهمة' لتحدي نفسك في ${formatLocalizedDate(selectedDate, true)} أو أضف ملاحظة يومية."
                                    else "Tap 'Set Quest' to challenge yourself on ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))}, or add a daily note.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    editingScript = null
                                    editorInitialTab = 0
                                    showEditorSheet = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FocusIndigo)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "تحديد مهمة لهذا اليوم" else "Set a Quest for This Day", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // 1. Quests & Goals Section
                if (scheduleFilterTab == "All" || scheduleFilterTab == "Quests") {
                    if (questsForDate.isNotEmpty()) {
                        item {
                            val completedCount = questsForDate.count { it.isCompleted }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isArabic) "🎯 مهمات وأهداف اليوم ($completedCount/${questsForDate.size} مكتمل)" else "🎯 Daily Quests & Goals ($completedCount/${questsForDate.size} done)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = FocusIndigo
                                )
                            }
                        }
                        items(questsForDate, key = { "quest_${it.id}" }) { quest ->
                            ScheduleQuestCard(
                                quest = quest,
                                onToggle = {
                                    viewModel.toggleQuestCompletion(quest.id, !quest.isCompleted)
                                },
                                onEdit = {
                                    editingScript = quest
                                    editorInitialTab = 0
                                    showEditorSheet = true
                                },
                                onDelete = {
                                    viewModel.deleteScript(quest.id)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else if (scheduleFilterTab == "Quests") {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isArabic) "لا توجد مهمات أو أهداف محددة لهذا التاريخ." else "No quests or goals set for this date.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = {
                                        editingScript = null
                                        editorInitialTab = 0
                                        showEditorSheet = true
                                    }) {
                                        Text(if (isArabic) "حدد مهمة الآن 🎯" else "Set a Quest Now 🎯")
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Routines Section
                if (scheduleFilterTab == "All" || scheduleFilterTab == "Routines") {
                    if (tasks.isNotEmpty()) {
                        item {
                            val completedCount = tasks.count { it.isCompleted }
                            Text(
                                text = if (isArabic) "⏱️ روتينات اليوم المجدولة ($completedCount/${tasks.size} مكتمل)" else "⏱️ Scheduled Routines ($completedCount/${tasks.size} done)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                            )
                        }
                        items(tasks, key = { "task_${it.task.id}" }) { taskWithStatus ->
                            ScheduleTaskCard(
                                taskWithStatus = taskWithStatus,
                                categories = allCategories,
                                onToggle = { viewModel.toggleTask(taskWithStatus.task.id, !taskWithStatus.isCompleted) },
                                onDelete = { viewModel.deleteTask(taskWithStatus.task) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else if (scheduleFilterTab == "Routines") {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "لا توجد مهام روتينية مجدولة لهذا اليوم من الأسبوع." else "No routine tasks scheduled for this day of the week.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 3. Notes & Plans Section
                if (scheduleFilterTab == "All" || scheduleFilterTab == "Notes") {
                    if (notesForDate.isNotEmpty()) {
                        item {
                            Text(
                                text = if (isArabic) "📝 ملاحظات وتأملات اليوم (${notesForDate.size})" else "📝 Day Notes & Reflections (${notesForDate.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                            )
                        }
                        items(notesForDate, key = { "note_${it.id}" }) { note ->
                            ScheduleNoteCard(
                                script = note,
                                onEdit = {
                                    editingScript = note
                                    editorInitialTab = 1
                                    showEditorSheet = true
                                },
                                onDelete = {
                                    viewModel.deleteScript(note.id)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else if (scheduleFilterTab == "Notes") {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "لا توجد ملاحظات مسجلة لهذا التاريخ." else "No notes recorded for this date.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button (defaults to setting a quest)
        FloatingActionButton(
            onClick = {
                editingScript = null
                editorInitialTab = 0
                showEditorSheet = true
            },
            containerColor = FocusIndigo,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("fab_add_schedule_quest")
        ) {
            Icon(Icons.Default.EmojiEvents, contentDescription = "Set Day Quest / Goal")
        }
    }

    if (showEditorSheet) {
        ScheduleGoalAndQuestEditorSheet(
            selectedDate = selectedDate,
            initialScript = editingScript,
            initialTab = editorInitialTab,
            onDismiss = {
                showEditorSheet = false
                editingScript = null
            },
            onSaveQuest = { title, description, questType, emoji, targetValue, xpReward ->
                viewModel.saveDailyQuest(
                    id = editingScript?.id ?: 0L,
                    title = title,
                    description = description,
                    date = selectedDate,
                    questType = questType,
                    emoji = emoji,
                    targetValue = targetValue,
                    xpReward = xpReward
                )
                showEditorSheet = false
                editingScript = null
            },
            onSaveNote = { emoji, content ->
                viewModel.saveScheduleNote(
                    id = editingScript?.id ?: 0L,
                    emoji = emoji,
                    content = content,
                    date = selectedDate
                )
                showEditorSheet = false
                editingScript = null
            }
        )
    }

    if (showTaskEditorForDate) {
        TaskEditorSheet(
            initialTask = null,
            allCategories = allCategories,
            initialDate = selectedDate,
            initialRepeatType = "SPECIFIC_DAY",
            onAddCategory = { name, colorHex -> viewModel.addCategory(name, colorHex) },
            onDismiss = { showTaskEditorForDate = false },
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
                showTaskEditorForDate = false
            }
        )
    }

    if (showClearTasksDialog) {
        val dateLabel = selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        AlertDialog(
            onDismissRequest = { showClearTasksDialog = false },
            title = { Text("Clear All Tasks for $dateLabel?") },
            text = {
                Text("This will remove all routine tasks scheduled for this day (${tasks.size} tasks). For recurring routines, it removes them from this day of the week. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearTasksDialog = false
                        viewModel.removeAllTasksForDate(selectedDate)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_tasks_button")
                ) {
                    Text("Remove All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearTasksDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ScheduleMonthHeader(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    isHijri: Boolean = false,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isArabic = LocalAppLanguage.current == "ar"
    val monthTitle = if (isHijri) {
        val hijriYear = HijriCalendarHelper.getHijriYear(selectedDate)
        val hijriMonth = HijriCalendarHelper.getHijriMonth(selectedDate)
        val hijriMonthName = HijriCalendarHelper.getHijriMonthName(hijriMonth, isArabic)
        val hijriYearStr = if (isArabic) HijriCalendarHelper.toArabicDigits(hijriYear) else hijriYear.toString()
        if (isArabic) "$hijriMonthName $hijriYearStr هـ" else "$hijriMonthName $hijriYear AH"
    } else {
        currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevMonth,
            modifier = Modifier.size(36.dp).testTag("prev_month_button")
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous Month",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = monthTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.size(36.dp).testTag("next_month_button")
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next Month",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun InteractiveCalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    allScripts: List<ScheduledScript>,
    allActiveTasks: List<RoutineTask> = emptyList(),
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1=Mon .. 7=Sun
    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")

    val questEpochDays = remember(allScripts) {
        val map = mutableMapOf<Long, Boolean>() // epochDay -> allCompleted
        val quests = allScripts.filter { it.category != "Note" }
        for (q in quests) {
            for (d in q.startDateEpochDay..q.endDateEpochDay) {
                val current = map[d]
                if (current == null) {
                    map[d] = q.isCompleted
                } else {
                    map[d] = current && q.isCompleted
                }
            }
        }
        map
    }

    val noteEpochDays = remember(allScripts) {
        val set = mutableSetOf<Long>()
        val notes = allScripts.filter { it.category == "Note" }
        for (n in notes) {
            for (d in n.startDateEpochDay..n.endDateEpochDay) {
                set.add(d)
            }
        }
        set
    }

    val taskDaysOfWeek = remember(allActiveTasks) {
        val set = mutableSetOf<Int>()
        for (task in allActiveTasks) {
            val days = task.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (days.isEmpty()) {
                set.addAll(1..7)
            } else {
                set.addAll(days)
            }
        }
        set
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("interactive_calendar_grid")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { dayName ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            var currentDay = 1
            for (row in 0 until 6) {
                if (currentDay > daysInMonth) break
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 1..7) {
                        val cellIndex = row * 7 + col
                        if (cellIndex < firstDayOfWeek || currentDay > daysInMonth) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val thisDate = yearMonth.atDay(currentDay)
                            val epochDay = thisDate.toEpochDay()
                            val isSelected = thisDate == selectedDate
                            val isToday = thisDate == today
                            val hasQuest = questEpochDays.containsKey(epochDay)
                            val allQuestsDone = questEpochDays[epochDay] == true
                            val hasNotes = noteEpochDays.contains(epochDay)
                            val hasTasks = taskDaysOfWeek.contains(thisDate.dayOfWeek.value)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isToday && !isSelected) 1.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .clickable { onDateSelected(thisDate) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$currentDay",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (hasQuest || hasTasks || hasNotes) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 1.dp)
                                        ) {
                                            // Quest Indicator (Gold/Orange or Green when done)
                                            if (hasQuest) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.5.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) {
                                                                MaterialTheme.colorScheme.onPrimary
                                                            } else if (allQuestsDone) {
                                                                SuccessGreen
                                                            } else {
                                                                FlameOrange
                                                            }
                                                        )
                                                )
                                            }
                                            // Tasks indicator (Teal)
                                            if (hasTasks) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.onPrimary else MindTeal
                                                        )
                                                )
                                            }
                                            // Notes indicator (Indigo)
                                            if (hasNotes) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(3.5.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.onPrimary else FocusIndigo
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            currentDay++
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HijriCalendarGrid(
    selectedDate: LocalDate,
    allScripts: List<ScheduledScript>,
    allActiveTasks: List<RoutineTask> = emptyList(),
    onDateSelected: (LocalDate) -> Unit
) {
    val isArabic = LocalAppLanguage.current == "ar"
    val today = LocalDate.now()
    val hijriYear = HijriCalendarHelper.getHijriYear(selectedDate)
    val hijriMonth = HijriCalendarHelper.getHijriMonth(selectedDate)
    val firstDayOfMonth = HijriCalendarHelper.hijriToLocalDate(hijriYear, hijriMonth, 1)
    val daysInMonth = HijriCalendarHelper.getHijriMonthLength(hijriYear, hijriMonth)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1=Mon .. 7=Sun
    val daysOfWeek = if (isArabic) {
        listOf("إث", "ثل", "أر", "خم", "جم", "سب", "أح")
    } else {
        listOf("M", "T", "W", "T", "F", "S", "S")
    }

    val questEpochDays = remember(allScripts) {
        val map = mutableMapOf<Long, Boolean>()
        val quests = allScripts.filter { it.category != "Note" }
        for (q in quests) {
            for (d in q.startDateEpochDay..q.endDateEpochDay) {
                val current = map[d]
                if (current == null) {
                    map[d] = q.isCompleted
                } else {
                    map[d] = current && q.isCompleted
                }
            }
        }
        map
    }

    val noteEpochDays = remember(allScripts) {
        val set = mutableSetOf<Long>()
        val notes = allScripts.filter { it.category == "Note" }
        for (n in notes) {
            for (d in n.startDateEpochDay..n.endDateEpochDay) {
                set.add(d)
            }
        }
        set
    }

    val taskDaysOfWeek = remember(allActiveTasks) {
        val set = mutableSetOf<Int>()
        for (task in allActiveTasks) {
            val days = task.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (days.isEmpty()) {
                set.addAll(1..7)
            } else {
                set.addAll(days)
            }
        }
        set
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("interactive_hijri_calendar_grid")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { dayName ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            var currentDay = 1
            for (row in 0 until 6) {
                if (currentDay > daysInMonth) break
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 1..7) {
                        val cellIndex = row * 7 + col
                        if (cellIndex < firstDayOfWeek || currentDay > daysInMonth) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val thisDate = HijriCalendarHelper.hijriToLocalDate(hijriYear, hijriMonth, currentDay)
                            val epochDay = thisDate.toEpochDay()
                            val isSelected = thisDate == selectedDate
                            val isToday = thisDate == today
                            val hasQuest = questEpochDays.containsKey(epochDay)
                            val allQuestsDone = questEpochDays[epochDay] == true
                            val hasNotes = noteEpochDays.contains(epochDay)
                            val hasTasks = taskDaysOfWeek.contains(thisDate.dayOfWeek.value)
                            val dayText = if (isArabic) HijriCalendarHelper.toArabicDigits(currentDay) else currentDay.toString()

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isToday && !isSelected) 1.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .clickable { onDateSelected(thisDate) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayText,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (hasQuest || hasTasks || hasNotes) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 1.dp)
                                        ) {
                                            if (hasQuest) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.5.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) {
                                                                MaterialTheme.colorScheme.onPrimary
                                                            } else if (allQuestsDone) {
                                                                SuccessGreen
                                                            } else {
                                                                FlameOrange
                                                            }
                                                        )
                                                )
                                            }
                                            if (hasTasks) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.onPrimary else MindTeal
                                                        )
                                                )
                                            }
                                            if (hasNotes) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(3.5.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.onPrimary else FocusIndigo
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            currentDay++
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Quest & Goal Card for a specific calendar day.
 */
@Composable
fun ScheduleQuestCard(
    quest: ScheduledScript,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = quest.isCompleted
    val emoji = quest.emoji.ifBlank { "🎯" }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isDone) SuccessGreen.copy(alpha = 0.4f) else FocusIndigo.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 1.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("schedule_quest_card_${quest.id}")
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Interactive Checkbox / Complete Button
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("quest_toggle_${quest.id}")
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = if (isDone) "Mark Quest Incomplete" else "Complete Quest",
                    tint = if (isDone) SuccessGreen else FocusIndigo,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Emoji icon container
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDone) SuccessGreen.copy(alpha = 0.12f) else FocusIndigo.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Quest Information
            Column(modifier = Modifier.weight(1f)) {
                // Quest Type Badge & XP Reward Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = (if (isDone) SuccessGreen else FocusIndigo).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = quest.questType.ifBlank { "Daily Quest" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) SuccessGreen else FocusIndigo,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (quest.targetValue.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = quest.targetValue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Title
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isDone) TextDecoration.LineThrough else null
                )

                // Optional Description / details
                if (quest.encryptedContent.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = quest.encryptedContent,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Action Buttons (Edit / Delete)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(30.dp).testTag("edit_quest_${quest.id}")
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Quest",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp).testTag("delete_quest_${quest.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Quest",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleTaskCard(
    taskWithStatus: TaskWithStatus,
    categories: List<CategoryItem>,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val task = taskWithStatus.task
    val isCompleted = taskWithStatus.isCompleted
    val catColor = getCategoryColor(task.category, categories)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("schedule_task_card_${task.id}")
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("schedule_task_toggle_${task.id}")
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = if (isCompleted) "Mark Incomplete" else "Mark Complete",
                    tint = if (isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = catColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = task.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = catColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (task.timeOfDay.isNotBlank()) {
                        Text(
                            text = task.timeOfDay,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                )
            }

            if (task.targetMinutes > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${task.targetMinutes}m",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(30.dp)
                        .padding(start = 4.dp)
                        .testTag("delete_task_${task.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleNoteCard(
    script: ScheduledScript,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val emoji = if (script.emoji.isNotBlank()) script.emoji else if (script.title.isNotBlank()) script.title else "📝"
    val content = script.encryptedContent.ifBlank { script.title }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_note_card_${script.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = emoji,
                        fontSize = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp).testTag("edit_note_${script.id}")
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Note",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("delete_note_${script.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Note",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bottom Sheet for creating or editing Goals, Quests, and Notes for a specific day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleGoalAndQuestEditorSheet(
    selectedDate: LocalDate,
    initialScript: ScheduledScript?,
    initialTab: Int = 0, // 0 = Goal/Quest, 1 = Note
    onDismiss: () -> Unit,
    onSaveQuest: (
        title: String,
        description: String,
        questType: String,
        emoji: String,
        targetValue: String,
        xpReward: Int
    ) -> Unit,
    onSaveNote: (emoji: String, content: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentTab by remember {
        mutableIntStateOf(
            if (initialScript != null) {
                if (initialScript.category == "Note") 1 else 0
            } else {
                initialTab
            }
        )
    }

    // Quest States
    var questTitle by remember {
        mutableStateOf(
            if (initialScript != null && initialScript.category != "Note") initialScript.title else ""
        )
    }
    var questDescription by remember {
        mutableStateOf(
            if (initialScript != null && initialScript.category != "Note") initialScript.encryptedContent else ""
        )
    }
    var selectedQuestType by remember {
        mutableStateOf(
            if (initialScript != null && initialScript.category != "Note") initialScript.questType else "🎯 Daily Quest"
        )
    }
    var questEmoji by remember {
        mutableStateOf(
            if (initialScript != null && initialScript.category != "Note") initialScript.emoji else "🎯"
        )
    }
    var questTarget by remember {
        mutableStateOf(
            if (initialScript != null && initialScript.category != "Note") initialScript.targetValue else ""
        )
    }

    // Note States
    var noteEmoji by remember {
        mutableStateOf(
            if (initialScript != null && initialScript.category == "Note") initialScript.emoji.ifBlank { "📝" } else "📝"
        )
    }
    var noteContent by remember {
        mutableStateOf(
            if (initialScript != null && initialScript.category == "Note") initialScript.encryptedContent else ""
        )
    }

    val questTypeOptions = listOf(
        "🎯 Daily Quest",
        "🏆 Milestone Goal",
        "⚔️ Epic Challenge",
        "⭐ Focus Target",
        "🧘 Daily Habit"
    )

    val emojiOptions = listOf(
        "📝", "💡", "🧠", "🎯", "⭐", "🔥", "🚀", "☕", "🌿", "🧘",
        "❤️", "✨", "🍎", "🏃", "📚", "🎨", "🎵", "💰", "🌈", "🍀"
    )

    val dateFormatted = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (initialScript == null) "Plan for $dateFormatted" else "Edit Entry",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (currentTab == 0) "Set a specific quest or goal for this date" else "Record thoughts or daily notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Selector: Goal / Quest vs Day Note
            TabRow(
                selectedTabIndex = currentTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .padding(2.dp)
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    text = { Text("🎯 Goal / Quest", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    text = { Text("📝 Daily Note", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentTab == 0) {
                // --- QUEST & GOAL MODE ---

                // Quest Title Field
                Text(
                    text = "Quest Title *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = questTitle,
                    onValueChange = { questTitle = it },
                    placeholder = { Text("e.g. Run 5km, Read Chapter 4, Deep Focus Sprint") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quest_title_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FocusIndigo,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Quest Type Selection Chips
                Text(
                    text = "Quest Type",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(questTypeOptions) { qType ->
                        val isSelected = selectedQuestType == qType
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) FocusIndigo else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedQuestType = qType }
                        ) {
                            Text(
                                text = qType,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Quest Emoji / Icon Selection (Type any emoji or pick from list)
                Text(
                    text = "Quest Emoji / Icon",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FocusIndigo.copy(alpha = 0.12f),
                        border = BorderStroke(1.5.dp, FocusIndigo),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = questEmoji.ifBlank { "🎯" }, fontSize = 26.sp)
                        }
                    }

                    OutlinedTextField(
                        value = questEmoji,
                        onValueChange = { input -> questEmoji = input },
                        label = { Text("Custom Emoji") },
                        placeholder = { Text("Type or paste ANY emoji e.g. 🎯, 🚀, 🧗, ⚡") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quest_emoji_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FocusIndigo,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Emoji Tap Options (Rich variety)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val extendedEmojis = listOf(
                        "🎯", "🏆", "⚔️", "⭐", "🚀", "🏃", "📚", "💻", "🔥", "💎",
                        "👑", "☕", "🧘", "🌿", "🎨", "🎵", "❤️", "⚡", "🍀", "🌈",
                        "🏋️", "🚴", "🏊", "⚽", "🏀", "💡", "🧠", "🍎", "💧", "💰",
                        "📈", "🛠️", "🎓", "🦁", "🧗", "✨", "⏱️", "📝", "🎯", "🛡️"
                    )
                    items(extendedEmojis) { em ->
                        val isSelected = questEmoji == em
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) FocusIndigo.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(if (isSelected) 1.5.dp else 0.dp, if (isSelected) FocusIndigo else Color.Transparent),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { questEmoji = em }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = em, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Value Field (Optional)
                Text(
                    text = "Target / Metric (Optional)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = questTarget,
                    onValueChange = { questTarget = it },
                    placeholder = { Text("e.g. 5 km, 45m, 100 reps, 1 chapter") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quest_target_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Description / Steps (Optional)
                Text(
                    text = "Details & Action Steps (Optional)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = questDescription,
                    onValueChange = { questDescription = it },
                    placeholder = { Text("Add tips, checklist steps, or notes for this quest...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .testTag("quest_description_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Quest Button
                Button(
                    onClick = {
                        if (questTitle.isNotBlank()) {
                            onSaveQuest(
                                questTitle,
                                questDescription,
                                selectedQuestType,
                                questEmoji.ifBlank { "🎯" },
                                questTarget,
                                0
                            )
                        }
                    },
                    enabled = questTitle.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FocusIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_set_quest_button")
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (initialScript == null) "Set Quest for ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))}" else "Save Quest Changes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            } else {
                // --- DAILY NOTE MODE ---
                Text(
                    text = "Choose Emoji / Mood",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(emojiOptions) { em ->
                        val isSelected = noteEmoji == em
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { noteEmoji = em }
                        ) {
                            Text(text = em, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Note / Thoughts",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    placeholder = { Text("Write your daily schedule, notes, thoughts, or reflections...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .testTag("schedule_note_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (noteContent.isNotBlank()) {
                            onSaveNote(noteEmoji, noteContent)
                        }
                    },
                    enabled = noteContent.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_schedule_note_button")
                ) {
                    Text(
                        text = if (initialScript == null) "Add Note to Schedule" else "Save Changes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
