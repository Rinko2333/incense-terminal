package com.rinko.incenseterminal.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "incense-terminal.db"
        const val DB_VERSION = 1

        const val TABLE_WORKLOAD = "workload"
        const val TABLE_FOCUS_SESSION = "focus_session"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_WORKLOAD (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                default_duration_minutes INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE $TABLE_FOCUS_SESSION (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                workload_name TEXT NOT NULL,
                duration_minutes INTEGER NOT NULL,
                start_timestamp INTEGER NOT NULL,
                end_timestamp INTEGER NOT NULL,
                completed INTEGER NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FOCUS_SESSION")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WORKLOAD")
        onCreate(db)
    }
}

data class WorkloadRow(
    val id: Long,
    val name: String,
    val defaultDurationMinutes: Int
)

data class FocusSessionRow(
    val id: Long = 0,
    val workloadName: String,
    val durationMinutes: Int,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val completed: Boolean
)

data class DailySummary(
    val workloadName: String,
    val totalMinutes: Int
)

data class HeatmapDay(
    val dayStartMs: Long,
    val totalMinutes: Int
)
