package dev.staticvar.vlr.utils

import io.ktor.client.plugins.compression.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.CoroutineScope
import okio.buffer
import okio.source
import org.brotli.dec.BrotliInputStream

/**
 * Helper class which handles Brotli (br) content-type simply by hooking up the incoming
 * [ByteReadChannel] to [BrotliInputStream] and then sending it back as [ByteReadChannel]
 *
 * Note: This class only decodes content from 'br', it doesn't encode back content to 'br'
 * @see <a href="https://www.brotli.org/">Brotli (br)</a>
 * @see [BrotliInputStream]
 */
object BrotliEncoder : ContentEncoder {
  override val name: String
    get() = "br"

  override fun CoroutineScope.decode(source: ByteReadChannel): ByteReadChannel {
    return source.toBrotliByteReaderChannel()
  }

  override fun CoroutineScope.encode(source: ByteReadChannel): ByteReadChannel {
    return source
  }
}

private fun ByteReadChannel.toBrotliByteReaderChannel(): ByteReadChannel {
  return BrotliInputStream(this.toInputStream()).source().buffer().readByteArray().asByteReadChannel
}

private val ByteArray.asByteReadChannel
  get() = ByteReadChannel(this)
