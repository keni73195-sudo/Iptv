package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("OndeTV", appName)
  }

  @Test
  fun `manifest supports Android TV leanback launcher and non-touchscreen`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pm = context.packageManager
    val leanbackIntent =
      Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        setPackage(context.packageName)
      }
    val resolveInfos = pm.queryIntentActivities(leanbackIntent, PackageManager.MATCH_ALL)
    assertTrue("Expected MainActivity to declare LEANBACK_LAUNCHER", resolveInfos.isNotEmpty())

    val packageInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_CONFIGURATIONS)
    val features = packageInfo.reqFeatures?.toList().orEmpty()
    val touchscreenFeature = features.firstOrNull { it.name == PackageManager.FEATURE_TOUCHSCREEN }
    assertNotNull("Expected android.hardware.touchscreen uses-feature declaration", touchscreenFeature)
    assertFalse(
      "Touchscreen must not be required for Android TV",
      (touchscreenFeature!!.flags and android.content.pm.FeatureInfo.FLAG_REQUIRED) != 0,
    )
    assertNotNull(context.getDrawable(R.drawable.tv_banner))
  }
}
