package com.university.vtexter.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.university.vtexter.data.repository.VTexterRepository
import com.university.vtexter.models.Message
import com.university.vtexter.models.MessageType
import com.university.vtexter.utils.AudioRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ChatDetailViewModel : ViewModel() {
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var repository: VTexterRepository
    private var audioRecorder: AudioRecorder? = null

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

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    var tempCameraUri: Uri? = null

    fun initialize(context: Context) {
        repository = VTexterRepository(context)
        audioRecorder = AudioRecorder(context)
    }

    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            try {
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
            } catch (e: Exception) {
                Log.e("ChatDetailViewModel", "Error loading messages", e)
            }
        }
    }

    fun sendMessage(chatId: String, text: String) {
        if (currentUserId == null || text.isBlank()) return

        viewModelScope.launch {
            try {
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
                Log.d("ChatDetailViewModel", "Text message sent")
            } catch (e: Exception) {
                Log.e("ChatDetailViewModel", "Error sending message", e)
            }
        }
    }

    fun sendImageMessage(chatId: String, imageUri: Uri, context: Context) {
        if (currentUserId == null) return

        _isUploading.value = true

        viewModelScope.launch {
            try {
                val senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "User"
                repository.saveImageMessage(chatId, currentUserId, senderName, imageUri)
                    .onSuccess {
                        Log.d("ChatDetailViewModel", "Image sent successfully")
                        _isUploading.value = false
                    }
                    .onFailure { e ->
                        Log.e("ChatDetailViewModel", "Error sending image", e)
                        _isUploading.value = false
                    }
            } catch (e: Exception) {
                Log.e("ChatDetailViewModel", "Error sending image", e)
                _isUploading.value = false
            }
        }
    }

    fun sendVideoMessage(chatId: String, videoUri: Uri, context: Context) {
        if (currentUserId == null) return

        _isUploading.value = true

        viewModelScope.launch {
            try {
                val senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "User"
                repository.saveVideoMessage(chatId, currentUserId, senderName, videoUri)
                    .onSuccess {
                        Log.d("ChatDetailViewModel", "Video sent successfully")
                        _isUploading.value = false
                    }
                    .onFailure { e ->
                        Log.e("ChatDetailViewModel", "Error sending video", e)
                        _isUploading.value = false
                    }
            } catch (e: Exception) {
                Log.e("ChatDetailViewModel", "Error sending video", e)
                _isUploading.value = false
            }
        }
    }

    fun sendDocumentMessage(chatId: String, documentUri: Uri, context: Context) {
        if (currentUserId == null) return

        _isUploading.value = true

        viewModelScope.launch {
            try {
                val fileName = "document_${System.currentTimeMillis()}"
                val senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "User"
                repository.saveDocumentMessage(chatId, currentUserId, senderName, documentUri, fileName)
                    .onSuccess {
                        Log.d("ChatDetailViewModel", "Document sent successfully")
                        _isUploading.value = false
                    }
                    .onFailure { e ->
                        Log.e("ChatDetailViewModel", "Error sending document", e)
                        _isUploading.value = false
                    }
            } catch (e: Exception) {
                Log.e("ChatDetailViewModel", "Error sending document", e)
                _isUploading.value = false
            }
        }
    }

    // ==================== VOICE RECORDING ====================

    fun startRecording() {
        try {
            audioRecorder?.startRecording()
            _isRecording.value = true

            // Update duration every second
            viewModelScope.launch {
                while (_isRecording.value) {
                    kotlinx.coroutines.delay(1000)
                    _recordingDuration.value = audioRecorder?.getCurrentDuration() ?: 0
                }
            }

            Log.d("ChatDetailViewModel", "Voice recording started")
        } catch (e: Exception) {
            Log.e("ChatDetailViewModel", "Error starting recording", e)
            _isRecording.value = false
        }
    }

    fun stopRecordingAndSend(chatId: String) {
        if (currentUserId == null) return

        try {
            val result = audioRecorder?.stopRecording()
            _isRecording.value = false
            _recordingDuration.value = 0

            if (result != null) {
                val (audioFile, duration) = result
                if (audioFile != null && audioFile.exists()) {
                    _isUploading.value = true

                    viewModelScope.launch {
                        try {
                            val senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "User"
                            repository.saveVoiceMessage(chatId, currentUserId, senderName, audioFile)
                                .onSuccess {
                                    Log.d("ChatDetailViewModel", "Voice message sent: ${duration}s")
                                    _isUploading.value = false
                                }
                                .onFailure { e ->
                                    Log.e("ChatDetailViewModel", "Error sending voice message", e)
                                    _isUploading.value = false
                                }
                        } catch (e: Exception) {
                            Log.e("ChatDetailViewModel", "Error sending voice message", e)
                            _isUploading.value = false
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatDetailViewModel", "Error stopping recording", e)
            _isRecording.value = false
            _recordingDuration.value = 0
        }
    }

    fun cancelRecording() {
        try {
            audioRecorder?.cancelRecording()
            _isRecording.value = false
            _recordingDuration.value = 0
            Log.d("ChatDetailViewModel", "Voice recording cancelled")
        } catch (e: Exception) {
            Log.e("ChatDetailViewModel", "Error cancelling recording", e)
        }
    }

    // ==================== OTHER ====================

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
        _isTyping.value = isTyping
    }

    fun listenToTypingStatus(chatId: String) {
        // Typing status listening
    }

    fun listenToOnlineStatus(chatId: String) {
        viewModelScope.launch {
            try {
                repository.getChatById(chatId)?.let { chat ->
                    repository.getUserById(chat.otherUserId)?.let { user ->
                        _isOnline.value = user.isOnline
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatDetailViewModel", "Error listening to online status", e)
            }
        }
    }

    fun initiateCall(chatId: String, isVoiceOnly: Boolean) {
        // TODO: Implement call initiation
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up audio recorder
        if (_isRecording.value) {
            audioRecorder?.cancelRecording()
        }
    }
}