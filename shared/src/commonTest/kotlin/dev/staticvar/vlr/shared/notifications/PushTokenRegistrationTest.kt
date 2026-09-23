/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.notifications

import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.core.identity.UserIdentityRepository
import dev.staticvar.vlr.core.notifications.NotificationAuthorization
import dev.staticvar.vlr.core.notifications.NotificationPermissionProvider
import dev.staticvar.vlr.core.notifications.PushPlatform
import dev.staticvar.vlr.core.notifications.PushTokenProvider
import dev.staticvar.vlr.core.settings.LiveMatchNotificationPreferencesRepository
import dev.staticvar.vlr.core.settings.LiveMatchNotificationSettingsController
import dev.staticvar.vlr.core.settings.PushTokenRegistrationPreferencesRepository
import dev.staticvar.vlr.remotesource.liveupdates.PushTokenRegistrationDataSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Checks token registration across permission, lifecycle, and identity changes. */
class PushTokenRegistrationTest {
  @Test
  fun synchronousNativeTokenIsStoredAndUploadedOnlyAfterEveryGatePasses() = runTest {
    val harness = Harness(backgroundScope, notificationsEnabled = true).apply {
      tokenProvider.tokenOnStart = "cached-native-token"
      permissionProvider.activitiesEnabled = true
    }

    harness.coordinator.start()
    harness.coordinator.onForeground()
    runCurrent()
    assertEquals(0, harness.tokenProvider.starts)
    assertEquals(listOf(LiveUpdateEligibility.Pending), harness.eligibility)
    harness.permissionProvider.completeRead(NotificationAuthorization.Authorized)
    runCurrent()

    assertEquals(1, harness.tokenProvider.starts)
    assertEquals("cached-native-token", harness.tokenPreferences.preferences.value.token)
    assertEquals(
      listOf(RegistrationRequest(harness.identity.id.value.toString(), PushPlatform.Ios, "cached-native-token")),
      harness.dataSource.requests,
    )
    assertTrue(
      harness.tokenPreferences.preferences.value.wasUploaded(
        harness.identity.id.value.toString(),
        PushPlatform.Ios,
        "cached-native-token",
      ),
    )
  }

  @Test
  fun deniedPermissionAndDisabledLiveActivitiesNeverStartNativeTokenProvider() = runTest {
    val denied = Harness(backgroundScope, notificationsEnabled = true)
    denied.coordinator.start()
    denied.coordinator.onForeground()
    runCurrent()
    denied.permissionProvider.completeRead(NotificationAuthorization.Denied)
    runCurrent()
    assertEquals(0, denied.tokenProvider.starts)
    assertTrue(denied.dataSource.requests.isEmpty())

    val activitiesDisabled = Harness(backgroundScope, notificationsEnabled = true).apply {
      permissionProvider.activitiesEnabled = false
    }
    activitiesDisabled.coordinator.start()
    activitiesDisabled.coordinator.onForeground()
    runCurrent()
    activitiesDisabled.permissionProvider.completeRead(NotificationAuthorization.Authorized)
    runCurrent()
    assertEquals(0, activitiesDisabled.tokenProvider.starts)
    assertTrue(activitiesDisabled.dataSource.requests.isEmpty())
  }

  @Test
  fun liveActivitiesIgnoreDeniedOrdinaryNotificationAuthorizationWithoutReadingIt() = runTest {
    val harness = Harness(backgroundScope, notificationsEnabled = true).apply {
      permissionProvider.requiresPermission = false
      permissionProvider.activitiesEnabled = true
      tokenProvider.tokenOnStart = "activity-token"
    }

    harness.coordinator.start()
    harness.coordinator.onForeground()
    runCurrent()

    assertEquals(0, harness.permissionProvider.reads)
    assertEquals(1, harness.tokenProvider.starts)
    assertEquals("activity-token", harness.dataSource.requests.single().token)
    assertEquals(listOf(LiveUpdateEligibility.Pending, LiveUpdateEligibility.Enabled), harness.eligibility)

    harness.coordinator.onAuthorizationChanged(NotificationAuthorization.Denied)
    runCurrent()

    assertEquals(1, harness.tokenProvider.starts)
    assertEquals(0, harness.tokenProvider.stops)
    assertEquals(1, harness.dataSource.requests.size)
  }

