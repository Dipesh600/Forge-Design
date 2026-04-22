package com.forge.vdesign.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.forge.vdesign.databinding.ActivityAuthBinding
import com.forge.vdesign.ui.MainActivity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()

    /** true = sign-in mode, false = create-account mode */
    private var isSignInMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already signed in, skip directly to chat
        if (viewModel.isAlreadySignedIn) {
            goToMain()
            return
        }

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUi()
        observeState()
    }

    private fun setupUi() {
        binding.btnPrimary.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            if (isSignInMode) viewModel.signInWithEmail(email, password)
            else              viewModel.createAccount(email, password)
        }

        binding.btnToggleMode.setOnClickListener {
            isSignInMode = !isSignInMode
            if (isSignInMode) {
                binding.tvTitle.text   = "Sign in"
                binding.btnPrimary.text = "Sign in"
                binding.btnToggleMode.text = "Don't have an account? Create one"
            } else {
                binding.tvTitle.text   = "Create account"
                binding.btnPrimary.text = "Create account"
                binding.btnToggleMode.text = "Already have an account? Sign in"
            }
            viewModel.resetState()
        }

        binding.btnGoogle.setOnClickListener { launchGoogleSignIn() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Idle    -> showIdle()
                        is AuthUiState.Loading -> showLoading()
                        is AuthUiState.Success -> goToMain()
                        is AuthUiState.Error   -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun showIdle() {
        binding.progressBar.visibility = View.GONE
        binding.tvError.visibility     = View.GONE
        binding.btnPrimary.isEnabled   = true
        binding.btnGoogle.isEnabled    = true
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvError.visibility     = View.GONE
        binding.btnPrimary.isEnabled   = false
        binding.btnGoogle.isEnabled    = false
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.tvError.visibility     = View.VISIBLE
        binding.tvError.text           = message
        binding.btnPrimary.isEnabled   = true
        binding.btnGoogle.isEnabled    = true
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun launchGoogleSignIn() {
        // Web Client ID (type 3) from Firebase Console → Authentication → Google → Web SDK config
        // Replace this with your actual Web Client ID from the Firebase Console
        val webClientId = "476586295818-hiohvdl5isdpmq69hstjl3rhp4iuugqc.apps.googleusercontent.com"

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(this)

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@AuthActivity, request)
                val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                viewModel.handleGoogleIdToken(googleIdToken)
            } catch (e: GetCredentialException) {
                showError("Google Sign-In failed: ${e.message}")
            }
        }
    }
}
