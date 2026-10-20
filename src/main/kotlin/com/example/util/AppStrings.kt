package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import java.time.DayOfWeek
import java.time.LocalDate

val LocalAppLanguage = compositionLocalOf { "en" }

@Composable
fun appStrings(): AppStringValues {
    val lang = LocalAppLanguage.current
    return if (lang == "ar") ArabicStrings else EnglishStrings
}

fun getAppStrings(lang: String): AppStringValues {
    return if (lang == "ar") ArabicStrings else EnglishStrings
}

interface AppStringValues {
    val appName: String
    val routine: String
    val focus: String
    val schedule: String
    val notes: String
    val analytics: String
    val progress: String
    val workspace: String
    val morePages: String
    val settings: String
    val language: String
    val appLanguageTitle: String
    val english: String
    val arabic: String
    val theme: String
    val lightMode: String
    val darkMode: String
    val systemDefault: String
    val today: String
    val all: String
    val search: String
    val save: String
    val cancel: String
    val delete: String
    val done: String
    val edit: String
    val clear: String

    // Routine Screen
    val dailyRoutines: String
    val dailyProgress: String
    val completed: String
    val addTask: String
    val taskForDay: String
    val newRoutineTask: String
    val editRoutineTask: String
    val routineTaskName: String
    val routineTaskPlaceholder: String
    val timeOfDay: String
    val morning: String
    val afternoon: String
    val evening: String
    val anytime: String
    val scheduleAndRecurrence: String
    val specificDay: String
    val weeklyDays: String
    val every23Days: String
    val weekendOnce: String
    val category: String
    val newCategory: String
    val categoryName: String
    val saveRoutine: String
    val updateRoutine: String
    val noTasksToday: String
    val selectAll: String
    val selectedCount: String
    val clearDayTasks: String
    val clearDayTasksTitle: String
    val clearDayTasksMessage: String
    val removeAll: String
    val prevDay: String
    val nextDay: String
    val editCategory: String

    // Focus Screen
    val focusTimer: String
    val pomodoro: String
    val stopwatch: String
    val countdown: String
    val start: String
    val pause: String
    val resume: String
    val reset: String
    val linkedTask: String
    val soundEffects: String
    val setFocusDuration: String
    val setTime: String
    val todaySessions: String
    val totalFocusTime: String
    val linkRoutineTask: String
    val close: String
    val adjustMinutes: String

    // Schedule Screen
    val setQuest: String
    val note: String
    val addNote: String
    val quests: String
    val scheduledRoutinesHeader: String
    val dayNotesHeader: String
    val questTitle: String
    val questType: String
    val customEmoji: String
    val targetMetric: String
    val detailsSteps: String
    val saveQuest: String
    val updateQuest: String
    val saveNote: String
    val updateNote: String
    val dailyNote: String
    val setQuestNow: String
    val setQuestForDay: String
    val planForDate: String
    val questOrGoalMode: String
    val noQuestsForDate: String
    val editEntry: String

    // Notes Screen
    val notesAndBoards: String
    val searchNotesPlaceholder: String
    val newNote: String
    val pinned: String
    val checklist: String
    val bullet: String
    val time: String
    val noteTitlePlaceholder: String
    val noteContentPlaceholder: String
    val unsavedNoteTitle: String
    val unsavedNoteMessage: String
    val discard: String

    // Settings
    val settingsAndTools: String
    val appearance: String
    val themeAndDisplay: String
    val focusAndBackground: String
    val batteryOptimization: String
    val dataAndBackup: String
    val fullBackup: String
    val tasksPack: String

    // Progress & History
    val completions: String
    val successRate: String
    val daysTracked: String
    val weeklyTrends: String

