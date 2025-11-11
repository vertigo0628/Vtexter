package com.university.vtexter.viewmodels

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.university.vtexter.models.Message
import com.university.vtexter.models.MessageType
import com.university.vtexter.utils.StorageUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ChatDetailViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

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

    fun loadMessages(chatId: String) {
        // Load other user's info
        firestore.collection("chats").document(chatId)
            .get()
            .addOnSuccessListener { doc ->
                val participants = doc.get("participants") as? List<String>
                val otherUserId = participants?.find { it != currentUserId }

                if (otherUserId != null) {
                    firestore.collection("users").document(otherUserId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            _otherUserName.value = userDoc.getString("name") ?: "User"
                            _otherUserProfilePic.value = userDoc.getString("profilePicture") ?: ""
                        }
                }
            }

        // Load messages with real-time updates
        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                val messagesList = mutableListOf<Message>()
                snapshots?.documents?.forEach { doc ->
                    val message = doc.toObject(Message::class.java)
                    if (message != null && !message.isDeleted) {
                        messagesList.add(message.copy(messageId = doc.id))
                    }
                }
                _messages.value = messagesList

                // Mark messages as read
                markMessagesAsRead(chatId)
            }
    }

    fun sendMessage(chatId: String, text: String) {
        if (currentUserId == null || text.isBlank()) return

        val message = Message(
            messageId = "",
            senderId = currentUserId,
            senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "User",
            text = text.trim(),
            timestamp = System.currentTimeMillis(),
            type = MessageType.TEXT,
            isRead = false
        )

        sendMessageToFirestore(chatId, message, text.trim())
    }

    fun sendImageMessage(chatId: String, imageUri: Uri, context: Context) {
        if (currentUserId == null) return

        _isUploading.value = true

        viewModelScope.launch {
            val result = StorageUtil.uploadChatImage(imageUri, chatId)
            result.onSuccess { downloadUrl ->
                val message = Message(
                    messageId = "",
                    senderId = currentUserId,
                    senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "User",
                    text = "",
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.IMAGE,
                    mediaUrl = downloadUrl,
                    isRead = false
                )

                sendMessageToFirestore(chatId, message, "📷 Image")
                _isUploading.value = false
            }.onFailure {
                _isUploading.value = false
            }
        }
    }

    fun sendVideoMessage(chatId: String, videoUri: Uri, context: Context) {
        if (currentUserId == null) return

        _isUploading.value = true

        viewModelScope.launch {
            val result = StorageUtil.uploadVideo(videoUri, chatId)
            result.onSuccess { downloadUrl ->
                val message = Message(
                    messageId = "",
                    senderId = currentUserId,
                    senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "User",
                    text = "",
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.VIDEO,
                    mediaUrl = downloadUrl,
                    isRead = false
                )

                sendMessageToFirestore(chatId, message, "🎥 Video")
                _isUploading.value = false
            }.onFailure {
                _isUploading.value = false
            }
        }
    }

    fun sendDocumentMessage(chatId: String, documentUri: Uri, context: Context) {
        if (currentUserId == null) return

        _isUploading.value = true

        viewModelScope.launch {
            val fileName = "document_${System.currentTimeMillis()}"
            val result = StorageUtil.uploadDocument(documentUri, chatId, fileName)
            result.onSuccess { downloadUrl ->
                val message = Message(
                    messageId = "",
                    senderId = currentUserId,
                    senderName = FirebaseAuth.getInstance().currentUser?.displayName ?: "User",
                    text = "",
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.DOCUMENT,
                    mediaUrl = downloadUrl,
                    fileName = fileName,
                    isRead = false
                )

                sendMessageToFirestore(chatId, message, "📄 Document")
                _isUploading.value = false
            }.onFailure {
                _isUploading.value = false
            }
        }
    }

    private fun sendMessageToFirestore(chatId: String, message: Message, lastMessageText: String) {
        // Add message to subcollection
        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                // Update chat's last message info
                firestore.collection("chats")
                    .document(chatId)
                    .update(
                        mapOf(
                            "lastMessage" to lastMessageText,
                            "lastMessageTime" to System.currentTimeMillis(),
                            "lastMessageType" to message.type.name
                        )
                    )
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
        if (currentUserId == null) return

        firestore.collection("chats")
            .document(chatId)
            .update("${currentUserId}_typing", isTyping)
    }

    fun listenToTypingStatus(chatId: String) {
        firestore.collection("chats")
            .document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val participants = snapshot.get("participants") as? List<String>
                val otherUserId = participants?.find { it != currentUserId }

                if (otherUserId != null) {
                    val isTyping = snapshot.getBoolean("${otherUserId}_typing") ?: false
                    _isTyping.value = isTyping
                }
            }
    }

    fun listenToOnlineStatus(chatId: String) {
        firestore.collection("chats")
            .document(chatId)
            .get()
            .addOnSuccessListener { doc ->
                val participants = doc.get("participants") as? List<String>
                val otherUserId = participants?.find { it != currentUserId }

                if (otherUserId != null) {
                    firestore.collection("users")
                        .document(otherUserId)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null || snapshot == null) return@addSnapshotListener
                            _isOnline.value = snapshot.getBoolean("isOnline") ?: false
                        }
                }
            }
    }

    private fun markMessagesAsRead(chatId: String) {
        if (currentUserId == null) return

        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .whereEqualTo("isRead", false)
            .whereNotEqualTo("senderId", currentUserId)
            .get()
            .addOnSuccessListener { snapshots ->
                snapshots.documents.forEach { doc ->
                    doc.reference.update("isRead", true)
                }
            }
    }

    fun initiateCall(chatId: String, isVoiceOnly: Boolean) {
        // TODO: Implement call initiation using CallManager
        // This will be implemented in the CallScreen
    }
}