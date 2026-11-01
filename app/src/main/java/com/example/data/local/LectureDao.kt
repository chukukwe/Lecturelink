package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureDao {

    @Query("SELECT * FROM lectures ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllLectures(): Flow<List<LectureEntity>>

    @Query("SELECT * FROM lectures WHERE id = :id")
    suspend fun getLectureById(id: String): LectureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecture(lecture: LectureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLectures(lectures: List<LectureEntity>)

    @Update
    suspend fun updateLecture(lecture: LectureEntity)

    @Query("UPDATE lectures SET status = :status, statusReason = :reason, venue = :venue, originalVenue = :origVenue, startTime = :startTime, endTime = :endTime, originalTime = :origTime, lastUpdatedMillis = :updatedTime WHERE id = :id")
    suspend fun updateLectureDetails(
        id: String,
        status: String,
        reason: String?,
        venue: String,
        origVenue: String?,
        startTime: String,
        endTime: String,
        origTime: String?,
        updatedTime: Long
    )

    @Query("UPDATE lectures SET isAlarmEnabled = :isEnabled, alarmMinutesBefore = :minutesBefore WHERE id = :id")
    suspend fun updateAlarm(id: String, isEnabled: Boolean, minutesBefore: Int)

    @Query("DELETE FROM lectures WHERE id = :id")
    suspend fun deleteLecture(id: String)

    @Query("SELECT COUNT(*) FROM lectures")
    suspend fun getLectureCount(): Int

    // Courses
    @Query("SELECT * FROM courses ORDER BY code ASC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)

    @Query("UPDATE courses SET isEnrolled = :isEnrolled WHERE code = :code")
    suspend fun setCourseEnrolled(code: String, isEnrolled: Boolean)

    // Announcements
    @Query("SELECT * FROM announcements ORDER BY timestampMillis DESC")
    fun getAllAnnouncements(): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<AnnouncementEntity>)

    // Directory
    @Query("SELECT * FROM directory_contacts ORDER BY role ASC, name ASC")
    fun getAllContacts(): Flow<List<DirectoryContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<DirectoryContactEntity>)
}
