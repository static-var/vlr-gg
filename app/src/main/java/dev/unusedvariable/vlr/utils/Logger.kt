package dev.unusedvariable.vlr.utils

import android.util.Log

/** Logger Logger class to log information with a hyperlink to source in logcat */
object Logger {

  var LOGGING_ENABLED = false
  private val ignoreTags = listOf(this::class.java.name)

  private val tag: String
    get() =
        Throwable().stackTrace.first { it.className !in ignoreTags }.let { stackTraceElement ->
          "(${stackTraceElement.fileName}:${stackTraceElement.lineNumber})${stackTraceElement.methodName}()"
        }

  fun init(logging: Boolean) {
    LOGGING_ENABLED = logging
    verbose(message = "Logger Initialized")
  }

  fun verbose(message: String) {
    if (LOGGING_ENABLED) Log.v(tag, message)
  }

  fun info(message: String) {
    if (LOGGING_ENABLED) Log.i(tag, message)
  }

  fun log(message: String) {
    if (LOGGING_ENABLED) Log.d(tag, message)
  }

  fun warn(message: String) {
    if (LOGGING_ENABLED) Log.w(tag, message)
  }

  fun error(message: String, throwable: Throwable?) {
    if (LOGGING_ENABLED) Log.e(tag, message, throwable)
  }
}

/**
 * i inline method to lazily evaluate log message at call site and print a info message
 *
 * usage: i {"message"}
 *
 * @param message
 * @receiver
 */
inline fun i(message: () -> String) {
  Logger.info(message())
}

/**
 * v inline method to lazily evaluate log message at call site and print a verbose message
 *
 * usage: v {"message"}
 *
 * @param message
 * @receiver
 */
inline fun v(message: () -> String) {
  Logger.verbose(message())
}

/**
 * log inline method to lazily evaluate log message at call site and print a verbose message
 *
 * usage: log {"message"}
 *
 * @param message
 * @receiver
 */
inline fun log(message: () -> String) {
  Logger.log(message())
}

/**
 * w inline method to lazily evaluate log message at call site and print a warning message
 *
 * usage: w {"message"}
 *
 * @param message
 * @receiver
 */
inline fun w(message: () -> String) {
  Logger.warn(message())
}

/**
 * e inline method to lazily evaluate log message at call site and print a error message with
 * exception
 *
 * usage: e {"message"}
 *
 * @param message
 * @param throwable
 * @receiver
 */
inline fun e(message: () -> String, throwable: Throwable? = null) {
  Logger.error(message(), throwable)
}

/**
 * e inline method to lazily evaluate log message at call site and print a error message with
 * exception
 *
 * usage: e {"message"}
 *
 * @param message
 * @receiver
 */
inline fun e(message: () -> String) {
  Logger.error(message(), null)
}
