package com.university.vtexter.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.university.vtexter.models.Chat
import com.university.vtexter.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ContactsViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private val _contacts = MutableStateFlow<List<User>>(emptyList())
    val contacts: StateFlow<List<User>> = _contacts.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        if (currentUserId == null) return

        firestore.collection("users")
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                val contactsList = mutableListOf<User>()
                snapshots?.documents?.forEach { doc ->
                    val user = doc.toObject(User::class.java)
                    if (user != null && user.userId != currentUserId) {
                        contactsList.add(user)
                    }
                }
                _contacts.value = contactsList.sortedBy { it.name }
            }
    }

    fun createOrOpenChat(otherUserId: String, onChatCreated: (String) -> Unit) {
        if (currentUserId == null) return

        val participants = listOf(currentUserId, otherUserId).sorted()

        // Check if chat already exists
        firestore.collection("chats")
            .whereEqualTo("participants", participants)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.documents.isNotEmpty()) {
                    // Chat exists
                    val chatId = querySnapshot.documents[0].id
                    onChatCreated(chatId)
                } else {
                    // Create new chat
                    val otherUser = _contacts.value.find { it.userId == otherUserId }

                    val chat = Chat(
                        chatId = "",
                        participants = participants,
                        otherUserName = otherUser?.name ?: "User",
                        otherUserId = otherUserId,
                        lastMessage = "",
                        lastMessageTime = System.currentTimeMillis(),
                        unreadCount = 0
                    )

                    firestore.collection("chats")
                        .add(chat)
                        .addOnSuccessListener { documentReference ->
                            onChatCreated(documentReference.id)
                        }
                }
            }
    }
}