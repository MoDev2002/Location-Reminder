package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    val instantExecutionRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun setupDB() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun clearDB() {
        database.close()
        stopKoin()
    }

    @Test
    fun insertReminderAndGetById() = runTest {
        // Given a reminder
        val reminder = ReminderDTO(
            "title", "description", "location", 90.0, 80.0
        )
        // When adding the reminder to the database
        database.reminderDao().saveReminder(reminder)

        // Then when getting it by id it shows same info
        val result = database.reminderDao().getReminderById(reminder.id)

        assertThat(result as ReminderDTO, notNullValue())
        assertThat(result.title, `is`("title"))
        assertThat(result.description, `is`("description"))
        assertThat(result.location, `is`("location"))
        assertThat(result.latitude, `is`(90.0))
        assertThat(result.longitude, `is`(80.0))
    }

}