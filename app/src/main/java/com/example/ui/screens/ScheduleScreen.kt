package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.firestore.FirestoreLecture
import com.example.data.model.LectureDay
import com.example.data.model.UserRole
import com.example.ui.LectureLinkUiState
import com.example.ui.components.LectureCard
import com.example.ui.components.RoleSwitchBanner
import com.example.ui.theme.AmberRescheduled
import com.example.ui.theme.EmeraldLive

@Composable
fun ScheduleScreen(
    uiState: LectureLinkUiState,
    lectures: List<FirestoreLecture>,
    onRoleSelected: (UserRole) -> Unit,
    onDaySelected: (LectureDay?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onAlarmClick: (FirestoreLecture) -> Unit,
    onEditLectureClick: (FirestoreLecture) -> Unit,
    onScheduleNewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val liveCount = lectures.count { it.status == "ONGOING" }
    val alteredCount = lectures.count { it.status == "VENUE_CHANGED" || it.status == "RESCHEDULED" }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Role switcher header
            item {
                RoleSwitchBanner(
                    currentRole = uiState.currentRole,
                    onRoleSelected = onRoleSelected
                )
            }

            // Hero Banner Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.lecture_hero_banner),
                            contentDescription = "University Lecture Hall",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Direct Lecture Link",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Never miss a rescheduled hall or cancelled class.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (liveCount > 0 || alteredCount > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (liveCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = EmeraldLive.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "🟢 $liveCount In Session",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldLive,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (alteredCount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AmberRescheduled.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "⚡ $alteredCount Update Alerts",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = AmberRescheduled,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search input
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_lectures_input"),
                    placeholder = { Text("Search course, lecturer, or hall...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Days of the Week
            item {
                Column {
                    Text(
                        text = "Days of the Week",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.selectedDay == null,
                                onClick = { onDaySelected(null) },
                                label = { Text("All Week") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        items(LectureDay.entries) { day ->
                            FilterChip(
                                selected = uiState.selectedDay == day,
                                onClick = { onDaySelected(day) },
                                label = { Text(day.fullName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("day_chip_${day.shortName.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Status Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf(
                        "ALL" to "All Statuses",
                        "LIVE" to "🟢 Live Now",
                        "ALTERED" to "⚡ Moved / Rescheduled",
                        "SCHEDULED" to "🔵 On Schedule"
                    )
                    items(filters) { (key, label) ->
                        FilterChip(
                            selected = uiState.statusFilter == key,
                            onClick = { onStatusFilterChange(key) },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.selectedDay != null) "${uiState.selectedDay.fullName}'s Lectures" else "All Scheduled Lectures",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${lectures.size} lectures",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Lecture List or Empty State
            if (lectures.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = "No lectures",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No lectures match your current filters",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try selecting another day or enroll in courses in the Courses tab.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    onDaySelected(null)
                                    onStatusFilterChange("ALL")
                                    onSearchQueryChange("")
                                }
                            ) {
                                Text("Reset Filters")
                            }
                        }
                    }
                }
            } else {
                items(lectures, key = { it.id }) { lecture ->
                    LectureCard(
                        lecture = lecture,
                        currentRole = uiState.currentRole,
                        alarmMinutes = uiState.alarmMinutesMap[lecture.id],
                        onAlarmClick = { onAlarmClick(lecture) },
                        onEditClick = { onEditLectureClick(lecture) }
                    )
                }
            }
        }

        // Floating Action Button for Lecturer / Course Rep
        if (uiState.currentRole == UserRole.LECTURER || uiState.currentRole == UserRole.COURSE_REP) {
            ExtendedFloatingActionButton(
                onClick = onScheduleNewClick,
                icon = { Icon(Icons.Default.Add, contentDescription = "Schedule") },
                text = { Text("Schedule Lecture") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 20.dp)
                    .testTag("schedule_lecture_fab")
            )
        }
    }
}
