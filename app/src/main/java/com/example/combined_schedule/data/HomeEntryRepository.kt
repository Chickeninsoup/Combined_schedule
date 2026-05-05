package com.example.combined_schedule.data

import android.content.Context
import com.example.combined_schedule.receiver.HomeReminderReceiver
import com.example.combined_schedule.util.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class HomeEntryRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).homeEntryDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val entries: StateFlow<List<HomeEntry>> = dao.getAll()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            if (dao.count() == 0) seedDefaults()
        }
    }

    fun getAll(): StateFlow<List<HomeEntry>> = entries

    fun findById(id: String): HomeEntry? = runBlocking(Dispatchers.IO) { dao.findById(id) }

    fun getAllSync(): List<HomeEntry> = runBlocking(Dispatchers.IO) { dao.getAllList() }

    fun insert(entry: HomeEntry) { scope.launch { dao.insert(entry) } }
    fun update(entry: HomeEntry) { scope.launch { dao.update(entry) } }
    fun delete(entry: HomeEntry) { scope.launch { dao.delete(entry) } }

    private suspend fun seedDefaults() {
        val defaults = listOf(
            HomeEntry(title = "MATH 241 Lecture", location = "Altgeld Hall 141", time = "09:00",
                daysOfWeek = listOf("Mon", "Wed", "Fri"), isBus = false,
                reminderEnabled = true, reminderMinutes = 10),
            HomeEntry(title = "22 Illini Bus", location = "Green & Wright Stop", time = "10:30",
                daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri"), isBus = true),
            HomeEntry(title = "CS 124 Lab", location = "Siebel Center 0216", time = "13:00",
                daysOfWeek = listOf("Mon", "Wed"), isBus = false,
                reminderEnabled = true, reminderMinutes = 15),
            HomeEntry(title = "PHYS 212 Discussion", location = "Loomis Lab 141", time = "15:00",
                daysOfWeek = listOf("Tue", "Thu"), isBus = false)
        )
        defaults.forEach { dao.insert(it) }
        // Schedule reminders for seeded entries so notifications work on first install.
        HomeReminderReceiver.ensureChannel(context)
        defaults.filter { it.reminderEnabled }.forEach { NotificationScheduler.schedule(context, it) }
    }

    companion object {
        @Volatile private var INSTANCE: HomeEntryRepository? = null

        fun getInstance(context: Context): HomeEntryRepository =
            INSTANCE ?: synchronized(this) {
                HomeEntryRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
