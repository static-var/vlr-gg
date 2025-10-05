package dev.staticvar.vlr.remotesource.rankings

import dev.staticvar.vlr.remotesource.readFixture
import dev.staticvar.vlr.remotesource.singleResponseClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class RankingsDataSourceTest {

  @Test
  fun list_parses_real_rankings_fixture() = runTest {
    val json = readFixture("rankings.json")
    val ds = RankingsDataSourceImpl(singleResponseClient(json))
    val rankings = ds.list().getOrThrow()
    assertTrue(rankings.isNotEmpty(), "Rankings list should not be empty")
    val firstRegion = rankings.first()
    assertTrue(firstRegion.teams.isNotEmpty(), "First region should contain teams")
    // basic sanity on region name not blank
    assertTrue(firstRegion.region.isNotBlank())
  }
}
