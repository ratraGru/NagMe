package com.gru.nagme.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gru.nagme.R
import com.gru.nagme.data.TaskStatus

/**
 * S4 — Daftar tugas dengan dua sub-menu: Aktif (belum dikerjakan & belum lewat)
 * dan Riwayat (sudah selesai atau sudah lewat). Filter kategori berlaku untuk keduanya.
 */
@Composable
fun TaskListScreen(
    onEditTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now = rememberNow(viewModel::now)
    var tab by rememberSaveable { mutableIntStateOf(0) }

    // Dipisah sekali per perubahan data / menit, bukan tiap recompose.
    val parts = remember(state.tasks, now) {
        val (active, history) = state.tasks.partition {
            it.status == TaskStatus.ACTIVE && it.deadlineEpoch > now
        }
        active to history.sortedByDescending { it.deadlineEpoch }
    }
    val shown = if (tab == 0) parts.first else parts.second

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.tasks_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
        )
        TabRow(
            selectedTabIndex = tab,
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text(stringResource(R.string.tasks_tab_active, parts.first.size)) }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text(stringResource(R.string.tasks_tab_history, parts.second.size)) }
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.filter == null,
                    onClick = { viewModel.setFilter(null) },
                    label = { Text(stringResource(R.string.filter_all)) }
                )
            }
            items(Categories.indices) { index ->
                FilterChip(
                    selected = state.filter == index,
                    onClick = { viewModel.setFilter(index) },
                    label = { Text(stringResource(Categories.labels[index])) }
                )
            }
        }
        if (shown.isEmpty()) {
            EmptyState(if (tab == 0) R.string.empty_tasks else R.string.empty_history)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(shown, key = { it.id }) { task ->
                    TaskCard(
                        modifier = Modifier.animateItem(),
                        task = task,
                        now = now,
                        onClick = { onEditTask(task.id) },
                        onToggleDone = { viewModel.toggleDone(task) }
                    )
                }
            }
        }
    }
}
