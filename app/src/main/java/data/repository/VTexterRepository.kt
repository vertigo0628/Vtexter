package com.university.vtexter.data.repository

import android.content.Context
import android.net.Uri
import com.university.vtexter.data.local.VTexterDatabase
import com.university.vtexter.data.local.entities.*
import com.university.vtexter.models.*
import com.university.vtexter.utils.FileStorageUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

/**
 * Main Repository - Handles all data operations
 * Uses Room Database for local storage
 */
class VTexterRepository(private val context: Context) {

    private val database = VTexterDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()
    private val mediaDao = database.mediaDao()
    private val contactDao = database.contactDao()

    // ==================== USER OPERATIONS ====================

    suspend fun saveUser(user: User) {
        val entity = UserEntity(
            userId = user.userId,
            name = user.name,
            email = user.email,
            profilePicturePath = user.profilePicture,
            status = user.status,
            isOnline = user.isOnline,
            lastSeen = user.lastSeen,
            phoneNumber = user.phoneNumber
        )
        userDao.insertUser(entity)
    }

    suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId)?.toUser()
    }

    fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers().map { list ->
            list.map { it.toUser() }
        }
    }

    fun searchUsers(query: String): Flow<List<User>> {
        return userDao.searchUsers(query).map { list ->
            list.map { it.toUser() }
        }
    }

    suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean) {
        userDao.updateUserOnlineStatus(userId, isOnline, System.currentTimeMillis())
    }

    suspend fun saveProfilePicture(userId: String, imageUri: Uri): Result<String> {
        return FileStorageUtil.saveProfilePicture(context, imageUri, userId)
    }

    // ==================== CHAT OPERATIONS ====================

    suspend fun saveChat(chat: Chat) {
        val entity = ChatEntity(
            chatId = chat.chatId,
            otherUserId = chat.otherUserId,
            otherUserName = chat.otherUserName,
            otherUserProfilePic = chat.otherUserProfilePic,
            lastMessage = chat.lastMessage,
            lastMessageTime = chat.lastMessageTime,
            lastMessageType = chat.lastMessageType.name,
            unreadCount = chat.unreadCount,
            isPinned = chat.isPinned,
            isArchived = chat.isArchived,
            isMuted = chat.isMuted
        )
        chatDao.insertChat(entity)
    }

    suspend fun getChatById(chatId: String): Chat? {
        return chatDao.getChatById(chatId)?.toChat()
    }

    suspend fun getChatByUserId(userId: String): Chat? {
        return chatDao.getChatByUserId(userId)?.toChat()
    }

    fun getAllChats(): Flow<List<Chat>> {
        return chatDao.getActiveChats().map { list ->
            list.map { it.toChat() }
        }
    }

    suspend fun createOrGetChat(currentUserId: String, otherUserId: String, otherUserName: String): String {
        val existing = chatDao.getChatByUserId(otherUserId)
        if (existing != null) {
            return existing.chatId
        }

        val chatId = UUID.randomUUID().toString()
        val chat = ChatEntity(
            chatId = chatId,
            otherUserId = otherUserId,
            otherUserName = otherUserName,
            otherUserProfilePic = "",
            lastMessage = "",
            lastMessageTime = System.currentTimeMillis(),
            lastMessageType = "TEXT",
            unreadCount = 0
        )
        chatDao.insertChat(chat)
        return chatId
    }

    suspend fun updateLastMessage(chatId: String, message: String, type: MessageType) {
        chatDao.updateLastMessage(chatId, message, System.currentTimeMillis(), type.name)
    }

    suspend fun clearUnreadCount(chatId: String) {
        chatDao.clearUnreadCount(chatId)
    }

    // ==================== MESSAGE OPERATIONS ====================

    suspend fun saveMessage(message: Message, chatId: String) {
        val entity = MessageEntity(
            messageId = message.messageId.ifEmpty { UUID.randomUUID().toString() },
            chatId = chatId,
            senderId = message.senderId,
            senderName = message.senderName,
            text = message.text,
            timestamp = message.timestamp,
            type = message.type.name,
            mediaPath = message.mediaUrl,
            mediaThumbnailPath = message.mediaThumbnail,
            mediaSize = message.mediaSize,
            mediaDuration = message.mediaDuration,
            fileName = message.fileName,
            isRead = message.isRead,
            isDelivered = true,
            isSent = true
        )
        messageDao.insertMessage(entity)

        // Update chat's last message
        val lastMessageText = when (message.type) {
            MessageType.TEXT -> message.text
            MessageType.IMAGE -> "📷 Photo"
            MessageType.VIDEO -> "🎥 Video"
            MessageType.AUDIO -> "🎤 Voice message"
            MessageType.DOCUMENT -> "📄 ${message.fileName}"
            else -> "Message"
        }
        updateLastMessage(chatId, lastMessageText, message.type)
    }

    fun getMessagesByChatId(chatId: String): Flow<List<Message>> {
        return messageDao.getActiveMessagesByChatId(chatId).map { list ->
            list.map { it.toMessage() }
        }
    }

    suspend fun markMessagesAsRead(chatId: String, currentUserId: String) {
        messageDao.markMessagesAsRead(chatId, currentUserId)
        clearUnreadCount(chatId)
    }

    // ==================== MEDIA OPERATIONS ====================

    suspend fun saveImageMessage(chatId: String, senderId: String, senderName: String, imageUri: Uri): Result<Unit> {
        return try {
            // Save image file
            val result = FileStorageUtil.saveImage(context, imageUri, senderId)
            result.fold(
                onSuccess = { filePath ->
                    // Create message
                    val message = Message(
                        messageId = UUID.randomUUID().toString(),
                        senderId = senderId,
                        senderName = senderName,
                        text = "",
                        timestamp = System.currentTimeMillis(),
                        type = MessageType.IMAGE,
                        mediaUrl = filePath,
                        mediaSize = FileStorageUtil.getFileSize(filePath)
                    )
                    saveMessage(message, chatId)

                    // Save media entity
                    val media = MediaFileEntity(
                        mediaId = UUID.randomUUID().toString(),
                        messageId = message.messageId,
                        filePath = filePath,
                        fileType = "IMAGE",
                        fileName = "image_${System.currentTimeMillis()}.jpg",
                        fileSize = FileStorageUtil.getFileSize(filePath),
                        mimeType = "image/jpeg",
                        timestamp = System.currentTimeMillis()
                    )
                    mediaDao.insertMedia(media)

                    Result.success(Unit)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveVideoMessage(chatId: String, senderId: String, senderName: String, videoUri: Uri): Result<Unit> {
        return try {
            val result = FileStorageUtil.saveVideo(context, videoUri, senderId)
            result.fold(
                onSuccess = { (videoPath, thumbPath) ->
                    val message = Message(
                        messageId = UUID.randomUUID().toString(),
                        senderId = senderId,
                        senderName = senderName,
                        text = "",
                        timestamp = System.currentTimeMillis(),
                        type = MessageType.VIDEO,
                        mediaUrl = videoPath,
                        mediaThumbnail = thumbPath,
                        mediaSize = FileStorageUtil.getFileSize(videoPath)
                    )
                    saveMessage(message, chatId)

                    val media = MediaFileEntity(
                        mediaId = UUID.randomUUID().toString(),
                        messageId = message.messageId,
                        filePath = videoPath,
                        fileType = "VIDEO",
                        fileName = "video_${System.currentTimeMillis()}.mp4",
                        fileSize = FileStorageUtil.getFileSize(videoPath),
                        mimeType = "video/mp4",
                        thumbnailPath = thumbPath,
                        timestamp = System.currentTimeMillis()
                    )
                    mediaDao.insertMedia(media)

                    Result.success(Unit)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveDocumentMessage(chatId: String, senderId: String, senderName: String, documentUri: Uri, fileName: String): Result<Unit> {
        return try {
            val result = FileStorageUtil.saveDocument(context, documentUri, senderId, fileName)
            result.fold(
                onSuccess = { filePath ->
                    val message = Message(
                        messageId = UUID.randomUUID().toString(),
                        senderId = senderId,
                        senderName = senderName,
                        text = "",
                        timestamp = System.currentTimeMillis(),
                        type = MessageType.DOCUMENT,
                        mediaUrl = filePath,
                        fileName = fileName,
                        mediaSize = FileStorageUtil.getFileSize(filePath)
                    )
                    saveMessage(message, chatId)

                    val media = MediaFileEntity(
                        mediaId = UUID.randomUUID().toString(),
                        messageId = message.messageId,
                        filePath = filePath,
                        fileType = "DOCUMENT",
                        fileName = fileName,
                        fileSize = FileStorageUtil.getFileSize(filePath),
                        mimeType = FileStorageUtil.getMimeType(filePath),
                        timestamp = System.currentTimeMillis()
                    )
                    mediaDao.insertMedia(media)

                    Result.success(Unit)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== CONTACT OPERATIONS ====================

    suspend fun saveContact(user: User) {
        val entity = ContactEntity(
            contactId = UUID.randomUUID().toString(),
            userId = user.userId,
            name = user.name,
            email = user.email,
            phoneNumber = user.phoneNumber,
            profilePicturePath = user.profilePicture,
            addedAt = System.currentTimeMillis()
        )
        contactDao.insertContact(entity)
    }

    fun getAllContacts(): Flow<List<User>> {
        return contactDao.getAllContacts().map { list ->
            list.map { it.toUser() }
        }
    }

    // ==================== EXTENSION FUNCTIONS ====================

    private fun UserEntity.toUser() = User(
        userId = userId,
        name = name,
        email = email,
        profilePicture = profilePicturePath,
        status = status,
        isOnline = isOnline,
        lastSeen = lastSeen,
        phoneNumber = phoneNumber
    )

    private fun ChatEntity.toChat() = Chat(
        chatId = chatId,
        participants = listOf(otherUserId),
        otherUserName = otherUserName,
        otherUserId = otherUserId,
        otherUserProfilePic = otherUserProfilePic,
        lastMessage = lastMessage,
        lastMessageTime = lastMessageTime,
        lastMessageType = MessageType.valueOf(lastMessageType),
        unreadCount = unreadCount,
        isPinned = isPinned,
        isArchived = isArchived,
        isMuted = isMuted
    )

    private fun MessageEntity.toMessage() = Message(
        messageId = messageId,
        senderId = senderId,
        senderName = senderName,
        text = text,
        timestamp = timestamp,
        type = MessageType.valueOf(type),
        mediaUrl = mediaPath,
        mediaThumbnail = mediaThumbnailPath,
        mediaSize = mediaSize,
        mediaDuration = mediaDuration,
        fileName = fileName,
        isRead = isRead
    )

    private fun ContactEntity.toUser() = User(
        userId = userId,
        name = name,
        email = email,
        profilePicture = profilePicturePath,
        phoneNumber = phoneNumber
    )
}