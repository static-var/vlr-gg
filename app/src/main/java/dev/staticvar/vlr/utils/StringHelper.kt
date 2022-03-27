package dev.staticvar.vlr.utils

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

fun String.openAsCustomTab(context: Context) {
  val builder = CustomTabsIntent.Builder()
  val customTabsIntent = builder.build()
  customTabsIntent.launchUrl(context, Uri.parse(this))
}
