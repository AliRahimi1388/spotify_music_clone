package com.alixa.spotifycloneyt.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.alixa.spotifycloneyt.exoplayer.callbacks.MusicPlaybackPreparer
import com.alixa.spotifycloneyt.exoplayer.callbacks.MusicPlayerEventListener
import com.alixa.spotifycloneyt.exoplayer.callbacks.MusicPlayerNotificationListener
import com.alixa.spotifycloneyt.other.Constants.MEDIA_ROOT_ID
import com.alixa.spotifycloneyt.other.Constants.NETWORK_ERROR
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: DefaultDataSource.Factory

    @Inject
    lateinit var exoPlayerFactory: SimpleExoPlayer

    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    private lateinit var musicNotificationManager: MusicNotificationManager

    private val serviceJob = Job()

    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaPlayerSession: MediaSessionCompat

    private lateinit var mediaSessionConnector: MediaSessionConnector

    var isForegroundService = false


    private var currentPlayingSong: MediaMetadataCompat? = null

    private var isPlayerInitialized = false

    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch { firebaseMusicSource.fetchMediaData() }

        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        mediaPlayerSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        sessionToken = mediaPlayerSession.sessionToken

        musicNotificationManager = MusicNotificationManager(
            this,
            mediaPlayerSession.sessionToken,
            MusicPlayerNotificationListener(this)
        )
        {
            curSongDuration = exoPlayerFactory.duration

        }

        val musicPlaybackPreperer = MusicPlaybackPreparer(firebaseMusicSource) {
            currentPlayingSong = it
            preparePlayer(firebaseMusicSource.songs, it, true)

        }

        mediaSessionConnector = MediaSessionConnector(mediaPlayerSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreperer)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayerFactory)

        musicPlayerEventListener = MusicPlayerEventListener(this)

        exoPlayerFactory.addListener(musicPlayerEventListener)

        musicNotificationManager.showNotification(exoPlayerFactory)

    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaPlayerSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return firebaseMusicSource.songs[windowIndex].description
        }
    }

    private fun preparePlayer(
        songs: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playNow: Boolean
    ) {
        val curSongIndex = if (currentPlayingSong == null) 0 else songs.indexOf(itemToPlay)
        exoPlayerFactory.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayerFactory.seekTo(curSongIndex, 0L)
        exoPlayerFactory.playWhenReady = playNow
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayerFactory.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        exoPlayerFactory.removeListener(musicPlayerEventListener)
        exoPlayerFactory.release()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_ID, null)

    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when (parentId) {
            MEDIA_ROOT_ID -> {
                val resultSent = firebaseMusicSource.whenReady { isInitialized ->
                    if (isInitialized) {
                        result.sendResult(firebaseMusicSource.asMediaItems())
                        if (!isPlayerInitialized && firebaseMusicSource.songs.isNotEmpty()) {
                            preparePlayer(
                                firebaseMusicSource.songs,
                                firebaseMusicSource.songs[0],
                                false
                            )
                            isPlayerInitialized = true
                        }
                    } else {
                        mediaPlayerSession.sendSessionEvent(NETWORK_ERROR, null)
                        result.sendResult(null)
                    }
                }

                if (!resultSent) {
                    result.detach()
                }
            }
        }
    }

    companion object {
        var curSongDuration = 0L
            private set
        private const val SERVICE_TAG = "MusicService"
    }

}