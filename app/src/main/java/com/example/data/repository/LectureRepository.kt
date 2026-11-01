package com.example.data.repository

import com.example.data.local.AnnouncementEntity
import com.example.data.local.CourseEntity
import com.example.data.local.DirectoryContactEntity
import com.example.data.local.LectureDao
import com.example.data.local.LectureEntity
import kotlinx.coroutines.flow.Flow

class LectureRepository(private val dao: LectureDao) {

    val allLectures: Flow<List<LectureEntity>> = dao.getAllLectures()
    val allCourses: Flow<List<CourseEntity>> = dao.getAllCourses()
    val allAnnouncements: Flow<List<AnnouncementEntity>> = dao.getAllAnnouncements()
    val allContacts: Flow<List<DirectoryContactEntity>> = dao.getAllContacts()

    suspend fun checkAndSeedDatabase() {
        if (dao.getLectureCount() == 0) {
            seedInitialData()
        }
    }

    suspend fun insertLecture(lecture: LectureEntity) {
        dao.insertLecture(lecture)
    }

    suspend fun updateLectureDetails(
        id: String,
        status: String,
        reason: String?,
        venue: String,
        origVenue: String?,
        startTime: String,
        endTime: String,
        origTime: String?,
        broadcastNotice: Boolean,
        courseCode: String,
        authorName: String,
        authorRole: String
    ) {
        val now = System.currentTimeMillis()
        dao.updateLectureDetails(
            id = id,
            status = status,
            reason = reason,
            venue = venue,
            origVenue = origVenue,
            startTime = startTime,
            endTime = endTime,
            origTime = origTime,
            updatedTime = now
        )

        if (broadcastNotice) {
            val title = when (status) {
                "VENUE_CHANGED" -> "Venue Moved: $courseCode"
                "RESCHEDULED" -> "Rescheduled: $courseCode"
                "CANCELLED" -> "Lecture Cancelled: $courseCode"
                "ONGOING" -> "Lecture In Progress: $courseCode"
                else -> "Schedule Update: $courseCode"
            }
            val content = buildString {
                append("Lecture for $courseCode has been updated to $status.")
                if (!reason.isNullOrBlank()) {
                    append("\nNote: $reason")
                }
                append("\nNew Venue: $venue | Time: $startTime - $endTime")
            }
            val urgency = when (status) {
                "CANCELLED" -> "URGENT"
                "VENUE_CHANGED" -> "VENUE_CHANGE"
                "RESCHEDULED" -> "TIME_SHIFT"
                else -> "GENERAL"
            }
            dao.insertAnnouncement(
                AnnouncementEntity(
                    id = "ann_${System.currentTimeMillis()}",
                    courseCode = courseCode,
                    title = title,
                    content = content,
                    authorName = authorName,
                    authorRole = authorRole,
                    urgency = urgency,
                    timestampMillis = now
                )
            )
        }
    }

    suspend fun toggleAlarm(id: String, isEnabled: Boolean, minutesBefore: Int) {
        dao.updateAlarm(id, isEnabled, minutesBefore)
    }

    suspend fun toggleCourseEnrollment(code: String, isEnrolled: Boolean) {
        dao.setCourseEnrolled(code, isEnrolled)
    }

    suspend fun createAnnouncement(announcement: AnnouncementEntity) {
        dao.insertAnnouncement(announcement)
    }

    suspend fun deleteLecture(id: String) {
        dao.deleteLecture(id)
    }

