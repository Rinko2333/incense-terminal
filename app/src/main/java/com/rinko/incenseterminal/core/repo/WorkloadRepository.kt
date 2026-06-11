package com.rinko.incenseterminal.core.repo

import android.content.ContentValues
import com.rinko.incenseterminal.data.AppDatabase
import com.rinko.incenseterminal.data.WorkloadRow

class WorkloadRepository(private val db: AppDatabase) {

    fun getAll(): List<WorkloadRow> {
        val list = mutableListOf<WorkloadRow>()
        val cursor = db.readableDatabase.query(
            AppDatabase.TABLE_WORKLOAD,
            arrayOf("id", "name", "default_duration_minutes"),
            null, null, null, null, "id ASC"
        )
        while (cursor.moveToNext()) {
            list.add(
                WorkloadRow(
                    id = cursor.getLong(0),
                    name = cursor.getString(1),
                    defaultDurationMinutes = cursor.getInt(2)
                )
            )
        }
        cursor.close()
        return list
    }

    fun insert(name: String, defaultDurationMinutes: Int) {
        val cv = ContentValues().apply {
            put("name", name)
            put("default_duration_minutes", defaultDurationMinutes)
        }
        db.writableDatabase.insert(AppDatabase.TABLE_WORKLOAD, null, cv)
    }

    fun delete(id: Long) {
        db.writableDatabase.delete(AppDatabase.TABLE_WORKLOAD, "id = ?", arrayOf(id.toString()))
    }

    fun update(id: Long, name: String, defaultDurationMinutes: Int) {
        val cv = ContentValues().apply {
            put("name", name)
            put("default_duration_minutes", defaultDurationMinutes)
        }
        db.writableDatabase.update(
            AppDatabase.TABLE_WORKLOAD,
            cv,
            "id = ?",
            arrayOf(id.toString())
        )
    }
}
