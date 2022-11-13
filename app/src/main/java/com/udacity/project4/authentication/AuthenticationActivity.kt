package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import com.firebase.ui.auth.AuthUI

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    companion object {
        const val SIGN_IN_REQUEST_CODE = 1001
    }

    val viewModel by viewModels<AuthenticationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_authentication)

       viewModel.authenticationState.observe(this) { authState ->
            if (authState == AuthenticationViewModel.AuthState.AUTHENTICATED) {
                navigateRemindersActivity()
            }
        }

        findViewById<Button>(R.id.loginBtn).setOnClickListener {
            viewModel.authenticationState.observe(this) { authState ->
                authStateCheck(authState)
            }
        }

    }

    private fun authStateCheck(authState: AuthenticationViewModel.AuthState) {
        when(authState) {
            AuthenticationViewModel.AuthState.UNAUTHENTICATED -> launchSignInFlow()
            AuthenticationViewModel.AuthState.AUTHENTICATED -> {
                navigateRemindersActivity()
            }
        }
    }

    private fun navigateRemindersActivity() {
        val intent = Intent(this, RemindersActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun launchSignInFlow() {
        // methods that the user can sign in with
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // start sign in firebase activity
        startActivityForResult(
            AuthUI
                .getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
                SIGN_IN_REQUEST_CODE
        )
    }
}
