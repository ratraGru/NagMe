package com.gru.nagme.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gru.nagme.data.TaskEntity
import com.gru.nagme.data.TaskRepository
import com.gru.nagme.data.TaskStatus
import com.gru.nagme.domain.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskListUiState(
    val allTasks: List<TaskEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val filter: Int? = null
)

class TaskListViewModel(
    private val repository: TaskRepository,
    private val clock: Clock
) : ViewModel() {

    private val filter = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<TaskListUiState> = combine(repository.observeAll(), filter) { all, f ->
        TaskListUiState(
            allTasks = all,
            tasks = if (f == null) all else all.filter { it.category == f },
            filter = f
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    fun setFilter(category: Int?) {
        filter.value = category
    }

    fun toggleDone(task: TaskEntity) {
        viewModelScope.launch {
            repository.setCompleted(task, task.status != TaskStatus.DONE)
        }
    }

    fun now(): Long = clock.now()
}
