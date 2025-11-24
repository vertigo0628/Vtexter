package com.university.vtexter.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Audio Recorder for Voice Messages
 * Records audio and saves as .m4a files
 */
class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false
    private var startTime: Long = 0

    /**
     * Start recording audio
     */
    fun startRecording(): File? {
        return try {
            // Create output file
            val audioDir = File(context.cacheDir, "audio_temp")
            if (!audioDir.exists()) audioDir.mkdirs()

            outputFile = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)

                prepare()
                start()

                isRecording = true
                startTime = System.currentTimeMillis()

                Log.d("AudioRecorder", "Recording started: ${outputFile?.absolutePath}")
            }

            outputFile
        } catch (e: IOException) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            null
        }
    }

    /**
     * Stop recording and return the file with duration
     */
    fun stopRecording(): Pair<File?, Long>? {
        return try {
            if (!isRecording || mediaRecorder == null) {
                return null
            }

            mediaRecorder?.apply {
                stop()
                release()
            }

            val duration = (System.currentTimeMillis() - startTime) / 1000 // in seconds
            isRecording = false

            Log.d("AudioRecorder", "Recording stopped. Duration: ${duration}s")

            Pair(outputFile, duration)
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop recording", e)
            null
        } finally {
            mediaRecorder = null
        }
    }

    /**
     * Cancel recording and delete file
     */
    fun cancelRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
            }

            outputFile?.delete()
            outputFile = null

            Log.d("AudioRecorder", "Recording cancelled")
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error cancelling recording", e)
        }
    }

    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Get current recording duration
     */
    fun getCurrentDuration(): Long {
        return if (isRecording) {
            (System.currentTimeMillis() - startTime) / 1000
        } else {
            0
        }
    }
}
