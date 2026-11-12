package com.gru.nagme.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gru.nagme.data.NO_CATEGORY
import com.gru.nagme.data.TaskEntity
import com.gru.nagme.data.TaskRepository
import com.gru.nagme.domain.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class TaskEditUiState(
    val title: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.of(23, 59),
    val category: Int = NO_CATEGORY,
    val nagEnabled: Boolean = true,
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val finished: Boolean = false
)

class TaskEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository,
    private val clock: Clock
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>(ARG_TASK_ID) ?: -1L
    private val zone: ZoneId = ZoneId.systemDefault()
    private var original: TaskEntity? = null

    private val _ui = MutableStateFlow(defaultState())
    val ui: StateFlow<TaskEditUiState> = _ui.asStateFlow()

    init {
        if (taskId != -1L) {
            _ui.update { it.copy(isEdit = true, isLoading = true) }
            viewModelScope.launch {
                val task = repository.get(taskId)
                if (task == null) {
                    _ui.update { it.copy(isLoading = false, finished = true) }
                } else {
                    original = task
                    val deadline = Instant.ofEpochMilli(task.deadlineEpoch).atZone(zone)
                    _ui.update {
                        it.copy(
                            title = task.title,
                            description = task.description.orEmpty(),
                            date = deadline.toLocalDate(),
                            time = deadline.toLocalTime().withSecond(0).withNano(0),
                            category = task.category,
                            nagEnabled = task.nagEnabled,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    private fun defaultState(): TaskEditUiState {
        val tomorrow = Instant.ofEpochMilli(clock.now()).atZone(zone).toLocalDate().plusDays(1)
        return TaskEditUiState(date = tomorrow, time = LocalTime.of(23, 59))
    }

    fun onTitleChange(value: String) = _ui.update { it.copy(title = value.take(MAX_TITLE)) }
    fun onDescriptionChange(value: String) = _ui.update { it.copy(description = value.take(MAX_DESCRIPTION)) }
    fun onDateChange(value: LocalDate) = _ui.update { it.copy(date = value) }
    fun onTimeChange(value: LocalTime) = _ui.update { it.copy(time = value) }
    fun onCategoryChange(value: Int) = _ui.update { it.copy(category = value) }
    fun onNagChange(value: Boolean) = _ui.update { it.copy(nagEnabled = value) }

    private fun deadlineEpoch(): Long {
        val s = _ui.value
        return LocalDateTime.of(s.date, s.time).atZone(zone).toInstant().toEpochMilli()
    }

    /** Peringatan hanya untuk tugas baru atau saat deadline diubah ke masa lalu (Bagian 5.1). */
    fun needsPastConfirmation(): Boolean {
        val deadline = deadlineEpoch()
        val changed = original?.deadlineEpoch != deadline
        return changed && deadline < clock.now()
    }

    fun save() {
        val s = _ui.value
        if (s.title.isBlank()) return
        viewModelScope.launch {
            val old = original
            if (old == null) {
                repository.add(s.title, s.description, deadlineEpoch(), s.category, s.nagEnabled)
            } else {
                repository.edit(old, s.title, s.description, deadlineEpoch(), s.category, s.nagEnabled)
            }
            _ui.update { it.copy(finished = true) }
        }
    }

    fun delete() {
        val old = original ?: return
        viewModelScope.launch {
            repository.delete(old)
            _ui.update { it.copy(finished = true) }
        }
    }

    companion object {
        const val ARG_TASK_ID = "taskId"
        const val MAX_TITLE = 100
        const val MAX_DESCRIPTION = 500
    }
}
