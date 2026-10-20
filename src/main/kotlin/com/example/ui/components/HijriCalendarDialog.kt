package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.HijriCalendarHelper
import com.example.util.LocalAppLanguage
import java.time.DayOfWeek
import java.time.LocalDate

private val IslamicEmerald = Color(0xFF0F766E)
private val IslamicGold = Color(0xFFD97706)
private val IslamicGreenLight = Color(0xFFCCFBF1)

@Composable
fun HijriCalendarDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val isArabic = LocalAppLanguage.current == "ar"

    // View state tracks current browsing Hijri year and month
    val initialHijriYear = remember(selectedDate) { HijriCalendarHelper.getHijriYear(selectedDate) }
    val initialHijriMonth = remember(selectedDate) { HijriCalendarHelper.getHijriMonth(selectedDate) }

    var browsingYear by remember { mutableIntStateOf(initialHijriYear) }
    var browsingMonth by remember { mutableIntStateOf(initialHijriMonth) }

    val monthName = HijriCalendarHelper.getHijriMonthName(browsingMonth, isArabic)
    val yearDisplay = if (isArabic) HijriCalendarHelper.toArabicDigits(browsingYear) + " هـ" else "$browsingYear AH"

    val daysInMonth = remember(browsingYear, browsingMonth) {
        HijriCalendarHelper.getHijriMonthLength(browsingYear, browsingMonth)
    }

    // First day of this Hijri month in Gregorian
    val firstDayGregorian = remember(browsingYear, browsingMonth) {
        HijriCalendarHelper.hijriToLocalDate(browsingYear, browsingMonth, 1)
    }

    // Days of week header (starting with Sunday in Islamic tradition: الأحد .. السبت)
    val dayOfWeekOrder = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    )

    // Offset of the first day relative to Sunday
    val firstDayOfWeek = firstDayGregorian.dayOfWeek
    val leadingEmptyCells = dayOfWeekOrder.indexOf(firstDayOfWeek).coerceAtLeast(0)

    val today = remember { LocalDate.now() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties()
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp)
                .testTag("hijri_calendar_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top Header: Title & Close
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
                                .background(IslamicEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mosque,
                                contentDescription = null,
                                tint = IslamicEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isArabic) "التقويم الهجري الإسلامي" else "Islamic Hijri Calendar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isArabic) "تقويم أم القرى المبارك" else "Umm al-Qura Calendar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_hijri_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Month Navigator: [<] [Month Name Year] [>] + [Today Button]
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                if (browsingMonth == 1) {
                                    browsingMonth = 12
                                    browsingYear -= 1
                                } else {
                                    browsingMonth -= 1
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isArabic) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Month"
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$monthName $yearDisplay",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IslamicEmerald
                            )
                            Text(
                                text = "${firstDayGregorian.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${firstDayGregorian.year}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                if (browsingMonth == 12) {
                                    browsingMonth = 1
                                    browsingYear += 1
                                } else {
                                    browsingMonth += 1
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isArabic) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Month"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Day of Week Headers (Sun to Sat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayOfWeekOrder.forEach { dayOfWeek ->
                        val isFriday = dayOfWeek == DayOfWeek.FRIDAY
                        Text(
                            text = HijriCalendarHelper.getLocalizedDayOfWeekShort(dayOfWeek, isArabic),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = if (isFriday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isFriday) IslamicEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Month Grid (Empty cells + Day cells)
                val totalCells = leadingEmptyCells + daysInMonth
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Empty leading cells
                    items(leadingEmptyCells) {
                        Box(modifier = Modifier.aspectRatio(1f))
                    }

                    // Actual Hijri days
                    items(daysInMonth) { index ->
                        val dayNumber = index + 1
                        val correspondingDate = remember(browsingYear, browsingMonth, dayNumber) {
                            HijriCalendarHelper.hijriToLocalDate(browsingYear, browsingMonth, dayNumber)
                        }

                        val isSelected = correspondingDate == selectedDate
                        val isCurrentDay = correspondingDate == today
                        val isFriday = correspondingDate.dayOfWeek == DayOfWeek.FRIDAY
                        val occasion = HijriCalendarHelper.getSpecialIslamicOccasion(correspondingDate, isArabic)

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isSelected -> IslamicEmerald
                                        isCurrentDay -> IslamicEmerald.copy(alpha = 0.15f)
                                        occasion != null -> IslamicGold.copy(alpha = 0.15f)
                                        else -> Color.Transparent
                                    }
                                )
                                .then(
                                    if (isCurrentDay && !isSelected) {
                                        Modifier.border(1.dp, IslamicEmerald, RoundedCornerShape(10.dp))
                                    } else Modifier
                                )
                                .clickable {
                                    onDateSelected(correspondingDate)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isArabic) HijriCalendarHelper.toArabicDigits(dayNumber) else dayNumber.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected || isCurrentDay || isFriday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> Color.White
                                        isCurrentDay -> IslamicEmerald
                                        isFriday -> IslamicEmerald
                                        occasion != null -> IslamicGold
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )

                                Text(
                                    text = correspondingDate.dayOfMonth.toString(),
                                    fontSize = 9.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )

                                if (occasion != null && !isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(IslamicGold)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom bar: Jump to Today button & Current Date info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            browsingYear = HijriCalendarHelper.getHijriYear(today)
                            browsingMonth = HijriCalendarHelper.getHijriMonth(today)
                            onDateSelected(today)
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicEmerald)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "اليوم الهجري" else "Today (Hijri)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IslamicEmerald
                        )
                    }

                    Text(
                        text = HijriCalendarHelper.formatShortHijri(selectedDate, isArabic),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
