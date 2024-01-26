package com.alixa.spotifycloneyt.ui.viewmodels

import com.alixa.spotifycloneyt.exoplayer.MusicServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
) {
}