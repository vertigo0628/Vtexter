package com.university.vtexter.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.university.vtexter.data.repository.VTexterRepository
import com.university.vtexter.models.User
import com.university.vtexter.utils.UserSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private lateinit var repository: VTexterRepository
    private lateinit var appContext: Context

    private val _authState = MutableStateFlow(false)
    val authState: StateFlow<Boolean> = _authState.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        repository = VTexterRepository(context)
        _authState.value = auth.currentUser != null

        // Start syncing users if logged in
        if (auth.currentUser != null) {
            UserSyncManager.startSync(context)
            UserSyncManager.syncCurrentUser(context)
        }
    }

    init {
        _authState.value = auth.currentUser != null
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please fill all fields"
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _authState.value = true
                _errorMessage.value = ""

                // Start syncing users after login
                if (::appContext.isInitialized) {
                    UserSyncManager.startSync(appContext)
                    UserSyncManager.syncCurrentUser(appContext)
                }
            }
            .addOnFailureListener { e ->
                _errorMessage.value = e.message ?: "Login failed"
            }
    }

    fun register(name: String, email: String, password: String, context: Context) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Please fill all fields"
            return
        }

        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters"
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: return@addOnSuccessListener

                // Update display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                result.user?.updateProfile(profileUpdates)

                val user = User(
                    userId = userId,
                    name = name,
                    email = email,
                    profilePicture = "",
                    status = "Hey there! I'm using VTexter"
                )

                // Save to Room database
                viewModelScope.launch {
                    if (::repository.isInitialized) {
                        repository.saveUser(user)
                    }
                }

                // Save to Firebase Realtime Database for syncing
                val userData = mapOf(
                    "userId" to userId,
                    "name" to name,
                    "email" to email,
                    "profilePicture" to "",
                    "status" to "Hey there! I'm using VTexter",
                    "isOnline" to true,
                    "lastSeen" to System.currentTimeMillis()
                )

                database.getReference("users")
                    .child(userId)
                    .setValue(userData)
                    .addOnSuccessListener {
                        _authState.value = true
                        _errorMessage.value = ""

                        // Start syncing users
                        UserSyncManager.startSync(context)
                    }
                    .addOnFailureListener { e ->
                        _errorMessage.value = e.message ?: "Failed to save user data"
                    }
            }
            .addOnFailureListener { e ->
                _errorMessage.value = e.message ?: "Registration failed"
            }
    }

    fun setError(message: String) {
        _errorMessage.value = message
    }
}