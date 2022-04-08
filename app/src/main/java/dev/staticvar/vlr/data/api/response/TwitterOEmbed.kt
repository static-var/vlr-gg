package dev.staticvar.vlr.data.api.response

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class TwitterOEmbed(
  @SerialName("author_name") val authorName: String = "", // Gen.G Esports
  @SerialName("author_url") val authorUrl: String = "", // https://twitter.com/GenG
  @SerialName("html")
  val html: String = "", // <blockquote class="twitter-tweet"><p lang="en" dir="ltr">As of today, <a
  // href="https://twitter.com/Guiiimond?ref_src=twsrc%5Etfw">@Guiiimond</a> has been removed
  // from the active <a
  // href="https://twitter.com/hashtag/GenGVAL?src=hash&amp;ref_src=twsrc%5Etfw">#GenGVAL</a>
  // roster.<br><br>Going forward, gMd will be a restricted free agent while he pursues other
  // opportunities. As an integral part of our original roster, we appreciate all you&#39;ve
  // done &amp; wish you nothing but the best! 💛🖤 <a
  // href="https://t.co/ARO3YmZTFy">pic.twitter.com/ARO3YmZTFy</a></p>&mdash; Gen.G Esports
  // (@GenG) <a
  // href="https://twitter.com/GenG/status/1509637260932792322?ref_src=twsrc%5Etfw">March 31,
  // 2022</a></blockquote><script async src="https://platform.twitter.com/widgets.js"
  // charset="utf-8"></script>
  @SerialName("url") val url: String = "" // https://twitter.com/GenG/status/1509637260932792322
)
