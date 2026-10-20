package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DaySummary
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AnalyticsScreen(
    viewModel: RoutineViewModel,
    analyticsData: AnalyticsData,
    modifier: Modifier = Modifier
) {
    val isArabic = LocalAppLanguage.current == "ar"
    val strings = appStrings()
    var selectedChartRange by remember { mutableStateOf("7 Days") }

    val chartData = when (selectedChartRange) {
        "All Time" -> analyticsData.completionAllTime
        "30 Days" -> analyticsData.completionPast30Days
        else -> analyticsData.completionPast7Days
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp)
    ) {
        // Streak & Consistency Card
        item {
            StreakCard(
                currentStreak = analyticsData.currentStreak,
                bestStreak = analyticsData.bestStreak
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Progress Over Time Chart Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "تقدم الروتين" else "Routine Progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = when (selectedChartRange) {
                                "7 Days" -> if (isArabic) "آخر 7 أيام" else "Past 7 Days"
                                "30 Days" -> if (isArabic) "آخر 30 يوماً" else "Past 30 Days"
                                else -> if (isArabic) "كل الأوقات" else "All-Time"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Full-width segmented control with equal weight per button (perfect on all phones!)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("7 Days", "30 Days", "All Time").forEach { range ->
                                val isSelected = selectedChartRange == range
                                val displayRange = when (range) {
                                    "7 Days" -> if (isArabic) "7 أيام" else "7 Days"
                                    "30 Days" -> if (isArabic) "30 يوماً" else "30 Days"
                                    else -> if (isArabic) "الكل" else "All Time"
                                }
                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedChartRange = range }
                                        .testTag("chart_range_${range.lowercase().replace(" ", "_")}")
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = displayRange,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (selectedChartRange == "All Time") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Text(if (isArabic) "الإنجازات" else "Completions", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(if (isArabic) "${analyticsData.allTimeCompletedTasks} مهمة" else "${analyticsData.allTimeCompletedTasks} tasks", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Text(if (isArabic) "نسبة النجاح" else "Success Rate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${analyticsData.allTimeCompletionPercentage.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Text(if (isArabic) "الأيام المسجلة" else "Days Tracked", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(if (isArabic) "${chartData.size} يوم" else "${chartData.size} days", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FocusIndigo)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bar Chart
                    CompletionBarChart(days = chartData, range = selectedChartRange)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Focus & Category Stats Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Focus time summary card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = FocusIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Focus Time",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${analyticsData.totalFocusMinutesThisWeek}m",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "This week (${analyticsData.totalFocusMinutesAllTime}m total)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Completion consistency card
                val avgCompletion = if (chartData.isNotEmpty()) {
                    (chartData.map { it.completionPercentage }.average()).toInt()
                } else 0

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Avg Completion",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$avgCompletion%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Text(
                            text = "Across $selectedChartRange",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Category Breakdown Card
        if (analyticsData.categoryDistribution.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Category Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        val totalCompleted = analyticsData.categoryDistribution.values.sum().coerceAtLeast(1)
                        analyticsData.categoryDistribution.forEach { (cat, count) ->
                            val color = when (cat.lowercase()) {
                                "health" -> SuccessGreen
                                "focus" -> FocusIndigo
                                "mind" -> MindTeal
                                "habit" -> HabitRose
                                else -> FlameOrange
                            }
                            val fraction = count.toFloat() / totalCompleted.toFloat()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isArabic = LocalAppLanguage.current == "ar"
                                Text(
                                    text = CategoryLocalization.getLocalizedCategoryName(cat, isArabic),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.width(90.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeCap = StrokeCap.Round
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "$count done",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // History Log Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HISTORY LOG",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (analyticsData.pastDaysHistory.isEmpty()) {
            item {
                Text(
                    text = "No past history recorded yet. Complete today's routines to build your streak!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(analyticsData.pastDaysHistory, key = { it.epochDay }) { day ->
                HistoryDayCard(
                    day = day,
                    onClick = {
                        viewModel.selectDate(LocalDate.ofEpochDay(day.epochDay))
                        viewModel.setTab(NavTab.ROUTINE)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun StreakCard(
    currentStreak: Int,
    bestStreak: Int
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FlameOrange.copy(alpha = 0.12f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = FlameOrange,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$currentStreak Day Streak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (currentStreak > 0) "Keep the routine alive!" else "Complete a routine today!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, FlameOrange.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "BEST",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$bestStreak d",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun CompletionBarChart(days: List<DaySummary>, range: String = "7 Days") {
    if (days.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No routine activity logged yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    if (range == "7 Days") {
        val displayDays = days.takeLast(7)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            displayDays.forEach { day ->
                val date = LocalDate.ofEpochDay(day.epochDay)
                val dayLabel = date.format(DateTimeFormatter.ofPattern("E"))
                val heightFraction = (day.completionPercentage / 100f).coerceIn(0.06f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    // Percentage text
                    Text(
                        text = "${day.completionPercentage.toInt()}%",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bar
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height((100 * heightFraction).dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                if (day.completionPercentage >= 80f) SuccessGreen
                                else if (day.completionPercentage > 0f) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Day name
                    Text(
                        text = dayLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    } else {
        // Horizontally scrollable row for 30 Days and All Time
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = (days.size - 7).coerceAtLeast(0))
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            items(days) { day ->
                val date = LocalDate.ofEpochDay(day.epochDay)
                val dayLabel = if (range == "All Time") {
                    date.format(DateTimeFormatter.ofPattern("M/d"))
                } else {
                    date.format(DateTimeFormatter.ofPattern("d"))
                }
                val heightFraction = (day.completionPercentage / 100f).coerceIn(0.06f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.width(28.dp)
                ) {
                    Text(
                        text = "${day.completionPercentage.toInt()}%",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height((100 * heightFraction).dp)
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .background(
                                if (day.completionPercentage >= 80f) SuccessGreen
                                else if (day.completionPercentage > 0f) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = dayLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryDayCard(
    day: DaySummary,
    onClick: () -> Unit
) {
    val date = LocalDate.ofEpochDay(day.epochDay)
    val dateString = date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_item_${day.epochDay}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${day.completedTasks} / ${day.totalTasks} tasks done • ${day.focusMinutes}m focus",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (day.completionPercentage >= 80f) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "${day.completionPercentage.toInt()}%",
                    color = if (day.completionPercentage >= 80f) SuccessGreen else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
