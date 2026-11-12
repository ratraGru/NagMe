package com.gru.nagme.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY status ASC, deadline_epoch ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    // Marker kalender: tugas dengan deadline di rentang [start, end] (epoch ms).
    @Query("SELECT * FROM tasks WHERE deadline_epoch BETWEEN :start AND :end ORDER BY deadline_epoch ASC")
    fun observeBetween(start: Long, end: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE status = 0 AND nag_enabled = 1")
    suspend fun getActiveNagTasks(): List<TaskEntity>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)
}
