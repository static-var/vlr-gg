package dev.staticvar.vlr.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.staticvar.vlr.MainActivity
import dev.staticvar.vlr.R
import dev.staticvar.vlr.ui.Destination
import dev.staticvar.vlr.utils.Constants
import dev.staticvar.vlr.utils.i

class VlrFirebaseNotificationService() : FirebaseMessagingService() {

  override fun onNewToken(p0: String) {
    super.onNewToken(p0)
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    i { "from ${remoteMessage.from}" }
    i { "payload ${remoteMessage.data}" }

    val title = remoteMessage.data["title"]
    val body = remoteMessage.data["body"]
    val matchId = remoteMessage.data["match_id"]?.toInt() ?: 0
    val streamLink = remoteMessage.data["stream_url"]

    val taskDetailIntent =
      Intent(
        Intent.ACTION_VIEW,
        "${Constants.DEEP_LINK_BASEURL}${Destination.Match.Args.ID}=${matchId}".toUri(),
        this,
        MainActivity::class.java
      )

    val pending: PendingIntent =
      TaskStackBuilder.create(this).run {
        addNextIntentWithParentStack(taskDetailIntent)
        getPendingIntent(matchId, PendingIntent.FLAG_IMMUTABLE)
      }

    val channelId = getString(R.string.notification_channel_id)
    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val notificationBuilder =
      NotificationCompat.Builder(this, channelId)
        .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground))
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .setSound(defaultSoundUri)
        .setContentIntent(pending)

    streamLink?.let { link ->
      val weblinkIntent =
        Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
      val pIntent = PendingIntent.getActivity(this, 0, weblinkIntent, PendingIntent.FLAG_IMMUTABLE)

      val streamAction =
        NotificationCompat.Action.Builder(0, getString(R.string.watch_stream), pIntent)
      notificationBuilder.addAction(streamAction.build())
    }

    getSystemService<NotificationManager>()?.notify(matchId, notificationBuilder.build())
  }
}
