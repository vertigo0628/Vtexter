package com.university.vtexter.data.local.dao

import androidx.room.*
import com.university.vtexter.data.local.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * USER DAO - User database operations
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE name LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%'")
    fun searchUsers(query: String): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isOnline = :isOnline, lastSeen = :lastSeen WHERE userId = :userId")
    suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean, lastSeen: Long)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}

/**
 * CHAT DAO - Chat operations
 */
@Dao
interface ChatDao {
    @Query("SELECT * FROM chats WHERE chatId = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 0 ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getActiveChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 1 ORDER BY lastMessageTime DESC")
    fun getArchivedChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE otherUserId = :userId")
    suspend fun getChatByUserId(userId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("UPDATE chats SET lastMessage = :message, lastMessageTime = :time, lastMessageType = :type WHERE chatId = :chatId")
    suspend fun updateLastMessage(chatId: String, message: String, time: Long, type: String)

    @Query("UPDATE chats SET unreadCount = unreadCount + 1 WHERE chatId = :chatId")
    suspend fun incrementUnreadCount(chatId: String)

    @Query("UPDATE chats SET unreadCount = 0 WHERE chatId = :chatId")
    suspend fun clearUnreadCount(chatId: String)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE chatId = :chatId")
    suspend fun updatePinStatus(chatId: String, isPinned: Boolean)

    @Delete
    suspend fun deleteChat(chat: ChatEntity)
}

/**
 * MESSAGE DAO - Message operations
 */
@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE messageId = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesByChatId(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND isDeleted = 0 ORDER BY timestamp ASC")
    fun getActiveMessagesByChatId(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessage(chatId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND isRead = 0 AND senderId != :currentUserId")
    suspend fun getUnreadMessages(chatId: String, currentUserId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE type != 'TEXT' AND chatId = :chatId ORDER BY timestamp DESC")
    fun getMediaMessages(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET isRead = 1 WHERE chatId = :chatId AND senderId != :currentUserId AND isRead = 0")
    suspend fun markMessagesAsRead(chatId: String, currentUserId: String)

    @Query("UPDATE messages SET isDelivered = 1 WHERE messageId = :messageId")
    suspend fun markAsDelivered(messageId: String)

    @Query("UPDATE messages SET isSent = 1 WHERE messageId = :messageId")
    suspend fun markAsSent(messageId: String)

    @Query("UPDATE messages SET isDeleted = 1 WHERE messageId = :messageId")
    suspend fun markAsDeleted(messageId: String)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteAllMessagesInChat(chatId: String)
}

/**
 * MEDIA DAO - Media file operations
 */
@Dao
interface MediaDao {
    @Query("SELECT * FROM media_files WHERE mediaId = :mediaId")
    suspend fun getMediaById(mediaId: String): MediaFileEntity?

    @Query("SELECT * FROM media_files WHERE messageId = :messageId")
    suspend fun getMediaByMessageId(messageId: String): MediaFileEntity?

    @Query("SELECT * FROM media_files WHERE fileType = :type ORDER BY timestamp DESC")
    fun getMediaByType(type: String): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files ORDER BY timestamp DESC")
    fun getAllMedia(): Flow<List<MediaFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaFileEntity)

    @Update
    suspend fun updateMedia(media: MediaFileEntity)

    @Delete
    suspend fun deleteMedia(media: MediaFileEntity)

    @Query("DELETE FROM media_files WHERE messageId = :messageId")
    suspend fun deleteMediaByMessageId(messageId: String)
}

/**
 * CONTACT DAO - Contact operations
 */
@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE userId = :userId")
    suspend fun getContactByUserId(userId: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%'")
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)
}