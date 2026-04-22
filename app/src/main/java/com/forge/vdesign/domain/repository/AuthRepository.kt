package com.forge.vdesign.domain.repository

import com.forge.vdesign.domain.model.ForgeResult
import com.google.firebase.auth.FirebaseUser

/**
 * Auth repository — abstracts all Firebase Auth operations from the ViewModel.
 */
interface AuthRepository {
    /** Currently signed-in user, or null if not authenticated. */
    val currentUser: FirebaseUser?

    /** Sign in with email + password. */
    suspend fun signInWithEmail(email: String, password: String): ForgeResult<FirebaseUser>

    /** Create a new account with email + password. */
    suspend fun createAccountWithEmail(email: String, password: String): ForgeResult<FirebaseUser>

    /** Sign in with a Google ID token obtained from Credential Manager. */
    suspend fun signInWithGoogle(idToken: String): ForgeResult<FirebaseUser>

    /** Sign out the current user. */
    fun signOut()
}
