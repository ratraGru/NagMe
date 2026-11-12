package com.gru.nagme.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private object Routes {
    const val MAIN = "main"
    const val EDIT = "task_edit?taskId={taskId}"
    fun edit(taskId: Long) = "task_edit?taskId=$taskId"
}

@Composable
fun NagMeRoot() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainTabs(
                onAddTask = { navController.navigate(Routes.edit(-1L)) },
                onEditTask = { id -> navController.navigate(Routes.edit(id)) }
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument(TaskEditViewModel.ARG_TASK_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            TaskEditScreen(onBack = { navController.popBackStack() })
        }
    }
}
