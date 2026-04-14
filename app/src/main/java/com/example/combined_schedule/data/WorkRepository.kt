package com.example.combined_schedule.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).workDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val allWorks: StateFlow<List<Work>> = dao.getAll()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun getAll(): StateFlow<List<Work>> = allWorks

    // Kept for API compatibility; ViewModel filters client-side by courseTitle.
    @Suppress("UnusedParameter")
    fun worksForCourse(courseTitle: String): StateFlow<List<Work>> = allWorks

    fun insert(work: Work) { scope.launch { dao.insert(work) } }
    fun update(work: Work) { scope.launch { dao.update(work) } }
    fun delete(work: Work) { scope.launch { dao.delete(work) } }

    companion object {
        @Volatile private var INSTANCE: WorkRepository? = null

        fun getInstance(context: Context): WorkRepository =
            INSTANCE ?: synchronized(this) {
                WorkRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
