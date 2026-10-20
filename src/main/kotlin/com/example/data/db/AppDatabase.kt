package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.data.model.BoardElement
import com.example.data.model.BoardItem
import com.example.data.model.FocusSession
import com.example.data.model.NoteItem
import com.example.data.model.RoutineTask
import com.example.data.model.ScheduledScript
import com.example.data.model.TaskCompletionLog
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ConcurrentHashMap

@Database(
    entities = [
        RoutineTask::class,
        TaskCompletionLog::class,
        FocusSession::class,
        ScheduledScript::class,
        NoteItem::class,
        BoardItem::class,
        BoardElement::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val userDbInstances = ConcurrentHashMap<String, AppDatabase>()

        fun sanitizeAccountKey(raw: String): String {
            val clean = raw.trim().lowercase().replace(Regex("[^a-zA-Z0-9_]"), "_")
            return if (clean.isBlank()) "guest_user" else clean
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context.getDatabasePath("daily_routine_database.db").absolutePath)
                INSTANCE = instance
                instance
            }
        }

        fun getDatabaseForUser(context: Context, userAccountKey: String): AppDatabase {
            val safeKey = sanitizeAccountKey(userAccountKey)
            return userDbInstances.computeIfAbsent(safeKey) {
                val dbName = if (safeKey == "guest_user" || safeKey == "default") {
                    "daily_routine_database.db"
                } else {
                    "routine_db_${safeKey}.db"
                }
                buildDatabase(context.getDatabasePath(dbName).absolutePath)
            }
        }

        private fun buildDatabase(path: String): AppDatabase {
            return Room.databaseBuilder<AppDatabase>(name = path)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .fallbackToDestructiveMigration(true)
                .build()
        }
    }
}
