package dev.staticvar.vlr.remotesource.search

import dev.staticvar.vlr.remotesource.common.SearchCategory
import dev.staticvar.vlr.remotesource.common.SearchCategoryNullableSerializer
import dev.staticvar.vlr.remotesource.readFixture
import dev.staticvar.vlr.remotesource.singleResponseClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchDataSourceTest {

  @Test
  fun search_parses_real_search_fixture() = runTest {
    val json = readFixture("search_teams_fnatic.json")
    val ds = SearchDataSourceImpl(singleResponseClient(json))
    val results = ds.search(SearchCategory.TEAM, "fnatic").getOrThrow()
    assertTrue(results.isNotEmpty())
    val first = results.first()
    assertTrue(first.name.contains("fnatic", ignoreCase = true))
  }

  @Test
  fun search_unknown_category_fallback() = runTest {
    val json = readFixture("search_teams_fnatic.json")
    // mutate a category token to something unsupported to exercise UNKNOWN serializer path
    val mutated = json.replace("\"team\"", "\"esports_org\"")
    val ds = SearchDataSourceImpl(singleResponseClient(mutated))
    val results = ds.search(SearchCategory.TEAM, "fnatic").getOrThrow()
    assertTrue(results.isNotEmpty())
    // Directly parse the mutated raw JSON to inspect category token fallback.
    // For isolated enum verification we also have EnumFallbackAsserts (see ApiEnumsTest).
    val rawElement = Json {
      ignoreUnknownKeys = true
      coerceInputValues = true
    }.parseToJsonElement(mutated)
    val firstObj = rawElement.jsonArray.first().jsonObject
    val categoryRaw = firstObj["category"]!!.jsonPrimitive.content
    // decode using serializer to ensure fallback occurs
    val decoded = Json.decodeFromString(SearchCategoryNullableSerializer, "\"$categoryRaw\"")
    if (categoryRaw == "esports_org") {
      assertEquals(null, decoded)
    }
  }
}
