package dev.staticvar.vlr.remotesource.version

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

interface VersionDataSource { suspend fun versions(): Result<VersionResponseDto> }
