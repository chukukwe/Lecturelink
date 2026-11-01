package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AnnouncementEntity
import com.example.data.local.CourseEntity
import com.example.data.local.DirectoryContactEntity
import com.example.data.local.LectureEntity
import com.example.data.local.LectureLinkDatabase
import com.example.data.model.LectureDay
import com.example.data.model.LectureStatus
import com.example.data.model.UrgencyLevel
import com.example.data.model.UserRole
import com.example.data.repository.LectureRepository
import com.example.notification.LectureAlarmManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LectureLinkUiState(
    val currentRole: UserRole = UserRole.STUDENT,
    val selectedDay: LectureDay? = LectureDay.MONDAY, // Default to Monday
    val searchQuery: String = "",
    val statusFilter: String = "ALL", // ALL, LIVE, ALTERED, SCHEDULED
    val activeDepartmentFilter: String = "All Departments",
    val userMessage: String? = null
)

class LectureLinkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LectureRepository

    private val _uiState = MutableStateFlow(LectureLinkUiState())
    val uiState: StateFlow<LectureLinkUiState> = _uiState.asStateFlow()

    // Dialog & sheet states
    private val _editingLecture = MutableStateFlow<LectureEntity?>(null)
    val editingLecture: StateFlow<LectureEntity?> = _editingLecture.asStateFlow()

    private val _isSchedulingNew = MutableStateFlow(false)
    val isSchedulingNew: StateFlow<Boolean> = _isSchedulingNew.asStateFlow()

    private val _isPostingNotice = MutableStateFlow(false)
    val isPostingNotice: StateFlow<Boolean> = _isPostingNotice.asStateFlow()

    private val _alarmConfigLecture = MutableStateFlow<LectureEntity?>(null)
    val alarmConfigLecture: StateFlow<LectureEntity?> = _alarmConfigLecture.asStateFlow()

    init {
        val db = LectureLinkDatabase.getDatabase(application)
        repository = LectureRepository(db.lectureDao())
        LectureAlarmManager.initNotificationChannel(application)

        viewModelScope.launch {
            repository.checkAndSeedDatabase()
        }
    }

    val courses: StateFlow<List<CourseEntity>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val announcements: StateFlow<List<AnnouncementEntity>> = repository.allAnnouncements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val directoryContacts: StateFlow<List<DirectoryContactEntity>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered lectures based on enrolled courses, active day, search query, and status
    val filteredLectures: StateFlow<List<LectureEntity>> = combine(
        repository.allLectures,
        courses,
        _uiState
    ) { allLecs, courseList, state ->
        val enrolledCourseCodes = courseList.filter { it.isEnrolled }.map { it.code }.toSet()

        allLecs.filter { lec ->
            // Filter by enrolled courses unless user is Lecturer or Rep who sees all
            val matchesEnrollment = if (state.currentRole == UserRole.STUDENT) {
                enrolledCourseCodes.contains(lec.courseCode)
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
                "LIVE" -> lec.status == LectureStatus.ONGOING.name
                "ALTERED" -> lec.status == LectureStatus.VENUE_CHANGED.name ||
                             lec.status == LectureStatus.RESCHEDULED.name ||
                             lec.status == LectureStatus.CANCELLED.name
                "SCHEDULED" -> lec.status == LectureStatus.SCHEDULED.name
                else -> true
            }

            matchesEnrollment && matchesDay && matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun switchRole(newRole: UserRole) {
        _uiState.value = _uiState.value.copy(
            currentRole = newRole,
            userMessage = "Switched to ${newRole.badgeTitle}"
        )
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

    fun setDepartmentFilter(dept: String) {
        _uiState.value = _uiState.value.copy(activeDepartmentFilter = dept)
    }

    fun openEditLecture(lecture: LectureEntity) {
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

    fun openPostNotice() {
        _isPostingNotice.value = true
    }

    fun closePostNotice() {
        _isPostingNotice.value = false
    }

    fun openAlarmConfig(lecture: LectureEntity) {
        _alarmConfigLecture.value = lecture
    }

    fun closeAlarmConfig() {
        _alarmConfigLecture.value = null
    }

    fun toggleAlarmSetting(lecture: LectureEntity, isEnabled: Boolean, minutesBefore: Int, context: Context) {
        viewModelScope.launch {
            repository.toggleAlarm(lecture.id, isEnabled, minutesBefore)
            if (isEnabled) {
                _uiState.value = _uiState.value.copy(
                    userMessage = "Alarm set: ${lecture.courseCode} ($minutesBefore mins before start)"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    userMessage = "Alarm disabled for ${lecture.courseCode}"
                )
            }
        }
    }

    fun testTriggerAlarmNow(lecture: LectureEntity, context: Context) {
        LectureAlarmManager.triggerLectureNotification(
            context = context,
            notificationId = lecture.id.hashCode(),
            courseCode = lecture.courseCode,
            title = lecture.courseTitle,
            message = "Reminder: Starting in ${lecture.alarmMinutesBefore} mins (${lecture.startTime})",
            venue = lecture.venue
        )
        _uiState.value = _uiState.value.copy(
            userMessage = "Test alarm triggered for ${lecture.courseCode}!"
        )
    }

    fun toggleCourseEnrollment(course: CourseEntity) {
        viewModelScope.launch {
            val newStatus = !course.isEnrolled
            repository.toggleCourseEnrollment(course.code, newStatus)
            _uiState.value = _uiState.value.copy(
                userMessage = if (newStatus) "Enrolled in ${course.code}. Timetable updated!" else "Unenrolled from ${course.code}"
            )
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
        courseCode: String
    ) {
        viewModelScope.launch {
            val role = _uiState.value.currentRole
            val author = when (role) {
                UserRole.LECTURER -> "Lecturer Portal"
                UserRole.COURSE_REP -> "Class Rep Desk"
                UserRole.STUDENT -> "Student Member"
            }
            repository.updateLectureDetails(
                id = id,
                status = status,
                reason = reason,
                venue = venue,
                origVenue = origVenue,
                startTime = startTime,
                endTime = endTime,
                origTime = origTime,
                broadcastNotice = broadcast,
                courseCode = courseCode,
                authorName = author,
                authorRole = role.name
            )
            closeEditLecture()
            _uiState.value = _uiState.value.copy(
                userMessage = "$courseCode updated successfully${if (broadcast) " & broadcasted!" else ""}"
            )
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
        lecturerContact: String,
        department: String,
        level: String,
        broadcast: Boolean
    ) {
        viewModelScope.launch {
            val newId = "lec_${System.currentTimeMillis()}"
            val role = _uiState.value.currentRole
            val authorName = if (role == UserRole.LECTURER) lecturerName else "Course Rep"
            val newLecture = LectureEntity(
                id = newId,
                courseCode = courseCode,
                courseTitle = courseTitle,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                venue = venue,
                originalVenue = null,
                originalTime = null,
                lecturerName = lecturerName,
                lecturerContact = lecturerContact,
                courseRepName = "David Okafor",
                status = "SCHEDULED",
                statusReason = null,
                department = department,
                level = level,
                isAlarmEnabled = true,
                alarmMinutesBefore = 15,
                lastUpdatedMillis = System.currentTimeMillis()
            )
            repository.insertLecture(newLecture)

            if (broadcast) {
                repository.createAnnouncement(
                    AnnouncementEntity(
                        id = "ann_${System.currentTimeMillis()}",
                        courseCode = courseCode,
                        title = "New Lecture Scheduled: $courseCode",
                        content = "A new lecture for $courseTitle has been added on $dayOfWeek at $startTime in $venue.",
                        authorName = authorName,
                        authorRole = role.name,
                        urgency = UrgencyLevel.GENERAL.name,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            }

            closeScheduleNew()
            _uiState.value = _uiState.value.copy(
                userMessage = "New lecture $courseCode scheduled successfully!"
            )
        }
    }

    fun broadcastAnnouncement(
        courseCode: String,
        title: String,
        content: String,
        urgency: String
    ) {
        viewModelScope.launch {
            val role = _uiState.value.currentRole
            val author = when (role) {
                UserRole.LECTURER -> "Lecturer Office"
                UserRole.COURSE_REP -> "Course Rep Desk"
                UserRole.STUDENT -> "Student"
            }
            repository.createAnnouncement(
                AnnouncementEntity(
                    id = "ann_${System.currentTimeMillis()}",
                    courseCode = courseCode,
                    title = title,
                    content = content,
                    authorName = author,
                    authorRole = role.name,
                    urgency = urgency,
                    timestampMillis = System.currentTimeMillis()
                )
            )
            closePostNotice()
            _uiState.value = _uiState.value.copy(
                userMessage = "Urgent notice broadcasted to all students!"
            )
        }
    }

    fun deleteLecture(id: String) {
        viewModelScope.launch {
            repository.deleteLecture(id)
            closeEditLecture()
            _uiState.value = _uiState.value.copy(userMessage = "Lecture removed from timetable.")
        }
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