  @Test
  fun liveActivitiesStillRequireActivityKitCapability() = runTest {
    val harness = Harness(backgroundScope, notificationsEnabled = true).apply {
      permissionProvider.requiresPermission = false
      permissionProvider.activitiesEnabled = false
      tokenProvider.tokenOnStart = "must-not-upload"
    }

    harness.coordinator.start()
    harness.coordinator.onForeground()
    runCurrent()

    assertEquals(0, harness.permissionProvider.reads)
    assertEquals(0, harness.tokenProvider.starts)
    assertTrue(harness.dataSource.requests.isEmpty())
  }

  @Test
  fun unsupportedPlatformDoesNotStartTokenProviderForRestoredOptIn() = runTest {
    val harness = Harness(backgroundScope, notificationsEnabled = true).apply {
      permissionProvider.supported = false
      tokenProvider.tokenOnStart = "must-not-upload"
    }

    harness.coordinator.start()
    harness.coordinator.onForeground()
    runCurrent()

    assertEquals(0, harness.permissionProvider.reads)
    assertEquals(0, harness.tokenProvider.starts)
    assertTrue(harness.dataSource.requests.isEmpty())
  }

  @Test
  fun settingsGrantStartsRegistrationAfterSettingsOwnerIsGone() = runTest {
    val harness = Harness(backgroundScope, notificationsEnabled = false).apply {
      tokenProvider.tokenOnStart = "after-grant"
    }
    harness.coordinator.start()
    harness.coordinator.onForeground()
    runCurrent()
    harness.permissionProvider.completeRead(NotificationAuthorization.NotDetermined)
    runCurrent()
    var controller: LiveMatchNotificationSettingsController? = LiveMatchNotificationSettingsController(
      harness.notificationPreferences,
      harness.permissionProvider,
      harness.coordinator::onAuthorizationChanged,
    )

    controller!!.setEnabled(true)
    runCurrent()
    controller = null
    harness.permissionProvider.completeRequest(NotificationAuthorization.Authorized)
    runCurrent()

    assertEquals(1, harness.tokenProvider.starts)
    assertEquals("after-grant", harness.dataSource.requests.single().token)
    assertEquals(null, controller)
  }

  @Test
  fun rotationAndLateCloudUuidKeepOnlyTheLocalClientRegistered() = runTest {
    val harness = Harness(backgroundScope, notificationsEnabled = true, trackCloudBackup = true).apply {
      tokenProvider.tokenOnStart = "token-one"
    }
    harness.coordinator.start()
    harness.coordinator.onForeground()
    runCurrent()
    harness.permissionProvider.completeRead(NotificationAuthorization.Authorized)
    runCurrent()

    harness.tokenProvider.emit("token-one")
    harness.tokenProvider.emit("token-two")
    harness.tokenProvider.emit("token-two")
    runCurrent()
    val restoredId = "01996ff9-3000-7000-8000-000000000002"
    val localId = harness.identity.id.value.toString()
    harness.identity.observeCloudIdentity(restoredId)
    runCurrent()

    assertEquals(
      listOf("token-one", "token-two"),
      harness.dataSource.requests.map(RegistrationRequest::token),
    )
    assertEquals(listOf(localId), harness.dataSource.requests.map(RegistrationRequest::clientId).distinct())
    assertEquals(listOf(restoredId), harness.dataSource.deletions)
    assertEquals(null, harness.identity.pendingTokenCleanup.value)
  }

  @Test
  fun oldTokenDeletionWaitsForCurrentRegistrationAndDoesNotDeleteTheActiveUuid() = runTest {
    val registrationGate = CompletableDeferred<Unit>()
    val harness = Harness(backgroundScope, notificationsEnabled = true, trackCloudBackup = true).apply {
      tokenProvider.tokenOnStart = "active-token"
      dataSource.beforeResponse = registrationGate
    }
    harness.coordinator.start()
    harness.coordinator.onForeground()
    runCurrent()
    harness.permissionProvider.completeRead(NotificationAuthorization.Authorized)
    runCurrent()

    val activeId = harness.identity.id.value.toString()
    val oldId = "01996ff9-3000-7000-8000-000000000002"
    harness.identity.observeCloudIdentity(oldId)
    runCurrent()
    assertTrue(harness.dataSource.deletions.isEmpty())

    registrationGate.complete(Unit)
    runCurrent()
    assertEquals(listOf(oldId), harness.dataSource.deletions)
    assertFalse(activeId in harness.dataSource.deletions)
  }

