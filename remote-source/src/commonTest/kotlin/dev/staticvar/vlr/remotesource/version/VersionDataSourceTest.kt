package dev.staticvar.vlr.remotesource.version

import dev.staticvar.vlr.remotesource.readFixture
import dev.staticvar.vlr.remotesource.singleResponseClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionDataSourceTest {
  @Test
  fun versions_parses_fixture() = runTest {
    val json = readFixture("version.json")
    val ds = VersionDataSourceImpl(singleResponseClient(json))
    val result = ds.versions()
    assertTrue(result.isSuccess)
    val v = result.getOrThrow()
    assertEquals(1, v.matchList)
  }
}
