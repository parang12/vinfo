package com.example.vinfo.service

import android.app.Notification
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.vinfo.data.nowplaying.NowPlayingEventBus
import com.example.vinfo.domain.model.NowPlayingTrack

class ActiveMediaMonitorService : NotificationListenerService() {

    private var lastController: MediaController? = null
    private val CACHE_PREFIX = "album_art_"
    private val MAX_CACHE_FILES = 50
    private val MAX_CACHE_BYTES = 10_485_760L // 10 MB

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // 먼저 MediaSession 기반으로 활성 세션 검색
        try {
            val msm = getSystemService(MediaSessionManager::class.java)
            val component = ComponentName(this, javaClass)
            val sessions = msm?.getActiveSessions(component)
            val controller = sessions?.firstOrNull()
            if (controller != null) {
                lastController = controller
                val meta = controller.metadata
                val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
                val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                val album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM)
                if (!title.isNullOrBlank() && !artist.isNullOrBlank()) {
                    val resolvedArt = resolveAlbumArt(meta, null)
                    val track = NowPlayingTrack(
                        artist = artist.trim(),
                        title = title.trim(),
                        album = album?.trim(),
                        albumArtUrl = resolvedArt,
                        sourcePackageName = controller.packageName
                    )
                    NowPlayingEventBus.publish(track)
                    Log.d(TAG, "Resolved session album art: $resolvedArt")
                    Log.d(TAG, "Now playing (session): ${track.artist} - ${track.title}")
                    return
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaSession lookup failed", e)
        }

        // 폴백: 알림에서 추출
        val activeSeq = getActiveNotifications()
            ?.asSequence()
            ?.sortedByDescending { it.postTime }

        // 우선적으로 미디어 알림만 찾아서 사용. 일반 알림의 title/text는 곡 정보로 취급하지 않는다.
        val mediaTrack = activeSeq
            ?.filter { isMediaNotification(it) }
            ?.firstNotNullOfOrNull { sbn -> extractTrack(sbn) }

        if (mediaTrack != null) {
            NowPlayingEventBus.publish(mediaTrack)
            Log.d(TAG, "Now playing detected (from media notification): ${mediaTrack.artist} - ${mediaTrack.title}")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        publishTrackIfPresent(sbn)
    }

    private fun publishTrackIfPresent(sbn: StatusBarNotification) {
        if (!isMediaNotification(sbn)) {
            return
        }

        // 먼저 MediaSession에서 가져올 수 있는지 확인
        try {
            val msm = getSystemService(MediaSessionManager::class.java)
            val component = ComponentName(this, javaClass)
            val sessions = msm?.getActiveSessions(component)
            val controller = sessions?.firstOrNull { it.packageName == sbn.packageName }
            if (controller != null) {
                lastController = controller
                val meta = controller.metadata
                val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
                val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                if (!title.isNullOrBlank() && !artist.isNullOrBlank()) {
                    val resolvedArt = resolveAlbumArt(meta, sbn)
                    val track = NowPlayingTrack(
                        artist = artist.trim(),
                        title = title.trim(),
                        album = meta.getString(MediaMetadata.METADATA_KEY_ALBUM),
                        sourcePackageName = controller.packageName,
                        albumArtUrl = resolvedArt
                    )
                    NowPlayingEventBus.publish(track)
                    Log.d(TAG, "Resolved session/notification album art: $resolvedArt")
                    Log.d(TAG, "Now playing (session): ${track.artist} - ${track.title}")
                    return
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaSession lookup failed", e)
        }

        val track = extractTrack(sbn) ?: return

        NowPlayingEventBus.publish(track)
        Log.d(TAG, "Now playing detected: ${track.artist} - ${track.title}")
    }

    // 재생 컨트롤러 헬퍼
    fun skipToNext() {
        lastController?.transportControls?.skipToNext()
    }

    fun playPause() {
        lastController?.let { ctrl ->
            val state = ctrl.playbackState?.state
            if (state == android.media.session.PlaybackState.STATE_PLAYING) {
                ctrl.transportControls.pause()
            } else {
                ctrl.transportControls.play()
            }
        }
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
            extras.containsKey(MediaMetadata.METADATA_KEY_ALBUM)
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

        val albumArtUrl = resolveAlbumArt(metadata = null, sbn = sbn)

        return NowPlayingTrack(
            artist = resolvedArtist,
            title = resolvedTitle,
            album = resolvedAlbum,
            sourcePackageName = sbn.packageName,
            albumArtUrl = albumArtUrl
        )
    }

    private fun resolveAlbumArt(
        metadata: MediaMetadata?,
        sbn: StatusBarNotification?
    ): String? {
        val metadataBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        val savedMetadataBitmap = metadataBitmap?.let { saveBitmapToCache(it) }
        if (!savedMetadataBitmap.isNullOrBlank()) {
            Log.d(TAG, "Saved album art from MediaMetadata bitmap: $savedMetadataBitmap")
            return savedMetadataBitmap
        }

        val notificationBitmap = sbn?.let { extractNotificationAlbumArtBitmap(it) }
        val savedNotificationBitmap = notificationBitmap?.let { saveBitmapToCache(it) }
        if (!savedNotificationBitmap.isNullOrBlank()) {
            Log.d(TAG, "Saved album art from notification icon: $savedNotificationBitmap")
            return savedNotificationBitmap
        }

        val metadataUri = metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
        if (!metadataUri.isNullOrBlank()) {
            Log.d(TAG, "Using album art URI from MediaMetadata: $metadataUri")
            return metadataUri.trim()
        }

        val extras = sbn?.notification?.extras
        val extrasUri = extras?.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: extras?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: extras?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
        if (!extrasUri.isNullOrBlank()) {
            Log.d(TAG, "Using album art URI from notification extras: $extrasUri")
            return extrasUri.trim()
        }

        return null
    }

    private fun extractNotificationAlbumArtBitmap(sbn: StatusBarNotification): Bitmap? {
        val notification = sbn.notification
        val extras = notification.extras
        val candidates = listOfNotNull(
            extras.get(Notification.EXTRA_LARGE_ICON_BIG),
            extras.get(Notification.EXTRA_LARGE_ICON),
            extras.get(Notification.EXTRA_PICTURE),
            notification.getLargeIcon()
        )

        for (candidate in candidates) {
            val bitmap = candidateToBitmap(candidate)
            if (bitmap != null) return bitmap
        }

        return null
    }

    private fun candidateToBitmap(candidate: Any?): Bitmap? {
        return when (candidate) {
            is Bitmap -> candidate
            is Icon -> {
                try {
                    drawableToBitmap(candidate.loadDrawable(this))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load Icon drawable", e)
                    null
                }
            }
            is Drawable -> drawableToBitmap(candidate)
            else -> null
        }
    }

    private fun saveBitmapToCache(bitmap: Bitmap): String? {
        return try {
            val filename = "album_art_${UUID.randomUUID()}.png"
            val albumArtDir = File(filesDir, "album_art").apply { mkdirs() }
            val file = File(albumArtDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            cleanupCache()
            // Coil can load file paths; return file URI form
            "file://${file.absolutePath}"
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save album art bitmap", e)
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) return null
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            try {
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 256
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 256
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            } catch (e: Exception) {
                Log.w(TAG, "Failed to convert drawable to bitmap", e)
                null
            }
        }
    }

    private fun cleanupCache() {
        try {
            val albumArtDir = File(filesDir, "album_art")
            val files = albumArtDir.listFiles { f -> f.name.startsWith(CACHE_PREFIX) } ?: return
            if (files.isEmpty()) return

            // 총 파일 수 및 총 바이트 계산
            var totalBytes = files.sumOf { it.length() }
            var totalFiles = files.size

            if (totalFiles <= MAX_CACHE_FILES && totalBytes <= MAX_CACHE_BYTES) return

            // 오래된 파일부터 삭제 (lastModified 오름차순)
            val toDelete = files.sortedBy { it.lastModified() }
            for (f in toDelete) {
                if (totalFiles <= MAX_CACHE_FILES && totalBytes <= MAX_CACHE_BYTES) break
                val len = f.length()
                if (f.delete()) {
                    totalFiles -= 1
                    totalBytes -= len
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cleanup album art cache", e)
        }
    }

    companion object {
        private const val TAG = "ActiveMediaMonitor"
        // 서비스 인스턴스 접근용 (간단한 전역 제어용)
        @Volatile
        var instance: ActiveMediaMonitorService? = null
            private set
    }
}