  @Test
  fun failedDeletionWaitsForForegroundRetryAndSurvivesRestartWithoutAnotherPut() = runTest {
    val identitySettings = MapSettings()
    val tokenSettings = MapSettings()
    val first = Harness(
      backgroundScope,
      notificationsEnabled = true,
      trackCloudBackup = true,
      identitySettings = identitySettings,
      tokenSettings = tokenSettings,
    ).apply {
      tokenProvider.tokenOnStart = "active-token"
      dataSource.deleteSucceeds = false
    }
    first.coordinator.start()
    first.coordinator.onForeground()
    runCurrent()
    first.permissionProvider.completeRead(NotificationAuthorization.Authorized)
    runCurrent()
    val oldId = "01996ff9-3000-7000-8000-000000000002"
    first.identity.observeCloudIdentity(oldId)
    runCurrent()
    assertEquals(listOf(oldId), first.dataSource.deletions)
    assertEquals(1, first.dataSource.requests.size)
    runCurrent()
    assertEquals(1, first.dataSource.deletions.size)

    first.uploader.retry()
    runCurrent()
    assertEquals(2, first.dataSource.deletions.size)
    assertEquals(1, first.dataSource.requests.size)

    val restarted = Harness(
      backgroundScope,
      notificationsEnabled = true,
      trackCloudBackup = true,
      identitySettings = identitySettings,
      tokenSettings = tokenSettings,
    )
    runCurrent()
    assertEquals(listOf(oldId), restarted.dataSource.deletions)
    assertTrue(restarted.dataSource.requests.isEmpty())
    assertEquals(null, restarted.identity.pendingTokenCleanup.value)
  }

  @Test
  fun appScopeUploadFinishesAfterCoordinatorStopsAndLateNativeTokenIsIgnored() = runTest {
    val releaseUpload = CompletableDeferred<Unit>()
    val harness = Harness(backgroundScope, notificationsEnabled = true).apply {
      tokenProvider.tokenOnStart = "in-flight"
      dataSource.beforeResponse = releaseUpload
    }
    harness.coordinator.start()
    harness.coordinator.onForeground()
    runCurrent()
    harness.permissionProvider.completeRead(NotificationAuthorization.Authorized)
    runCurrent()
    assertEquals(1, harness.dataSource.requests.size)

    harness.coordinator.stop()
    harness.tokenProvider.emit("late-token")
    releaseUpload.complete(Unit)
    runCurrent()

    assertEquals(listOf("in-flight"), harness.dataSource.requests.map(RegistrationRequest::token))
    assertTrue(
      harness.tokenPreferences.preferences.value.wasUploaded(
        harness.identity.id.value.toString(),
        PushPlatform.Ios,
        "in-flight",
      ),
    )
    assertFalse(harness.tokenPreferences.preferences.value.token == "late-token")
  }

  @Test
  fun failedUploadKeepsTokenWithoutMarkingTheTupleUploaded() = runTest {
    val harness = Harness(backgroundScope, notificationsEnabled = true).apply {
      tokenProvider.tokenOnStart = "not-uploaded"
      dataSource.succeeds = false
    }
    harness.coordinator.start()
    harness.coordinator.onForeground()
    runCurrent()
    harness.permissionProvider.completeRead(NotificationAuthorization.Authorized)
    runCurrent()

    assertEquals("not-uploaded", harness.tokenPreferences.preferences.value.token)
    assertFalse(
      harness.tokenPreferences.preferences.value.wasUploaded(
        harness.identity.id.value.toString(),
        PushPlatform.Ios,
        "not-uploaded",
      ),
    )
    assertEquals(1, harness.dataSource.requests.size)
  }

  @Test
  fun favoriteSyncWaitsForAccessAndClearsOnlyAfterAConfirmedDenial() = runTest {
    val harness = Harness(backgroundScope, notificationsEnabled = true)
    harness.coordinator.start()
    harness.coordinator.onForeground()
    runCurrent()

    assertEquals(listOf(LiveUpdateEligibility.Pending), harness.eligibility)

    harness.permissionProvider.completeRead(NotificationAuthorization.Error)
    runCurrent()
    assertEquals(listOf(LiveUpdateEligibility.Pending), harness.eligibility)

    harness.coordinator.onAuthorizationChanged(NotificationAuthorization.Denied)
    runCurrent()
    assertEquals(
      listOf(LiveUpdateEligibility.Pending, LiveUpdateEligibility.Disabled),
      harness.eligibility,
    )

    harness.coordinator.stop()
    runCurrent()
    assertEquals(2, harness.eligibility.size)
  }
}

