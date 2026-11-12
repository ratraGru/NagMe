package com.gru.nagme.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gru.nagme.data.TaskEntity
import com.gru.nagme.data.TaskRepository
import com.gru.nagme.data.TaskStatus
import com.gru.nagme.domain.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class CalendarUiState(
    val month: YearMonth,
    /** Semua tugas (aktif + selesai) di bulan ini, dikelompokkan per tanggal deadline. */
    val tasksByDate: Map<LocalDate, List<TaskEntity>> = emptyMap(),
    val selectedDate: LocalDate? = null
)

/** S3 — Kalender bulanan (Bagian 5.2). Waktu lewat Clock supaya ikut Mode Demo nanti. */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val repository: TaskRepository,
    private val clock: Clock
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.from(today()))
    private val selected = MutableStateFlow<LocalDate?>(null)

    private val monthTasks = month.flatMapLatest { m ->
        val zone = ZoneId.systemDefault()
        val start = m.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = m.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        repository.observeBetween(start, end)
    }

    val uiState: StateFlow<CalendarUiState> = combine(month, selected, monthTasks) { m, sel, tasks ->
        val zone = ZoneId.systemDefault()
        CalendarUiState(
            month = m,
            tasksByDate = tasks.groupBy {
                Instant.ofEpochMilli(it.deadlineEpoch).atZone(zone).toLocalDate()
            },
            selectedDate = sel
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState(month.value))

    fun today(): LocalDate =
        Instant.ofEpochMilli(clock.now()).atZone(ZoneId.systemDefault()).toLocalDate()

    fun now(): Long = clock.now()

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }

    fun goToToday() {
        month.value = YearMonth.from(today())
    }

    fun selectDate(date: LocalDate) {
        selected.value = date
    }

    fun clearSelection() {
        selected.value = null
    }

    fun toggleDone(task: TaskEntity) {
        viewModelScope.launch {
            repository.setCompleted(task, task.status != TaskStatus.DONE)
        }
    }
}
