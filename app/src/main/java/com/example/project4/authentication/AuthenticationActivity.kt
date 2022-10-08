package com.example.project4.authentication

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.project4.R
import com.example.project4.locationreminders.RemindersActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.auth.FirebaseAuth

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
        // TODO: Implement the create account and sign in using FirebaseUI, use sign in using email and sign in using Google
        launchSignInFlow()
        // TODO: If the user was authenticated, send him to RemindersActivity
        viewModel.authenticationState.observe(this) { authState ->
            authStateCheck(authState)
        }
        // TODO: a bonus is to customize the sign in flow to look nice using :
        //https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout

    }

    private fun authStateCheck(authState: AuthenticationViewModel.AuthState) {
        when(authState) {
            AuthenticationViewModel.AuthState.UNAUTHENTICATED -> launchSignInFlow()
            AuthenticationViewModel.AuthState.AUTHENTICATED -> {
                val intent = Intent(this, RemindersActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
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