    private suspend fun seedInitialData() {
        val courses = listOf(
            CourseEntity(
                code = "CSC 301",
                title = "Operating Systems & Concurrency",
                department = "Computer Science",
                level = "300 Level",
                creditUnits = 3,
                lecturer = "Prof. Emmanuel Adeyemi",
                courseRep = "David Okafor",
                lectureVenueDefault = "Lecture Theatre 1 (LT 1)",
                isEnrolled = true
            ),
            CourseEntity(
                code = "CSC 305",
                title = "Database Systems Architecture",
                department = "Computer Science",
                level = "300 Level",
                creditUnits = 3,
                lecturer = "Dr. Amina Bello",
                courseRep = "David Okafor",
                lectureVenueDefault = "Faculty Lab 3, Block C",
                isEnrolled = true
            ),
            CourseEntity(
                code = "SEN 302",
                title = "Software Engineering Principles",
                department = "Software Engineering",
                level = "300 Level",
                creditUnits = 2,
                lecturer = "Engr. Kenneth Nnamdi",
                courseRep = "Chioma Nwosu",
                lectureVenueDefault = "Engineering Hall B",
                isEnrolled = true
            ),
            CourseEntity(
                code = "MTH 201",
                title = "Mathematical Methods & Linear Algebra",
                department = "Mathematics",
                level = "200 Level",
                creditUnits = 3,
                lecturer = "Dr. Samuel Olatunji",
                courseRep = "Michael Eze",
                lectureVenueDefault = "Central Auditorium",
                isEnrolled = false
            ),
            CourseEntity(
                code = "EEE 303",
                title = "Digital Electronics & Embedded Systems",
                department = "Electrical Engineering",
                level = "300 Level",
                creditUnits = 3,
                lecturer = "Prof. Fatima Garba",
                courseRep = "Sarah Peters",
                lectureVenueDefault = "Hardware Lab 2",
                isEnrolled = false
            ),
            CourseEntity(
                code = "CSC 309",
                title = "Artificial Intelligence & Heuristics",
                department = "Computer Science",
                level = "300 Level",
                creditUnits = 3,
                lecturer = "Dr. Victor Chen",
                courseRep = "David Okafor",
                lectureVenueDefault = "AI Research Center Hall",
                isEnrolled = true
            )
        )
        dao.insertCourses(courses)

        val lectures = listOf(
            LectureEntity(
                id = "lec_1",
                courseCode = "CSC 301",
                courseTitle = "Operating Systems & Concurrency",
                dayOfWeek = "Monday",
                startTime = "09:00 AM",
                endTime = "11:00 AM",
                venue = "Lecture Theatre 1 (LT 1)",
                originalVenue = null,
                originalTime = null,
                lecturerName = "Prof. Emmanuel Adeyemi",
                lecturerContact = "e.adeyemi@uni.edu.ng",
                courseRepName = "David Okafor",
                status = "SCHEDULED",
                statusReason = null,
                department = "Computer Science",
                level = "300 Level",
                isAlarmEnabled = true,
                alarmMinutesBefore = 15
            ),
            LectureEntity(
                id = "lec_2",
                courseCode = "CSC 305",
                courseTitle = "Database Systems Architecture",
                dayOfWeek = "Monday",
                startTime = "12:00 PM",
                endTime = "02:00 PM",
                venue = "New Hall 4 (AC Building)",
                originalVenue = "Faculty Lab 3, Block C",
                originalTime = null,
                lecturerName = "Dr. Amina Bello",
                lecturerContact = "a.bello@uni.edu.ng",
                courseRepName = "David Okafor",
                status = "VENUE_CHANGED",
                statusReason = "Moved to New Hall 4 due to ongoing projector repair in Lab 3",
                department = "Computer Science",
                level = "300 Level",
                isAlarmEnabled = true,
                alarmMinutesBefore = 30
            ),
            LectureEntity(
                id = "lec_3",
                courseCode = "SEN 302",
                courseTitle = "Software Engineering Principles",
                dayOfWeek = "Tuesday",
                startTime = "10:00 AM",
                endTime = "12:00 PM",
                venue = "Engineering Hall B",
                originalVenue = null,
                originalTime = null,
                lecturerName = "Engr. Kenneth Nnamdi",
                lecturerContact = "k.nnamdi@uni.edu.ng",
                courseRepName = "Chioma Nwosu",
                status = "ONGOING",
                statusReason = "Lecture currently underway. Attendance sheet circulating.",
                department = "Software Engineering",
                level = "300 Level",
                isAlarmEnabled = false,
                alarmMinutesBefore = 15
            ),
            LectureEntity(
                id = "lec_4",
                courseCode = "CSC 309",
                courseTitle = "Artificial Intelligence & Heuristics",
                dayOfWeek = "Wednesday",
                startTime = "02:00 PM",
                endTime = "04:00 PM",
                venue = "AI Research Center Hall",
                originalVenue = null,
                originalTime = "10:00 AM",
                lecturerName = "Dr. Victor Chen",
                lecturerContact = "v.chen@uni.edu.ng",
                courseRepName = "David Okafor",
                status = "RESCHEDULED",
                statusReason = "Shifted from 10:00 AM to 02:00 PM to accommodate faculty accreditation",
                department = "Computer Science",
                level = "300 Level",
                isAlarmEnabled = true,
                alarmMinutesBefore = 15
            ),
            LectureEntity(
                id = "lec_5",
                courseCode = "MTH 201",
                courseTitle = "Mathematical Methods & Linear Algebra",
                dayOfWeek = "Thursday",
                startTime = "08:00 AM",
                endTime = "10:00 AM",
                venue = "Central Auditorium",
                originalVenue = null,
                originalTime = null,
                lecturerName = "Dr. Samuel Olatunji",
                lecturerContact = "s.olatunji@uni.edu.ng",
                courseRepName = "Michael Eze",
                status = "CANCELLED",
                statusReason = "Lecturer attending National Mathematics Colloquium in Abuja",
                department = "Mathematics",
                level = "200 Level",
                isAlarmEnabled = false,
                alarmMinutesBefore = 15
            ),
            LectureEntity(
                id = "lec_6",
                courseCode = "EEE 303",
                courseTitle = "Digital Electronics & Embedded Systems",
                dayOfWeek = "Friday",
                startTime = "10:00 AM",
                endTime = "01:00 PM",
                venue = "Hardware Lab 2",
                originalVenue = null,
                originalTime = null,
                lecturerName = "Prof. Fatima Garba",
                lecturerContact = "f.garba@uni.edu.ng",
                courseRepName = "Sarah Peters",
                status = "SCHEDULED",
                statusReason = null,
                department = "Electrical Engineering",
                level = "300 Level",
                isAlarmEnabled = true,
                alarmMinutesBefore = 30
            )
        )
        dao.insertLectures(lectures)

        val announcements = listOf(
            AnnouncementEntity(
                id = "ann_1",
                courseCode = "CSC 305",
                title = "Venue Relocation for Today's Lecture",
                content = "Please note that today's Database Architecture lecture will hold in New Hall 4 instead of Lab 3. Power supply and projectors are confirmed working there.",
                authorName = "David Okafor",
                authorRole = "COURSE_REP",
                urgency = "VENUE_CHANGE",
                timestampMillis = System.currentTimeMillis() - 1000 * 60 * 45 // 45 mins ago
            ),
            AnnouncementEntity(
                id = "ann_2",
                courseCode = "CSC 309",
                title = "Time Adjustment Notice - AI Lecture",
                content = "Good day all. Due to the Dean's accreditation committee session in the morning, Dr. Victor Chen has agreed to take our lecture at 2:00 PM. Please spread the word.",
                authorName = "David Okafor",
                authorRole = "COURSE_REP",
                urgency = "TIME_SHIFT",
                timestampMillis = System.currentTimeMillis() - 1000 * 60 * 180 // 3 hours ago
            ),
            AnnouncementEntity(
                id = "ann_3",
                courseCode = "CSC 301",
                title = "Mid-Semester Test Scope & Submission",
                content = "The mid-semester test for OS will be administered next Monday during lecture hours. Topics include Process Scheduling, Deadlocks, and Semaphore concurrency. Bring your university ID cards.",
                authorName = "Prof. Emmanuel Adeyemi",
                authorRole = "LECTURER",
                urgency = "URGENT",
                timestampMillis = System.currentTimeMillis() - 1000 * 60 * 60 * 14 // 14 hours ago
            ),
            AnnouncementEntity(
                id = "ann_4",
                courseCode = "ALL",
                title = "Welcome to LectureLink University Pilot",
                content = "Direct connection active! Course Reps and Lecturers can update lecture statuses and post instant broadcasts. Students: enable alarms to receive notifications before class starts.",
                authorName = "Faculty Academic Office",
                authorRole = "LECTURER",
                urgency = "GENERAL",
                timestampMillis = System.currentTimeMillis() - 1000 * 60 * 60 * 48 // 2 days ago
            )
        )
        dao.insertAnnouncements(announcements)

        val directory = listOf(
            DirectoryContactEntity(
                id = "rep_1",
                name = "David Okafor",
                role = "COURSE_REP",
                department = "Computer Science",
                level = "300 Level",
                email = "david.okafor@students.uni.edu.ng",
                phone = "+234 803 123 4567",
                officeRoom = "Faculty Rep Desk, Block A",
                note = "Official Course Rep for CS 300L. Admin of Main Lecture Updates group.",
                classGroupLink = "https://chat.whatsapp.com/invite/lecturelink-csc300"
            ),
            DirectoryContactEntity(
                id = "rep_2",
                name = "Chioma Nwosu",
                role = "COURSE_REP",
                department = "Software Engineering",
                level = "300 Level",
                email = "chioma.nwosu@students.uni.edu.ng",
                phone = "+234 814 987 6543",
                officeRoom = "Engineering Wing C",
                note = "Official Course Rep for SEN 300L. Coordinates with HOD and Lab staff.",
                classGroupLink = "https://chat.whatsapp.com/invite/lecturelink-sen300"
            ),
            DirectoryContactEntity(
                id = "rep_3",
                name = "Michael Eze",
                role = "COURSE_REP",
                department = "Mathematics",
                level = "200 Level",
                email = "michael.eze@students.uni.edu.ng",
                phone = "+234 802 555 1212",
                officeRoom = "Math Dept Notice Area",
                note = "Course Rep for MTH 200L. Class schedule inquiries.",
                classGroupLink = "https://chat.whatsapp.com/invite/lecturelink-mth200"
            ),
            DirectoryContactEntity(
                id = "lec_contact_1",
                name = "Prof. Emmanuel Adeyemi",
                role = "LECTURER",
                department = "Computer Science",
                level = "Faculty Staff",
                email = "e.adeyemi@uni.edu.ng",
                phone = "+234 803 999 1100",
                officeRoom = "Block B, Room 204",
                note = "Consultation hours: Mon & Wed 2:00 PM - 4:00 PM.",
                classGroupLink = null
            ),
            DirectoryContactEntity(
                id = "lec_contact_2",
                name = "Dr. Amina Bello",
                role = "LECTURER",
                department = "Computer Science",
                level = "Faculty Staff",
                email = "a.bello@uni.edu.ng",
                phone = "+234 806 777 2211",
                officeRoom = "Block C, Faculty Lab 3 Office",
                note = "Head of Database Lab. Office hours: Tue & Thu 11:00 AM - 1:00 PM.",
                classGroupLink = null
            ),
            DirectoryContactEntity(
                id = "lec_contact_3",
                name = "Engr. Kenneth Nnamdi",
                role = "LECTURER",
                department = "Software Engineering",
                level = "Faculty Staff",
                email = "k.nnamdi@uni.edu.ng",
                phone = "+234 818 333 4455",
                officeRoom = "Engineering Faculty, Room 108",
                note = "Software Engineering Lab Director. Available after Tuesday lectures.",
                classGroupLink = null
            )
        )
        dao.insertContacts(directory)
    }
}
