package com.university.vtexter.utils

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.*

object StorageUtil {
    private val storage = FirebaseStorage.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    /**
     * Upload profile picture
     */
    suspend fun uploadProfilePicture(imageUri: Uri): Result<String> {
        return try {
            if (currentUserId == null) return Result.failure(Exception("Not authenticated"))

            val ref = storage.reference
                .child("profile_pictures")
                .child("$currentUserId.jpg")

            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload chat image
     */
    suspend fun uploadChatImage(imageUri: Uri, chatId: String): Result<String> {
        return try {
            if (currentUserId == null) return Result.failure(Exception("Not authenticated"))

            val fileName = "${UUID.randomUUID()}.jpg"
            val ref = storage.reference
                .child("chat_images")
                .child(chatId)
                .child(fileName)

            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload video
     */
    suspend fun uploadVideo(videoUri: Uri, chatId: String): Result<String> {
        return try {
            if (currentUserId == null) return Result.failure(Exception("Not authenticated"))

            val fileName = "${UUID.randomUUID()}.mp4"
            val ref = storage.reference
                .child("chat_videos")
                .child(chatId)
                .child(fileName)

            ref.putFile(videoUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload voice message
     */
    suspend fun uploadVoiceMessage(audioUri: Uri, chatId: String): Result<String> {
        return try {
            if (currentUserId == null) return Result.failure(Exception("Not authenticated"))

            val fileName = "${UUID.randomUUID()}.m4a"
            val ref = storage.reference
                .child("voice_messages")
                .child(chatId)
                .child(fileName)

            ref.putFile(audioUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload document
     */
    suspend fun uploadDocument(documentUri: Uri, chatId: String, fileName: String): Result<String> {
        return try {
            if (currentUserId == null) return Result.failure(Exception("Not authenticated"))

            val ref = storage.reference
                .child("documents")
                .child(chatId)
                .child(fileName)

            ref.putFile(documentUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload status image
     */
    suspend fun uploadStatusImage(imageUri: Uri): Result<String> {
        return try {
            if (currentUserId == null) return Result.failure(Exception("Not authenticated"))

            val fileName = "${System.currentTimeMillis()}.jpg"
            val ref = storage.reference
                .child("status_images")
                .child(currentUserId)
                .child(fileName)

            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete file from storage
     */
    suspend fun deleteFile(fileUrl: String): Result<Unit> {
        return try {
            val ref = storage.getReferenceFromUrl(fileUrl)
            ref.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}