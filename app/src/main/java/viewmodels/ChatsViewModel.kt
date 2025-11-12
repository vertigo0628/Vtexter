package com.university.vtexter.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.university.vtexter.data.repository.VTexterRepository
import com.university.vtexter.models.Chat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatsViewModel : ViewModel() {
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var repository: VTexterRepository

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    fun initialize(context: Context) {
        repository = VTexterRepository(context)
        loadChats()
    }

    private fun loadChats() {
        if (currentUserId == null) return

        viewModelScope.launch {
            repository.getAllChats().collect { chatsList ->
                _chats.value = chatsList
            }
        }
    }
}