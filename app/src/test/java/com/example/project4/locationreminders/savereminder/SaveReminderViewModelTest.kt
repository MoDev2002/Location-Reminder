package com.example.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.project4.locationreminders.data.FakeDataSource
import com.example.project4.locationreminders.reminderslist.ReminderDataItem
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

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get:Rule
    var instantExecutionRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var fakeReminderDataSource: FakeDataSource

    @Before
    fun setupRepo() {
        fakeReminderDataSource = FakeDataSource()
        viewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(), fakeReminderDataSource
        )
    }

    @Test
    fun validateEnteredData_shouldReturnError() = runTest {
        // Given incomplete reminder data
        val incompleteReminder = ReminderDataItem("", "description", "Test", 67.0, 98.0)

        // When validating reminder
        val result = viewModel.validateEnteredData(incompleteReminder)

        // Then result returns error
        assertThat(result, `is`(false))
    }

    @Test
    fun saveReminder_showLoadingIndicator() {
        // Pause dispatcher to make sure that the indicator is shown
        mainCoroutineRule.pauseDispatcher()

        // Given a new Reminder
        val reminder = ReminderDataItem("title", "description", "Test", 67.0, 98.0)

        // When saving the reminder
        viewModel.saveReminder(reminder)

        // Then the loading indicator is shown (showLoading == true)
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        // After resuming the dispatcher
        // loading is done (showLoading == false)
        mainCoroutineRule.resumeDispatcher()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }
}