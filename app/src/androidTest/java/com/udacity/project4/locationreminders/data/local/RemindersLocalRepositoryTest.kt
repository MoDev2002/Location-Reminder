package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    val instantExecutionRule = InstantTaskExecutorRule()


    private lateinit var database : RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    @Before
    fun initRepo() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
        repository = RemindersLocalRepository(database.reminderDao())
    }

    @After
    fun clearRepo() {
        database.close()
        stopKoin()
    }

    @Test
    fun getReminder_reminderNotFound() = runTest {
        // Given the reminder Repository
        // When getting a reminder that doesn't exist
        val result = repository.getReminder("id1")

        // Then the result is Error
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }

    @Test
    fun saveReminder_retrieveReminder() = runTest {
        // Given a Reminder saved to the repository
        val reminder = ReminderDTO("title", "description", "location", 62.0, 95.0)
        repository.saveReminder(reminder)

        // When retrieving reminder by ID
        val result = repository.getReminder(reminder.id)

        // Then same reminder is returned
        assertThat(result != null, `is`(true))
        result as Result.Success
        assertThat(result.data.id, `is`(reminder.id))
        assertThat(result.data.title, `is`(reminder.title))
        assertThat(result.data.description, `is`(reminder.description))
        assertThat(result.data.location, `is`(reminder.location))
        assertThat(result.data.latitude, `is`(reminder.latitude))
        assertThat(result.data.longitude, `is`(reminder.longitude))
    }
}