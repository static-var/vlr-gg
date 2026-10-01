/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.mp.KoinPlatform

class TeamLogoDirectoryTest {
  private lateinit var directory: File
  private lateinit var context: Context

  @Before
  fun createIsolatedFilesDirectory() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    directory = File(appContext.cacheDir, "team-logo-directory-test-${UUID.randomUUID()}")
    check(directory.mkdirs())
    context = object : ContextWrapper(appContext) {
      override fun getApplicationContext(): Context = this
      override fun getFilesDir(): File = directory
    }
  }

  @After
  fun removeIsolatedFilesDirectory() {
    if (::directory.isInitialized) directory.deleteRecursively()
  }

  @Test
  fun manifestKeepsAllowedLogosAndIgnoresMissingLogosAndUnknownMetadata() = runBlocking {
    writeManifest(
      """
      {
        "624":{"name":"Paper Rex","logo":{"url":"https://files.akhilnarang.dev/cdn/valorant/teams/624.png","width":256}},
        "474":{"logo":{"url":"https://owcdn.net/img/team.png"}},
        "1":{"name":"No logo"},
        "2":{"logo":null},
        "3":{"logo":{}},
        "4":{"logo":{"url":null}},
        "5":{"logo":{"url":"https://evil.example/team.png"}},
        "6":{"logo":{"url":"http://files.akhilnarang.dev/cdn/valorant/teams/6.png"}},
        "7":{"logo":{"url":"https://files.akhilnarang.dev.evil.example/team.png"}}
      }
      """.trimIndent(),
    )

    for (json in listOf(Json, KoinPlatform.getKoin().get<Json>())) {
      val logos = TeamLogoDirectory(context, json)
      assertTrue(logos.refresh())
      assertEquals("https://files.akhilnarang.dev/cdn/valorant/teams/624.png", logos.logoUrl("624"))
      assertEquals("https://owcdn.net/img/team.png", logos.logoUrl("474"))
      for (teamId in listOf(null, "1", "2", "3", "4", "5", "6", "7", "999")) {
        assertNull(teamId, logos.logoUrl(teamId))
      }
      assertFalse(logos.refresh())
    }
  }

  @Test
  fun malformedManifestDoesNotExposePartiallyDecodedLogos() = runBlocking {
    for (json in listOf(Json, KoinPlatform.getKoin().get<Json>())) {
      for (invalidEntry in listOf("null", "[]", """{"logo":[]}""", """{"logo":{"url":[]}}""")) {
        writeManifest(
          """{"624":{"logo":{"url":"https://files.akhilnarang.dev/cdn/valorant/teams/624.png"}},"474":$invalidEntry}""",
        )
        val logos = TeamLogoDirectory(context, json)
        assertFalse(invalidEntry, logos.refresh())
        assertNull(logos.logoUrl("624"))
        assertNull(logos.logoUrl("474"))
      }
      writeManifest("{not-json")
      val logos = TeamLogoDirectory(context, json)
      assertFalse(logos.refresh())
      assertNull(logos.logoUrl("624"))
    }
  }

  private fun writeManifest(body: String) {
    val manifest = File(directory, "team_logos.json")
    manifest.writeText(body)
    check(manifest.setLastModified(System.currentTimeMillis()))
  }
}
