/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LauncherIconTaskTest {
  private fun awaitResumedMainActivity(): MainActivity {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val deadline = SystemClock.uptimeMillis() + 15_000
    while (SystemClock.uptimeMillis() < deadline) {
      var resumed: MainActivity? = null
      instrumentation.runOnMainSync {
        resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
          .filterIsInstance<MainActivity>().firstOrNull()
      }
      resumed?.let { return it }
      SystemClock.sleep(100)
    }
    throw AssertionError("MainActivity did not resume after launching from the icon alias")
  }

  @Test
  fun disablingLauncherAliasPreservesRunningActivityAndTask() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = instrumentation.targetContext
    val packages = context.packageManager
    val launcher = packages.getLaunchIntentForPackage(context.packageName)!!
    val original = launcher.component!!
    val replacement = ComponentName(context.packageName,
      "dev.staticvar.vlr.android.MainActivity" +
        if (original.className.endsWith("Ticket")) "Default" else "Ticket")
    val originalState = packages.getComponentEnabledSetting(original)
    val replacementState = packages.getComponentEnabledSetting(replacement)
    // The alias opens LauncherActivity, which hands off to MainActivity in its own task.
    context.startActivity(launcher.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    val activity = awaitResumedMainActivity()
    val tasks = context.getSystemService(ActivityManager::class.java)
    fun assertPermanentTaskIdentity() {
      instrumentation.waitForIdleSync()
      // The launcher's short-lived task can disappear while the list is being read.
      val task = tasks.appTasks.mapNotNull { runCatching { it.taskInfo }.getOrNull() }
        .single { it.taskId == activity.taskId }
      assertEquals(ComponentName(context, MainActivity::class.java), task.baseIntent.component)
      assertFalse(activity.isFinishing)
      assertFalse(activity.isDestroyed)
      instrumentation.runOnMainSync {
        assertEquals(Stage.RESUMED, ActivityLifecycleMonitorRegistry.getInstance().getLifecycleStageOf(activity))
      }
    }
    try {
      assertPermanentTaskIdentity()
      packages.setComponentEnabledSetting(replacement, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP)
      packages.setComponentEnabledSetting(original, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP)
      SystemClock.sleep(2000)
      assertPermanentTaskIdentity()
      context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(replacement).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      instrumentation.waitForIdleSync()
      assertPermanentTaskIdentity()
      packages.setComponentEnabledSetting(original, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP)
      packages.setComponentEnabledSetting(replacement, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP)
      SystemClock.sleep(2000)
      assertPermanentTaskIdentity()
    } finally {
      instrumentation.runOnMainSync { activity.finishAndRemoveTask() }
      packages.setComponentEnabledSetting(original, originalState, PackageManager.DONT_KILL_APP)
      packages.setComponentEnabledSetting(replacement, replacementState, PackageManager.DONT_KILL_APP)
    }
  }
}
