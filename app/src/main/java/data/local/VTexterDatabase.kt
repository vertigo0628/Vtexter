package com.university.vtexter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.university.vtexter.data.local.dao.*
import com.university.vtexter.data.local.entities.*

/**
 * VTexter Room Database
 * Local storage for all app data
 */
@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        MediaFileEntity::class,
        ContactEntity::class
    ],
    version = 1,
    exportSchema = false // CHANGED: Disabled schema export
)
@TypeConverters(Converters::class)
abstract class VTexterDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun mediaDao(): MediaDao
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: VTexterDatabase? = null

        fun getDatabase(context: Context): VTexterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VTexterDatabase::class.java,
                    "vtexter_database"
                )
                    .fallbackToDestructiveMigration() // For development
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}