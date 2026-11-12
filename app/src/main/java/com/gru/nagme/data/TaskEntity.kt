package com.gru.nagme.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

object TaskStatus {
    const val ACTIVE = 0
    const val DONE = 1
}

/** category 0..5 = preset warna; -1 = tanpa kategori. */
const val NO_CATEGORY = -1

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String?,
    @ColumnInfo(name = "deadline_epoch") val deadlineEpoch: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    val status: Int = TaskStatus.ACTIVE,
    val category: Int = NO_CATEGORY,
    @ColumnInfo(name = "nag_enabled") val nagEnabled: Boolean = true,
    @ColumnInfo(name = "snooze_count") val snoozeCount: Int = 0,
    @ColumnInfo(name = "snooze_until_epoch") val snoozeUntilEpoch: Long = 0,
    @ColumnInfo(name = "last_triggered_level") val lastTriggeredLevel: Int = -1
)
