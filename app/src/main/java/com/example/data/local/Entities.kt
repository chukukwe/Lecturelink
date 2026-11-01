package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.LectureDay
import com.example.data.model.LectureStatus
import com.example.data.model.UrgencyLevel
import com.example.data.model.UserRole

@Entity(tableName = "lectures")
data class LectureEntity(
    @PrimaryKey
    val id: String,
    val courseCode: String,
    val courseTitle: String,
    val dayOfWeek: String, // "Monday", etc.
    val startTime: String, // "09:00 AM"
    val endTime: String,   // "11:00 AM"
    val venue: String,     // "LT 1, Science Complex"
    val originalVenue: String? = null,
    val originalTime: String? = null,
    val lecturerName: String,
    val lecturerContact: String,
    val courseRepName: String,
    val status: String,    // "SCHEDULED", "ONGOING", "RESCHEDULED", "VENUE_CHANGED", "CANCELLED"
    val statusReason: String? = null,
    val department: String,
    val level: String,     // "300 Level"
    val isAlarmEnabled: Boolean = false,
    val alarmMinutesBefore: Int = 15,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey
    val code: String,      // "CSC 301"
    val title: String,
    val department: String,
    val level: String,
    val creditUnits: Int,
    val lecturer: String,
    val courseRep: String,
    val lectureVenueDefault: String,
    val isEnrolled: Boolean = true
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey
    val id: String,
    val courseCode: String, // e.g. "CSC 301" or "ALL"
    val title: String,
    val content: String,
    val authorName: String,
    val authorRole: String, // "LECTURER" or "COURSE_REP"
    val urgency: String,    // "URGENT", "VENUE_CHANGE", "TIME_SHIFT", "GENERAL"
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "directory_contacts")
data class DirectoryContactEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val role: String,       // "LECTURER" or "COURSE_REP"
    val department: String,
    val level: String,
    val email: String,
    val phone: String,
    val officeRoom: String,
    val note: String,
    val classGroupLink: String? = null
)
