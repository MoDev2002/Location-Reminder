package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders : MutableList<ReminderDTO> = mutableListOf()) : ReminderDataSource {

    // Error Handling
    private var shouldReturnError = false

    fun setReturnError(value : Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return try {
            if (shouldReturnError) {
                throw Exception("Reminders not found")
            }
            Result.Success(ArrayList(reminders))
        } catch (ex : Exception) {
            Result.Error(ex.localizedMessage)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return try {
            val reminder = reminders.find { it.id == id }
            if(reminder == null || shouldReturnError) {
                Result.Error("Reminder not found!")
            } else {
                Result.Success(reminder)
            }
        } catch (ex : Exception) {
            Result.Error(ex.localizedMessage)
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }


}