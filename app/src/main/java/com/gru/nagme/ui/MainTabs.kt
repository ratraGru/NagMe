package com.gru.nagme.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.gru.nagme.R

private data class TabItem(val label: Int, val icon: ImageVector)

private val tabs = listOf(
    TabItem(R.string.tab_today, Icons.Default.Home),
    TabItem(R.string.tab_calendar, Icons.Default.DateRange),
    TabItem(R.string.tab_tasks, Icons.AutoMirrored.Filled.List),
    TabItem(R.string.tab_settings, Icons.Default.Settings)
)

/** Navigasi bawah 4 tab (Bagian 10.2): Hari Ini | Kalender | Tugas | Pengaturan. */
@Composable
fun MainTabs(
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 2) {
                FloatingActionButton(onClick = onAddTask) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add))
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            0 -> TodayScreen(
                onEditTask = onEditTask,
                onOpenPermissions = { selectedTab = 3 },
                modifier = contentModifier
            )
            1 -> CalendarScreen(onEditTask = onEditTask, modifier = contentModifier)
            2 -> TaskListScreen(onEditTask = onEditTask, modifier = contentModifier)
            else -> SettingsScreen(modifier = contentModifier)
        }
    }
}
