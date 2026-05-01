package com.forge.vdesign.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.forge.vdesign.databinding.ActivityProfileBinding
import com.forge.vdesign.ui.auth.AuthActivity
import com.forge.vdesign.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUi()
        observeState()
    }

    private fun setupUi() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnLogout.setOnClickListener {
            authViewModel.signOut()
        }

        // Display user info
        val user = authViewModel.currentUser
        binding.tvUserName.text = (user?.displayName ?: user?.email ?: "Designer").ifBlank { "Designer" }
        binding.tvUserEmail.text = user?.email ?: "studio@forge.ai"
    }

    private fun observeState() {
        lifecycleScope.launch {
            authViewModel.uiState.collect { state ->
                if (!authViewModel.isAlreadySignedIn) {
                    goToAuth()
                }
            }
        }
    }

    private fun goToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        fun launch(context: Context) {
            context.startActivity(Intent(context, ProfileActivity::class.java))
        }
    }
}
