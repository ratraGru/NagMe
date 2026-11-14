package com.gru.nagme.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.gru.nagme.NagMeApp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val EDIT = "task_edit?taskId={taskId}"
    fun edit(taskId: Long) = "task_edit?taskId=$taskId"
}

@Composable
fun NagMeRoot() {
    val navController = rememberNavController()
    val settings = (LocalContext.current.applicationContext as NagMeApp).container.settings
    val start = if (settings.onboardingDone) Routes.MAIN else Routes.ONBOARDING
    NavHost(navController = navController, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinish = {
                settings.onboardingDone = true
                navController.navigate(Routes.MAIN) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
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
