package com.alixa.spotifycloneyt.exoplayer

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alixa.spotifycloneyt.other.Constants.NETWORK_ERROR
import com.alixa.spotifycloneyt.other.Event
import com.alixa.spotifycloneyt.other.Resource

class MusicServiceConnection(
    context: Context
) {
    private val _isConnected = MutableLiveData<Event<Resource<Boolean>>>()

    val isConnected: LiveData<Event<Resource<Boolean>>> = _isConnected


    private val _networkError = MutableLiveData<Event<Resource<Boolean>>>()

    val networkError: LiveData<Event<Resource<Boolean>>> = _networkError


    private val _playbackState = MutableLiveData<PlaybackStateCompat?>()

    val playbackState: LiveData<PlaybackStateCompat?> = _playbackState


    private val _curPlayingSong = MutableLiveData<MediaMetadataCompat?>()

    val curPlayingSong: LiveData<MediaMetadataCompat?> = _curPlayingSong


    lateinit var mediaController: MediaControllerCompat

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(
            context,
            MusicService::class.java
        ),
        mediaBrowserConnectionCallback,
        null
    ).apply {
        connect()
    }
    val TransportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    fun subscribe(parentId: String, callBack: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callBack)
    }

    fun unsubscribe(parentId: String, callBack: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(parentId, callBack)
    }

    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            _isConnected.postValue(Event(Resource.success(true)))
        }

        override fun onConnectionSuspended() {
            _isConnected.postValue(
                Event(Resource.error(null, "The connection was suspended."))
            )
        }

        override fun onConnectionFailed() {
            _isConnected.postValue(
                Event(Resource.error(null, "Couldnt connect to Media Browser"))
            )
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {

            _playbackState.postValue(state)

        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            _curPlayingSong.postValue(metadata)
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when (event) {
                NETWORK_ERROR -> _networkError.postValue(
                    Event(
                        Resource.error(
                            message = "Couldn`t connect to server, please check your Internet connection First",
                            data = null
                        )
                    )
                )
            }
        }
    }

}