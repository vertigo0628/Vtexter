package com.university.vtexter.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.university.vtexter.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow(false)
    val authState: StateFlow<Boolean> = _authState.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

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
            }
            .addOnFailureListener { e ->
                _errorMessage.value = e.message ?: "Login failed"
            }
    }

    fun register(name: String, email: String, password: String) {
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

                // Save user to Firestore
                val user = User(
                    userId = userId,
                    name = name,
                    email = email,
                    profilePicture = "",
                    status = "Hey there! I'm using VTexter"
                )

                firestore.collection("users")
                    .document(userId)
                    .set(user)
                    .addOnSuccessListener {
                        _authState.value = true
                        _errorMessage.value = ""
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