    // Routine Presets, Forms & Scheduling
    val routinePresets: String
    val routinePresetsSubtitle: String
    val presets: String
    val presetTasks: String
    val activatePreset: String
    val activateNow: String
    val schedulePreset: String
    val saveCurrentAsPreset: String
    val newPreset: String
    val editPreset: String
    val presetName: String
    val presetDescription: String
    val replaceTasks: String
    val appendTasks: String
    val scheduleDate: String
    val scheduledPresetsTitle: String
    val noScheduledPresets: String
    val applyOnDate: String
    val upcoming: String
    val activeToday: String
    val builtInPreset: String
    val customPreset: String
    val mainCategoryCannotDelete: String
    val builtInCategories: String
    val customCategories: String
    val addTaskToPreset: String
    val selectStartDate: String
    val applyScheduleNow: String
    val cancelSchedule: String
    val scheduledFor: String
    val replaceOptionDesc: String
    val appendOptionDesc: String
    val presetSavedSuccess: String
    val presetActivatedSuccess: String
    val exportImportPresets: String
    val exportPresetTab: String
    val importPresetTab: String
    val exportPresetAsFile: String
    val sharePresetFile: String
    val sharePresetText: String
    val importFromFile: String
    val importFromText: String
    val pastePresetJson: String
    val noPresetsYet: String
    val everyDayLabel: String
    val selectDaysLabel: String
    val timeOfDayLabel: String
    val timingRecurrence: String
    val presetExportedSuccess: String
    val presetImportedSuccess: String
    val islamicEdition: String
    val standardEdition: String
    val hijriCalendar: String
    val switchToIslamicEdition: String
    val switchToStandardEdition: String
    val islamicEditionActive: String
    val islamicModeTag: String
    val openHijriCalendar: String
}

object EnglishStrings : AppStringValues {
    override val appName = "Wave Focus"
    override val routine = "Routine"
    override val focus = "Focus"
    override val schedule = "Schedule"
    override val notes = "Notes"
    override val analytics = "Progress"
    override val progress = "Progress"
    override val workspace = "Workspace"
    override val morePages = "More Pages"
    override val settings = "Settings & Backup"
    override val language = "Language"
    override val appLanguageTitle = "App Language"
    override val english = "English"
    override val arabic = "العربية (Arabic)"
    override val theme = "Theme"
    override val lightMode = "Light Mode"
    override val darkMode = "Dark Mode"
    override val systemDefault = "System Default"
    override val today = "Today"
    override val all = "All"
    override val search = "Search..."
    override val save = "Save"
    override val cancel = "Cancel"
    override val delete = "Delete"
    override val done = "Done"
    override val edit = "Edit"
    override val clear = "Clear"

    override val dailyRoutines = "Daily Routines"
    override val dailyProgress = "Daily Progress"
    override val completed = "Completed"
    override val addTask = "Add Task"
    override val taskForDay = "+ Task for Day"
    override val newRoutineTask = "New Routine Task"
    override val editRoutineTask = "Edit Routine Task"
    override val routineTaskName = "Routine Task Name"
    override val routineTaskPlaceholder = "e.g. Read 20 pages or Workout"
    override val timeOfDay = "Time of Day"
    override val morning = "Morning"
    override val afternoon = "Afternoon"
    override val evening = "Evening"
    override val anytime = "Anytime"
    override val scheduleAndRecurrence = "Schedule & Recurrence"
    override val specificDay = "📅 Specific Day"
    override val weeklyDays = "Weekly Days"
    override val every23Days = "Every 2, 3... Days"
    override val weekendOnce = "1 Weekend Day"
    override val category = "Category"
    override val newCategory = "New Category"
    override val categoryName = "Category Name"
    override val saveRoutine = "Save Routine"
    override val updateRoutine = "Update Routine"
    override val noTasksToday = "No routine tasks scheduled for this day."
    override val selectAll = "Select All"
    override val selectedCount = "Selected"
    override val clearDayTasks = "Clear Tasks"
    override val clearDayTasksTitle = "Clear All Tasks for this day?"
    override val clearDayTasksMessage = "This will remove all tasks scheduled for this day. This action cannot be undone."
    override val removeAll = "Remove All"
    override val prevDay = "Previous Day"
    override val nextDay = "Next Day"
    override val editCategory = "Edit Category"

    override val focusTimer = "Focus Timer"
    override val pomodoro = "Pomodoro"
    override val stopwatch = "Stopwatch"
    override val countdown = "Countdown"
    override val start = "Start"
    override val pause = "Pause"
    override val resume = "Resume"
    override val reset = "Reset"
    override val linkedTask = "Linked Routine Task"
    override val soundEffects = "Subtle Sound Effects"
    override val setFocusDuration = "Set Focus Duration"
    override val setTime = "Set Time"
    override val todaySessions = "Today's Focus Sessions"
    override val totalFocusTime = "Total Focus Time"
    override val linkRoutineTask = "Link Routine Task"
    override val close = "Close"
    override val adjustMinutes = "Adjust minutes for your focus session:"

