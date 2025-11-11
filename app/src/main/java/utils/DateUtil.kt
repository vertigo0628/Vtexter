package com.university.vtexter.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtil {

    /**
     * Format timestamp for chat list (e.g., "10:30 AM", "Yesterday", "12/05/24")
     */
    fun formatChatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            // Today - show time
            diff < TimeUnit.DAYS.toMillis(1) && isSameDay(timestamp, now) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            // Yesterday
            diff < TimeUnit.DAYS.toMillis(2) && isYesterday(timestamp) -> {
                "Yesterday"
            }
            // This week - show day name
            diff < TimeUnit.DAYS.toMillis(7) -> {
                SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
            }
            // This year - show date without year
            isSameYear(timestamp, now) -> {
                SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
            }
            // Older - show full date
            else -> {
                SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    /**
     * Format timestamp for message (e.g., "10:30 AM")
     */
    fun formatMessageTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * Format timestamp with date header (e.g., "Today", "Yesterday", "December 5, 2024")
     */
    fun formatDateHeader(timestamp: Long): String {
        val now = System.currentTimeMillis()

        return when {
            isSameDay(timestamp, now) -> "Today"
            isYesterday(timestamp) -> "Yesterday"
            isSameYear(timestamp, now) -> {
                SimpleDateFormat("MMMM dd", Locale.getDefault()).format(Date(timestamp))
            }
            else -> {
                SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    /**
     * Format last seen time (e.g., "last seen today at 10:30 AM")
     */
    fun formatLastSeen(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

        return when {
            isSameDay(timestamp, now) -> "last seen today at $time"
            isYesterday(timestamp) -> "last seen yesterday at $time"
            else -> {
                val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
                "last seen $date at $time"
            }
        }
    }

    /**
     * Format call duration (e.g., "1:23", "10:05")
     */
    fun formatCallDuration(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%d:%02d", minutes, secs)
    }

    /**
     * Check if two timestamps are on the same day
     */
    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Check if timestamp was yesterday
     */
    private fun isYesterday(timestamp: Long): Boolean {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(timestamp, yesterday.timeInMillis)
    }

    /**
     * Check if two timestamps are in the same year
     */
    private fun isSameYear(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    /**
     * Get relative time (e.g., "2 minutes ago", "1 hour ago")
     */
    fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours ${if (hours == 1L) "hour" else "hours"} ago"
            }
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days ${if (days == 1L) "day" else "days"} ago"
            }
            else -> formatChatTime(timestamp)
        }
    }
}