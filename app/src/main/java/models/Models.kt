package com.university.vtexter.models

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicture: String = "",
    val status: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0,
    val phoneNumber: String = ""
)

data class Chat(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val otherUserName: String = "",
    val otherUserId: String = "",
    val otherUserProfilePic: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val lastMessageType: MessageType = MessageType.TEXT,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val isTyping: Boolean = false
)

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val type: MessageType = MessageType.TEXT,
    val mediaUrl: String = "",
    val mediaThumbnail: String = "",
    val mediaSize: Long = 0,
    val mediaDuration: Long = 0, // For audio/video in seconds
    val fileName: String = "",
    val replyTo: String = "", // messageId of replied message
    val isRead: Boolean = false,
    val deliveredTo: List<String> = emptyList(),
    val readBy: List<String> = emptyList(),
    val isDeleted: Boolean = false,
    val reactions: Map<String, String> = emptyMap() // userId to emoji
)

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    LOCATION,
    CONTACT
}

data class Status(
    val statusId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfilePic: String = "",
    val mediaUrl: String = "",
    val mediaType: MediaType = MediaType.IMAGE,
    val caption: String = "",
    val timestamp: Long = 0,
    val expiresAt: Long = 0, // 24 hours from timestamp
    val views: List<String> = emptyList(), // List of userIds who viewed
    val backgroundColor: String = "#000000" // For text status
)

enum class MediaType {
    IMAGE,
    VIDEO,
    TEXT
}

data class CallRecord(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerProfilePic: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverProfilePic: String = "",
    val type: CallType = CallType.VOICE,
    val status: CallStatus = CallStatus.RINGING,
    val timestamp: Long = 0,
    val duration: Long = 0, // in seconds
    val isIncoming: Boolean = false
)

enum class CallType {
    VOICE,
    VIDEO
}

enum class CallStatus {
    RINGING,
    ACCEPTED,
    REJECTED,
    MISSED,
    ENDED,
    BUSY,
    NO_ANSWER
}

data class TypingStatus(
    val userId: String = "",
    val chatId: String = "",
    val isTyping: Boolean = false,
    val timestamp: Long = 0
)

data class OnlineStatus(
    val userId: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0
)

data class MessageReaction(
    val messageId: String = "",
    val userId: String = "",
    val emoji: String = "",
    val timestamp: Long = 0
)