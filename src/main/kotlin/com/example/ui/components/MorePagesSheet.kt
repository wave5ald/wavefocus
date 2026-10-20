package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import com.example.ui.viewmodel.AppEdition
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.LocalAppLanguage
import com.example.util.appStrings
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.HabitRose
import com.example.ui.theme.MindTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.CustomPageItem
import com.example.ui.viewmodel.NavTab
import com.example.ui.viewmodel.RoutineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorePagesSheet(
    viewModel: RoutineViewModel,
    currentTab: NavTab,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val customPages by viewModel.customPages.collectAsStateWithLifecycle()
    val activeCustomPage by viewModel.activeCustomPage.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("more_pages_modal_sheet")
    ) {
        val isArabic = LocalAppLanguage.current == "ar"
        val strings = appStrings()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isArabic) "مساحات العمل والمزيد" else "Workspaces & More",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isArabic) "مساحات العمل المخصصة والإحصائيات والأدوات" else "Custom workspaces, analytics & app tools",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_more_pages_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isArabic) "إغلاق" else "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isArabic) "صفحات ومساحات عمل إضافية" else "Additional Pages & Workspaces",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Only list pages that are NOT in the bottom navigation bar ("down")
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Dynamically created custom pages
                if (customPages.isNotEmpty()) {
                    customPages.forEach { cPage ->
                        PageItemRow(
                            title = cPage.title,
                            subtitle = cPage.description.ifBlank { if (isArabic) "${cPage.items.size} أهداف / عناصر" else "${cPage.items.size} goals / targets" },
                            icon = Icons.Default.Bookmark,
                            iconTint = FocusIndigo,
                            isSelected = currentTab == NavTab.CUSTOM_PAGE && activeCustomPage?.id == cPage.id,
                            testTag = "custom_page_item_${cPage.id}",
                            onClick = {
                                viewModel.openCustomPage(cPage)
                                onDismiss()
                            }
                        )
                    }
                }

                // Progress & Analytics (Secondary page accessed via More)
                PageItemRow(
                    title = if (isArabic) "التقدم والإحصائيات" else "Progress & History",
                    subtitle = if (isArabic) "معدلات الإنجاز والإحصائيات والتحليلات الأسبوعية" else "Completion trends, weekly analytics & rewards",
                    icon = Icons.Default.BarChart,
                    iconTint = FocusIndigo,
                    isSelected = currentTab == NavTab.ANALYTICS,
                    testTag = "page_item_analytics",
                    onClick = {
                        viewModel.setTab(NavTab.ANALYTICS)
                        onDismiss()
                    }
                )

                // Language Switcher Row
                PageItemRow(
                    title = if (isArabic) "لغة التطبيق (العربية)" else "App Language (English)",
                    subtitle = if (isArabic) "اضغط للتبديل الفوري إلى الإنجليزية" else "Tap to switch instantly to العربية (Arabic)",
                    icon = Icons.Default.Language,
                    iconTint = MindTeal,
                    isSelected = false,
                    testTag = "page_item_language",
                    onClick = {
                        viewModel.toggleLanguage()
                        onDismiss()
                    }
                )

                // App Edition Switcher (Islamic Hijri Version vs Standard)
                val appEdition by viewModel.appEdition.collectAsStateWithLifecycle()
                PageItemRow(
                    title = if (appEdition == AppEdition.ISLAMIC) {
                        if (isArabic) "النسخة الإسلامية (التقويم الهجري 🌙)" else "Islamic Edition (Hijri Calendar 🌙)"
                    } else {
                        if (isArabic) "النسخة العامة (التقويم الميلادي ⚡)" else "Standard Edition (Gregorian Calendar ⚡)"
                    },
                    subtitle = if (appEdition == AppEdition.ISLAMIC) {
                        if (isArabic) "اضغط للعودة إلى النسخة العادية للروتين والمهام" else "Tap to switch back to Standard Edition"
                    } else {
                        if (isArabic) "اضغط لفتح النسخة الإسلامية ببيانات منفصلة وتقويم هجري" else "Tap to open Islamic Edition with separate data & Hijri calendar"
                    },
                    icon = Icons.Default.Mosque,
                    iconTint = if (appEdition == AppEdition.ISLAMIC) Color(0xFF0F766E) else FocusIndigo,
                    isSelected = appEdition == AppEdition.ISLAMIC,
                    testTag = "page_item_edition",
                    onClick = {
                        viewModel.toggleAppEdition()
                        onDismiss()
                    }
                )

                // Settings & Backup
                PageItemRow(
                    title = if (isArabic) "الإعدادات واللغة والنسخ" else "Settings & Backup",
                    subtitle = if (isArabic) "لغة التطبيق، المظهر، النسخ الاحتياطي المشفر" else "Language, themes, encrypted local backup & prefs",
                    icon = Icons.Default.Settings,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    isSelected = false,
                    testTag = "page_item_settings",
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    }
                )
            }
        }
    }
}


@Composable
private fun PageItemRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
