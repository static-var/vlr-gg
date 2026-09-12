/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.baselineprofile

import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

internal const val TARGET_PACKAGE = "dev.staticvar.vlr"

internal fun MacrobenchmarkScope.startup() {
  pressHome()
  startActivityAndWait()
  device.requireObject(By.desc("Home"))
  device.requireObject(By.desc("Settings"))
}

internal fun UiDevice.browseNewsAndSettings() {
  browseNews()
  navigateTo("Home", "Your Favorites")
  requireObject(By.desc("Settings")).click(100)
  requireObject(By.text("// MAKE VLR YOURS."))
  scrollTo(By.text("About VLR")).click(100)
  requireObject(By.text("About"))
  pressBack()
  requireObject(By.text("// MAKE VLR YOURS."))
  pressBack()
  requireObject(By.desc("Settings"))
}

internal fun UiDevice.browseMatches() {
  navigateTo("Matches", "Results, schedules and live scores")
  selectTab("Live")
  browseListOrEmpty("No live matches")
  selectTab("Upcoming")
  browseListOrEmpty("No upcoming matches")
  selectTab("Completed")
  browseList()
  openFirstCard(By.clickable(true).longClickable(true))
  requireSubtitle("Maps, scores and player stats")
  val addedFavorite = addFavorite("match", inheritedDescription = "Favorite match")
  check(wait(Until.gone(By.textStartsWith("Loading match details")), TIMEOUT)) {
    "Match breakdown did not finish loading"
  }
  scrollTo(By.text(Pattern.compile("Maps|No match breakdown yet")))
  browseList()
  if (addedFavorite) {
    scrollTo(By.desc("Remove match from favorites"), Direction.UP).click(100)
    requireObject(By.desc("Add match to favorites"))
  }
  pressBack()
  requireSubtitle("Results, schedules and live scores")
}

internal fun UiDevice.browseEvents() {
  navigateTo("Events", "Tournaments around the world")
  selectTab("Ongoing")
  browseListOrEmpty("No live events")
  selectTab("Upcoming")
  browseListOrEmpty("No upcoming events")
  selectTab("Completed")
  browseList()
  openFirstCard(By.clickable(true).hasDescendant(By.text("COMPLETED")))
  requireSubtitle("Teams, matches and standings")
  scrollTo(By.text(Pattern.compile("Favorite event|Remove from favorites")))
  val addedFavorite = hasObject(By.text("Favorite event"))
  if (addedFavorite) {
    requireObject(By.clickable(true).enabled(true).hasDescendant(By.text("Favorite event"))).activate()
    requireObject(By.text("Remove from favorites"))
  }
  for (section in listOf("Standings", "Prizes", "Matches")) {
    scrollTo(By.text(section))
    selectTab(section)
    browseList(allowSinglePage = true)
  }
  if (addedFavorite) {
    scrollTo(By.clickable(true).enabled(true).hasDescendant(By.text("Remove from favorites")), Direction.UP).activate()
    requireObject(By.text("Favorite event"))
  }
  pressBack()
  requireSubtitle("Tournaments around the world")
}

internal fun UiDevice.browseTeamAndPlayer() {
  navigateTo("Ranking", "The top teams in every region")
  browseList()
  openFirstCard()
  requireSubtitle("Roster, results and recent form")
  val addedTeam = addFavorite("team")
  val teamName = checkNotNull(verticalList().findObject(By.clazz("android.widget.TextView"))?.text) {
    "Expected the team name in its summary card"
  }
  scrollTo(By.text("Roster"))
  openFirstCard()
  requireSubtitle("Stats, agents and team history")
  val addedPlayer = addFavorite("player")
  browseList(allowSinglePage = true)
  if (addedPlayer) {
    requireObject(By.desc("Remove player from favorites")).click(100)
    requireObject(By.desc("Add player to favorites"))
  }
  pressBack()
  requireSubtitle("Roster, results and recent form")
  scrollTo(By.text("Completed"))
  selectTab("Completed")
  browseList()
  scrollTo(By.text("Upcoming"))
  selectTab("Upcoming")
  pressBack()
  requireSubtitle("The top teams in every region")
  navigateTo("Home", "Your Favorites")
  scrollTo(By.text("Favorites"))
  scrollTo(By.text("Teams"))
  scrollTo(By.clickable(true).hasDescendant(By.text(teamName)).hasDescendant(By.text("→"))).activate()
  requireSubtitle("Roster, results and recent form")
  if (addedTeam) {
    requireObject(By.desc("Remove team from favorites")).click(100)
    requireObject(By.desc("Add team to favorites"))
  }
  pressBack()
  requireSubtitle("Your Favorites")
}

private fun UiDevice.addFavorite(kind: String, inheritedDescription: String? = null): Boolean {
  val add = By.desc("Add $kind to favorites")
  val remove = By.desc("Remove $kind from favorites")
  awaitUi("$kind favorite action") {
    hasObject(add) || hasObject(remove) || inheritedDescription?.let { hasObject(By.desc(it)) } == true
  }
  if (!hasObject(add)) return false
  requireObject(add).click(100)
  requireObject(remove)
  return true
}

