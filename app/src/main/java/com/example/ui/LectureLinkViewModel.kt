package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.data.firestore.FirestoreAnnouncement
import com.example.data.firestore.FirestoreCourse
import com.example.data.firestore.FirestoreLecture
import com.example.data.firestore.FirestoreLectureRepository
import com.example.data.firestore.UserProfile
import com.example.data.model.LectureDay
import com.example.data.model.LectureStatus
import com.example.data.model.UrgencyLevel
import com.example.data.model.UserRole
import com.example.notification.LectureAlarmManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LectureLinkUiState(
    val currentRole: UserRole = UserRole.STUDENT,
    val selectedDay: LectureDay? = LectureDay.MONDAY,
    val searchQuery: String = "",
    val statusFilter: String = "ALL", // ALL, LIVE, ALTERED, SCHEDULED
    val activeDepartmentFilter: String = "All Departments",
    val userMessage: String? = null,
    val enrolledCourseCodes: Set<String> = emptySet(),
    val alarmMinutesMap: Map<String, Int> = emptyMap() // Map of lectureId to minutesBefore (e.g. 15, 60)
)

class LectureLinkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FirestoreLectureRepository(application)

    private val _uiState = MutableStateFlow(LectureLinkUiState())
    val uiState: StateFlow<LectureLinkUiState> = _uiState.asStateFlow()

    // Dialog & sheet states
    private val _editingLecture = MutableStateFlow<FirestoreLecture?>(null)
    val editingLecture: StateFlow<FirestoreLecture?> = _editingLecture.asStateFlow()

    private val _isSchedulingNew = MutableStateFlow(false)
    val isSchedulingNew: StateFlow<Boolean> = _isSchedulingNew.asStateFlow()

    private val _isCreatingCourse = MutableStateFlow(false)
    val isCreatingCourse: StateFlow<Boolean> = _isCreatingCourse.asStateFlow()

    private val _isPostingNotice = MutableStateFlow(false)
    val isPostingNotice: StateFlow<Boolean> = _isPostingNotice.asStateFlow()

    private val _alarmConfigLecture = MutableStateFlow<FirestoreLecture?>(null)
    val alarmConfigLecture: StateFlow<FirestoreLecture?> = _alarmConfigLecture.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    val courses: StateFlow<List<FirestoreCourse>> = repository.observeCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lectures: StateFlow<List<FirestoreLecture>> = repository.observeLectures()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val announcements: StateFlow<List<FirestoreAnnouncement>> = repository.observeAnnouncements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        LectureAlarmManager.initNotificationChannel(application)

        // Observe auth user and load profile
        viewModelScope.launch {
            AuthManager.currentUser.collect { user ->
                if (user != null) {
                    repository.seedInitialCloudDataIfEmpty()
                    repository.observeUserProfile(user.uid).collect { profile ->
                        if (profile != null) {
                            _userProfile.value = profile
                            val role = when (profile.role) {
                                "Lecturer" -> UserRole.LECTURER
                                "Course Rep" -> UserRole.COURSE_REP
                                else -> UserRole.STUDENT
                            }
                            _uiState.value = _uiState.value.copy(
                                currentRole = role,
                                enrolledCourseCodes = profile.enrolledCourseCodes.toSet()
                            )
                        } else {
                            // First time login profile
                            val newProfile = UserProfile(
                                userId = user.uid,
                                displayName = user.displayName ?: "Student User",
                                email = user.email ?: "",
                                role = _uiState.value.currentRole.label,
                                enrolledCourseCodes = listOf("CSC 301", "CSC 305", "SEN 302")
                            )
                            repository.saveUserProfile(newProfile)
                        }
                    }
                }
            }
        }
    }

    // Filtered lectures based on enrolled courses, active day, search query, and status
    val filteredLectures: StateFlow<List<FirestoreLecture>> = combine(
        lectures,
        _uiState
    ) { allLecs, state ->
        allLecs.filter { lec ->
            // Student sees enrolled courses only; Lecturer and Rep see all
            val matchesEnrollment = if (state.currentRole == UserRole.STUDENT) {
                state.enrolledCourseCodes.isEmpty() || state.enrolledCourseCodes.contains(lec.courseCode)
            } else {
                true
            }

            val matchesDay = if (state.selectedDay != null) {
                lec.dayOfWeek.equals(state.selectedDay.fullName, ignoreCase = true)
            } else {
                true
            }

            val query = state.searchQuery.trim().lowercase()
            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                lec.courseCode.lowercase().contains(query) ||
                lec.courseTitle.lowercase().contains(query) ||
                lec.venue.lowercase().contains(query) ||
                lec.lecturerName.lowercase().contains(query)
            }

            val matchesStatus = when (state.statusFilter) {
                "LIVE" -> lec.status == "ONGOING"
                "ALTERED" -> lec.status == "VENUE_CHANGED" || lec.status == "RESCHEDULED" || lec.status == "CANCELLED"
                "SCHEDULED" -> lec.status == "SCHEDULED"
                else -> true
            }

            matchesEnrollment && matchesDay && matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun switchRole(newRole: UserRole) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                currentRole = newRole,
                userMessage = "Switched to ${newRole.badgeTitle}"
            )
            val currentUid = Firebase.auth.currentUser?.uid
            if (currentUid != null) {
                _userProfile.value?.let { prof ->
                    repository.saveUserProfile(prof.copy(role = newRole.label))
                }
            }
        }
    }

    fun selectDay(day: LectureDay?) {
        _uiState.value = _uiState.value.copy(selectedDay = day)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setStatusFilter(filter: String) {
        _uiState.value = _uiState.value.copy(statusFilter = filter)
    }

    fun openEditLecture(lecture: FirestoreLecture) {
        _editingLecture.value = lecture
    }

    fun closeEditLecture() {
        _editingLecture.value = null
    }

    fun openScheduleNew() {
        _isSchedulingNew.value = true
    }

    fun closeScheduleNew() {
        _isSchedulingNew.value = false
    }

    fun openCreateCourse() {
        _isCreatingCourse.value = true
    }

    fun closeCreateCourse() {
        _isCreatingCourse.value = false
    }

    fun openPostNotice() {
        _isPostingNotice.value = true
    }

    fun closePostNotice() {
        _isPostingNotice.value = false
    }

    fun openAlarmConfig(lecture: FirestoreLecture) {
        _alarmConfigLecture.value = lecture
    }

    fun closeAlarmConfig() {
        _alarmConfigLecture.value = null
    }

    fun setLectureAlarm(lecture: FirestoreLecture, isEnabled: Boolean, minutesBefore: Int, context: Context) {
        val currentMap = _uiState.value.alarmMinutesMap.toMutableMap()
        if (isEnabled) {
            currentMap[lecture.id] = minutesBefore
            _uiState.value = _uiState.value.copy(
                alarmMinutesMap = currentMap,
                userMessage = "Reminder alarm set: ${lecture.courseCode} ($minutesBefore min before start)"
            )
        } else {
            currentMap.remove(lecture.id)
            _uiState.value = _uiState.value.copy(
                alarmMinutesMap = currentMap,
                userMessage = "Alarm disabled for ${lecture.courseCode}"
            )
        }
    }

    fun testTriggerAlarmNow(lecture: FirestoreLecture, context: Context) {
        val minutes = _uiState.value.alarmMinutesMap[lecture.id] ?: 15
        LectureAlarmManager.triggerLectureNotification(
            context = context,
            notificationId = lecture.id.hashCode(),
            courseCode = lecture.courseCode,
            title = lecture.courseTitle,
            message = "Reminder: Starting in $minutes mins (${lecture.startTime})",
            venue = lecture.venue
        )
        _uiState.value = _uiState.value.copy(
            userMessage = "Notification triggered for ${lecture.courseCode}!"
        )
    }

    fun toggleCourseEnrollment(courseCode: String) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val currentEnrolled = _uiState.value.enrolledCourseCodes.toMutableSet()
        val isCurrentlyEnrolled = currentEnrolled.contains(courseCode)
        viewModelScope.launch {
            try {
                if (isCurrentlyEnrolled) {
                    currentEnrolled.remove(courseCode)
                    repository.enrollInCourse(uid, courseCode, false)
                    _uiState.value = _uiState.value.copy(
                        enrolledCourseCodes = currentEnrolled,
                        userMessage = "Unenrolled from $courseCode"
                    )
                } else {
                    currentEnrolled.add(courseCode)
                    repository.enrollInCourse(uid, courseCode, true)
                    _uiState.value = _uiState.value.copy(
                        enrolledCourseCodes = currentEnrolled,
                        userMessage = "Enrolled in $courseCode! Timetable synced."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userMessage = "Failed to update enrollment: ${e.message}")
            }
        }
    }

    fun createNewCourse(
        code: String,
        name: String,
        department: String,
        level: String,
        creditUnits: Long,
        courseRepName: String
    ) {
        viewModelScope.launch {
            try {
                val lecturerName = Firebase.auth.currentUser?.displayName ?: "Lecturer"
                val course = FirestoreCourse(
                    code = code,
                    name = name,
                    department = department,
                    level = level,
                    creditUnits = creditUnits,
                    lecturerId = Firebase.auth.currentUser?.uid ?: "",
                    lecturerName = lecturerName,
                    courseRepId = "",
                    courseRepName = courseRepName
                )
                repository.createCourse(course)
                closeCreateCourse()
                _uiState.value = _uiState.value.copy(userMessage = "Course $code successfully created!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userMessage = "Error creating course: ${e.message}")
            }
        }
    }

    fun saveLectureUpdate(
        id: String,
        status: String,
        reason: String?,
        venue: String,
        origVenue: String?,
        startTime: String,
        endTime: String,
        origTime: String?,
        broadcast: Boolean,
        courseCode: String,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                val existing = lectures.value.firstOrNull { it.id == id }
                val updatedLecture = FirestoreLecture(
                    id = id,
                    courseCode = courseCode,
                    courseTitle = existing?.courseTitle ?: "Lecture",
                    dayOfWeek = existing?.dayOfWeek ?: "Monday",
                    startTime = startTime,
                    endTime = endTime,
                    venue = venue,
                    originalVenue = origVenue,
                    originalTime = origTime,
                    lecturerId = existing?.lecturerId ?: "",
                    lecturerName = existing?.lecturerName ?: "Lecturer",
                    status = status,
                    statusReason = reason,
                    department = existing?.department ?: "",
                    level = existing?.level ?: ""
                )
                repository.updateLecture(updatedLecture)

                if (broadcast) {
                    val author = Firebase.auth.currentUser?.displayName ?: _uiState.value.currentRole.label
                    val noticeTitle = when (status) {
                        "VENUE_CHANGED" -> "Venue Moved: $courseCode"
                        "RESCHEDULED" -> "Rescheduled: $courseCode"
                        "CANCELLED" -> "Cancelled: $courseCode"
                        "ONGOING" -> "Live Now: $courseCode"
                        else -> "Schedule Update: $courseCode"
                    }
                    val content = buildString {
                        append("Lecture for $courseCode has been updated to $status.")
                        if (!reason.isNullOrBlank()) append("\nNote: $reason")
                        append("\nVenue: $venue | Time: $startTime - $endTime")
                    }
                    val urgency = when (status) {
                        "CANCELLED" -> "URGENT"
                        "VENUE_CHANGED" -> "VENUE_CHANGE"
                        "RESCHEDULED" -> "TIME_SHIFT"
                        else -> "GENERAL"
                    }
                    repository.broadcastAnnouncement(
                        FirestoreAnnouncement(
                            id = "ann_${System.currentTimeMillis()}",
                            courseCode = courseCode,
                            title = noticeTitle,
                            content = content,
                            authorId = Firebase.auth.currentUser?.uid ?: "",
                            authorName = author,
                            authorRole = _uiState.value.currentRole.label,
                            urgency = urgency
                        )
                    )

                    // Trigger immediate push notification to device!
                    LectureAlarmManager.triggerLectureNotification(
                        context = context,
                        notificationId = id.hashCode(),
                        courseCode = courseCode,
                        title = noticeTitle,
                        message = "Update: $status ($venue)",
                        venue = venue
                    )
                }

                closeEditLecture()
                _uiState.value = _uiState.value.copy(userMessage = "$courseCode updated successfully!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userMessage = "Update failed: ${e.message}")
            }
        }
    }

    fun scheduleNewLecture(
        courseCode: String,
        courseTitle: String,
        dayOfWeek: String,
        startTime: String,
        endTime: String,
        venue: String,
        lecturerName: String,
        department: String,
        level: String,
        broadcast: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                val newId = "lec_${System.currentTimeMillis()}"
                val lecture = FirestoreLecture(
                    id = newId,
                    courseCode = courseCode,
                    courseTitle = courseTitle,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    venue = venue,
                    originalVenue = null,
                    originalTime = null,
                    lecturerId = Firebase.auth.currentUser?.uid ?: "",
                    lecturerName = lecturerName,
                    status = "SCHEDULED",
                    statusReason = null,
                    department = department,
                    level = level
                )
                repository.createLecture(lecture)

                if (broadcast) {
                    val author = Firebase.auth.currentUser?.displayName ?: "Lecturer"
                    repository.broadcastAnnouncement(
                        FirestoreAnnouncement(
                            id = "ann_${System.currentTimeMillis()}",
                            courseCode = courseCode,
                            title = "New Lecture Scheduled: $courseCode",
                            content = "$courseTitle scheduled on $dayOfWeek at $startTime in $venue.",
                            authorId = Firebase.auth.currentUser?.uid ?: "",
                            authorName = author,
                            authorRole = _uiState.value.currentRole.label,
                            urgency = "GENERAL"
                        )
                    )

                    LectureAlarmManager.triggerLectureNotification(
                        context = context,
                        notificationId = newId.hashCode(),
                        courseCode = courseCode,
                        title = "New Lecture Scheduled",
                        message = "$courseCode on $dayOfWeek at $startTime",
                        venue = venue
                    )
                }

                closeScheduleNew()
                _uiState.value = _uiState.value.copy(userMessage = "Lecture $courseCode scheduled!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userMessage = "Failed to schedule: ${e.message}")
            }
        }
    }

    fun broadcastAnnouncement(
        courseCode: String,
        title: String,
        content: String,
        urgency: String,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                val author = Firebase.auth.currentUser?.displayName ?: _uiState.value.currentRole.label
                repository.broadcastAnnouncement(
                    FirestoreAnnouncement(
                        id = "ann_${System.currentTimeMillis()}",
                        courseCode = courseCode,
                        title = title,
                        content = content,
                        authorId = Firebase.auth.currentUser?.uid ?: "",
                        authorName = author,
                        authorRole = _uiState.value.currentRole.label,
                        urgency = urgency
                    )
                )

                LectureAlarmManager.triggerLectureNotification(
                    context = context,
                    notificationId = System.currentTimeMillis().toInt(),
                    courseCode = courseCode,
                    title = "🚨 $title",
                    message = content,
                    venue = "All Enrolled Students"
                )

                closePostNotice()
                _uiState.value = _uiState.value.copy(userMessage = "Urgent announcement broadcasted!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userMessage = "Broadcast failed: ${e.message}")
            }
        }
    }

    fun deleteLecture(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteLecture(id)
                closeEditLecture()
                _uiState.value = _uiState.value.copy(userMessage = "Lecture removed from schedule.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(userMessage = "Delete failed: ${e.message}")
            }
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
