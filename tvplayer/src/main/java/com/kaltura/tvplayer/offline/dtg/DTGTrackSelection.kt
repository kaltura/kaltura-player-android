package com.kaltura.tvplayer.offline.dtg

import com.kaltura.dtg.CodecSupport
import com.kaltura.dtg.DownloadItem
import com.kaltura.dtg.DownloadItem.Track
import com.kaltura.dtg.DownloadItem.TrackSelector
import com.kaltura.dtg.DownloadItem.TrackType.*
import com.kaltura.dtg.exoparser.util.MimeTypes
import com.kaltura.playkit.PKLog
import com.kaltura.tvplayer.OfflineManager.SelectionPrefs
import com.kaltura.tvplayer.OfflineManager.TrackCodec
import com.kaltura.tvplayer.OfflineManager.TrackCodec.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


internal fun selectTracks(trackSelector: TrackSelector, prefs: SelectionPrefs) {
    DTGTrackSelection(prefs, trackSelector).selectTracks()
}

private val log = PKLog.get("DTGTrackSelection")

class DTGTrackSelection(
    private val prefs: SelectionPrefs,
    private val trackSelector: TrackSelector
) {

    private val allowedVideoCodecs = videoCodecs.filter { it.isAllowed() }
    private val allowedAudioCodecs = audioCodecs.filter { it.isAllowed() }

    private val allowedVideoCodecTags = allowedVideoCodecs.map { it.tag() }
    private val allowedAudioCodecTags = allowedAudioCodecs.map { it.tag() }

    private val allowedCodecTags = allowedVideoCodecTags + allowedAudioCodecTags

    private var selectedVideoTrack : Track? = null

    internal fun selectTracks() {
        // Video
        selectVideoTrack()

        // Audio
        selectLingualTracks(AUDIO)

        // Text
        selectLingualTracks(TEXT)
    }

    private fun selectLingualTracks(trackType: DownloadItem.TrackType) {
        val audioTracks = selectLingualTracks(trackSelector.getAvailableTracks(trackType))

        trackSelector.setSelectedTracks(trackType, audioTracks)
    }

    private fun selectVideoTrack() {
        val videoTracks = trackSelector.getAvailableTracks(VIDEO)

        selectedVideoTrack = selectVideoTrack(videoTracks)

        trackSelector.setSelectedTracks(VIDEO, listOf(selectedVideoTrack))
    }

    private fun canonicalLangList(list: List<String>?) : List<String> {
        return (list ?: listOf()).map { iso3LangCode(it) }
    }

    private fun iso3LangCode(langCode: String) = Locale(langCode).isO3Language

    private fun canonicalLangPref(type: DownloadItem.TrackType) : List<String> {
        return canonicalLangList(if (type == AUDIO) prefs.audioLanguages else prefs.textLanguages)
    }

    private fun selectLingualTracks(tracks: List<Track>?): List<Track> {
        if (tracks.isNullOrEmpty()) {return emptyList()}

        val matchingTracks = mutableListOf<Track>()

        val type = tracks[0].type

        val langList = canonicalLangPref(type)
        val allLangs = if (type == AUDIO) prefs.allAudioLanguages else prefs.allTextLanguages

        val groupId = selectedVideoTrack?.audioGroupId

        for (track in tracks) {
            if (groupId != null && track.audioGroupId != null && track.audioGroupId != groupId) {
                continue    // can't use this track
            }

            // if the stream has a declared language, check if it matches.
            val trackLang = track.language
            if (trackLang != null) {
                // Filter out this track, unless either allLangs == true or trackLang is in the list of prefs
                if (!(allLangs || langList.any { it == trackLang || it == iso3LangCode(trackLang) } )) {
                    continue    // the app doesn't want this language
                }
            }

            matchingTracks += track
        }

        return matchingTracks
    }

    private fun selectVideoTrack(videoTracks: List<Track>) : Track {
        val videoCodecTags = videoCodecs.map { it.tag() }
        val audioCodecTags = audioCodecs.map { it.tag() }

        val allCodecs = videoCodecTags + audioCodecTags

        // Create a dictionary of tracks by codec.
        // ONLY THE MAIN TRACKS are included -- not the alternates.
        val mainTracks = mutableMapOf<CodecTag, MutableList<Track>>()
        for (c in allCodecs) {
            mainTracks[c] = mutableListOf()
        }

        var hasVideo = false
        var hasAudio = false
        var hasResolution = false

        for (s in videoTracks) {
            if (s.usesUnsupportedCodecs()) {
                continue
            }
            if (s.height > 0 && s.width > 0) {
                hasResolution = true
            }

            // add the track to the correct array
            if (s.codecs == null) {
                // If no codec is specified, assume avc1/mp4a
                mainTracks[AVC1.tag()]?.add(s)
                hasVideo = true

            }

            val videoCodec = s.videoCodec()
            if (videoCodec != null) {
                // A video codec was specified
                mainTracks[videoCodec]?.add(s)
                hasVideo = true
            }

            val audioCodec = s.audioCodec()
            if (audioCodec != null) {
                // An audio codec was specified with no video codec
                mainTracks[audioCodec]?.add(s)
                hasAudio = true
            }
        }

        log.d("Playable video tracks: $mainTracks")

        // Filter tracks by video HEIGHT and WIDTH
        if (hasResolution) {
            // Don't sort/filter by resolution if not set
            for (c in videoCodecTags) {
                val height = prefs.videoHeight
                if (height != null) {
                    mainTracks[c] = filterTracks(mainTracks[c]!!, Track.heightComparator, {it.height >= height})
                }
                val width = prefs.videoWidth
                if (width != null) {
                    mainTracks[c] = filterTracks(mainTracks[c]!!, Track.widthComparator, {it.width >= width})
                }
            }
        }

        // Filter by bitrate

        val videoBitrates = HashMap(prefs.codecVideoBitrates ?: mapOf())
        if (videoBitrates[AVC1] == null) {
            videoBitrates[AVC1] = 180_000
        }
        if (videoBitrates[HEVC] == null) {
            videoBitrates[HEVC] = 120_000
        }

        for ((codec, bitrate) in videoBitrates) {
            val codecTracks = mainTracks[codec.tag()] ?: continue
            mainTracks[codec.tag()] = filterTracks(codecTracks, Track.bitrateComparator, { it.bitrate >= bitrate })
        }

        log.d("Filtered video tracks: $mainTracks")

        if (hasVideo) {
            for (codec in fullVideoCodecPriority()) {
                val codecTracks = mainTracks[codec]!!
                val first = codecTracks.firstOrNull()
                if (first != null) {
                    return first
                }
            }
        } else if (hasAudio) {
            for (codec in fullAudioCodecPriority()) {
                val codecTracks = mainTracks[codec]!!
                val first = codecTracks.firstOrNull()
                if (first != null) {
                    return first
                }
            }
        }

        TODO()
//        throw HLSLocalizerError.malformedPlaylist
    }


    private fun filterTracks(
        tracks: MutableList<Track>,
        comparator: Comparator<Track?>,
        predicate: (Track) -> Boolean
    ): MutableList<Track> {
        if (tracks.size < 2) {
            return tracks
        }

        val sorted = tracks.sortedWith(comparator)

        val filtered = sorted.filter(predicate)

        return if (filtered.isEmpty()) sorted.subList(0, 1).toMutableList() else filtered.toMutableList()
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

    fun Track.usesUnsupportedCodecs(): Boolean {
        for (tag in codecTags()) {
            if (!allowedCodecTags.contains(tag)) {
                return true
            }
        }
        return false
    }

    fun TrackCodec.isAllowed() = when (this) {
        AVC1 -> true
        HEVC -> OMCodecSupport.hardwareHEVC || OMCodecSupport.anyHEVC && prefs.allowInefficientCodecs
        MP4A -> true
        AC3 -> OMCodecSupport.AC3
        EAC3 -> OMCodecSupport.EAC3
    }

    internal fun TrackCodec.tag(): CodecTag = when (this) {
        AVC1 -> "avc1"
        HEVC -> "hvc1"
        MP4A -> "mp4a"
        AC3 -> "ac-3"
        EAC3 -> "ec-3"
    }

    fun Track.codecTags(): Set<CodecTag> {
        // `codecs` is an array of String, but it's declared as a non-generic `Array`.
        // It might also be nil.

        val codecs = this.codecs ?: return setOf()

        val codecList = codecs.split(',')

        val tags = mutableSetOf<CodecTag>()

        for (c in codecList) {
            val tag = c.split('.').first()
            tags.add(tag)
        }

        return tags
    }

    fun Track.videoCodec(): CodecTag? {
        for (tag in codecTags()) {
            if (videoCodecs.any { it.tag() == tag }) {
                return tag
            }
        }
        return null
    }

    fun Track.audioCodec(): CodecTag? {
        for (tag in codecTags()) {
            if (audioCodecs.any { it.tag() == tag }) {
                return tag
            }
        }
        return null
    }


    fun fullVideoCodecPriority() : List<CodecTag> =
        fullCodecPriority(requestedTags = (prefs.videoCodecs ?: listOf()).map { it.tag() }, allowedTags = allowedVideoCodecTags)

    fun fullAudioCodecPriority() : List<CodecTag> =
        fullCodecPriority(requestedTags = (prefs.audioCodecs ?: listOf()).map { it.tag() }, allowedTags = allowedAudioCodecTags)

    fun fullCodecPriority(requestedTags: List<CodecTag>, allowedTags: List<CodecTag>) : List<CodecTag> {
        val codecPriority = ArrayList(requestedTags)
        for (codec in allowedTags) {
            if (!codecPriority.contains(codec)) {
                codecPriority.add(codec)
            }
        }
        return codecPriority
    }

}

internal val videoCodecs = listOf(HEVC, AVC1)
internal val audioCodecs = listOf(EAC3, AC3, MP4A)

typealias CodecTag = String

object OMCodecSupport {
    val AC3 = CodecSupport.hasDecoder(MimeTypes.AUDIO_AC3, true, true)
    val EAC3 = CodecSupport.hasDecoder(MimeTypes.AUDIO_E_AC3, true, true)
    val hardwareHEVC = CodecSupport.hasDecoder(MimeTypes.VIDEO_H265, true, false)
    val anyHEVC = CodecSupport.hasDecoder(MimeTypes.VIDEO_H265, true, true)
}
