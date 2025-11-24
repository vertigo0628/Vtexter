package com.university.vtexter.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * Local File Storage Utility
 * Handles saving and retrieving media files locally
 */
object FileStorageUtil {

    // Storage directories
    private const val IMAGES_DIR = "images"
    private const val VIDEOS_DIR = "videos"
    private const val AUDIO_DIR = "audio"
    private const val DOCUMENTS_DIR = "documents"
    private const val THUMBNAILS_DIR = "thumbnails"
    private const val PROFILE_PICS_DIR = "profile_pictures"

    /**
     * Save image to local storage
     */
    suspend fun saveImage(context: Context, imageUri: Uri, userId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                // Create directory
                val imagesDir = File(context.filesDir, IMAGES_DIR)
                if (!imagesDir.exists()) imagesDir.mkdirs()

                // Generate unique filename
                val fileName = "${userId}_${System.currentTimeMillis()}.jpg"
                val imageFile = File(imagesDir, fileName)

                // Compress and save
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }

                Result.success(imageFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Save video to local storage with thumbnail
     */
    suspend fun saveVideo(context: Context, videoUri: Uri, userId: String): Result<Pair<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                // Copy video file
                val inputStream = context.contentResolver.openInputStream(videoUri)
                val videosDir = File(context.filesDir, VIDEOS_DIR)
                if (!videosDir.exists()) videosDir.mkdirs()

                val fileName = "${userId}_${System.currentTimeMillis()}.mp4"
                val videoFile = File(videosDir, fileName)

                inputStream?.use { input ->
                    FileOutputStream(videoFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Generate thumbnail
                val thumbnailPath = generateVideoThumbnail(context, videoFile.absolutePath)

                Result.success(Pair(videoFile.absolutePath, thumbnailPath))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Generate video thumbnail
     */
    private suspend fun generateVideoThumbnail(context: Context, videoPath: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(videoPath)

                // Get frame at 1 second
                val bitmap = retriever.getFrameAtTime(1000000) // 1 second in microseconds
                retriever.release()

                if (bitmap != null) {
                    val thumbDir = File(context.filesDir, THUMBNAILS_DIR)
                    if (!thumbDir.exists()) thumbDir.mkdirs()

                    val fileName = "thumb_${System.currentTimeMillis()}.jpg"
                    val thumbFile = File(thumbDir, fileName)

                    FileOutputStream(thumbFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    }

                    thumbFile.absolutePath
                } else {
                    ""
                }
            } catch (e: Exception) {
                ""
            }
        }
    }

    /**
     * Save audio file (voice message) to local storage
     */
    suspend fun saveAudio(context: Context, audioFile: File, userId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val audioDir = File(context.filesDir, AUDIO_DIR)
                if (!audioDir.exists()) audioDir.mkdirs()

                val fileName = "${userId}_${System.currentTimeMillis()}.m4a"
                val destFile = File(audioDir, fileName)

                // Copy file to permanent storage
                audioFile.copyTo(destFile, overwrite = true)

                // Delete temp file
                audioFile.delete()

                Result.success(destFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Save document to local storage
     */
    suspend fun saveDocument(context: Context, documentUri: Uri, userId: String, fileName: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(documentUri)
                val docsDir = File(context.filesDir, DOCUMENTS_DIR)
                if (!docsDir.exists()) docsDir.mkdirs()

                val uniqueFileName = "${userId}_${System.currentTimeMillis()}_$fileName"
                val docFile = File(docsDir, uniqueFileName)

                inputStream?.use { input ->
                    FileOutputStream(docFile).use { output ->
                        input.copyTo(output)
                    }
                }

                Result.success(docFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Save profile picture
     */
    suspend fun saveProfilePicture(context: Context, imageUri: Uri, userId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                val profileDir = File(context.filesDir, PROFILE_PICS_DIR)
                if (!profileDir.exists()) profileDir.mkdirs()

                val fileName = "${userId}_profile.jpg"
                val imageFile = File(profileDir, fileName)

                // Compress and save
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }

                Result.success(imageFile.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Get file from path
     */
    fun getFile(filePath: String): File? {
        return try {
            val file = File(filePath)
            if (file.exists()) file else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Delete file
     */
    suspend fun deleteFile(filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                file.delete()
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Get file size
     */
    fun getFileSize(filePath: String): Long {
        return try {
            File(filePath).length()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get video duration in seconds
     */
    fun getVideoDuration(filePath: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            (duration?.toLongOrNull() ?: 0) / 1000 // Convert to seconds
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get audio duration in seconds
     */
    fun getAudioDuration(filePath: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            (duration?.toLongOrNull() ?: 0) / 1000 // Convert to seconds
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get mime type from file path
     */
    fun getMimeType(filePath: String): String {
        return when (File(filePath).extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "mp4", "mov" -> "video/mp4"
            "m4a", "mp3" -> "audio/mp4"
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/octet-stream"
        }
    }

    /**
     * Get storage usage
     */
    fun getStorageUsage(context: Context): Long {
        var totalSize = 0L
        val dirs = listOf(IMAGES_DIR, VIDEOS_DIR, AUDIO_DIR, DOCUMENTS_DIR, THUMBNAILS_DIR, PROFILE_PICS_DIR)

        dirs.forEach { dirName ->
            val dir = File(context.filesDir, dirName)
            if (dir.exists()) {
                totalSize += dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            }
        }

        return totalSize
    }

    /**
     * Clear all storage
     */
    suspend fun clearAllStorage(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val dirs = listOf(IMAGES_DIR, VIDEOS_DIR, AUDIO_DIR, DOCUMENTS_DIR, THUMBNAILS_DIR, PROFILE_PICS_DIR)

                dirs.forEach { dirName ->
                    val dir = File(context.filesDir, dirName)
                    if (dir.exists()) {
                        dir.deleteRecursively()
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}