package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CastForEducation
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.UserRole
import com.example.ui.components.AlarmConfigDialog
import com.example.ui.components.EditLectureDialog
import com.example.ui.components.PostNoticeDialog
import com.example.ui.components.ScheduleLectureDialog
import com.example.ui.screens.CoursesScreen
import com.example.ui.screens.DirectoryScreen
import com.example.ui.screens.NoticesScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.theme.AmberRescheduled
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldLive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectureLinkApp(
    viewModel: LectureLinkViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lectures by viewModel.filteredLectures.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val directoryContacts by viewModel.directoryContacts.collectAsStateWithLifecycle()

    val editingLecture by viewModel.editingLecture.collectAsStateWithLifecycle()
    val isSchedulingNew by viewModel.isSchedulingNew.collectAsStateWithLifecycle()
    val isPostingNotice by viewModel.isPostingNotice.collectAsStateWithLifecycle()
    val alarmConfigLecture by viewModel.alarmConfigLecture.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Request notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Show toast / snackbar whenever userMessage changes
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "LectureLink",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        androidx.compose.foundation.layout.Column {
                            Text(
                                text = "LectureLink",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Direct University Timetable & Alarms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    // Current Role Pill Indicator in TopBar
                    val (roleColor, roleIcon) = when (uiState.currentRole) {
                        UserRole.STUDENT -> BluePrimary to Icons.Default.School
                        UserRole.COURSE_REP -> AmberRescheduled to Icons.Default.Campaign
                        UserRole.LECTURER -> EmeraldLive to Icons.Default.CastForEducation
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = roleColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = roleIcon,
                                contentDescription = uiState.currentRole.label,
                                tint = roleColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = uiState.currentRole.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = roleColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                // Tab 0: Schedule
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 0) Icons.Filled.Schedule else Icons.Outlined.Schedule,
                            contentDescription = "Schedule"
                        )
                    },
                    label = { Text("Schedule", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_schedule")
                )

                // Tab 1: Courses
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 1) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                            contentDescription = "Courses"
                        )
                    },
                    label = { Text("Courses", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_courses")
                )

                // Tab 2: Directory (Course Reps & Lecturers)
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 2) Icons.Filled.People else Icons.Outlined.People,
                            contentDescription = "Directory"
                        )
                    },
                    label = { Text("Directory", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_directory")
                )

                // Tab 3: Notices Bulletin
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (announcements.isNotEmpty()) {
                                    Badge { Text("${announcements.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == 3) Icons.Filled.Campaign else Icons.Outlined.Campaign,
                                contentDescription = "Notices"
                            )
                        }
                    },
                    label = { Text("Notices", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_notices")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> ScheduleScreen(
                    uiState = uiState,
                    lectures = lectures,
                    onRoleSelected = { viewModel.switchRole(it) },
                    onDaySelected = { viewModel.selectDay(it) },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onStatusFilterChange = { viewModel.setStatusFilter(it) },
                    onAlarmClick = { viewModel.openAlarmConfig(it) },
                    onEditLectureClick = { viewModel.openEditLecture(it) },
                    onScheduleNewClick = { viewModel.openScheduleNew() }
                )
                1 -> CoursesScreen(
                    courses = courses,
                    onToggleEnrollment = { viewModel.toggleCourseEnrollment(it) }
                )
                2 -> DirectoryScreen(
                    contacts = directoryContacts
                )
                3 -> NoticesScreen(
                    notices = announcements,
                    currentRole = uiState.currentRole,
                    onPostNoticeClick = { viewModel.openPostNotice() }
                )
            }
        }
    }

    // Dialogs
    editingLecture?.let { lecture ->
        EditLectureDialog(
            lecture = lecture,
            onDismiss = { viewModel.closeEditLecture() },
            onDelete = { viewModel.deleteLecture(it) },
            onSave = { id, status, reason, venue, origVenue, startTime, endTime, origTime, broadcast, courseCode ->
                viewModel.saveLectureUpdate(
                    id = id,
                    status = status,
                    reason = reason,
                    venue = venue,
                    origVenue = origVenue,
                    startTime = startTime,
                    endTime = endTime,
                    origTime = origTime,
                    broadcast = broadcast,
                    courseCode = courseCode
                )
            }
        )
    }

    if (isSchedulingNew) {
        ScheduleLectureDialog(
            onDismiss = { viewModel.closeScheduleNew() },
            onSchedule = { code, title, day, start, end, venue, lecName, lecContact, dept, level, broadcast ->
                viewModel.scheduleNewLecture(
                    courseCode = code,
                    courseTitle = title,
                    dayOfWeek = day,
                    startTime = start,
                    endTime = end,
                    venue = venue,
                    lecturerName = lecName,
                    lecturerContact = lecContact,
                    department = dept,
                    level = level,
                    broadcast = broadcast
                )
            }
        )
    }

    if (isPostingNotice) {
        PostNoticeDialog(
            onDismiss = { viewModel.closePostNotice() },
            onBroadcast = { courseCode, title, content, urgency ->
                viewModel.broadcastAnnouncement(
                    courseCode = courseCode,
                    title = title,
                    content = content,
                    urgency = urgency
                )
            }
        )
    }

    alarmConfigLecture?.let { lecture ->
        AlarmConfigDialog(
            lecture = lecture,
            onDismiss = { viewModel.closeAlarmConfig() },
            onToggleAlarm = { isEnabled, minutes, ctx ->
                viewModel.toggleAlarmSetting(lecture, isEnabled, minutes, ctx)
            },
            onTriggerTestNow = { ctx ->
                viewModel.testTriggerAlarmNow(lecture, ctx)
            }
        )
    }
}
