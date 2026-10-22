/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.content.Context
import android.util.AtomicFile
import dev.staticvar.vlr.shared.widget.favorite.StoredWidgetRecord
import dev.staticvar.vlr.shared.widget.favorite.WidgetKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

internal class FavoriteWidgetRecordStore(context: Context) {
  private val directory = File(context.filesDir, "favorite-widgets/v1")

  suspend fun read(key: WidgetKey): StoredWidgetRecord? = withContext(Dispatchers.IO) {
    synchronized(fileLock) {
      val text = try {
        file(key).openRead().bufferedReader().use { it.readText() }
      } catch (_: FileNotFoundException) {
        return@synchronized null
      }
      val stored = try {
        Json.decodeFromString<RecordFile>(text)
      } catch (_: IllegalArgumentException) {
        return@synchronized null
      }
      stored.takeIf { it.key == key }?.let { StoredWidgetRecord(it.record) }
    }
  }

  suspend fun commit(key: WidgetKey, record: StoredWidgetRecord) = withContext(Dispatchers.IO) {
    val bytes = Json.encodeToString(RecordFile(key, record.json)).toByteArray(Charsets.UTF_8)
    val coroutineContext = currentCoroutineContext()
    coroutineContext.ensureActive()
    synchronized(fileLock) {
      val target = file(key)
      val output = target.startWrite()
      try {
        output.write(bytes)
        coroutineContext.ensureActive()
        target.finishWrite(output)
      } catch (failure: Throwable) {
        target.failWrite(output)
        throw failure
      }
    }
  }

  suspend fun retain(keys: Set<WidgetKey>) = withContext(Dispatchers.IO) {
    val names = keys.map { "${it.stableHash()}.json" }.toSet()
    synchronized(fileLock) {
      directory.listFiles().orEmpty()
        .map { it.name.removeSuffix(".bak").removeSuffix(".new") }
        .distinct()
        .filter { it !in names }
        .forEach { AtomicFile(File(directory, it)).delete() }
    }
  }

  suspend fun <T> withKey(key: WidgetKey, block: suspend () -> T): T =
    keyLocks.getOrPut(key) { Mutex() }.withLock { block() }

  private fun file(key: WidgetKey) = AtomicFile(File(directory, "${key.stableHash()}.json"))

  @Serializable
  private data class RecordFile(val key: WidgetKey, val record: String)

  private companion object {
    val fileLock = Any()
    val keyLocks = ConcurrentHashMap<WidgetKey, Mutex>()
  }
}

internal fun WidgetKey.stableHash(): String = MessageDigest.getInstance("SHA-256")
  .digest(Json.encodeToString(this).toByteArray(Charsets.UTF_8))
  .joinToString("") { "%02x".format(it) }
