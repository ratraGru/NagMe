package com.gru.nagme.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gru.nagme.NagMeApp

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val container = nagMeApp().container
            TaskListViewModel(container.taskRepository, container.clock)
        }
        initializer {
            val container = nagMeApp().container
            TaskEditViewModel(createSavedStateHandle(), container.taskRepository, container.clock)
        }
        initializer {
            val container = nagMeApp().container
            CalendarViewModel(container.taskRepository, container.clock)
        }
    }
}

fun CreationExtras.nagMeApp(): NagMeApp = this[APPLICATION_KEY] as NagMeApp
