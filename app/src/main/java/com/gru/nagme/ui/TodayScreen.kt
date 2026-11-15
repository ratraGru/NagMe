package com.gru.nagme.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.gru.nagme.data.TaskStatus

/** S2 — Beranda "Hari Ini": tugas aktif yang deadline-nya hari ini. */
@Composable
fun TodayScreen(
    onEditTask: (Long) -> Unit,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions = rememberPermissionStatus()
    val now = viewModel.now()
    val today = state.allTasks.filter {
        it.status == TaskStatus.ACTIVE && isSameDay(it.deadlineEpoch, now)
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.today_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
        )
        Text(
            text = if (today.isEmpty()) stringResource(R.string.today_sub_empty)
            else stringResource(R.string.today_sub, today.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        if (permissions.criticalMissing) {
            PermissionBanner(
                onClick = onOpenPermissions,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        if (today.isEmpty()) {
            EmptyState(R.string.empty_today)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(today, key = { it.id }) { task ->
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
