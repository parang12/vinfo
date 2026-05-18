package com.example.vinfo.service

import android.app.Notification
import android.media.MediaMetadata
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.vinfo.data.nowplaying.NowPlayingEventBus
import com.example.vinfo.domain.model.NowPlayingTrack

class ActiveMediaMonitorService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        getActiveNotifications()
            ?.asSequence()
            ?.sortedByDescending { it.postTime }
            ?.firstNotNullOfOrNull { sbn -> extractTrack(sbn) }
            ?.let { track ->
                NowPlayingEventBus.publish(track)
                Log.d(TAG, "Now playing detected: ${track.artist} - ${track.title}")
            }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        publishTrackIfPresent(sbn)
    }

    private fun publishTrackIfPresent(sbn: StatusBarNotification) {
        if (!isMediaNotification(sbn)) {
            return
        }

        val track = extractTrack(sbn) ?: return

        NowPlayingEventBus.publish(track)
        Log.d(TAG, "Now playing detected: ${track.artist} - ${track.title}")
    }

    private fun isMediaNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        val extras = notification.extras

        return notification.category == Notification.CATEGORY_TRANSPORT ||
            extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
            hasTrackSignals(extras)
    }

    private fun hasTrackSignals(extras: Bundle): Boolean {
        return extras.containsKey(MediaMetadata.METADATA_KEY_ARTIST) ||
            extras.containsKey(MediaMetadata.METADATA_KEY_TITLE) ||
            extras.containsKey(MediaMetadata.METADATA_KEY_ALBUM) ||
            extras.containsKey(Notification.EXTRA_TITLE) ||
            extras.containsKey(Notification.EXTRA_TEXT) ||
            extras.containsKey(Notification.EXTRA_SUB_TEXT)
    }

    private fun extractTrack(sbn: StatusBarNotification): NowPlayingTrack? {
        val extras = sbn.notification.extras ?: return null

        val artist = extras.getCharSequence(MediaMetadata.METADATA_KEY_ARTIST)
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)
        val title = extras.getCharSequence(MediaMetadata.METADATA_KEY_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)
        val album = extras.getCharSequence(MediaMetadata.METADATA_KEY_ALBUM)

        val resolvedArtist = artist?.toString()?.trim().orEmpty()
        val resolvedTitle = title?.toString()?.trim().orEmpty()
        val resolvedAlbum = album?.toString()?.trim().takeIf { !it.isNullOrBlank() }

        if (resolvedArtist.isBlank() || resolvedTitle.isBlank()) {
            return null
        }

        return NowPlayingTrack(
            artist = resolvedArtist,
            title = resolvedTitle,
            album = resolvedAlbum,
            sourcePackageName = sbn.packageName
        )
    }

    companion object {
        private const val TAG = "ActiveMediaMonitor"
    }
}