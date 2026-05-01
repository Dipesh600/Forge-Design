package com.forge.vdesign.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.vdesign.domain.model.ForgeResult
import com.forge.vdesign.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val user: FirebaseUser) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** True if the user is already signed in when the screen opens. */
    val isAlreadySignedIn: Boolean
        get() = authRepository.currentUser != null

    val currentUser: FirebaseUser?
        get() = authRepository.currentUser

    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password cannot be empty.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.signInWithEmail(email, password)) {
                is ForgeResult.Success -> _uiState.value = AuthUiState.Success(result.data)
                is ForgeResult.Error   -> _uiState.value = AuthUiState.Error(result.exception.message ?: "Sign-in failed.")
                ForgeResult.Loading    -> Unit
            }
        }
    }

    fun createAccount(email: String, password: String) {
        if (email.isBlank() || password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.createAccountWithEmail(email, password)) {
                is ForgeResult.Success -> _uiState.value = AuthUiState.Success(result.data)
                is ForgeResult.Error   -> _uiState.value = AuthUiState.Error(result.exception.message ?: "Sign-up failed.")
                ForgeResult.Loading    -> Unit
            }
        }
    }

    fun handleGoogleIdToken(idToken: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is ForgeResult.Success -> _uiState.value = AuthUiState.Success(result.data)
                is ForgeResult.Error   -> _uiState.value = AuthUiState.Error(result.exception.message ?: "Google sign-in failed.")
                ForgeResult.Loading    -> Unit
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = AuthUiState.Idle // Reset to idle so the UI can redirect
    }
}
