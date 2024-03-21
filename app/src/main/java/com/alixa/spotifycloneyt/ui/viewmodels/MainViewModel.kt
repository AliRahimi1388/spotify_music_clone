package com.alixa.spotifycloneyt.ui.viewmodels

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.alixa.spotifycloneyt.data.entities.Song
import com.alixa.spotifycloneyt.exoplayer.MusicServiceConnection
import com.alixa.spotifycloneyt.exoplayer.isPLayEnabled
import com.alixa.spotifycloneyt.exoplayer.isPlaying
import com.alixa.spotifycloneyt.exoplayer.isPrepared
import com.alixa.spotifycloneyt.common.Constants.MEDIA_ROOT_ID
import com.alixa.spotifycloneyt.common.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    private val _mediaItem = MutableLiveData<Resource<List<Song>>>()
    val mediaItems: LiveData<Resource<List<Song>>> = _mediaItem


    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkError
    val curPlayingSong = musicServiceConnection.curPlayingSong
    val playbackState = musicServiceConnection.playbackState


    init {
        _mediaItem.postValue(Resource.loading((null)))
        musicServiceConnection.subscribe(MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {
                override fun onChildrenLoaded(
                    parentId: String,
                    children: MutableList<MediaBrowserCompat.MediaItem>
                ) {
                    super.onChildrenLoaded(parentId, children)
                    val items = children.map {
                        Song(
                            it.mediaId!!,
                            it.description.title.toString(),
                            it.description.subtitle.toString(),
                            it.description.mediaUri.toString(),
                            it.description.iconUri.toString()
                        )
                    }

                    _mediaItem.postValue(Resource.success(items))
                }
            })
    }


    fun skipToNextSong() {
        musicServiceConnection.TransportControls.skipToNext()
    }

    fun skipToPreviousSong() {
        musicServiceConnection.TransportControls.skipToPrevious()
    }

    fun seekTo(pos: Long) {
        musicServiceConnection.TransportControls.seekTo(pos)
    }

    fun playOrToggleSong(mediaItem: Song, toggle: Boolean = false) {
        val isPrepared = playbackState.value?.isPrepared ?: false
        if (isPrepared &&
            mediaItem.mediaId ==
            curPlayingSong
                .value?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
        ) {
            playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying -> if (toggle)
                        musicServiceConnection.TransportControls.pause()

                    playbackState.isPLayEnabled -> musicServiceConnection.TransportControls.play()
                    else -> Unit
                }
            }
        } else {
            musicServiceConnection.TransportControls.playFromMediaId(mediaItem.mediaId, null)
        }

    }

    override fun onCleared() {
        musicServiceConnection.unsubscribe(MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {})
    }


}