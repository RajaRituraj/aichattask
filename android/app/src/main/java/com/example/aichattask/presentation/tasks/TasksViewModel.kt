package com.example.aichattask.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichattask.domain.model.Task
import com.example.aichattask.domain.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val filterDone: Boolean? = null // null = all, true = done, false = pending
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _filterDone = MutableStateFlow<Boolean?>(null)
    val uiState: StateFlow<TasksUiState> = combine(
        aiRepository.getAllTasks(),
        _filterDone
    ) { tasks, filter ->
        val filtered = when (filter) {
            true -> tasks.filter { it.isDone }
            false -> tasks.filter { !it.isDone }
            null -> tasks
        }
        TasksUiState(tasks = filtered, filterDone = filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksUiState())

    fun toggleDone(task: Task) {
        viewModelScope.launch {
            aiRepository.updateTask(task.copy(isDone = !task.isDone))
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            aiRepository.deleteTask(taskId)
        }
    }

    fun setFilter(done: Boolean?) = _filterDone.update { done }
}
