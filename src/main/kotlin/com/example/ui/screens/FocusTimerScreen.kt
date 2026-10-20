package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FocusSession
import com.example.data.model.RoutineTask
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.RoutineViewModel
import com.example.ui.viewmodel.TimerMode
import com.example.util.BatteryOptimizationHelper
import com.example.util.LocalAppLanguage
import com.example.util.appStrings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun FocusTimerScreen(
    viewModel: RoutineViewModel,
    timerMode: TimerMode,
    totalSeconds: Int,
    remainingSeconds: Int,
    isRunning: Boolean,
    linkedTask: RoutineTask?,
    activeTasks: List<RoutineTask>,
    todaySessions: List<FocusSession>,
    modifier: Modifier = Modifier
) {
    val isArabic = LocalAppLanguage.current == "ar"
    val strings = appStrings()
    val context = LocalContext.current
    var showCustomDialog by remember { mutableStateOf(false) }
    var showTaskPicker by remember { mutableStateOf(false) }
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }

    var isIgnoringBatteryOptimizations by remember(context) {
        mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isIgnoringBatteryOptimizations = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
    }

    val postNotificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    LaunchedEffect(Unit) {
        viewModel.syncTimerWithBackgroundService()
    }

    val progressFraction = if (totalSeconds > 0) {
        remainingSeconds.toFloat() / totalSeconds.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "timerProgress")

    val totalMinutesSetting = totalSeconds / 60
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val totalMinutesToday = todaySessions.sumOf { it.durationSeconds } / 60

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Single Adjustable Category Header (removed all category clutter, only 1 adjustable focus target)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .clickable { showCustomDialog = true }
                    .testTag("single_focus_category_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "التركيز: $totalMinutesSetting دقيقة" else "Focus: $totalMinutesSetting mins",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = if (isArabic) "تعديل المدة" else "Edit duration",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Linked Task Indicator
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTaskPicker = true }
                    .testTag("link_task_button")
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
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = linkedTask?.let { if (isArabic) "التركيز على: ${it.title}" else "Focusing on: ${it.title}" }
                                ?: if (isArabic) "ربط بمهمة روتينية (اختياري)" else "Link to a Routine Task (optional)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (linkedTask != null) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }

                    if (linkedTask != null) {
                        IconButton(
                            onClick = { viewModel.setLinkedTask(null) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Unlink",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Large Circular Dial
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(250.dp)
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

                Canvas(modifier = Modifier.size(240.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    // Track circle
                    drawCircle(
                        color = outlineColor,
                        style = Stroke(width = strokeWidth)
                    )
                    // Animated progress arc
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRunning) (if (isArabic) "جلسة التركيز جارية" else "Focus Active") else (if (isArabic) "جاهز للتركيز" else "Ready to Focus"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isRunning) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Stepper Controls: Decrease & Increase buttons (Fixes: "we can only increase and we cant decrease fix this")
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // -5m Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clickable { viewModel.subtractFiveMinutes() }
                        .testTag("decrease_5m_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = if (isArabic) "إنقاص 5 دقائق" else "Decrease 5 mins",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (isArabic) "5 د" else "5m",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Reset Button
                IconButton(
                    onClick = { viewModel.resetTimer() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("reset_timer_button")
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = strings.reset,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Primary Start/Pause Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            if (isRunning) {
                                viewModel.pauseTimer()
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                                viewModel.startTimer()
                            }
                        }
                        .testTag("toggle_timer_button")
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) strings.pause else strings.start,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // +5m Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clickable { viewModel.addFiveMinutes() }
                        .testTag("increase_5m_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = if (isArabic) "زيادة 5 دقائق" else "Increase 5 mins",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (isArabic) "5 د" else "5m",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            if (!isIgnoringBatteryOptimizations) {
                Spacer(modifier = Modifier.height(14.dp))

                // Continuous Background Persistence & Battery Exemption Indicator (Only shown before enabling)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBatteryOptimizationDialog = true }
                        .testTag("battery_optimization_status_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = if (isArabic) "حماية التركيز أثناء إيقاف الشاشة" else "Screen-Off Focus: Tap to Protect",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isArabic) "استثناء من توفير البطارية لمنع إيقاف المؤقت" else "Grant battery exemption so phone doesn't stop the timer",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = if (isArabic) "تفعيل" else "Enable",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Today's Focus Summary Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isArabic) "وقت التركيز اليوم" else "Today's Focus Time",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isArabic) "$totalMinutesToday دقيقة • ${todaySessions.size} جلسات مكتملة" else "$totalMinutesToday minutes • ${todaySessions.size} sessions completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = FocusIndigo.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = FocusIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // List of Today's Focus Sessions
        if (todaySessions.isNotEmpty()) {
            items(todaySessions, key = { it.id }) { session ->
                val timeStr = Instant.ofEpochMilli(session.completedAt)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("h:mm a"))
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = session.taskTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${session.durationSeconds / 60}m ($timeStr)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Custom Duration Stepper Dialog
    if (showCustomDialog) {
        var customMinsInput by remember { mutableStateOf(totalMinutesSetting.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text(if (isArabic) "تحديد مدة التركيز" else "Set Focus Duration") },
            text = {
                Column {
                    Text(
                        text = if (isArabic) "اضبط عدد الدقائق لجلسة التركيز:" else "Adjust minutes for your focus session:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customMinsInput,
                        onValueChange = { if (it.all { char -> char.isDigit() }) customMinsInput = it },
                        singleLine = true,
                        trailingIcon = { Text(if (isArabic) "د" else "min", modifier = Modifier.padding(end = 12.dp)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_mins_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // Quick presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(15, 25, 30, 60, 120).forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { customMinsInput = preset.toString() }
                            ) {
                                Text(
                                    text = if (isArabic) "$preset د" else "${preset}m",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mins = customMinsInput.toIntOrNull()?.coerceIn(1, 360) ?: 25
                        viewModel.setTimerMinutes(mins)
                        showCustomDialog = false
                    },
                    modifier = Modifier.testTag("confirm_set_timer")
                ) {
                    Text(if (isArabic) "تعيين الوقت" else "Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Task Picker Dialog
    if (showTaskPicker) {
        AlertDialog(
            onDismissRequest = { showTaskPicker = false },
            title = { Text(if (isArabic) "ربط مهمة روتينية" else "Link Routine Task") },
            text = {
                LazyColumn {
                    items(activeTasks) { task ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (linkedTask?.id == task.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.setLinkedTask(task)
                                    showTaskPicker = false
                                }
                        ) {
                            Text(
                                text = task.title,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTaskPicker = false }) {
                    Text(if (isArabic) "إغلاق" else "Close")
                }
            }
        )
    }

    // Battery Optimization & Screen-Off Protection Dialog
    if (showBatteryOptimizationDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryOptimizationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (isArabic) "حماية إيقاف الشاشة والبطارية" else "Screen-Off & Battery Protection",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isArabic) "هل يعمل مؤقت التركيز حتى عند قفل الهاتف أو إيقاف تشغيل الشاشة؟" else "Does Focus work even if the phone is turned off or locked?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isArabic) "نعم! يستخدم ويف فوكس خدمة تعمل في الخلفية مع ميزة Wake Lock لضمان استمرار العد التنازلي دون انقطاع.\n\nلمنع نظام توفير الطاقة من إيقاف التطبيق، يرجى منح استثناء تحسين البطارية."
                            else "Yes! Wave Focus uses a foreground service with wake lock to keep your timer counting down without interruption.\n\nTo prevent aggressive battery management from stopping or pausing the app when the screen turns off, please grant battery optimization exemption.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatteryOptimizationDialog = false
                        BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                        isIgnoringBatteryOptimizations = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                    },
                    modifier = Modifier.testTag("confirm_grant_battery_exemption")
                ) {
                    Text(if (isArabic) "منح الاستثناء" else "Grant Exemption")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryOptimizationDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
