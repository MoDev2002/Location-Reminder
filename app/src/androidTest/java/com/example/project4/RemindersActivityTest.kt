package com.example.project4

import android.app.Activity
import android.app.Application
import android.os.Build
import android.view.View
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.project4.locationreminders.RemindersActivity
import com.example.project4.locationreminders.data.ReminderDataSource
import com.example.project4.locationreminders.data.local.LocalDB
import com.example.project4.locationreminders.data.local.RemindersLocalRepository
import com.example.project4.locationreminders.reminderslist.RemindersListViewModel
import com.example.project4.locationreminders.savereminder.SaveReminderViewModel
import com.example.project4.util.DataBindingIdlingResource
import com.example.project4.util.monitorActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {
    // Extended Koin Test - embed autoclose @after method to close Koin after every test

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(RemindersActivity::class.java)

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResources() {
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResources() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }


    @Test
    fun saveReminderTitleErrorSnackBar() {
        // load activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // click on the add reminder FAB to navigate to add reminder fragment
        onView(withId(R.id.addReminderFAB)).perform(click())
        // click on save reminder FAB without adding any info
        onView(withId(R.id.saveReminder)).perform(click())
        // Check that enter title error snackbar is showing
        onView(withText(appContext.getString(R.string.err_enter_title))).check(matches(isDisplayed()))

        // close activity
        activityScenario.close()
    }

    @Test
    fun saveReminderLocationErrorSnackBar() {
        // load activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // click on the add reminder FAB to navigate to add reminder fragment
        onView(withId(R.id.addReminderFAB)).perform(click())

        // add title but not choosing location
        onView(withId(R.id.reminderTitle)).perform(replaceText("title"))
        onView(withId(R.id.saveReminder)).perform(click())

        // location not selected error snackbar showing
        onView(withText(appContext.getString(R.string.err_select_location))).check(matches(isDisplayed()))

        // close activity
        activityScenario.close()
    }

    @Test
    fun saveReminder() {
        // load the activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // click on the add reminder FAB to navigate to add reminder fragment
        onView(withId(R.id.addReminderFAB)).perform(click())

        // add reminder info and choose location on map
        onView(withId(R.id.reminderTitle)).perform(replaceText("title"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("description"))
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.map)).perform(click())
        onView(withId(R.id.saveLocationBtn)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())

        // check if reminder info is displayed
        onView(withText("title")).check(matches(isDisplayed()))
        onView(withText("description")).check(matches(isDisplayed()))

        // Toast messages tests not working on Sdk 30 or Above
        // https://lightrun.com/answers/android-android-test-toast-message-assertions-not-working-with-android-11-and-target-sdk-30
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            onView(withText(R.string.reminder_saved))
                .inRoot(withDecorView(not(`is`(getActivity(activityScenario).window.decorView))))
                .check(matches(isDisplayed()))
        }

        // close activity
        activityScenario.close()
    }

    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>) : Activity {
        lateinit var activity: Activity
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }
}
