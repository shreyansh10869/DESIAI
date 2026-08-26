package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("My Test App", appName)
  }

  @Test
  fun `button click changes message text to Button clicked!`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        TestAppScreen()
      }
    }

    // Initial state
    composeTestRule.onNodeWithText("Press the button below").assertIsDisplayed()

    // Click the button
    composeTestRule.onNodeWithTag("click_me_button").performClick()

    // Message changes
    composeTestRule.onNodeWithText("Button clicked!").assertIsDisplayed()
  }
}

