package com.example.combined_schedule.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.combined_schedule.data.Work
import com.example.combined_schedule.data.WorkRepository
import kotlinx.coroutines.flow.map

class CourseDetailViewModel(
    private val repo: WorkRepository,
    val courseTitle: String
) : ViewModel() {

    val works = repo.getAll().map { list ->
        list.filter { it.courseTitle == courseTitle }
            .sortedWith(
                // Incomplete before complete; within each group, dated items first sorted
                // by date, then undated items last.
                compareBy(
                    { it.isCompleted },
                    { if (it.dueDate.isBlank()) "9999-99-99" else it.dueDate }
                )
            )
    }

    fun addWork(title: String, description: String, dueDate: String) {
        if (title.isBlank()) return
        repo.insert(Work(courseTitle = courseTitle, title = title, description = description, dueDate = dueDate))
    }

    fun updateWork(work: Work, title: String, description: String, dueDate: String) {
        if (title.isBlank()) return
        repo.update(work.copy(title = title, description = description, dueDate = dueDate))
    }

    fun toggleComplete(work: Work) {
        repo.update(work.copy(isCompleted = !work.isCompleted))
    }

    fun deleteWork(work: Work) {
        repo.delete(work)
    }

    class Factory(private val context: Context, private val courseTitle: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CourseDetailViewModel(WorkRepository.getInstance(context), courseTitle) as T
    }
}
