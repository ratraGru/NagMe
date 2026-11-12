package com.gru.nagme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gru.nagme.R
import com.gru.nagme.data.NO_CATEGORY
import com.gru.nagme.data.TaskEntity
import com.gru.nagme.data.TaskStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** S3 — Kalender bulanan: grid, marker kategori, navigasi bulan, bottom sheet per hari. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onEditTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = viewModel.today()
    val now = viewModel.now()
    val locale = Locale.getDefault()
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val month = state.month
    val leadingBlanks = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7

    Column(modifier = modifier.fillMaxWidth()) {
        // Header: bulan sebelumnya | judul bulan | bulan berikutnya | Hari ini
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::previousMonth) {
                Text(
                    text = "‹",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = viewModel::nextMonth) {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            TextButton(onClick = viewModel::goToToday) {
                Text(stringResource(R.string.cal_today))
            }
        }

        // Nama hari (mengikuti awal pekan sesuai locale)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            for (i in 0 until 7) {
                Text(
                    text = firstDayOfWeek.plus(i.toLong()).getDisplayName(TextStyle.SHORT, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(leadingBlanks) {
                Spacer(modifier = Modifier.aspectRatio(1f))
            }
            items(month.lengthOfMonth()) { index ->
                val date = month.atDay(index + 1)
                DayCell(
                    date = date,
                    tasks = state.tasksByDate[date].orEmpty(),
                    isToday = date == today,
                    onClick = { viewModel.selectDate(date) }
                )
            }
        }
    }

    state.selectedDate?.let { date ->
        val dayTasks = state.tasksByDate[date].orEmpty()
        ModalBottomSheet(
            onDismissRequest = viewModel::clearSelection,
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Text(
                text = formatDate(date),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (dayTasks.isEmpty()) {
                Text(
                    text = stringResource(R.string.cal_empty_day),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dayTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            now = now,
                            onClick = {
                                viewModel.clearSelection()
                                onEditTask(task.id)
                            },
                            onToggleDone = { viewModel.toggleDone(task) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Satu sel tanggal. Marker = titik warna kategori (maks 3 kategori berbeda) dari tugas yang
 * belum selesai; makin banyak tugas, latar sel makin pekat (Bagian 5.2).
 */
@Composable
private fun DayCell(
    date: LocalDate,
    tasks: List<TaskEntity>,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val pending = tasks.filter { it.status == TaskStatus.ACTIVE }
    val tint = if (pending.isEmpty()) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.primaryContainer
            .copy(alpha = (0.35f + 0.2f * (pending.size - 1)).coerceAtMost(1f))
    }
    val shape = RoundedCornerShape(12.dp)
    val fallbackDot = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(tint)
            .then(
                if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (pending.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                pending.map { it.category }.distinct().take(3).forEach { category ->
                    val color = if (category != NO_CATEGORY && category in Categories.colors.indices) {
                        Categories.colors[category]
                    } else {
                        fallbackDot
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}