/** Connects token registration to controllable platform providers and storage. */
private class Harness(
  scope: kotlinx.coroutines.CoroutineScope,
  notificationsEnabled: Boolean,
  trackCloudBackup: Boolean = false,
  identitySettings: MapSettings = MapSettings(),
  tokenSettings: MapSettings = MapSettings(),
) {
  val permissionProvider = FakePermissionProvider()
  val tokenProvider = FakePushTokenProvider()
  val dataSource = FakeRegistrationDataSource()
  val notificationPreferences = LiveMatchNotificationPreferencesRepository(MapSettings()).apply {
    setEnabled(notificationsEnabled)
  }
  val tokenPreferences = PushTokenRegistrationPreferencesRepository(tokenSettings)
  val identity = UserIdentityRepository(identitySettings, trackCloudBackup = trackCloudBackup)
  val eligibility = mutableListOf<LiveUpdateEligibility>()
  val uploader = PushTokenRegistrationUploader(tokenPreferences, identity, dataSource, scope)
  val coordinator = PushTokenRegistrationCoordinator(
    pushTokenProvider = tokenProvider,
    permissionProvider = permissionProvider,
    notificationPreferences = notificationPreferences,
    uploader = uploader,
    mainScope = scope,
    onEligibilityChanged = eligibility::add,
  )
}

/** Lets tests control platform capabilities and permission callbacks. */
private class FakePermissionProvider : NotificationPermissionProvider {
  var supported: Boolean = true
  var requiresPermission: Boolean = true
  var activitiesEnabled: Boolean? = null
  var reads: Int = 0
  private var readCallback: ((NotificationAuthorization) -> Unit)? = null
  private var requestCallback: ((NotificationAuthorization) -> Unit)? = null

  override fun supportsLiveUpdates(): Boolean = supported

  override fun requiresNotificationPermission(): Boolean = requiresPermission

  override fun areLiveActivitiesEnabled(): Boolean? = activitiesEnabled

  override fun readNotificationAuthorization(onResult: (NotificationAuthorization) -> Unit) {
    reads++
    readCallback = onResult
  }

  override fun requestNotificationAuthorization(onResult: (NotificationAuthorization) -> Unit) {
    requestCallback = onResult
  }

  override fun openSettings() = Unit

  fun completeRead(result: NotificationAuthorization) {
    checkNotNull(readCallback)(result)
  }

  fun completeRequest(result: NotificationAuthorization) {
    checkNotNull(requestCallback)(result)
  }
}

/** Emits test push tokens and records provider starts and stops. */
private class FakePushTokenProvider : PushTokenProvider {
  override val platform: PushPlatform = PushPlatform.Ios
  var tokenOnStart: String? = null
  var starts: Int = 0
  var stops: Int = 0
  private var callback: ((String) -> Unit)? = null

  override fun start(onToken: (String) -> Unit) {
    starts++
    callback = onToken
    tokenOnStart?.let(onToken)
  }

  override fun stop() {
    stops++
  }

  fun emit(token: String) {
    callback?.invoke(token)
  }
}

/** Records a client, platform, and token uploaded by a test. */
private data class RegistrationRequest(val clientId: String, val platform: PushPlatform, val token: String)

/** Records token uploads and lets tests delay or fail responses. */
private class FakeRegistrationDataSource : PushTokenRegistrationDataSource {
  val requests = mutableListOf<RegistrationRequest>()
  val deletions = mutableListOf<String>()
  var beforeResponse: CompletableDeferred<Unit>? = null
  var succeeds: Boolean = true
  var deleteSucceeds: Boolean = true

  override suspend fun register(clientId: String, platform: PushPlatform, token: String): Boolean {
    requests += RegistrationRequest(clientId, platform, token)
    beforeResponse?.await()
    return succeeds
  }

  override suspend fun delete(clientId: String): Boolean {
    deletions += clientId
    return deleteSucceeds
  }
}
