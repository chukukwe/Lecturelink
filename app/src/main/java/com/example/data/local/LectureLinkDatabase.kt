package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LectureEntity::class,
        CourseEntity::class,
        AnnouncementEntity::class,
        DirectoryContactEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LectureLinkDatabase : RoomDatabase() {
    abstract fun lectureDao(): LectureDao

    companion object {
        @Volatile
        private var INSTANCE: LectureLinkDatabase? = null

        fun getDatabase(context: Context): LectureLinkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LectureLinkDatabase::class.java,
                    "lecturelink_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