private fun UiDevice.swipeList(direction: Direction, allowSinglePage: Boolean = false) {
  val bounds = awaitUiValue("current list bounds") {
    verticalListOrNull()?.visibleBounds ?: if (allowSinglePage) {
      Rect(0, displayHeight / 5, displayWidth, displayHeight * 9 / 10)
    } else null
  }
  val x = bounds.centerX()
  val upper = bounds.top + bounds.height() / 5
  val lower = bounds.bottom - bounds.height() / 5
  when (direction) {
    Direction.DOWN -> swipe(x, lower, x, upper, 30)
    Direction.UP -> swipe(x, upper, x, lower, 30)
    else -> error("Expected a vertical list direction")
  }
  waitForIdle(2_000)
}

private fun UiDevice.browseNews() {
  navigateTo("News", "Stories from competitive VALORANT")
  browseList()
  openFirstCard()
  requireObject(By.desc("Back"))
  awaitUi("loaded article content") {
    hasObject(By.text("Read on VLR.gg")) || verticalListOrNull() != null
  }
  browseList(allowSinglePage = true)
  scrollTo(By.text("Read on VLR.gg"))
  findObject(By.text("Top"))?.click(100)
  pressBack()
  requireSubtitle("Stories from competitive VALORANT")
}

private fun UiDevice.navigateTo(destination: String, subtitle: String) {
  requireObject(By.desc(destination)).click(100)
  requireSubtitle(subtitle)
}

private fun UiDevice.requireSubtitle(subtitle: String) {
  requireObject(By.text("// ${subtitle.uppercase()}"))
}

private fun UiDevice.selectTab(label: String) {
  val selectedTab = By.selected(true).hasDescendant(By.text(label))
  repeat(3) {
    if (hasObject(selectedTab)) return
    requireObject(By.clickable(true).hasDescendant(By.text(label))).activate()
    if (wait(Until.hasObject(selectedTab), 1_500L)) {
      waitForIdle(2_000)
      return
    }
  }
  requireObject(selectedTab)
}

private fun UiDevice.browseListOrEmpty(emptyTitle: String) {
  awaitUi("a populated list or '$emptyTitle'") {
    hasObject(By.text(emptyTitle)) || verticalListOrNull()?.hasObject(By.clickable(true)) == true
  }
  if (!hasObject(By.text(emptyTitle))) browseList()
}

private fun UiDevice.browseList(allowSinglePage: Boolean = false) {
  if (!allowSinglePage) awaitUi("loaded scrollable content") { verticalListOrNull() != null }
  repeat(2) { swipeList(Direction.DOWN, allowSinglePage) }
  repeat(2) { swipeList(Direction.UP, allowSinglePage) }
}

private fun UiDevice.openFirstCard(selector: BySelector = By.clickable(true)) {
  waitForIdle(2_000)
  awaitUiValue("a clickable card in the content list") {
    verticalListOrNull()?.let { list ->
      val viewport = list.visibleBounds
      val card = list.findObjects(selector).firstOrNull { card ->
        val bounds = card.visibleBounds
        bounds.height() > 80 && bounds.top >= viewport.top + 8 && bounds.bottom <= viewport.bottom - 8
      } ?: return@awaitUiValue null
      val bounds = card.visibleBounds
      SystemClock.sleep(200)
      if (card.visibleBounds == bounds) {
        Log.i("VlrProfile", "Opening card at $bounds: ${card.findObjects(By.clazz("android.widget.TextView")).map { it.text }}")
        card.click(100)
      } else null
    }
  }
}

private fun UiDevice.verticalListOrNull(): UiObject2? =
  findObjects(By.scrollable(true))
    .filter { it.visibleBounds.height() > displayHeight / 3 }
    .lastOrNull()

private fun UiDevice.verticalList(): UiObject2 =
  checkNotNull(verticalListOrNull()) { "Expected a vertical VLR content list" }

private fun UiDevice.scrollTo(selector: BySelector, direction: Direction = Direction.DOWN): UiObject2 {
  awaitUi("scrollable content or $selector") { hasObject(selector) || verticalListOrNull() != null }
  repeat(12) {
    findObject(selector)?.let { return it }
    swipeList(direction)
  }
  return requireObject(selector)
}

private fun UiDevice.awaitUi(description: String, condition: UiDevice.() -> Boolean) {
  awaitUiValue(description) { true.takeIf { condition() } }
}

private fun <T> UiDevice.awaitUiValue(description: String, query: UiDevice.() -> T?): T {
  val deadline = SystemClock.uptimeMillis() + TIMEOUT
  while (true) {
    try {
      query()?.let { return it }
    } catch (_: StaleObjectException) {
      // Compose replaced the accessibility node; query the current tree again.
    }
    check(SystemClock.uptimeMillis() < deadline) { "Expected VLR $description; verify backend data and connectivity" }
    SystemClock.sleep(100)
  }
}

private fun UiDevice.requireObject(selector: BySelector): UiObject2 =
  checkNotNull(wait(Until.findObject(selector), TIMEOUT)) {
    val visibleText = findObjects(By.clazz("android.widget.TextView")).map { it.text }.take(12)
    "Expected VLR UI element was not displayed: $selector. Visible text: $visibleText"
  }

private fun UiObject2.activate() {
  check(accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
    "The selected control did not accept its accessibility click action"
  }
}

private const val TIMEOUT = 30_000L