    override val setQuest = "Set Quest"
    override val note = "Note"
    override val addNote = "+ Note"
    override val quests = "Quests"
    override val scheduledRoutinesHeader = "⏱️ Scheduled Routines"
    override val dayNotesHeader = "📝 Day Notes & Reflections"
    override val questTitle = "Quest Title *"
    override val questType = "Quest Type"
    override val customEmoji = "Custom Emoji"
    override val targetMetric = "Target / Metric (Optional)"
    override val detailsSteps = "Details & Action Steps (Optional)"
    override val saveQuest = "Save Quest"
    override val updateQuest = "Update Quest"
    override val saveNote = "Save Note"
    override val updateNote = "Update Note"
    override val dailyNote = "Daily Note"
    override val setQuestNow = "Set a Quest Now 🎯"
    override val setQuestForDay = "Set a Quest for This Day"
    override val planForDate = "Plan for"
    override val questOrGoalMode = "🎯 Goal / Quest"
    override val noQuestsForDate = "No quests or goals set for this date."
    override val editEntry = "Edit Entry"

    override val notesAndBoards = "Notes & Notebooks"
    override val searchNotesPlaceholder = "Search notes..."
    override val newNote = "New Note"
    override val pinned = "Pinned"
    override val checklist = "Checklist"
    override val bullet = "Bullet"
    override val time = "Time"
    override val noteTitlePlaceholder = "Title"
    override val noteContentPlaceholder = "Write your thoughts or checklist..."
    override val unsavedNoteTitle = "Save Note Changes?"
    override val unsavedNoteMessage = "You have unsaved changes in this note. Would you like to save them before leaving, or discard?"
    override val discard = "Discard"

    override val settingsAndTools = "Settings & Tools"
    override val appearance = "APPEARANCE"
    override val themeAndDisplay = "Theme & Display"
    override val focusAndBackground = "FOCUS & BACKGROUND EXECUTION"
    override val batteryOptimization = "Unrestricted Battery Permission"
    override val dataAndBackup = "DATA & BACKUP"
    override val fullBackup = "Full App Backup & Restore"
    override val tasksPack = "Routine Tasks Pack Export/Import"

    override val completions = "Completions"
    override val successRate = "Success Rate"
    override val daysTracked = "Days Tracked"
    override val weeklyTrends = "Weekly Trends"

    override val routinePresets = "Routine Presets & Forms"
    override val routinePresetsSubtitle = "Switch task presets or schedule future routine changes"
    override val presets = "Presets"
    override val presetTasks = "Preset Tasks"
    override val activatePreset = "Activate Preset"
    override val activateNow = "Activate Now"
    override val schedulePreset = "Schedule for Date"
    override val saveCurrentAsPreset = "Save Current Tasks as Preset"
    override val newPreset = "New Preset Form"
    override val editPreset = "Edit Preset"
    override val presetName = "Preset Name"
    override val presetDescription = "Preset Description"
    override val replaceTasks = "Replace all active tasks"
    override val appendTasks = "Add alongside existing tasks"
    override val scheduleDate = "Schedule Start Date"
    override val scheduledPresetsTitle = "Scheduled Routine Switches"
    override val noScheduledPresets = "No routine presets scheduled yet."
    override val applyOnDate = "Apply on this date"
    override val upcoming = "Upcoming"
    override val activeToday = "Active Today"
    override val builtInPreset = "Curated Preset"
    override val customPreset = "Custom Preset"
    override val mainCategoryCannotDelete = "Main categories are built into the app and automatically translate."
    override val builtInCategories = "Main Built-in Categories"
    override val customCategories = "Custom Categories"
    override val addTaskToPreset = "Add Task to Preset"
    override val selectStartDate = "Select start date for this routine preset:"
    override val applyScheduleNow = "Apply Now"
    override val cancelSchedule = "Cancel Schedule"
    override val scheduledFor = "Scheduled for"
    override val replaceOptionDesc = "Archives/replaces existing active routine tasks with this preset's tasks"
    override val appendOptionDesc = "Keeps your current tasks and appends this preset's tasks"
    override val presetSavedSuccess = "Routine preset saved successfully"
    override val presetActivatedSuccess = "Routine preset activated successfully"
    override val exportImportPresets = "Export & Import"
    override val exportPresetTab = "Export & Share"
    override val importPresetTab = "Import Preset"
    override val exportPresetAsFile = "Export as File (.json)"
    override val sharePresetFile = "Share Preset File"
    override val sharePresetText = "Share Text / Copy"
    override val importFromFile = "Choose File from Device"
    override val importFromText = "Load from JSON Text"
    override val pastePresetJson = "Paste Preset JSON Code"
    override val noPresetsYet = "No presets yet. Create a preset or save your current routine to get started."
    override val everyDayLabel = "Everyday"
    override val selectDaysLabel = "Specific Days"
    override val timeOfDayLabel = "When It Happens"
    override val timingRecurrence = "Timing & Recurrence"
    override val presetExportedSuccess = "Preset exported successfully"
    override val presetImportedSuccess = "Preset imported successfully"
    override val islamicEdition = "Islamic Edition"
    override val standardEdition = "Standard Edition"
    override val hijriCalendar = "Hijri Calendar"
    override val switchToIslamicEdition = "Switch to Islamic Edition (Hijri)"
    override val switchToStandardEdition = "Switch to Standard Edition"
    override val islamicEditionActive = "Islamic Edition Active • Hijri Calendar"
    override val islamicModeTag = "Hijri"
    override val openHijriCalendar = "Open Hijri Calendar"
}

