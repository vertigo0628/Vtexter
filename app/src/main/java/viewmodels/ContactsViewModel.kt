package com.university.vtexter.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.university.vtexter.data.repository.VTexterRepository
import com.university.vtexter.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactsViewModel : ViewModel() {
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val database = FirebaseDatabase.getInstance()
    private lateinit var repository: VTexterRepository

    private val _contacts = MutableStateFlow<List<User>>(emptyList())
    val contacts: StateFlow<List<User>> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun initialize(context: Context) {
        repository = VTexterRepository(context)
        loadContactsFromFirebase()
        loadContactsFromRoom()
    }

    /**
     * Load contacts directly from Firebase Realtime Database
     */
    private fun loadContactsFromFirebase() {
        if (currentUserId == null) {
            _isLoading.value = false
            return
        }

        Log.d("ContactsViewModel", "Loading contacts from Firebase...")

        database.getReference("users")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("ContactsViewModel", "Firebase data received: ${snapshot.childrenCount} users")

                    val usersList = mutableListOf<User>()

                    snapshot.children.forEach { userSnapshot ->
                        try {
                            val userId = userSnapshot.key ?: return@forEach

                            // Skip current user
                            if (userId == currentUserId) return@forEach

                            val name = userSnapshot.child("name").getValue(String::class.java) ?: ""
                            val email = userSnapshot.child("email").getValue(String::class.java) ?: ""
                            val status = userSnapshot.child("status").getValue(String::class.java) ?: ""
                            val profilePic = userSnapshot.child("profilePicture").getValue(String::class.java) ?: ""

                            if (name.isNotEmpty() && email.isNotEmpty()) {
                                val user = User(
                                    userId = userId,
                                    name = name,
                                    email = email,
                                    profilePicture = profilePic,
                                    status = status
                                )

                                usersList.add(user)

                                // Also save to Room for offline access
                                viewModelScope.launch {
                                    try {
                                        repository.saveUser(user)
                                        repository.saveContact(user)
                                    } catch (e: Exception) {
                                        Log.e("ContactsViewModel", "Error saving user to Room", e)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ContactsViewModel", "Error parsing user", e)
                        }
                    }

                    Log.d("ContactsViewModel", "Loaded ${usersList.size} contacts")
                    _contacts.value = usersList.sortedBy { it.name }
                    _isLoading.value = false
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ContactsViewModel", "Firebase error: ${error.message}")
                    _isLoading.value = false
                    // Fallback to Room
                    loadContactsFromRoom()
                }
            })
    }

    /**
     * Load contacts from Room database (offline fallback)
     */
    private fun loadContactsFromRoom() {
        if (currentUserId == null) return

        viewModelScope.launch {
            try {
                repository.getAllContacts().collect { contactsList ->
                    if (_contacts.value.isEmpty()) {
                        // Only use Room data if Firebase didn't load anything
                        _contacts.value = contactsList.filter { it.userId != currentUserId }
                    }
                }
            } catch (e: Exception) {
                Log.e("ContactsViewModel", "Error loading from Room", e)
            }
        }
    }

    fun createOrOpenChat(otherUserId: String, onChatCreated: (String) -> Unit) {
        if (currentUserId == null) return

        viewModelScope.launch {
            try {
                // Find the user
                val otherUser = _contacts.value.find { it.userId == otherUserId }
                val otherUserName = otherUser?.name ?: "User"

                // Check if chat exists
                val existingChat = repository.getChatByUserId(otherUserId)
                if (existingChat != null) {
                    onChatCreated(existingChat.chatId)
                } else {
                    // Create new chat
                    val chatId = repository.createOrGetChat(currentUserId, otherUserId, otherUserName)
                    onChatCreated(chatId)
                }
            } catch (e: Exception) {
                Log.e("ContactsViewModel", "Error creating chat", e)
            }
        }
    }
}