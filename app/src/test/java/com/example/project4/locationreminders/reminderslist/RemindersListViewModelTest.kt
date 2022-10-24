package com.example.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.project4.locationreminders.data.FakeDataSource
import com.example.project4.locationreminders.data.dto.ReminderDTO
import com.example.project4.locationreminders.utils.MainCoroutineRule
import com.example.project4.locationreminders.utils.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    @get:Rule
    var instantExecutionRule = InstantTaskExecutorRule()
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: RemindersListViewModel
    private lateinit var reminderFakeDataSource: FakeDataSource

    @Before
    fun setupRepo() {
        reminderFakeDataSource = FakeDataSource()
        viewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            reminderFakeDataSource
        )
    }

    @Test
    fun loadReminders_shouldReturnError() = runTest {
        // Given a data source that returns error
        reminderFakeDataSource.setReturnError(true)

        // When loading reminders
        viewModel.loadReminders()

        // Then the snackbar shows error
        assertThat(viewModel.showSnackBar.getOrAwaitValue(), `is`("Test Exception"))
    }

    @Test
    fun loadReminders_loading() {
        // Pause dispatcher
        mainCoroutineRule.pauseDispatcher()

        // When loading reminders
        viewModel.loadReminders()

        // Then loading indicator is shown
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Resuming dispatcher loading is done
        mainCoroutineRule.resumeDispatcher()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }
}