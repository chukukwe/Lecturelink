package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreLectureRepository(context: Context) {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.getString(R.string.firestore_database_id)
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google to access Firestore.")
    }

    // Observe User Profile
    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val docRef = db.collection("users").document(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = try {
                    snapshot.toObject(UserProfile::class.java)
                } catch (t: Throwable) {
                    null
                }
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "displayName" to profile.displayName,
                "email" to profile.email,
                "role" to profile.role,
                "enrolledCourseCodes" to profile.enrolledCourseCodes,
                "department" to profile.department,
                "level" to profile.level,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val existing = docRef.get().await()
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            } else {
                existing.getTimestamp("createdAt")?.let { payload["createdAt"] = it }
            }
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // Courses
    fun observeCourses(): Flow<List<FirestoreCourse>> = callbackFlow {
        val colRef = db.collection("courses")
        val listener = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val courses = try {
                snapshot?.documents?.mapNotNull { it.toObject(FirestoreCourse::class.java) } ?: emptyList()
            } catch (t: Throwable) {
                emptyList()
            }
            trySend(courses)
        }
        awaitClose { listener.remove() }
    }

    suspend fun createCourse(course: FirestoreCourse) {
        val uid = requireUserId()
        val docRef = db.collection("courses").document(course.code.uppercase().trim())
        try {
            val payload = hashMapOf<String, Any>(
                "code" to course.code.uppercase().trim(),
                "name" to course.name.trim(),
                "department" to course.department,
                "level" to course.level,
                "creditUnits" to course.creditUnits,
                "lecturerId" to uid,
                "lecturerName" to (course.lecturerName.ifBlank { auth.currentUser?.displayName ?: "Lecturer" }),
                "courseRepId" to course.courseRepId,
                "courseRepName" to course.courseRepName,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun enrollInCourse(userId: String, courseCode: String, isEnrolled: Boolean) {
        val docRef = db.collection("users").document(userId)
        try {
            if (isEnrolled) {
                docRef.update(
                    "enrolledCourseCodes", FieldValue.arrayUnion(courseCode),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            } else {
                docRef.update(
                    "enrolledCourseCodes", FieldValue.arrayRemove(courseCode),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // Lectures
    fun observeLectures(): Flow<List<FirestoreLecture>> = callbackFlow {
        val colRef = db.collection("lectures")
        val listener = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val lectures = try {
                snapshot?.documents?.mapNotNull { it.toObject(FirestoreLecture::class.java) } ?: emptyList()
            } catch (t: Throwable) {
                emptyList()
            }
            trySend(lectures)
        }
        awaitClose { listener.remove() }
    }

    suspend fun createLecture(lecture: FirestoreLecture) {
        val uid = requireUserId()
        val docId = if (lecture.id.isNotBlank()) lecture.id else "lec_${System.currentTimeMillis()}"
        val docRef = db.collection("lectures").document(docId)
        try {
            val payload = hashMapOf<String, Any?>(
                "id" to docId,
                "courseCode" to lecture.courseCode,
                "courseTitle" to lecture.courseTitle,
                "dayOfWeek" to lecture.dayOfWeek,
                "startTime" to lecture.startTime,
                "endTime" to lecture.endTime,
                "venue" to lecture.venue,
                "originalVenue" to lecture.originalVenue,
                "originalTime" to lecture.originalTime,
                "lecturerId" to uid,
                "lecturerName" to lecture.lecturerName,
                "status" to lecture.status,
                "statusReason" to lecture.statusReason,
                "department" to lecture.department,
                "level" to lecture.level,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateLecture(lecture: FirestoreLecture) {
        val docRef = db.collection("lectures").document(lecture.id)
        try {
            val existing = docRef.get().await()
            val createdAt = existing.getTimestamp("createdAt") ?: Timestamp.now()

            val payload = hashMapOf<String, Any?>(
                "id" to lecture.id,
                "courseCode" to lecture.courseCode,
                "courseTitle" to lecture.courseTitle,
                "dayOfWeek" to lecture.dayOfWeek,
                "startTime" to lecture.startTime,
                "endTime" to lecture.endTime,
                "venue" to lecture.venue,
                "originalVenue" to lecture.originalVenue,
                "originalTime" to lecture.originalTime,
                "lecturerId" to lecture.lecturerId,
                "lecturerName" to lecture.lecturerName,
                "status" to lecture.status,
                "statusReason" to lecture.statusReason,
                "department" to lecture.department,
                "level" to lecture.level,
                "createdAt" to createdAt,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteLecture(lectureId: String) {
        val docRef = db.collection("lectures").document(lectureId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // Announcements / Immediate Updates
    fun observeAnnouncements(): Flow<List<FirestoreAnnouncement>> = callbackFlow {
        val colRef = db.collection("announcements")
        val listener = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val announcements = try {
                snapshot?.documents?.mapNotNull { it.toObject(FirestoreAnnouncement::class.java) } ?: emptyList()
            } catch (t: Throwable) {
                emptyList()
            }
            trySend(announcements)
        }
        awaitClose { listener.remove() }
    }

    suspend fun broadcastAnnouncement(announcement: FirestoreAnnouncement) {
        val uid = requireUserId()
        val docId = if (announcement.id.isNotBlank()) announcement.id else "ann_${System.currentTimeMillis()}"
        val docRef = db.collection("announcements").document(docId)
        try {
            val payload = hashMapOf<String, Any>(
                "id" to docId,
                "courseCode" to announcement.courseCode,
                "title" to announcement.title,
                "content" to announcement.content,
                "authorId" to uid,
                "authorName" to announcement.authorName,
                "authorRole" to announcement.authorRole,
                "urgency" to announcement.urgency,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun seedInitialCloudDataIfEmpty() {
        try {
            val coursesSnapshot = db.collection("courses").limit(1).get().await()
            if (coursesSnapshot.isEmpty) {
                val uid = auth.currentUser?.uid ?: "system_lecturer"
                val defaultCourses = listOf(
                    FirestoreCourse(
                        code = "CSC 301",
                        name = "Operating Systems & Concurrency",
                        department = "Computer Science",
                        level = "300 Level",
                        creditUnits = 3,
                        lecturerId = uid,
                        lecturerName = "Prof. Emmanuel Adeyemi",
                        courseRepId = "rep_david",
                        courseRepName = "David Okafor"
                    ),
                    FirestoreCourse(
                        code = "CSC 305",
                        name = "Database Systems Architecture",
                        department = "Computer Science",
                        level = "300 Level",
                        creditUnits = 3,
                        lecturerId = uid,
                        lecturerName = "Dr. Amina Bello",
                        courseRepId = "rep_david",
                        courseRepName = "David Okafor"
                    ),
                    FirestoreCourse(
                        code = "SEN 302",
                        name = "Software Engineering Principles",
                        department = "Software Engineering",
                        level = "300 Level",
                        creditUnits = 2,
                        lecturerId = uid,
                        lecturerName = "Engr. Kenneth Nnamdi",
                        courseRepId = "rep_chioma",
                        courseRepName = "Chioma Nwosu"
                    ),
                    FirestoreCourse(
                        code = "CSC 309",
                        name = "Artificial Intelligence & Heuristics",
                        department = "Computer Science",
                        level = "300 Level",
                        creditUnits = 3,
                        lecturerId = uid,
                        lecturerName = "Dr. Victor Chen",
                        courseRepId = "rep_david",
                        courseRepName = "David Okafor"
                    )
                )
                for (course in defaultCourses) {
                    db.collection("courses").document(course.code).set(
                        mapOf(
                            "code" to course.code,
                            "name" to course.name,
                            "department" to course.department,
                            "level" to course.level,
                            "creditUnits" to course.creditUnits,
                            "lecturerId" to uid,
                            "lecturerName" to course.lecturerName,
                            "courseRepId" to course.courseRepId,
                            "courseRepName" to course.courseRepName,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }

                val defaultLectures = listOf(
                    FirestoreLecture(
                        id = "lec_1",
                        courseCode = "CSC 301",
                        courseTitle = "Operating Systems & Concurrency",
                        dayOfWeek = "Monday",
                        startTime = "09:00 AM",
                        endTime = "11:00 AM",
                        venue = "Lecture Theatre 1 (LT 1)",
                        lecturerId = uid,
                        lecturerName = "Prof. Emmanuel Adeyemi",
                        status = "SCHEDULED",
                        department = "Computer Science",
                        level = "300 Level"
                    ),
                    FirestoreLecture(
                        id = "lec_2",
                        courseCode = "CSC 305",
                        courseTitle = "Database Systems Architecture",
                        dayOfWeek = "Monday",
                        startTime = "12:00 PM",
                        endTime = "02:00 PM",
                        venue = "New Hall 4 (AC Building)",
                        originalVenue = "Faculty Lab 3",
                        lecturerId = uid,
                        lecturerName = "Dr. Amina Bello",
                        status = "VENUE_CHANGED",
                        statusReason = "Moved to New Hall 4 due to ongoing projector repair in Lab 3",
                        department = "Computer Science",
                        level = "300 Level"
                    ),
                    FirestoreLecture(
                        id = "lec_3",
                        courseCode = "SEN 302",
                        courseTitle = "Software Engineering Principles",
                        dayOfWeek = "Tuesday",
                        startTime = "10:00 AM",
                        endTime = "12:00 PM",
                        venue = "Engineering Hall B",
                        lecturerId = uid,
                        lecturerName = "Engr. Kenneth Nnamdi",
                        status = "ONGOING",
                        statusReason = "Class underway. Attendance signing active.",
                        department = "Software Engineering",
                        level = "300 Level"
                    )
                )
                for (lec in defaultLectures) {
                    val map = hashMapOf<String, Any?>(
                        "id" to lec.id,
                        "courseCode" to lec.courseCode,
                        "courseTitle" to lec.courseTitle,
                        "dayOfWeek" to lec.dayOfWeek,
                        "startTime" to lec.startTime,
                        "endTime" to lec.endTime,
                        "venue" to lec.venue,
                        "originalVenue" to lec.originalVenue,
                        "lecturerId" to uid,
                        "lecturerName" to lec.lecturerName,
                        "status" to lec.status,
                        "statusReason" to lec.statusReason,
                        "department" to lec.department,
                        "level" to lec.level,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }

                    db.collection("lectures").document(lec.id).set(map).await()
                }

                val defaultNotice = FirestoreAnnouncement(
                    id = "ann_init",
                    courseCode = "CSC 305",
                    title = "Venue Shift: Database Architecture in New Hall 4",
                    content = "Today's lecture has been moved from Lab 3 to New Hall 4. Projectors and AC confirmed ready.",
                    authorId = uid,
                    authorName = "David Okafor",
                    authorRole = "Course Rep",
                    urgency = "VENUE_CHANGE"
                )
                db.collection("announcements").document(defaultNotice.id).set(
                    mapOf(
                        "id" to defaultNotice.id,
                        "courseCode" to defaultNotice.courseCode,
                        "title" to defaultNotice.title,
                        "content" to defaultNotice.content,
                        "authorId" to uid,
                        "authorName" to defaultNotice.authorName,
                        "authorRole" to defaultNotice.authorRole,
                        "urgency" to defaultNotice.urgency,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (_: Exception) {
            // Seed handled gracefully
        }
    }
}
