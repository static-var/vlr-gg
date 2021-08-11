package dev.unusedvariable.vlr.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Status(
    var code: Int = 0,
    var message: String = ""
)
