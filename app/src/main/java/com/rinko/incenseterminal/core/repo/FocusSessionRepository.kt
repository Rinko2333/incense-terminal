package com.rinko.incenseterminal.core.repo

import android.content.ContentValues
import com.rinko.incenseterminal.data.AppDatabase
import com.rinko.incenseterminal.data.DailySummary
import com.rinko.incenseterminal.data.FocusSessionRow
import com.rinko.incenseterminal.data.HeatmapDay

class FocusSessionRepository(private val db: AppDatabase) {

    fun record(session: FocusSessionRow) {
        val cv = ContentValues().apply {
            put("workload_name", session.workloadName)
            put("duration_minutes", session.durationMinutes)
            put("start_timestamp", session.startTimestamp)
            put("end_timestamp", session.endTimestamp)
            put("completed", if (session.completed) 1 else 0)
        }
        db.writableDatabase.insert(AppDatabase.TABLE_FOCUS_SESSION, null, cv)
    }

    fun getSessionsForDay(dayStart: Long, dayEnd: Long): List<FocusSessionRow> {
        val list = mutableListOf<FocusSessionRow>()
        val cursor = db.readableDatabase.query(
            AppDatabase.TABLE_FOCUS_SESSION,
            arrayOf("id", "workload_name", "duration_minutes", "start_timestamp", "end_timestamp", "completed"),
            "start_timestamp >= ? AND start_timestamp < ?",
            arrayOf(dayStart.toString(), dayEnd.toString()),
            null, null, "start_timestamp DESC"
        )
        while (cursor.moveToNext()) {
            list.add(
                FocusSessionRow(
                    id = cursor.getLong(0),
                    workloadName = cursor.getString(1),
                    durationMinutes = cursor.getInt(2),
                    startTimestamp = cursor.getLong(3),
                    endTimestamp = cursor.getLong(4),
                    completed = cursor.getInt(5) == 1
                )
            )
        }
        cursor.close()
        return list
    }

    fun getDailySummary(dayStart: Long, dayEnd: Long): List<DailySummary> {
        val list = mutableListOf<DailySummary>()
        val cursor = db.readableDatabase.rawQuery(
            "SELECT workload_name, SUM(duration_minutes) FROM ${AppDatabase.TABLE_FOCUS_SESSION} " +
            "WHERE start_timestamp >= ? AND start_timestamp < ? AND completed = 1 " +
            "GROUP BY workload_name",
            arrayOf(dayStart.toString(), dayEnd.toString())
        )
        while (cursor.moveToNext()) {
            list.add(
                DailySummary(
                    workloadName = cursor.getString(0),
                    totalMinutes = cursor.getInt(1)
                )
            )
        }
        cursor.close()
        return list
    }

    fun getHeatmapData(): List<HeatmapDay> {
        val list = mutableListOf<HeatmapDay>()
        val cursor = db.readableDatabase.rawQuery(
            "SELECT (start_timestamp / 86400000) * 86400000 as dayStart, SUM(duration_minutes) as totalMinutes " +
            "FROM ${AppDatabase.TABLE_FOCUS_SESSION} WHERE completed = 1 " +
            "GROUP BY dayStart ORDER BY dayStart DESC LIMIT 365",
            null
        )
        while (cursor.moveToNext()) {
            list.add(
                HeatmapDay(
                    dayStartMs = cursor.getLong(0),
                    totalMinutes = cursor.getInt(1)
                )
            )
        }
        cursor.close()
        return list
    }

    fun getCompletedSessionCount(): Int {
        val cursor = db.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM ${AppDatabase.TABLE_FOCUS_SESSION} WHERE completed = 1",
            null
        )
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        return count
    }

    fun getFocusMinutesForRange(fromMs: Long, toMs: Long): Int {
        val cursor = db.readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(duration_minutes), 0) FROM ${AppDatabase.TABLE_FOCUS_SESSION} " +
            "WHERE completed = 1 AND start_timestamp >= ? AND start_timestamp < ?",
            arrayOf(fromMs.toString(), toMs.toString())
        )
        cursor.moveToFirst()
        val total = cursor.getInt(0)
        cursor.close()
        return total
    }

    fun getStreakDays(todayDayStartMs: Long): Int {
        val cursor = db.readableDatabase.rawQuery(
            "SELECT DISTINCT (start_timestamp / 86400000) * 86400000 as dayStart " +
            "FROM ${AppDatabase.TABLE_FOCUS_SESSION} " +
            "WHERE completed = 1 ORDER BY dayStart DESC LIMIT 365",
            null
        )
        val activeDays = mutableListOf<Long>()
        while (cursor.moveToNext()) {
            activeDays.add(cursor.getLong(0))
        }
        cursor.close()
        if (activeDays.isEmpty()) return 0

        val today = todayDayStartMs
        val yesterday = today - 86_400_000L
        val start = if (activeDays.contains(today)) today
        else if (activeDays.contains(yesterday)) yesterday
        else return 0

        var streak = 0
        var cursor2 = start
        while (activeDays.contains(cursor2)) {
            streak++
            cursor2 -= 86_400_000L
        }
        return streak
    }
}
