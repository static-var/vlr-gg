package com.example.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.*

fun MacrobenchmarkScope.userflow() {
  pressHome()
  startActivityAndWait()
  // Accept notification permission
  device.acceptPermission()

  device.waitForIdle()

  device.findObject(By.text("Matches"))?.click() // Navigate to Match screen
  device.waitForIdle()

  device.findObject(By.text("Completed"))?.click() // Select Upcoming tab from Match screen
  device.visitMatchDetailsAndBack() // Visit match details page 2 times from Upcoming tab
  device.waitForIdle()

  device.findObject(By.text("Events"))?.click() // Navigate to Events screen
  device.visitEventDetailsAndBack() // Visit event details page 2 times from Event screen
  device.waitForIdle()

  device.findObject(By.text("Rankings"))?.click() // Navigate to Events screen
  device.visitRanksAndBack() // Visit team details page 2 times from Event screen
  device.waitForIdle()

  device.findObject(By.text("About"))?.click() // Navigate to About screen
  device.waitForIdle()

  device.findObject(By.text("News"))?.click() // Navigate to News screen
  device.readNewsAndBack()
  device.waitForIdle()
}

fun UiDevice.visitMatchDetailsAndBack() {
  wait(Until.gone(By.res("common:loader")), 30_000)
  val upcomingMatch = wait(Until.hasObject(By.res("matchOverview:result")), 30_000)
  if (upcomingMatch) {
    var visitedMatchDetails = 0
    var child = 0
    // Visit match details screen 2 times
    while (
      visitedMatchDetails < 2 &&
        child < (findObject(By.res("matchOverview:result"))?.children?.size ?: 0)
    ) {
      findObject(By.res("matchOverview:result"))?.children?.let { children ->
        if (children[child]?.text == null) { // isClickable didn't work
          children[child].click()
          wait(
            Until.hasObject(By.res("details:more_info")),
            30_000
          ) // Ensure static Ui elements are loaded
          wait(Until.gone(By.res("common:loader")), 30_000) // Ensure loader is gone
          findObject(By.res("matchDetails:mapHeader")).click()
          findObject(By.res("matchDetails:map")).click()
          wait(Until.hasObject(By.res("matchDetails:mapStats")), 30_000)

          findObject(By.res("matchDetails:mapStats")).fling(Direction.RIGHT)
          waitForIdle()

          findObject(By.res("matchDetails:root")).fling(Direction.DOWN)
          waitForIdle()
          pressBack()
          visitedMatchDetails++
        }
        child++
      }
    }
  }
}

fun UiDevice.visitEventDetailsAndBack() {
  wait(Until.gone(By.res("common:loader")), 30_000)
  val liveEvent = wait(Until.hasObject(By.res("eventOverview:live")), 30_000)
  if (liveEvent) {
    // Visit match details screen 2 times
    findObject(By.res("eventOverview:live"))?.fling(Direction.DOWN)
    repeat(2) {
      findObject(By.res("eventOverview:live"))?.children?.get(it)?.click()
      wait(
        Until.hasObject(By.res("eventDetails:teams")),
        30_000
      ) // Ensure static Ui elements are loaded
      wait(Until.hasObject(By.res("eventDetails:teams")), 30_000)
      wait(Until.gone(By.res("common:loader")), 30_000) // Ensure loader is gone
      findObject(By.res("eventDetails:teamList"))?.children?.get(0)?.click()
      wait(Until.hasObject(By.res("team:banner")), 30_0000)
      waitForIdle()
      pressBack()
      findObject(By.res("eventDetails:root")).fling(Direction.DOWN)
      waitForIdle()
      pressBack()
    }
  }
}

fun UiDevice.visitRanksAndBack() {
  wait(Until.gone(By.res("common:loader")), 30_000)
  val liveEvent = wait(Until.hasObject(By.res("rankOverview:live")), 30_000)
  if (liveEvent) {
    // Visit match details screen 2 times
    findObject(By.res("rankOverview:live"))?.fling(Direction.DOWN)
    repeat(2) {
      findObject(By.res("rankOverview:live"))?.children?.get(it + 2)?.click()
      wait(Until.gone(By.res("common:loader")), 30_000) // Ensure loader is gone
      wait(Until.hasObject(By.res("team:banner")), 30_0000)
      waitForIdle()
      findObject(By.res("team:roster"))?.click()
      pressBack()
    }
  }
}

fun UiDevice.readNewsAndBack() {
  val newsOverview = wait(Until.gone(By.res("common:loader")), 30_000) // Ensure loader is gone
  val newsRoot =
    wait(Until.hasObject(By.res("newsOverview:root")), 30_000) // Ensure news list is available
  if (newsOverview && newsRoot) {
    findObject(By.res("newsOverview:root"))?.children?.get(0)?.click()
    wait(Until.gone(By.res("common:loader")), 30_000) // Ensure loader is gone
    wait(Until.hasObject(By.res("news:root")), 30_000) // Ensure news details are drawn
    findObject(By.res("news:root"))?.fling(Direction.DOWN)
    waitForIdle()
    pressBack()
  }
}

fun UiDevice.acceptPermission() {
  val allowPermissions: UiObject = findObject(UiSelector().text("Allow"))
  if (allowPermissions.exists()) {
    try {
      allowPermissions.click()
    } catch (e: UiObjectNotFoundException) {
      println("There is no permissions dialog to interact with ")
    }
  }
}
