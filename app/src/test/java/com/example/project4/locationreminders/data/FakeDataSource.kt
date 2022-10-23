package com.example.project4.locationreminders.data

import com.example.project4.locationreminders.data.ReminderDataSource
import com.example.project4.locationreminders.data.dto.ReminderDTO
import com.example.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders : MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

//    TODO: Create a fake data source to act as a double to the real data source

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        reminders?.let {
            return Result.Success(ArrayList(it))
        }
        return Result.Error("No Reminders found")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminder = reminders?.find { it.id == id }
        return try {
            if(reminder == null) {
                Result.Error("No reminder")
            } else {
                Result.Success(reminder)
            }
        } catch (ex : Exception) {
            Result.Error(ex.localizedMessage)
        }
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }


}