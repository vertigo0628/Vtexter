package com.university.vtexter.viewmodels

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.university.vtexter.data.repository.VTexterRepository
import com.university.vtexter.models.Message
import com.university.vtexter.models.MessageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ChatDetailViewModel : ViewModel() {
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var repository: VTexterRepository

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _otherUserName = MutableStateFlow("")
    val otherUserName: StateFlow<String> = _otherUserName.asStateFlow()

    private val _otherUserProfilePic = MutableStateFlow("")
    val otherUserProfilePic: StateFlow<String> = _otherUserProfilePic.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    var tempCameraUri: Uri? = null

    fun initialize(context: Context) {
        repository = VTexterRepository(context)
    }

    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            // Load chat details
            repository.getChatById(chatId)?.let { chat ->
                _otherUserName.value = chat.otherUserName
                _otherUserProfilePic.value = chat.otherUserProfilePic
            }

            // Load other user's info
            repository.getChatById(chatId)?.let { chat ->
                repository.getUserById(chat.otherUserId)?.let { user ->
                    _isOnline.value = user.isOnline
                }
            }

            // Load messages from Room database
            repository.getMessagesByChatId(chatId).collect { messagesList ->
                _messages.value = messagesList
            }

            // Mark messages as read
            if (currentUserId != null) {
                repository.markMessagesAsRead(chatId, currentUserId)
            }
        }
    }

    fun sendMessage(chatId: String, text: String) {
        if (currentUserId == null || text.isBlank()) return

        viewModelScope.launch {
            val message = Message(
                messageId = UUID.randomUUID().toString(),
                senderId = currentUserId,
                senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "User",
                text = text.trim(),
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT,
                isRead = false
            )

            repository.saveMessage(message, chatId)
        }
    }

    fun sendImageMessage(chatId: String, imageUri: Uri, context: Context) {
        if (currentUserId == null) return

        _isUploading.value = true

        viewModelScope.launch {
            val senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "User"
            repository.saveImageMessage(chatId, currentUserId, senderName, imageUri)
                .onSuccess {
                    _isUploading.value = false
                }
                .onFailure {
                    _isUploading.value = false
                }
        }
    }

    fun sendVideoMessage(chatId: String, videoUri: Uri, context: Context) {
        if (currentUserId == null) return

        _isUploading.value = true

        viewModelScope.launch {
            val senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "User"
            repository.saveVideoMessage(chatId, currentUserId, senderName, videoUri)
                .onSuccess {
                    _isUploading.value = false
                }
                .onFailure {
                    _isUploading.value = false
                }
        }
    }

    fun sendDocumentMessage(chatId: String, documentUri: Uri, context: Context) {
        if (currentUserId == null) return

        _isUploading.value = true

        viewModelScope.launch {
            val fileName = "document_${System.currentTimeMillis()}"
            val senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "User"
            repository.saveDocumentMessage(chatId, currentUserId, senderName, documentUri, fileName)
                .onSuccess {
                    _isUploading.value = false
                }
                .onFailure {
                    _isUploading.value = false
                }
        }
    }

    fun createCameraImageUri(context: Context): Uri {
        val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        tempCameraUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return tempCameraUri!!
    }

    fun updateTypingStatus(chatId: String, isTyping: Boolean) {
        // Typing status can be implemented with Room if needed
        _isTyping.value = isTyping
    }

    fun listenToTypingStatus(chatId: String) {
        // Typing status listening
    }

    fun listenToOnlineStatus(chatId: String) {
        viewModelScope.launch {
            repository.getChatById(chatId)?.let { chat ->
                repository.getUserById(chat.otherUserId)?.let { user ->
                    _isOnline.value = user.isOnline
                }
            }
        }
    }

    fun initiateCall(chatId: String, isVoiceOnly: Boolean) {
        // TODO: Implement call initiation
    }
}