package com.forge.vdesign.data.repository

import com.forge.vdesign.domain.model.ForgeException
import com.forge.vdesign.domain.model.ForgeResult
import com.forge.vdesign.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): ForgeResult<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return ForgeResult.Error(
                ForgeException.AuthException("Sign-in succeeded but no user returned.")
            )
            ForgeResult.Success(user)
        } catch (e: Exception) {
            ForgeResult.Error(ForgeException.AuthException(e.message ?: "Sign-in failed.", e))
        }
    }

    override suspend fun createAccountWithEmail(
        email: String,
        password: String
    ): ForgeResult<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return ForgeResult.Error(
                ForgeException.AuthException("Account created but no user returned.")
            )
            ForgeResult.Success(user)
        } catch (e: Exception) {
            ForgeResult.Error(ForgeException.AuthException(e.message ?: "Sign-up failed.", e))
        }
    }

    override suspend fun signInWithGoogle(idToken: String): ForgeResult<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: return ForgeResult.Error(
                ForgeException.AuthException("Google sign-in succeeded but no user returned.")
            )
            ForgeResult.Success(user)
        } catch (e: Exception) {
            ForgeResult.Error(
                ForgeException.AuthException(e.message ?: "Google sign-in failed.", e)
            )
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }
}