object ArabicStrings : AppStringValues {
    override val appName = "ويف فوكس"
    override val routine = "الروتين"
    override val focus = "التركيز"
    override val schedule = "الجدول"
    override val notes = "الملاحظات"
    override val analytics = "الإحصائيات"
    override val progress = "التقدم"
    override val workspace = "مساحة العمل"
    override val morePages = "المزيد من الصفحات"
    override val settings = "الإعدادات والنسخ"
    override val language = "اللغة"
    override val appLanguageTitle = "لغة التطبيق"
    override val english = "English (الإنجليزية)"
    override val arabic = "العربية"
    override val theme = "المظهر"
    override val lightMode = "الوضع الفاتح"
    override val darkMode = "الوضع الداكن"
    override val systemDefault = "افتراضي النظام"
    override val today = "اليوم"
    override val all = "الكل"
    override val search = "بحث..."
    override val save = "حفظ"
    override val cancel = "إلغاء"
    override val delete = "حذف"
    override val done = "تم"
    override val edit = "تعديل"
    override val clear = "مسح"

    override val dailyRoutines = "الروتينات اليومية"
    override val dailyProgress = "التقدم اليومي"
    override val completed = "مكتمل"
    override val addTask = "إضافة مهمة"
    override val taskForDay = "+ مهمة لليوم"
    override val newRoutineTask = "مهمة روتين جديدة"
    override val editRoutineTask = "تعديل مهمة الروتين"
    override val routineTaskName = "اسم مهمة الروتين"
    override val routineTaskPlaceholder = "مثال: قراءة 20 صفحة أو تمرين"
    override val timeOfDay = "وقت اليوم"
    override val morning = "الصباح"
    override val afternoon = "بعد الظهر"
    override val evening = "المساء"
    override val anytime = "أي وقت"
    override val scheduleAndRecurrence = "الجدول والتكرار"
    override val specificDay = "📅 يوم محدد"
    override val weeklyDays = "أيام أسبوعية"
    override val every23Days = "كل ٢، ٣... أيام"
    override val weekendOnce = "يوم في عطلة الأسبوع"
    override val category = "التصنيف"
    override val newCategory = "تصنيف جديد"
    override val categoryName = "اسم التصنيف"
    override val saveRoutine = "حفظ الروتين"
    override val updateRoutine = "تحديث الروتين"
    override val noTasksToday = "لا توجد مهام روتينية مجدولة لهذا اليوم."
    override val selectAll = "تحديد الكل"
    override val selectedCount = "محدد"
    override val clearDayTasks = "مسح المهام"
    override val clearDayTasksTitle = "مسح جميع المهام لهذا اليوم؟"
    override val clearDayTasksMessage = "سيؤدي هذا إلى إزالة جميع المهام المجدولة لهذا اليوم. لا يمكن التراجع عن هذا الإجراء."
    override val removeAll = "إزالة الكل"
    override val prevDay = "اليوم السابق"
    override val nextDay = "اليوم التالي"
    override val editCategory = "تعديل التصنيف"

    override val focusTimer = "مؤقت التركيز"
    override val pomodoro = "بومودورو"
    override val stopwatch = "ساعة إيقاف"
    override val countdown = "العد التنازلي"
    override val start = "بدء"
    override val pause = "إيقاف مؤقت"
    override val resume = "استئناف"
    override val reset = "إعادة ضبط"
    override val linkedTask = "المهمة المرتبطة"
    override val soundEffects = "المؤثرات الصوتية"
    override val setFocusDuration = "تحديد مدة التركيز"
    override val setTime = "ضبط الوقت"
    override val todaySessions = "جلسات التركيز اليوم"
    override val totalFocusTime = "إجمالي وقت التركيز"
    override val linkRoutineTask = "ربط مهمة روتينية"
    override val close = "إغلاق"
    override val adjustMinutes = "حدد الدقائق لجلسة التركيز الخاصة بك:"

