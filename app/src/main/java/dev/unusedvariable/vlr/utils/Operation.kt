package dev.unusedvariable.vlr.utils

sealed class Operation<T> {
    fun dataOrNull(): T? {
        return when (this) {
            is Fail -> null
            is Pass -> data
            is Waiting -> null
        }
    }

    fun dataOrException(): T {
        return when (this) {
            is Fail -> {
                throw exception
            }
            is Pass -> {
                dataNotNull
            }
            is Waiting -> {
                throw NullPointerException("Currently in waiting state")
            }
        }
    }
}

data class Pass<T>(val data: T?) : Operation<T>() {
    val dataNotNull: T by lazy { data ?: throw NullPointerException() }
}

data class Fail<T>(val error: String = "", val exception: Exception = NullPointerException()) :
    Operation<T>() {

    fun message(): String {
        return error.plus(exception)
    }

}

data class Waiting<T>(val loading: Boolean = true) : Operation<T>()


inline fun <T> Operation<T>.onPass(crossinline block: Pass<T>.() -> Unit): Operation<T> {
    if (this is Pass)
        block(this)
    return this
}

inline fun <T> Operation<T>.onFail(crossinline block: Fail<T>.() -> Unit): Operation<T> {
    if (this is Fail)
        block(this)
    return this
}

inline fun <T> Operation<T>.onWaiting(crossinline block: Waiting<T>.() -> Unit): Operation<T> {
    if (this is Waiting)
        block(this)
    return this
}