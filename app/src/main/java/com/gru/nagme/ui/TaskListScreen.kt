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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gru.nagme.R

/** S4 — Daftar semua tugas, urut deadline, filter kategori. */
@Composable
fun TaskListScreen(
    onEditTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now = viewModel.now()

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.tasks_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
        )
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
        if (state.tasks.isEmpty()) {
            EmptyState(R.string.empty_tasks)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.tasks, key = { it.id }) { task ->
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