    override val setQuest = "تحديد مهمة"
    override val note = "ملاحظة"
    override val addNote = "+ ملاحظة"
    override val quests = "المهمات"
    override val scheduledRoutinesHeader = "⏱️ الروتينات المجدولة"
    override val dayNotesHeader = "📝 ملاحظات وأفكار اليوم"
    override val questTitle = "عنوان المهمة *"
    override val questType = "نوع المهمة"
    override val customEmoji = "رمز تعبيري مخصص"
    override val targetMetric = "الهدف / المقياس (اختياري)"
    override val detailsSteps = "التفاصيل والخطوات (اختياري)"
    override val saveQuest = "حفظ المهمة"
    override val updateQuest = "تحديث المهمة"
    override val saveNote = "حفظ الملاحظة"
    override val updateNote = "تحديث الملاحظة"
    override val dailyNote = "ملاحظة يومية"
    override val setQuestNow = "حدد مهمة الآن 🎯"
    override val setQuestForDay = "تحديد مهمة لهذا اليوم"
    override val planForDate = "خطة يوم"
    override val questOrGoalMode = "🎯 الهدف / المهمة"
    override val noQuestsForDate = "لا توجد مهمات أو أهداف محددة لهذا التاريخ."
    override val editEntry = "تعديل العنصر"

    override val notesAndBoards = "الملاحظات والدفاتر"
    override val searchNotesPlaceholder = "البحث في الملاحظات..."
    override val newNote = "ملاحظة جديدة"
    override val pinned = "المثبتة"
    override val checklist = "قائمة مهام"
    override val bullet = "نقطة"
    override val time = "الوقت"
    override val noteTitlePlaceholder = "العنوان"
    override val noteContentPlaceholder = "اكتب أفكارك أو قائمة المهام هنا..."
    override val unsavedNoteTitle = "حفظ تغييرات الملاحظة؟"
    override val unsavedNoteMessage = "لديك تغييرات غير محفوظة في هذه الملاحظة. هل ترغب في حفظها قبل المغادرة، أو تجاهلها؟"
    override val discard = "تجاهل"

    override val settingsAndTools = "الإعدادات والأدوات"
    override val appearance = "المظهر والشاشة"
    override val themeAndDisplay = "المظهر والعرض"
    override val focusAndBackground = "التركيز والعمل في الخلفية"
    override val batteryOptimization = "إذن بطارية غير مقيد"
    override val dataAndBackup = "البيانات والنسخ الاحتياطي"
    override val fullBackup = "نسخ احتياطي واستعادة كاملة"
    override val tasksPack = "تصدير / استيراد حزمة الروتينات"

    override val completions = "المهام المكتملة"
    override val successRate = "معدل النجاح"
    override val daysTracked = "الأيام المسجلة"
    override val weeklyTrends = "الاتجاهات الأسبوعية"

