package dev.staticvar.vlr.utils

import android.content.Context
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import androidx.annotation.Keep

@Keep
enum class DomainVerificationStatus {
  VERIFIED,
  NOT_VERIFIED
}

fun domainVerificationStatus(context: Context): DomainVerificationStatus {
  if(android.os.Build.VERSION.SDK_INT <= 30)
    return DomainVerificationStatus.VERIFIED

  val manager = context.getSystemService(DomainVerificationManager::class.java)
  val userState = manager.getDomainVerificationUserState(context.packageName)

  val unapprovedDomains =
    userState
      ?.hostToStateMap
      ?.filterValues { it == DomainVerificationUserState.DOMAIN_STATE_NONE }
      ?.size
      ?: 0

  return if (unapprovedDomains > 0) DomainVerificationStatus.NOT_VERIFIED
  else DomainVerificationStatus.VERIFIED
}
