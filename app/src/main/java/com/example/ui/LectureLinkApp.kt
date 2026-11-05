package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CastForEducation
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthManager
import com.example.data.local.DirectoryContactEntity
import com.example.data.model.UserRole
import com.example.ui.components.AlarmConfigDialog
import com.example.ui.components.CreateCourseDialog
import com.example.ui.components.EditLectureDialog
import com.example.ui.components.PostNoticeDialog
import com.example.ui.components.ScheduleLectureDialog
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CoursesScreen
import com.example.ui.screens.DirectoryScreen
import com.example.ui.screens.NoticesScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.theme.AmberRescheduled
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldLive
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

@Composable
fun LectureLinkApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    var initialRoleName by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Attempt silent auto-sign in on launch if not authenticated
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = coroutineScope
            )
        }
    }

    // Auth Gate: if not signed in, show AuthScreen with Google Sign-In
    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = { user, roleName ->
                initialRoleName = roleName
                currentUser = user
            }
        )
    } else {
        AuthenticatedLectureContent(
            currentUser = currentUser!!,
            initialRoleName = initialRoleName,
            onSignOut = {
                AuthManager.signOut(
                    context = context,
                    credentialManager = credentialManager,
                    scope = coroutineScope,
                    onComplete = {
                        initialRoleName = null
                        currentUser = null
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedLectureContent(
    currentUser: FirebaseUser,
    initialRoleName: String?,
    onSignOut: () -> Unit,
    viewModel: LectureLinkViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(initialRoleName) {
        initialRoleName?.let { roleName ->
            val role = when (roleName) {
                "Lecturer" -> UserRole.LECTURER
                "Course Rep" -> UserRole.COURSE_REP
                else -> UserRole.STUDENT
            }
            viewModel.switchRole(role)
        }
    }

    // Permission launcher for Android 13+ push notifications (requested safely after sign-in)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lectures by viewModel.filteredLectures.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()

    val editingLecture by viewModel.editingLecture.collectAsStateWithLifecycle()
    val isSchedulingNew by viewModel.isSchedulingNew.collectAsStateWithLifecycle()
    val isCreatingCourse by viewModel.isCreatingCourse.collectAsStateWithLifecycle()
    val isPostingNotice by viewModel.isPostingNotice.collectAsStateWithLifecycle()
    val alarmConfigLecture by viewModel.alarmConfigLecture.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

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
                        Column {
                            Text(
                                text = "LectureLink",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentUser?.email ?: "Direct Lecture Connection",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    // Active Role Pill
                    val (roleColor, roleIcon) = when (uiState.currentRole) {
                        UserRole.STUDENT -> BluePrimary to Icons.Default.School
                        UserRole.COURSE_REP -> AmberRescheduled to Icons.Default.Campaign
                        UserRole.LECTURER -> EmeraldLive to Icons.Default.CastForEducation
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = roleColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 4.dp)
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

                    // Sign-Out button
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

                // Tab 2: AI & Audio (Gemini Multi-turn Chat & gemini-3.5-transcribe)
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 2) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "AI & Voice"
                        )
                    },
                    label = { Text("AI & Voice", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_ai_voice")
                )

                // Tab 3: Directory
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 3) Icons.Filled.People else Icons.Outlined.People,
                            contentDescription = "Directory"
                        )
                    },
                    label = { Text("Directory", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_directory")
                )

                // Tab 4: Notices Bulletin
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (announcements.isNotEmpty()) {
                                    Badge { Text("${announcements.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == 4) Icons.Filled.Campaign else Icons.Outlined.Campaign,
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
                    enrolledCodes = uiState.enrolledCourseCodes,
                    currentRole = uiState.currentRole,
                    onToggleEnrollment = { viewModel.toggleCourseEnrollment(it) },
                    onCreateCourseClick = { viewModel.openCreateCourse() }
                )
                2 -> AiAssistantScreen()
                3 -> {
                    // Directory of verified Reps and Lecturers
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
                            id = "lec_1",
                            name = "Prof. Emmanuel Adeyemi",
                            role = "LECTURER",
                            department = "Computer Science",
                            level = "Faculty Staff",
                            email = "e.adeyemi@uni.edu.ng",
                            phone = "+234 803 999 1100",
                            officeRoom = "Block B, Room 204",
                            note = "Consultation hours: Mon & Wed 2:00 PM - 4:00 PM."
                        ),
                        DirectoryContactEntity(
                            id = "lec_2",
                            name = "Dr. Amina Bello",
                            role = "LECTURER",
                            department = "Computer Science",
                            level = "Faculty Staff",
                            email = "a.bello@uni.edu.ng",
                            phone = "+234 806 777 2211",
                            officeRoom = "Block C, Faculty Lab 3 Office",
                            note = "Head of Database Lab. Office hours: Tue & Thu 11:00 AM - 1:00 PM."
                        )
                    )
                    DirectoryScreen(contacts = directory)
                }
                4 -> NoticesScreen(
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
                    courseCode = courseCode,
                    context = context
                )
            }
        )
    }

    if (isSchedulingNew) {
        ScheduleLectureDialog(
            onDismiss = { viewModel.closeScheduleNew() },
            onSchedule = { code, title, day, start, end, venue, lecName, dept, level, broadcast ->
                viewModel.scheduleNewLecture(
                    courseCode = code,
                    courseTitle = title,
                    dayOfWeek = day,
                    startTime = start,
                    endTime = end,
                    venue = venue,
                    lecturerName = lecName,
                    department = dept,
                    level = level,
                    broadcast = broadcast,
                    context = context
                )
            }
        )
    }

    if (isCreatingCourse) {
        CreateCourseDialog(
            onDismiss = { viewModel.closeCreateCourse() },
            onCreateCourse = { code, name, dept, lvl, credits, repName ->
                viewModel.createNewCourse(code, name, dept, lvl, credits, repName)
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
                    urgency = urgency,
                    context = context
                )
            }
        )
    }

    alarmConfigLecture?.let { lecture ->
        AlarmConfigDialog(
            lecture = lecture,
            currentMinutes = uiState.alarmMinutesMap[lecture.id],
            onDismiss = { viewModel.closeAlarmConfig() },
            onToggleAlarm = { isEnabled, minutes, ctx ->
                viewModel.setLectureAlarm(lecture, isEnabled, minutes, ctx)
            },
            onTriggerTestNow = { ctx ->
                viewModel.testTriggerAlarmNow(lecture, ctx)
            }
        )
    }
}