    override val routinePresets = "قوالب ونماذج الروتين"
    override val routinePresetsSubtitle = "تبديل حزم الروتين أو جدولة روتين لتاريخ قادم"
    override val presets = "القوالب"
    override val presetTasks = "مهام القالب"
    override val activatePreset = "تفعيل القالب"
    override val activateNow = "تفعيل الآن"
    override val schedulePreset = "جدولة لتاريخ"
    override val saveCurrentAsPreset = "حفظ الروتين الحالي كقالب"
    override val newPreset = "قالب روتين جديد"
    override val editPreset = "تعديل القالب"
    override val presetName = "اسم القالب"
    override val presetDescription = "وصف القالب"
    override val replaceTasks = "استبدال جميع المهام الحالية"
    override val appendTasks = "إضافة بجانب المهام الحالية"
    override val scheduleDate = "تاريخ بدء الروتين"
    override val scheduledPresetsTitle = "الروتينات المجدولة للبدء"
    override val noScheduledPresets = "لا توجد قوالب روتين مجدولة بعد."
    override val applyOnDate = "تطبيق في هذا التاريخ"
    override val upcoming = "قادم"
    override val activeToday = "نشط اليوم"
    override val builtInPreset = "قالب جاهز"
    override val customPreset = "قالب مخصص"
    override val mainCategoryCannotDelete = "التصنيفات الأساسية مدمجة مع التطبيق وتُترجم تلقائياً ولا يمكن حذفها."
    override val builtInCategories = "التصنيفات الأساسية المدمجة"
    override val customCategories = "التصنيفات المخصصة"
    override val addTaskToPreset = "إضافة مهمة إلى القالب"
    override val selectStartDate = "حدد تاريخ بدء تفعيل هذا القالب الروتيني:"
    override val applyScheduleNow = "تفعيل الآن"
    override val cancelSchedule = "إلغاء الجدولة"
    override val scheduledFor = "مجدول لتاريخ"
    override val replaceOptionDesc = "يستبدل مهام الروتين النشطة الحالية بالمهام المحددة في هذا القالب"
    override val appendOptionDesc = "يحافظ على مهامك الحالية ويضيف مهام هذا القالب إليها"
    override val presetSavedSuccess = "تم حفظ قالب الروتين بنجاح"
    override val presetActivatedSuccess = "تم تفعيل قالب الروتين بنجاح"
    override val exportImportPresets = "تصدير واستيراد"
    override val exportPresetTab = "تصدير ومشاركة"
    override val importPresetTab = "استيراد قالب"
    override val exportPresetAsFile = "تصدير كملف (.json)"
    override val sharePresetFile = "مشاركة ملف القالب"
    override val sharePresetText = "مشاركة كنص / نسخ"
    override val importFromFile = "اختيار ملف من الجهاز"
    override val importFromText = "استيراد من نص JSON"
    override val pastePresetJson = "الصق كود JSON للقالب هنا"
    override val noPresetsYet = "لا توجد أي قوالب بعد. أنشئ قالباً أو احفظ روتينك الحالي للبدء."
    override val everyDayLabel = "كل يوم"
    override val selectDaysLabel = "أيام محددة"
    override val timeOfDayLabel = "وقت التنفيذ"
    override val timingRecurrence = "التوقيت والتكرار"
    override val presetExportedSuccess = "تم تصدير القالب بنجاح"
    override val presetImportedSuccess = "تم استيراد القالب بنجاح"
    override val islamicEdition = "النسخة الإسلامية"
    override val standardEdition = "النسخة العامة"
    override val hijriCalendar = "التقويم الهجري"
    override val switchToIslamicEdition = "التبديل إلى النسخة الإسلامية (هجري)"
    override val switchToStandardEdition = "العودة إلى النسخة العادية"
    override val islamicEditionActive = "النسخة الإسلامية مفعّلة • التقويم الهجري"
    override val islamicModeTag = "هجري"
    override val openHijriCalendar = "فتح التقويم الهجري"
}

fun formatLocalizedDate(date: LocalDate, isArabic: Boolean): String {
    if (!isArabic) {
        return date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d"))
    }
    val dayName = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "الإثنين"
        DayOfWeek.TUESDAY -> "الثلاثاء"
        DayOfWeek.WEDNESDAY -> "الأربعاء"
        DayOfWeek.THURSDAY -> "الخميس"
        DayOfWeek.FRIDAY -> "الجمعة"
        DayOfWeek.SATURDAY -> "السبت"
        DayOfWeek.SUNDAY -> "الأحد"
    }
    val monthName = when (date.monthValue) {
        1 -> "يناير"
        2 -> "فبراير"
        3 -> "مارس"
        4 -> "أبريل"
        5 -> "مايو"
        6 -> "يونيو"
        7 -> "يوليو"
        8 -> "أغسطس"
        9 -> "سبتمبر"
        10 -> "أكتوبر"
        11 -> "نوفمبر"
        12 -> "ديسمبر"
        else -> ""
    }
    return "$dayName، ${date.dayOfMonth} $monthName"
}

fun getLocalizedTimeSlot(slot: String, isArabic: Boolean): String {
    if (!isArabic) return slot
    return when (slot.lowercase()) {
        "morning" -> "الصباح"
        "afternoon" -> "بعد الظهر"
        "evening" -> "المساء"
        "anytime" -> "أي وقت"
        else -> slot
    }
}

fun getLocalizedDayShort(day: DayOfWeek, isArabic: Boolean): String {
    if (!isArabic) {
        return day.name.take(3)
    }
    return when (day) {
        DayOfWeek.MONDAY -> "إثن"
        DayOfWeek.TUESDAY -> "ثلا"
        DayOfWeek.WEDNESDAY -> "أرب"
        DayOfWeek.THURSDAY -> "خمي"
        DayOfWeek.FRIDAY -> "جمع"
        DayOfWeek.SATURDAY -> "سبت"
        DayOfWeek.SUNDAY -> "أحد"
    }
}
