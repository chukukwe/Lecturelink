package com.example.data.model

enum class UserRole(val label: String, val badgeTitle: String) {
    STUDENT("Student", "Enrolled Student"),
    COURSE_REP("Course Rep", "Official Course Rep"),
    LECTURER("Lecturer", "Faculty Lecturer")
}

enum class LectureStatus(val title: String) {
    SCHEDULED("Scheduled"),
    ONGOING("Live Now"),
    RESCHEDULED("Rescheduled"),
    VENUE_CHANGED("Venue Moved"),
    CANCELLED("Cancelled")
}

enum class UrgencyLevel(val label: String) {
    URGENT("URGENT"),
    VENUE_CHANGE("VENUE UPDATE"),
    TIME_SHIFT("TIME SHIFT"),
    GENERAL("ANNOUNCEMENT")
}

enum class LectureDay(val fullName: String, val shortName: String) {
    MONDAY("Monday", "Mon"),
    TUESDAY("Tuesday", "Tue"),
    WEDNESDAY("Wednesday", "Wed"),
    THURSDAY("Thursday", "Thu"),
    FRIDAY("Friday", "Fri");

    companion object {
        fun fromString(day: String): LectureDay {
            return entries.firstOrNull { 
                it.fullName.equals(day, ignoreCase = true) || it.shortName.equals(day, ignoreCase = true) 
            } ?: MONDAY
        }
    }
}
