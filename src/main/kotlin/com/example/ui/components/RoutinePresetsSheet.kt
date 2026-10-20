package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryItem
import com.example.data.model.PresetTaskItem
import com.example.data.model.RoutinePreset
import com.example.data.model.ScheduledPresetPlan
import com.example.util.CategoryLocalization
import com.example.util.LocalAppLanguage
import com.example.util.PresetTransferHelper
import com.example.util.appStrings
import com.example.util.formatLocalizedDate
import com.example.util.getLocalizedTimeSlot
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinePresetsSheet(
    presets: List<RoutinePreset>,
    scheduledPlans: List<ScheduledPresetPlan>,
    categories: List<CategoryItem> = emptyList(),
    onDismiss: () -> Unit,
    onActivatePreset: (preset: RoutinePreset, replaceExisting: Boolean) -> Unit,
    onSchedulePreset: (preset: RoutinePreset, startDate: LocalDate, replaceExisting: Boolean, note: String) -> Unit,
    onSaveCurrentAsPreset: (name: String, icon: String, colorHex: String, description: String) -> Unit,
    onCreatePreset: (RoutinePreset) -> Unit,
    onDeletePreset: (presetId: String) -> Unit,
    onApplyScheduledNow: (planId: String) -> Unit,
    onCancelScheduled: (planId: String) -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val strings = appStrings()
    val isArabic = LocalAppLanguage.current == "ar"
    val context = LocalContext.current

    // Tabs: 0 -> Presets, 1 -> Scheduled, 2 -> Export & Import
    var selectedTab by remember { mutableStateOf(0) }
    var presetToActivate by remember { mutableStateOf<RoutinePreset?>(null) }
    var presetToSchedule by remember { mutableStateOf<RoutinePreset?>(null) }
    var showSaveCurrentDialog by remember { mutableStateOf(false) }
    var showCreatePresetDialog by remember { mutableStateOf(false) }
    var presetToEdit by remember { mutableStateOf<RoutinePreset?>(null) }

    // Quick export & share state from card
    var presetForShareMenu by remember { mutableStateOf<RoutinePreset?>(null) }
    var importedPresetToConfirm by remember { mutableStateOf<RoutinePreset?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("routine_presets_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.routinePresets,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.routinePresetsSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_presets_sheet_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = strings.close)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Save current routine as preset & New preset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showSaveCurrentDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_current_as_preset_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.saveCurrentAsPreset,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = { showCreatePresetDialog = true },
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("create_new_preset_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.newPreset,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Segmented Tabs: Presets, Scheduled, Export & Import
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("${strings.presets} (${presets.size})", fontSize = 11.sp) },
                    leadingIcon = {
                        if (selectedTab == 0) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("tab_all_presets")
                )

                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("${strings.scheduledPresetsTitle} (${scheduledPlans.size})", fontSize = 11.sp, maxLines = 1) },
                    leadingIcon = {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(13.dp))
                    },
                    modifier = Modifier.weight(1.2f).testTag("tab_scheduled_presets")
                )

                FilterChip(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    label = { Text(strings.exportImportPresets, fontSize = 11.sp, maxLines = 1) },
                    leadingIcon = {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                    },
                    modifier = Modifier.weight(1f).testTag("tab_export_import_presets")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Content List depending on selected tab
            when (selectedTab) {
                0 -> {
                    // Presets Tab - NO EXAMPLES by default
                    if (presets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = if (isArabic) "لا توجد قوالب روتين بعد" else "No Routine Presets Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = strings.noPresetsYet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { showCreatePresetDialog = true },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.newPreset, fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { selectedTab = 2 },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.importPresetTab, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(presets, key = { it.id }) { preset ->
                                PresetCard(
                                    preset = preset,
                                    isArabic = isArabic,
                                    onActivate = { presetToActivate = preset },
                                    onSchedule = { presetToSchedule = preset },
                                    onEdit = { presetToEdit = preset },
                                    onDelete = { onDeletePreset(preset.id) },
                                    onShare = { presetForShareMenu = preset }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Scheduled Presets Tab
                    if (scheduledPlans.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = strings.noScheduledPresets,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(scheduledPlans, key = { it.id }) { plan ->
                                ScheduledPlanCard(
                                    plan = plan,
                                    isArabic = isArabic,
                                    onApplyNow = { onApplyScheduledNow(plan.id) },
                                    onCancel = { onCancelScheduled(plan.id) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Dedicated Export & Import Presets Page
                    PresetExportImportPage(
                        presets = presets,
                        isArabic = isArabic,
                        strings = strings,
                        onImportPreset = { importedPreset ->
                            onCreatePreset(importedPreset)
                            selectedTab = 0
                            onShowMessage(strings.presetImportedSuccess)
                        },
                        onShowMessage = onShowMessage
                    )
                }
            }
        }
    }

    // Quick Share Menu for a preset
    presetForShareMenu?.let { preset ->
        SharePresetModal(
            preset = preset,
            isArabic = isArabic,
            onDismiss = { presetForShareMenu = null },
            onShowMessage = onShowMessage
        )
    }

    // Activate Preset Confirmation Dialog
    presetToActivate?.let { preset ->
        ActivatePresetDialog(
            preset = preset,
            onDismiss = { presetToActivate = null },
            onConfirm = { replaceExisting ->
                onActivatePreset(preset, replaceExisting)
                presetToActivate = null
                onDismiss()
            }
        )
    }

    // Schedule Preset Dialog
    presetToSchedule?.let { preset ->
        SchedulePresetDateDialog(
            preset = preset,
            onDismiss = { presetToSchedule = null },
            onConfirm = { startDate, replaceExisting, note ->
                onSchedulePreset(preset, startDate, replaceExisting, note)
                presetToSchedule = null
                selectedTab = 1
            }
        )
    }

    // Save Current Routine as Preset Dialog
    if (showSaveCurrentDialog) {
        SaveCurrentAsPresetDialog(
            onDismiss = { showSaveCurrentDialog = false },
            onConfirm = { name, icon, colorHex, desc ->
                onSaveCurrentAsPreset(name, icon, colorHex, desc)
                showSaveCurrentDialog = false
            }
        )
    }

    // Create / Edit Preset Dialog (WITH FULL TIMING, DAYS, AND TIME OF DAY CHOICES)
    if (showCreatePresetDialog || presetToEdit != null) {
        val initial = presetToEdit
        PresetEditorDialog(
            initialPreset = initial,
            userCategories = categories,
            onDismiss = {
                showCreatePresetDialog = false
                presetToEdit = null
            },
            onSave = { savedPreset ->
                onCreatePreset(savedPreset)
                showCreatePresetDialog = false
                presetToEdit = null
                onShowMessage(strings.presetSavedSuccess)
            }
        )
    }
}

/**
 * Preset Card with edit, delete, and direct export/share buttons.
 */
@Composable
fun PresetCard(
    preset: RoutinePreset,
    isArabic: Boolean,
    onActivate: () -> Unit,
    onSchedule: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val strings = appStrings()
    var isExpanded by remember { mutableStateOf(false) }
    val accentColor = try {
        Color(android.graphics.Color.parseColor(preset.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val totalMinutes = preset.tasks.sumOf { it.targetMinutes }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("preset_card_${preset.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = preset.iconEmoji, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (preset.description.isNotBlank()) {
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(32.dp).testTag("share_preset_${preset.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp).testTag("edit_preset_${preset.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = strings.edit, modifier = Modifier.size(15.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_preset_${preset.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata row: tasks count, total minutes, toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📋 ${preset.tasks.size} ${strings.tasksPack}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (totalMinutes > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "⏱️ $totalMinutes min",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isExpanded) (if (isArabic) "إخفاء التفاصيل" else "Hide details") else (if (isArabic) "عرض التفاصيل" else "View details"),
                        fontSize = 11.sp,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Expanded task list with timing and days badges
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    preset.tasks.forEachIndexed { idx, task ->
                        val localizedCat = CategoryLocalization.getLocalizedCategoryName(task.category, isArabic)
                        val localizedSlot = getLocalizedTimeSlot(task.timeOfDay, isArabic)
                        val daysSummary = formatPresetDaysSummary(task.daysOfWeek, task.repeatType, task.repeatIntervalDays, isArabic)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${idx + 1}.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = task.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "$localizedSlot • $daysSummary",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = accentColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = localizedCat,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (task.targetMinutes > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${task.targetMinutes}m",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Activate Now / Schedule for Later
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onActivate,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("activate_preset_btn_${preset.id}"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.activateNow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSchedule,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("schedule_preset_btn_${preset.id}"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.schedulePreset, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Scheduled Routine Plan Card
 */
@Composable
fun ScheduledPlanCard(
    plan: ScheduledPresetPlan,
    isArabic: Boolean,
    onApplyNow: () -> Unit,
    onCancel: () -> Unit
) {
    val strings = appStrings()
    val planDate = LocalDate.ofEpochDay(plan.startDateEpochDay)
    val today = LocalDate.now()
    val isPastOrToday = planDate <= today
    val formattedDate = formatLocalizedDate(planDate, isArabic)

    val accentColor = try {
        Color(android.graphics.Color.parseColor(plan.presetColorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(
            width = if (isPastOrToday) 1.5.dp else 1.dp,
            color = if (isPastOrToday) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = plan.presetIcon, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = plan.presetName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${strings.scheduledFor}: $formattedDate",
                            fontSize = 11.sp,
                            color = if (isPastOrToday) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isPastOrToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (plan.isApplied) Color(0xFF10B981).copy(alpha = 0.15f) else accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (plan.isApplied) (if (isArabic) "تم التطبيق" else "Applied") else (if (isPastOrToday) strings.activeToday else strings.upcoming),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (plan.isApplied) Color(0xFF10B981) else accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (plan.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📝 ${plan.note}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!plan.isApplied) {
                    Button(
                        onClick = onApplyNow,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text(strings.applyScheduleNow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(0.8f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(strings.cancelSchedule, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/**
 * Dedicated Export and Import Presets View.
 */
@Composable
fun PresetExportImportPage(
    presets: List<RoutinePreset>,
    isArabic: Boolean,
    strings: com.example.util.AppStringValues,
    onImportPreset: (RoutinePreset) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedPresetIndex by remember { mutableStateOf(0) }
    var pasteJsonText by remember { mutableStateOf("") }
    var presetToReview by remember { mutableStateOf<RoutinePreset?>(null) }
    var presetToSaveViaSaf by remember { mutableStateOf<RoutinePreset?>(null) }

    // SAF Create Document launcher for exporting preset file directly
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val preset = presetToSaveViaSaf
            if (preset != null) {
                val json = PresetTransferHelper.presetToJson(preset)
                val ok = PresetTransferHelper.writeJsonToUri(context, uri, json)
                if (ok) {
                    onShowMessage(strings.presetExportedSuccess)
                } else {
                    onShowMessage("Failed to write preset file")
                }
            }
        }
        presetToSaveViaSaf = null
    }

    // SAF Open Document launcher for importing preset file
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val json = PresetTransferHelper.readJsonFromUri(context, uri)
            if (!json.isNullOrBlank()) {
                val parsed = PresetTransferHelper.jsonToPreset(json)
                if (parsed != null) {
                    presetToReview = parsed
                } else {
                    onShowMessage("Invalid routine preset file")
                }
            } else {
                onShowMessage("Could not read file")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Export & Share
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.exportPresetTab,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isArabic) "قم بتصدير قالب الروتين كملف JSON أو مشاركته مع أجهزة وتطبيقات أخرى." else "Export any preset routine as a file or share it directly to other devices.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (presets.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.noPresetsYet,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    val safeIndex = selectedPresetIndex.coerceIn(0, presets.size - 1)
                    val activePreset = presets[safeIndex]

                    Text(
                        text = if (isArabic) "اختر القالب للتصدير:" else "Select Preset to Export:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset selection chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEachIndexed { idx, p ->
                            val isSel = idx == safeIndex
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedPresetIndex = idx }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(text = p.iconEmoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = p.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Selected Preset Preview Summary
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(activePreset.iconEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(activePreset.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("📋 ${activePreset.tasks.size} ${strings.presetTasks}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Export Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                presetToSaveViaSaf = activePreset
                                val safeSlug = activePreset.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(24)
                                createDocLauncher.launch("routine_preset_${safeSlug}.json")
                            },
                            modifier = Modifier.weight(1f).testTag("export_preset_file_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.exportPresetAsFile, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                val ok = PresetTransferHelper.sharePresetAsFile(context, activePreset)
                                if (!ok) {
                                    // Fallback to text share
                                    PresetTransferHelper.sharePresetAsText(context, activePreset)
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("share_preset_file_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.sharePresetFile, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = {
                            val json = PresetTransferHelper.presetToJson(activePreset)
                            clipboard.setText(AnnotatedString(json))
                            onShowMessage(if (isArabic) "تم نسخ كود القالب JSON إلى الحافظة" else "Preset JSON copied to clipboard")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.sharePresetText, fontSize = 11.sp)
                    }
                }
            }
        }

        // Section 2: Import Preset
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.importPresetTab,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isArabic) "استيراد قالب روتين تم تصديره من قبل عن طريق ملف JSON أو لصق كود القالب." else "Import a preset from a saved .json file on your phone, or paste its JSON code.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Import from File button
                Button(
                    onClick = { openDocLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth().testTag("import_preset_file_btn"),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.importFromFile, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isArabic) "أو الصق نص JSON للقالب مباشرة:" else "Or paste JSON code directly:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = pasteJsonText,
                    onValueChange = { pasteJsonText = it },
                    placeholder = { Text("{\"version\": 1, \"name\": \"...\"}", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 130.dp).testTag("paste_preset_json_input"),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val parsed = PresetTransferHelper.jsonToPreset(pasteJsonText)
                        if (parsed != null) {
                            presetToReview = parsed
                        } else {
                            onShowMessage(if (isArabic) "صيغة JSON غير صحيحة" else "Invalid preset JSON format")
                        }
                    },
                    enabled = pasteJsonText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("import_preset_text_btn"),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.importFromText, fontSize = 11.sp)
                }
            }
        }
    }

    // Review & Confirmation Dialog before adding the imported preset
    presetToReview?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetToReview = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(preset.iconEmoji, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isArabic) "تأكيد استيراد القالب" else "Confirm Import Preset")
                }
            },
            text = {
                Column {
                    Text(
                        text = preset.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (preset.description.isNotBlank()) {
                        Text(
                            text = preset.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${strings.presetTasks} (${preset.tasks.size}):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        preset.tasks.forEachIndexed { i, t ->
                            val slot = getLocalizedTimeSlot(t.timeOfDay, isArabic)
                            val days = formatPresetDaysSummary(t.daysOfWeek, t.repeatType, t.repeatIntervalDays, isArabic)
                            Text(
                                text = "${i + 1}. ${t.title} (${slot} • ${days} • ${t.targetMinutes}m)",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportPreset(preset)
                        presetToReview = null
                        pasteJsonText = ""
                    },
                    modifier = Modifier.testTag("confirm_import_preset_button")
                ) {
                    Text(if (isArabic) "استيراد القالب" else "Import Preset")
                }
            },
            dismissButton = {
                TextButton(onClick = { presetToReview = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

/**
 * Share preset bottom sheet modal with multiple export/share options.
 */
@Composable
fun SharePresetModal(
    preset: RoutinePreset,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val strings = appStrings()

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val json = PresetTransferHelper.presetToJson(preset)
            val ok = PresetTransferHelper.writeJsonToUri(context, uri, json)
            if (ok) {
                onShowMessage(strings.presetExportedSuccess)
                onDismiss()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(preset.iconEmoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(preset.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isArabic) "اختر طريقة تصدير أو مشاركة هذا القالب (${preset.tasks.size} مهام):" else "Choose how to export or share this preset (${preset.tasks.size} tasks):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option 1: Share as File (.json)
                Button(
                    onClick = {
                        val ok = PresetTransferHelper.sharePresetAsFile(context, preset)
                        if (!ok) {
                            PresetTransferHelper.sharePresetAsText(context, preset)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("modal_share_file_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.sharePresetFile, fontSize = 12.sp)
                }

                // Option 2: Save File (.json) to Device
                OutlinedButton(
                    onClick = {
                        val safeSlug = preset.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(24)
                        createDocLauncher.launch("routine_preset_${safeSlug}.json")
                    },
                    modifier = Modifier.fillMaxWidth().testTag("modal_save_file_btn")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.exportPresetAsFile, fontSize = 12.sp)
                }

                // Option 3: Copy JSON
                TextButton(
                    onClick = {
                        val json = PresetTransferHelper.presetToJson(preset)
                        clipboard.setText(AnnotatedString(json))
                        onShowMessage(if (isArabic) "تم نسخ كود القالب JSON إلى الحافظة" else "Preset JSON copied to clipboard")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.sharePresetText, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.close)
            }
        }
    )
}

/**
 * Dialog to activate a preset (with Replace or Append option).
 */
@Composable
fun ActivatePresetDialog(
    preset: RoutinePreset,
    onDismiss: () -> Unit,
    onConfirm: (replaceExisting: Boolean) -> Unit
) {
    val strings = appStrings()
    var replaceExisting by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(preset.iconEmoji, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.activatePreset)
            }
        },
        text = {
            Column {
                Text(
                    text = "Apply preset '${preset.name}' (${preset.tasks.size} tasks)?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Option: Replace
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { replaceExisting = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = replaceExisting,
                        onClick = { replaceExisting = true }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(strings.replaceTasks, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(strings.replaceOptionDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Option: Append
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { replaceExisting = false }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !replaceExisting,
                        onClick = { replaceExisting = false }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(strings.appendTasks, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(strings.appendOptionDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(replaceExisting) },
                modifier = Modifier.testTag("confirm_activate_preset_button")
            ) {
                Text(strings.activateNow)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

/**
 * Schedule Preset for a Date Dialog.
 */
@Composable
fun SchedulePresetDateDialog(
    preset: RoutinePreset,
    onDismiss: () -> Unit,
    onConfirm: (startDate: LocalDate, replaceExisting: Boolean, note: String) -> Unit
) {
    val strings = appStrings()
    val isArabic = LocalAppLanguage.current == "ar"
    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var replaceExisting by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.schedulePreset)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = strings.selectStartDate,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Quick Date selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val today = LocalDate.now()
                    listOf(
                        (if (isArabic) "اليوم" else "Today") to today,
                        (if (isArabic) "غداً" else "Tomorrow") to today.plusDays(1),
                        (if (isArabic) "بعد يومين" else "In 2 Days") to today.plusDays(2),
                        (if (isArabic) "+أسبوع" else "+1 Week") to today.plusWeeks(1)
                    ).forEach { (label, dateVal) ->
                        val isSelected = selectedDate == dateVal
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDate = dateVal }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedDate = selectedDate.minusDays(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Day", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = formatLocalizedDate(selectedDate, isArabic),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { selectedDate = selectedDate.plusDays(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Replacement Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { replaceExisting = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = replaceExisting,
                        onClick = { replaceExisting = true }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.replaceTasks, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { replaceExisting = false }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !replaceExisting,
                        onClick = { replaceExisting = false }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.appendTasks, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isArabic) "ملاحظة أو تذكير (اختياري)" else "Note or reminder (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedDate, replaceExisting, note) },
                modifier = Modifier.testTag("confirm_schedule_preset_button")
            ) {
                Text(strings.schedulePreset)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

/**
 * Dialog to save active tasks as a new Routine Preset.
 */
@Composable
fun SaveCurrentAsPresetDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String, colorHex: String, desc: String) -> Unit
) {
    val strings = appStrings()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var iconEmoji by remember { mutableStateOf("📋") }
    var colorHex by remember { mutableStateOf("#6366F1") }

    val emojis = listOf("📋", "💼", "🧘", "📚", "🌿", "⚡", "🎯", "🌅", "🌙", "🔥")
    val colors = listOf("#6366F1", "#10B981", "#F59E0B", "#EC4899", "#3B82F6", "#8B5CF6", "#14B8A6", "#EF4444")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.saveCurrentAsPreset)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.presetName) },
                    placeholder = { Text("e.g. Morning Sprint, Study Mode") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preset_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.presetDescription) },
                    placeholder = { Text("Short purpose description...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Icon Emoji:", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    emojis.take(5).forEach { emoji ->
                        val isSelected = iconEmoji == emoji
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { iconEmoji = emoji }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Theme Color:", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.take(6).forEach { hex ->
                        val isSelected = colorHex == hex
                        Surface(
                            shape = CircleShape,
                            color = Color(android.graphics.Color.parseColor(hex)),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { colorHex = hex }
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), iconEmoji, colorHex, description.trim()) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_current_preset_confirm_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

/**
 * Dialog to create or edit a Routine Preset.
 * PROVIDES FULL TIMING SELECTION: Everyday vs Select Days (e.g. 2 days, 1 day, custom combination)
 * and WHEN IT HAPPENS: Morning, Afternoon, Evening, Night, Anytime!
 */
@Composable
fun PresetEditorDialog(
    initialPreset: RoutinePreset?,
    userCategories: List<CategoryItem> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (RoutinePreset) -> Unit
) {
    val strings = appStrings()
    val isArabic = LocalAppLanguage.current == "ar"
    var name by remember { mutableStateOf(initialPreset?.name ?: "") }
    var description by remember { mutableStateOf(initialPreset?.description ?: "") }
    var iconEmoji by remember { mutableStateOf(initialPreset?.iconEmoji ?: "📋") }
    var colorHex by remember { mutableStateOf(initialPreset?.colorHex ?: "#6366F1") }

    // Start with empty task list if new preset, or initial preset tasks if editing
    val taskList = remember {
        mutableStateOf(initialPreset?.tasks ?: emptyList())
    }

    // New/Editing Task Subform States
    var editingTaskIndex by remember { mutableStateOf<Int?>(null) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskCategory by remember { mutableStateOf(userCategories.firstOrNull()?.name ?: "Health") }
    var newTaskTimeOfDay by remember { mutableStateOf("Morning") }
    var newTaskMinutes by remember { mutableStateOf("15") }

    // Timing & Recurrence: Everyday vs Specific Days
    var repeatType by remember { mutableStateOf("WEEKLY") } // "WEEKLY", "INTERVAL"
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) } // Default Everyday
    var repeatIntervalDays by remember { mutableStateOf(2) }

    val emojis = listOf("📋", "💼", "🧘", "📚", "🌿", "⚡", "🎯", "🌅", "🌙", "🔥")
    val colors = listOf("#6366F1", "#10B981", "#F59E0B", "#EC4899", "#3B82F6", "#8B5CF6", "#14B8A6", "#EF4444")

    val categoriesList = if (userCategories.isNotEmpty()) {
        userCategories.map { it.name }
    } else {
        listOf("Health", "Focus", "Mind", "Habit", "Other")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialPreset != null) strings.editPreset else strings.newPreset)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.presetName) },
                    placeholder = { Text("e.g. My Workout & Study Routine") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("preset_form_name_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.presetDescription) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Icon and Color selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Icon:", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            emojis.take(5).forEach { emoji ->
                                val isSelected = iconEmoji == emoji
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(30.dp).clickable { iconEmoji = emoji }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(emoji, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    Column {
                        Text("Color:", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            colors.take(4).forEach { hex ->
                                val isSelected = colorHex == hex
                                Surface(
                                    shape = CircleShape,
                                    color = Color(android.graphics.Color.parseColor(hex)),
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null,
                                    modifier = Modifier.size(24.dp).clickable { colorHex = hex }
                                ) {}
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tasks in this preset header
                Text(
                    text = "${strings.presetTasks} (${taskList.value.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (taskList.value.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isArabic) "لا توجد مهام في هذا القالب بعد. استخدم النموذج أدناه لإضافة مهام مع تحديد التوقيت والأيام." else "No tasks added yet. Use the form below to add tasks with timing & days.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    taskList.value.forEachIndexed { index, task ->
                        val localizedCat = CategoryLocalization.getLocalizedCategoryName(task.category, isArabic)
                        val slot = getLocalizedTimeSlot(task.timeOfDay, isArabic)
                        val daysDesc = formatPresetDaysSummary(task.daysOfWeek, task.repeatType, task.repeatIntervalDays, isArabic)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${index + 1}. ${task.title}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "$localizedCat • $slot • $daysDesc • ${task.targetMinutes}m",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            // Load into form for editing
                                            editingTaskIndex = index
                                            newTaskTitle = task.title
                                            newTaskCategory = task.category
                                            newTaskTimeOfDay = task.timeOfDay
                                            newTaskMinutes = task.targetMinutes.toString()
                                            repeatType = task.repeatType
                                            repeatIntervalDays = task.repeatIntervalDays
                                            selectedDays = task.daysOfWeek.split(",")
                                                .mapNotNull { it.trim().toIntOrNull() }
                                                .toSet()
                                                .ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            taskList.value = taskList.value.filterIndexed { i, _ -> i != index }
                                            if (editingTaskIndex == index) editingTaskIndex = null
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sub-form for adding / editing a task in the preset
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (editingTaskIndex != null) (if (isArabic) "تعديل المهمة بالقالب" else "Edit Task in Preset") else strings.addTaskToPreset,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (editingTaskIndex != null) {
                                TextButton(
                                    onClick = {
                                        editingTaskIndex = null
                                        newTaskTitle = ""
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(if (isArabic) "إلغاء التعديل" else "Cancel Edit", fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = newTaskTitle,
                            onValueChange = { newTaskTitle = it },
                            placeholder = { Text("Task name (e.g. 30m Workout, Reading)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("preset_new_task_title_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category chips
                        Text(
                            text = strings.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            categoriesList.take(5).forEach { cat ->
                                val isSelected = newTaskCategory == cat
                                val localized = CategoryLocalization.getLocalizedCategoryName(cat, isArabic)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { newTaskCategory = cat }
                                ) {
                                    Text(
                                        text = localized,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 1. WHEN IT HAPPENS (Time of Day: Morning, Afternoon, Evening, Night, Anytime)
                        Text(
                            text = "${strings.timeOfDayLabel} (${if (isArabic) "الصباح، بعد الظهر، المساء، إلخ" else "Morning, Evening, etc."}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val timeSlots = listOf(
                                ("Morning" to "🌅"),
                                ("Afternoon" to "☀️"),
                                ("Evening" to "🌇"),
                                ("Night" to "🌙"),
                                ("Anytime" to "⏱️")
                            )
                            timeSlots.forEach { (slot, emoji) ->
                                val isSelected = newTaskTimeOfDay.equals(slot, ignoreCase = true)
                                val label = getLocalizedTimeSlot(slot, isArabic)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { newTaskTimeOfDay = slot }
                                        .testTag("preset_time_slot_${slot.lowercase()}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = emoji, fontSize = 13.sp)
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. TIMING & RECURRENCE (Everyday vs Select Two Days or Any Days)
                        Text(
                            text = "${strings.timingRecurrence} (${if (isArabic) "كل يوم أو تحديد يومين أو أيام مخصصة" else "Everyday, 2 days, or specific days"}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Quick presets: Everyday, Weekdays, Weekend, or Interval
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isEveryday = repeatType == "WEEKLY" && selectedDays == setOf(1, 2, 3, 4, 5, 6, 7)
                            val isWeekdays = repeatType == "WEEKLY" && selectedDays == setOf(1, 2, 3, 4, 5)
                            val isWeekend = repeatType == "WEEKLY" && selectedDays == setOf(6, 7)
                            val isInterval = repeatType == "INTERVAL"

                            // Everyday
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isEveryday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, if (isEveryday) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        repeatType = "WEEKLY"
                                        selectedDays = setOf(1, 2, 3, 4, 5, 6, 7)
                                    }
                                    .testTag("preset_recurrence_everyday")
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = strings.everyDayLabel,
                                        fontSize = 10.sp,
                                        fontWeight = if (isEveryday) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isEveryday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Weekdays
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isWeekdays) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, if (isWeekdays) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        repeatType = "WEEKLY"
                                        selectedDays = setOf(1, 2, 3, 4, 5)
                                    }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (isArabic) "أيام العمل" else "Weekdays",
                                        fontSize = 10.sp,
                                        fontWeight = if (isWeekdays) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isWeekdays) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Weekend (2 days)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isWeekend) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, if (isWeekend) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        repeatType = "WEEKLY"
                                        selectedDays = setOf(6, 7)
                                    }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (isArabic) "يومان (عطلة)" else "Sat & Sun",
                                        fontSize = 10.sp,
                                        fontWeight = if (isWeekend) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isWeekend) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Every 2/3 Days Interval
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isInterval) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, if (isInterval) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { repeatType = "INTERVAL" }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (isArabic) "كل يومين" else "Every 2d",
                                        fontSize = 10.sp,
                                        fontWeight = if (isInterval) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isInterval) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (repeatType == "INTERVAL") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isArabic) "يتكرر كل $repeatIntervalDays أيام" else "Repeats every $repeatIntervalDays days",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (repeatIntervalDays > 2) repeatIntervalDays-- },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("$repeatIntervalDays d", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { if (repeatIntervalDays < 14) repeatIntervalDays++ },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // Day pills for custom day selection: Mon(1), Tue(2), Wed(3), Thu(4), Fri(5), Sat(6), Sun(7)
                            // Allows selecting two days (e.g. Mon & Wed), or any custom set of days!
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
                                    val isSelected = selectedDays.contains(dayInt)
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clickable {
                                                selectedDays = if (isSelected) {
                                                    if (selectedDays.size > 1) selectedDays - dayInt else selectedDays
                                                } else {
                                                    selectedDays + dayInt
                                                }
                                                repeatType = "WEEKLY"
                                            }
                                            .testTag("preset_day_chip_$dayInt")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Feedback on selected days
                            val daysSummary = formatPresetDaysSummary(selectedDays.sorted().joinToString(","), "WEEKLY", 1, isArabic)
                            Text(
                                text = "📅 $daysSummary",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3. TARGET MINUTES
                        Text(
                            text = if (isArabic) "المدة المستهدفة (بالدقائق):" else "Target Duration (minutes):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("5", "10", "15", "25", "30", "45", "60").forEach { min ->
                                val isSelected = newTaskMinutes == min
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { newTaskMinutes = min }
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${min}m",
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Add or Update Task Button
                        Button(
                            onClick = {
                                if (newTaskTitle.isNotBlank()) {
                                    val daysStr = selectedDays.sorted().joinToString(",")
                                    val item = PresetTaskItem(
                                        title = newTaskTitle.trim(),
                                        category = newTaskCategory,
                                        timeOfDay = newTaskTimeOfDay,
                                        targetMinutes = newTaskMinutes.toIntOrNull() ?: 15,
                                        daysOfWeek = daysStr,
                                        repeatType = repeatType,
                                        repeatIntervalDays = repeatIntervalDays
                                    )
                                    val editIdx = editingTaskIndex
                                    if (editIdx != null && editIdx in taskList.value.indices) {
                                        taskList.value = taskList.value.toMutableList().also { it[editIdx] = item }
                                        editingTaskIndex = null
                                    } else {
                                        taskList.value = taskList.value + item
                                    }
                                    newTaskTitle = ""
                                }
                            },
                            enabled = newTaskTitle.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("add_task_to_preset_btn"),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(if (editingTaskIndex != null) Icons.Default.Check else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (editingTaskIndex != null) (if (isArabic) "تحديث المهمة في القالب" else "Update Task in Preset") else strings.addTaskToPreset,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val preset = RoutinePreset(
                        id = initialPreset?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name.trim().ifBlank { "Custom Preset" },
                        description = description.trim(),
                        iconEmoji = iconEmoji,
                        colorHex = colorHex,
                        tasks = taskList.value,
                        isBuiltIn = false
                    )
                    onSave(preset)
                },
                enabled = name.isNotBlank() && taskList.value.isNotEmpty(),
                modifier = Modifier.testTag("save_preset_form_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

/**
 * Format days summary helper for presets.
 */
private fun formatPresetDaysSummary(
    daysOfWeek: String,
    repeatType: String,
    repeatIntervalDays: Int,
    isArabic: Boolean
): String {
    if (repeatType == "INTERVAL") {
        return if (isArabic) "كل $repeatIntervalDays أيام" else "Every $repeatIntervalDays days"
    }
    val daysSet = daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    return when {
        daysSet.size == 7 -> if (isArabic) "كل يوم" else "Everyday"
        daysSet == setOf(1, 2, 3, 4, 5) -> if (isArabic) "أيام العمل (إثن-جمع)" else "Weekdays (Mon-Fri)"
        daysSet == setOf(6, 7) -> if (isArabic) "عطلة الأسبوع (سبت وأحد)" else "Weekend (Sat & Sun)"
        daysSet.size == 2 -> {
            val dayNames = daysSet.sorted().map { dayIntToShort(it, isArabic) }
            if (isArabic) "يومان: ${dayNames.joinToString(", ")}" else "2 days: ${dayNames.joinToString(", ")}"
        }
        daysSet.size == 1 -> {
            val dayName = dayIntToShort(daysSet.first(), isArabic)
            if (isArabic) "يوم $dayName فقط" else "$dayName only"
        }
        else -> {
            val dayNames = daysSet.sorted().map { dayIntToShort(it, isArabic) }
            dayNames.joinToString(", ")
        }
    }
}

private fun dayIntToShort(day: Int, isArabic: Boolean): String {
    return when (day) {
        1 -> if (isArabic) "إثن" else "Mon"
        2 -> if (isArabic) "ثلا" else "Tue"
        3 -> if (isArabic) "أرب" else "Wed"
        4 -> if (isArabic) "خمي" else "Thu"
        5 -> if (isArabic) "جمع" else "Fri"
        6 -> if (isArabic) "سبت" else "Sat"
        7 -> if (isArabic) "أحد" else "Sun"
        else -> "$day"
    }
}
