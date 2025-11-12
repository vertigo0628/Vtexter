package com.university.vtexter.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * USER ENTITY - Stores user information locally
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val name: String,
    val email: String,
    val profilePicturePath: String = "", // Local file path
    val status: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0,
    val phoneNumber: String = ""
)

/**
 * CHAT ENTITY - Stores chat conversations
 */
@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val chatId: String,
    val otherUserId: String,
    val otherUserName: String,
    val otherUserProfilePic: String = "", // Local file path
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val lastMessageType: String = "TEXT",
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false
)

/**
 * MESSAGE ENTITY - Stores individual messages with media
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val messageId: String,
    val chatId: String, // Foreign key to chat
    val senderId: String,
    val senderName: String,
    val text: String = "",
    val timestamp: Long,
    val type: String = "TEXT", // TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT
    val mediaPath: String = "", // Local file path for media
    val mediaThumbnailPath: String = "", // Thumbnail for videos
    val mediaSize: Long = 0,
    val mediaDuration: Long = 0,
    val fileName: String = "",
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val isSent: Boolean = false,
    val isDeleted: Boolean = false
)

/**
 * MEDIA ENTITY - Stores media files (images, videos, documents)
 */
@Entity(tableName = "media_files")
data class MediaFileEntity(
    @PrimaryKey
    val mediaId: String,
    val messageId: String, // Link to message
    val filePath: String, // Local storage path
    val fileType: String, // IMAGE, VIDEO, AUDIO, DOCUMENT
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val thumbnailPath: String = "",
    val duration: Long = 0, // For audio/video in seconds
    val width: Int = 0, // For images/videos
    val height: Int = 0,
    val timestamp: Long
)

/**
 * CONTACT ENTITY - Stores contacts locally
 */
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val contactId: String,
    val userId: String, // Firebase user ID
    val name: String,
    val email: String,
    val phoneNumber: String = "",
    val profilePicturePath: String = "",
    val addedAt: Long
)

/**
 * TYPE CONVERTERS - For complex data types
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromStringMap(value: String): Map<String, String> {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType)
    }

    @TypeConverter
    fun toStringMap(map: Map<String, String>): String {
        return gson.toJson(map)
    }
}