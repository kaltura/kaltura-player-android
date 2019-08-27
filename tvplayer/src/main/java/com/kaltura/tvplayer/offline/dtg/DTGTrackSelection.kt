package com.kaltura.tvplayer.offline.dtg

import com.kaltura.dtg.DownloadItem.Track
import com.kaltura.dtg.DownloadItem.TrackSelector
import com.kaltura.dtg.DownloadItem.TrackType.*
import com.kaltura.tvplayer.OfflineManager.SelectionPrefs
import java.util.*


fun selectTracks(trackSelector: TrackSelector, prefs: SelectionPrefs) {
    DTGTrackSelection(trackSelector, prefs)
}



class DTGTrackSelection
    (trackSelector: TrackSelector, prefs: SelectionPrefs) {

    init {
        // TODO: 2019-08-26 handle groups, audio bitrate, codecs

        // Video

        var videoTracks = trackSelector.getAvailableTracks(VIDEO)

        if (prefs.videoBitrates != null) {
            videoTracks = filterTracks(videoTracks, Track.bitrateComparator) { it.bitrate >= 1}
        }
        if (prefs.videoHeight != null) {
            videoTracks = filterTracks(videoTracks, Track.heightComparator) { it.height >= prefs.videoHeight }
        }
        if (prefs.videoWidth != null) {
            videoTracks = filterTracks(videoTracks, Track.widthComparator) { it.width >= prefs.videoWidth }
        }

        trackSelector.setSelectedTracks(VIDEO, listOf(videoTracks[0]))


        // Audio

        var audioTracks = trackSelector.getAvailableTracks(AUDIO)

        audioTracks = filterTracks(
            audioTracks,
            prefs.audioLanguages,
            prefs.allAudioLanguages
        )
        trackSelector.setSelectedTracks(AUDIO, audioTracks)

        // Text

        var textTracks = trackSelector.getAvailableTracks(TEXT)

        textTracks = filterTracks(
            textTracks,
            prefs.textLanguages,
            prefs.allTextLanguages
        )
        trackSelector.setSelectedTracks(TEXT, textTracks)

    }

    private fun filterTracks(
        tracks: List<Track>,
        comparator: Comparator<Track?>,
        predicate: (Track) -> Boolean
    ): List<Track> {
        if (tracks.size < 2) {
            return tracks
        }

        val sorted = tracks.sortedWith(comparator)

        val filtered = sorted.filter(predicate)

        return if (filtered.isEmpty()) sorted.subList(0, 1) else filtered
    }

    private fun filterTracks(
        tracks: List<Track>,
        languages: List<String>?,
        allLanguages: Boolean
    ): List<Track> {
        if (tracks.size < 2 || allLanguages) {  // TODO: this is wrong, we may get duplicate tracks per language
            return tracks
        }

        if (languages.isNullOrEmpty()) {
            return emptyList()
        }

        val filtered = tracks.filter { languages.contains(it.language) }

        return if (filtered.isEmpty()) tracks.subList(0, 1) else filtered
    }
}