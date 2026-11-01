package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LectureEntity
import com.example.data.model.LectureDay
import com.example.data.model.LectureStatus
import com.example.data.model.UrgencyLevel
import com.example.data.model.UserRole
import com.example.ui.theme.RoseCancelled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLectureDialog(
    lecture: LectureEntity,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onSave: (
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
    ) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(lecture.status) }
    var reasonText by remember { mutableStateOf(lecture.statusReason ?: "") }
    var venueText by remember { mutableStateOf(lecture.venue) }
    var startTimeText by remember { mutableStateOf(lecture.startTime) }
    var endTimeText by remember { mutableStateOf(lecture.endTime) }
    var broadcastNotice by remember { mutableStateOf(true) }

    var statusDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Manage ${lecture.courseCode}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = lecture.courseTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = statusDropdownExpanded,
                    onExpandedChange = { statusDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = LectureStatus.valueOf(selectedStatus).title,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Lecture Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        LectureStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.title) },
                                onClick = {
                                    selectedStatus = status.name
                                    statusDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Venue input
                OutlinedTextField(
                    value = venueText,
                    onValueChange = { venueText = it },
                    label = { Text("Lecture Venue / Hall") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Time Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTimeText,
                        onValueChange = { startTimeText = it },
                        label = { Text("Start Time") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTimeText,
                        onValueChange = { endTimeText = it },
                        label = { Text("End Time") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reason / Note
                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text("Update Reason / Student Note") },
                    placeholder = { Text("e.g. Relocated due to AC repair") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Broadcast notice toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { broadcastNotice = !broadcastNotice },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = broadcastNotice,
                        onCheckedChange = { broadcastNotice = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Broadcast urgent notice to all students",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val origVenue = if (venueText != lecture.venue) lecture.venue else lecture.originalVenue
                    val origTime = if (startTimeText != lecture.startTime) lecture.startTime else lecture.originalTime
                    onSave(
                        lecture.id,
                        selectedStatus,
                        reasonText.ifBlank { null },
                        venueText,
                        origVenue,
                        startTimeText,
                        endTimeText,
                        origTime,
                        broadcastNotice,
                        lecture.courseCode
                    )
                },
                modifier = Modifier.testTag("save_lecture_button")
            ) {
                Text("Save & Apply")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onDelete(lecture.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = RoseCancelled)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleLectureDialog(
    onDismiss: () -> Unit,
    onSchedule: (
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
    ) -> Unit
) {
    var courseCode by remember { mutableStateOf("") }
    var courseTitle by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf(LectureDay.MONDAY) }
    var startTime by remember { mutableStateOf("09:00 AM") }
    var endTime by remember { mutableStateOf("11:00 AM") }
    var venue by remember { mutableStateOf("LT 1, Science Complex") }
    var lecturerName by remember { mutableStateOf("Prof. Emmanuel Adeyemi") }
    var lecturerContact by remember { mutableStateOf("e.adeyemi@uni.edu.ng") }
    var department by remember { mutableStateOf("Computer Science") }
    var level by remember { mutableStateOf("300 Level") }
    var broadcast by remember { mutableStateOf(true) }

    var dayDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Schedule New Lecture",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = courseCode,
                    onValueChange = { courseCode = it.uppercase() },
                    label = { Text("Course Code (e.g. CSC 301)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_course_code")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = courseTitle,
                    onValueChange = { courseTitle = it },
                    label = { Text("Course Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Day Selector
                ExposedDropdownMenuBox(
                    expanded = dayDropdownExpanded,
                    onExpandedChange = { dayDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedDay.fullName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day of Week") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = dayDropdownExpanded,
                        onDismissRequest = { dayDropdownExpanded = false }
                    ) {
                        LectureDay.entries.forEach { day ->
                            DropdownMenuItem(
                                text = { Text(day.fullName) },
                                onClick = {
                                    selectedDay = day
                                    dayDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Hall / Venue") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lecturerName,
                    onValueChange = { lecturerName = it },
                    label = { Text("Lecturer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { broadcast = !broadcast },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = broadcast, onCheckedChange = { broadcast = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Broadcast announcement to student notice board",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (courseCode.isNotBlank() && courseTitle.isNotBlank()) {
                        onSchedule(
                            courseCode.trim(),
                            courseTitle.trim(),
                            selectedDay.fullName,
                            startTime.trim(),
                            endTime.trim(),
                            venue.trim(),
                            lecturerName.trim(),
                            lecturerContact.trim(),
                            department.trim(),
                            level.trim(),
                            broadcast
                        )
                    }
                },
                enabled = courseCode.isNotBlank() && courseTitle.isNotBlank(),
                modifier = Modifier.testTag("confirm_schedule_button")
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostNoticeDialog(
    onDismiss: () -> Unit,
    onBroadcast: (courseCode: String, title: String, content: String, urgency: String) -> Unit
) {
    var courseCode by remember { mutableStateOf("ALL") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedUrgency by remember { mutableStateOf(UrgencyLevel.URGENT) }

    var urgencyDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Broadcast Urgent Notice",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Announcements are immediately visible on student devices with priority tagging.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Urgency level
                ExposedDropdownMenuBox(
                    expanded = urgencyDropdownExpanded,
                    onExpandedChange = { urgencyDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedUrgency.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Urgency Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = urgencyDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = urgencyDropdownExpanded,
                        onDismissRequest = { urgencyDropdownExpanded = false }
                    ) {
                        UrgencyLevel.entries.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.label) },
                                onClick = {
                                    selectedUrgency = level
                                    urgencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = courseCode,
                    onValueChange = { courseCode = it.uppercase() },
                    label = { Text("Target Course (or ALL for all students)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notice Headline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("notice_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Announcement Details") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("notice_content_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onBroadcast(courseCode.trim(), title.trim(), content.trim(), selectedUrgency.name)
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                modifier = Modifier.testTag("broadcast_confirm_button")
            ) {
                Text("Broadcast Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AlarmConfigDialog(
    lecture: LectureEntity,
    onDismiss: () -> Unit,
    onToggleAlarm: (isEnabled: Boolean, minutesBefore: Int, context: Context) -> Unit,
    onTriggerTestNow: (context: Context) -> Unit
) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(lecture.isAlarmEnabled) }
    var selectedMinutes by remember { mutableIntStateOf(lecture.alarmMinutesBefore) }

    val options = listOf(10, 15, 30, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = "Lecture Alarm",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Lecture Alarm: ${lecture.courseCode}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${lecture.dayOfWeek} at ${lecture.startTime} (${lecture.venue})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle alarm enable
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Alarm Reminder",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        modifier = Modifier.testTag("alarm_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Notify me before class starts:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                options.forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isEnabled) { selectedMinutes = minutes },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMinutes == minutes && isEnabled,
                            onClick = { selectedMinutes = minutes },
                            enabled = isEnabled
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (minutes == 60) "1 hour before start" else "$minutes minutes before start",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Test Alarm button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTriggerTestNow(context) }
                        .testTag("test_alarm_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Test Notification",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Test Alarm Notification Now",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onToggleAlarm(isEnabled, selectedMinutes, context)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_alarm_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
