package com.university.vtexter.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.university.vtexter.models.Chat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatsViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        if (currentUserId == null) return

        firestore.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val chatsList = mutableListOf<Chat>()
                snapshots?.documents?.forEach { doc ->
                    val chat = doc.toObject(Chat::class.java)
                    if (chat != null) {
                        chatsList.add(chat.copy(chatId = doc.id))
                    }
                }
                _chats.value = chatsList
            }
    }
}