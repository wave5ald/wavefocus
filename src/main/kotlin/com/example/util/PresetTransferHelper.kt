package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.PresetTaskItem
import com.example.data.model.RoutinePreset
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

object PresetTransferHelper {

    fun presetToJson(preset: RoutinePreset): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("type", "WAVE_FOCUS_ROUTINE_PRESET")
        root.put("id", preset.id)
        root.put("name", preset.name)
        root.put("description", preset.description)
        root.put("iconEmoji", preset.iconEmoji)
        root.put("colorHex", preset.colorHex)
        root.put("createdAt", preset.createdAt)

        val taskArray = JSONArray()
        for (task in preset.tasks) {
            val tObj = JSONObject()
            tObj.put("title", task.title)
            tObj.put("category", task.category)
            tObj.put("timeOfDay", task.timeOfDay)
            tObj.put("targetMinutes", task.targetMinutes)
            tObj.put("daysOfWeek", task.daysOfWeek)
            tObj.put("repeatType", task.repeatType)
            tObj.put("repeatIntervalDays", task.repeatIntervalDays)
            taskArray.put(tObj)
        }
        root.put("tasks", taskArray)
        return root.toString(2)
    }

    fun jsonToPreset(rawJson: String): RoutinePreset? {
        return try {
            val clean = rawJson.trim()
            val obj = JSONObject(clean)
            val name = obj.optString("name", "").trim()
            if (name.isEmpty()) return null

            val taskArray = obj.optJSONArray("tasks") ?: JSONArray()
            val taskList = mutableListOf<PresetTaskItem>()
            for (i in 0 until taskArray.length()) {
                val tObj = taskArray.getJSONObject(i)
                val title = tObj.optString("title", "").trim()
                if (title.isNotEmpty()) {
                    taskList.add(
                        PresetTaskItem(
                            title = title,
                            category = tObj.optString("category", "Health"),
                            timeOfDay = tObj.optString("timeOfDay", "Morning"),
                            targetMinutes = tObj.optInt("targetMinutes", 15),
                            daysOfWeek = tObj.optString("daysOfWeek", "1,2,3,4,5,6,7"),
                            repeatType = tObj.optString("repeatType", "WEEKLY"),
                            repeatIntervalDays = tObj.optInt("repeatIntervalDays", 1)
                        )
                    )
                }
            }

            RoutinePreset(
                id = UUID.randomUUID().toString(),
                name = name,
                description = obj.optString("description", ""),
                iconEmoji = obj.optString("iconEmoji", "📋").ifBlank { "📋" },
                colorHex = obj.optString("colorHex", "#6366F1").ifBlank { "#6366F1" },
                tasks = taskList,
                isBuiltIn = false,
                createdAt = System.currentTimeMillis()
            )
        } catch (_: Exception) {
            null
        }
    }

    fun sharePresetAsFile(context: Context, preset: RoutinePreset): Boolean {
        return try {
            val presetsDir = File(context.cacheDir, "presets")
            if (!presetsDir.exists()) {
                presetsDir.mkdirs()
            }
            val safeName = preset.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
            val file = File(presetsDir, "preset_${safeName}.json")
            file.writeText(presetToJson(preset))

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Routine Preset: ${preset.name}")
                putExtra(Intent.EXTRA_TEXT, "Here is my routine preset '${preset.name}' with ${preset.tasks.size} tasks for Wave Focus.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Routine Preset")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun sharePresetAsText(context: Context, preset: RoutinePreset) {
        val json = presetToJson(preset)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Routine Preset: ${preset.name}")
            putExtra(Intent.EXTRA_TEXT, json)
        }
        val chooser = Intent.createChooser(intent, "Share Preset JSON")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun readJsonFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                    reader.readText()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun writeJsonToUri(context: Context, uri: Uri, json: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
