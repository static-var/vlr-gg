package dev.staticvar.vlr.utils

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import java.util.concurrent.CancellationException
import kotlin.Result
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import com.github.michaelbull.result.Result as ResultMonad

/**
 * Calls the specified function [block] with [this] value as its receiver and returns its
 * encapsulated result if invocation was successful, catching any [Throwable] except
 * [CancellationException] that was thrown from the [block] function execution and encapsulating it
 * as a failure.
 */
public suspend inline fun <V> runSuspendCatching(block: () -> V): ResultMonad<V, Throwable> {
  contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

  return try {
    Ok(block())
  } catch (e: Throwable) {
    if (e is CancellationException) throw e
    println(e.printStackTrace())
    Err(e)
  }
}


inline fun <T, R> T.cancellableRunCatching(block: T.() -> R): Result<R> {
  contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
  return try {
    Result.success(block())
  } catch (ce: CancellationException) {
    throw ce
  } catch (e: Throwable) {
    Result.failure(e)
  }
}