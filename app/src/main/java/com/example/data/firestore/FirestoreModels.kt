package com.example.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "Student", // "Lecturer", "Course Rep", "Student"
    val enrolledCourseCodes: List<String> = emptyList(),
    val department: String = "Computer Science",
    val level: String = "300 Level",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreCourse(
    @DocumentId val code: String = "",
    val name: String = "",
    val department: String = "",
    val level: String = "300 Level",
    val creditUnits: Long = 3L,
    val lecturerId: String = "",
    val lecturerName: String = "",
    val courseRepId: String = "",
    val courseRepName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreLecture(
    @DocumentId val id: String = "",
    val courseCode: String = "",
    val courseTitle: String = "",
    val dayOfWeek: String = "Monday",
    val startTime: String = "09:00 AM",
    val endTime: String = "11:00 AM",
    val venue: String = "",
    val originalVenue: String? = null,
    val originalTime: String? = null,
    val lecturerId: String = "",
    val lecturerName: String = "",
    val status: String = "SCHEDULED", // SCHEDULED, ONGOING, RESCHEDULED, VENUE_CHANGED, CANCELLED
    val statusReason: String? = null,
    val department: String = "",
    val level: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreAnnouncement(
    @DocumentId val id: String = "",
    val courseCode: String = "",
    val title: String = "",
    val content: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorRole: String = "",
    val urgency: String = "GENERAL",
    @ServerTimestamp val createdAt: Timestamp? = null
)
