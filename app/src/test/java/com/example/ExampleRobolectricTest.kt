package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.LectureDay
import com.example.data.model.LectureStatus
import com.example.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("LectureLink", appName)
  }

  @Test
  fun `verify enum values`() {
    assertEquals("Student", UserRole.STUDENT.label)
    assertEquals("Live Now", LectureStatus.ONGOING.title)
    assertEquals("Monday", LectureDay.MONDAY.fullName)
  }
}
