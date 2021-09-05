package com.kaltura.tvplayer.offline.exo

import android.content.Context
import android.text.TextUtils
import com.kaltura.android.exoplayer2.C
import com.kaltura.android.exoplayer2.offline.DownloadHelper
import com.kaltura.android.exoplayer2.source.TrackGroupArray
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector
import com.kaltura.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride
import com.kaltura.android.exoplayer2.trackselection.MappingTrackSelector
import com.kaltura.dtg.CodecSupport
import com.kaltura.dtg.DownloadItem
import com.kaltura.dtg.DownloadItem.Track
import com.kaltura.dtg.exoparser.util.MimeTypes
import com.kaltura.playkit.PKAudioCodec
import com.kaltura.playkit.PKLog
import com.kaltura.playkit.PKVideoCodec
import com.kaltura.playkit.player.*
import com.kaltura.playkit.utils.Consts
import com.kaltura.tvplayer.OfflineManager
import com.kaltura.tvplayer.OfflineManager.SelectionPrefs
import com.kaltura.tvplayer.OfflineManager.TrackCodec
import com.kaltura.tvplayer.OfflineManager.TrackCodec.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ExoPlayerTrackSelection(
    private val appContext: Context,
    private val downloadHelper: DownloadHelper,
    private val selectionPrefs: SelectionPrefs) {

    private val log = PKLog.get("ExoPlayerTrackSelection")

    private val allowedVideoCodecs = videoCodecs.filter { it.isAllowed() }
    private val allowedAudioCodecs = audioCodecs.filter { it.isAllowed() }

    private val allowedVideoCodecTags = allowedVideoCodecs.map { it.tag() }
    private val allowedAudioCodecTags = allowedAudioCodecs.map { it.tag() }

    //private val allowedCodecTags = allowedVideoCodecTags + allowedAudioCodecTags

    private lateinit var trackSelectionHelper: TrackSelectionHelper
    private val lastSelectedTrackIds = arrayOf(
        "none",
        "none",
        "none",
        "none"
    )

    companion object {
        @JvmStatic fun selectTracks(
            appContext: Context,
            downloadHelper: DownloadHelper,
            selectionPrefs: SelectionPrefs) {
            if (selectionPrefs.downloadType == OfflineManager.DownloadType.PREFETCH) {
                ExoPlayerTrackSelection(appContext, downloadHelper, SelectionPrefs()).selectAllTracks()
            } else {
                ExoPlayerTrackSelection(appContext, downloadHelper, selectionPrefs).selectTracks()
            }
        }

        @JvmStatic fun selectDefaultTracks(
            appContext: Context,
            downloadHelper: DownloadHelper) {
            ExoPlayerTrackSelection(appContext, downloadHelper, SelectionPrefs()).selectDefaultTracks()
        }
    }

    internal fun selectDefaultTracks() {
        downloadDefaultTracks(downloadHelper)
    }

    internal fun selectAllTracks() {
        downloadAllTracks(downloadHelper)
    }

    internal fun selectTracks() {
        val playerSettings = applyPlayerSettings()
        trackSelectionHelper.applyPlayerSettings(playerSettings)
        val pkTracks: PKTracks = trackSelectionHelper.buildTracks(null, listOf()) ?: return
        if (pkTracks.videoTracks.size > 0) {
            downloadVideoTrack(pkTracks)
        }

        if (selectionPrefs.audioLanguages == null && selectionPrefs.audioCodecs != null) {
            if (pkTracks.audioTracks.size > 0) {
                downloadAudioTrack(pkTracks)
            }
        } else {
            downloadLanguageTracks(Consts.TRACK_TYPE_AUDIO, downloadHelper, selectionPrefs)
        }

        downloadLanguageTracks(Consts.TRACK_TYPE_TEXT, downloadHelper, selectionPrefs)
    }

    private fun applyPlayerSettings(): PlayerSettings {
        val playerSettings = PlayerSettings()
        val trackSelector = DefaultTrackSelector(appContext)
        trackSelectionHelper = TrackSelectionHelper(appContext, trackSelector, lastSelectedTrackIds)
        trackSelectionHelper.setMappedTrackInfo(downloadHelper.getMappedTrackInfo(0))

        val videoCodecSettings = VideoCodecSettings()
        if (selectionPrefs.allowInefficientCodecs) {
            videoCodecSettings.isAllowSoftwareDecoder = true
        }

        selectionPrefs.videoCodecs?.let {
            if (it.isNotEmpty()) {
                val videoCodecs = mutableListOf<PKVideoCodec>()
                for (codec in it) {
                    if (codec == HEVC) {
                        videoCodecs.add(PKVideoCodec.HEVC)
                    } else if (codec == AVC1) {
                        videoCodecs.add(PKVideoCodec.AVC)
                    }
                }
                if (videoCodecs.size > 0) {
                    videoCodecSettings.codecPriorityList = videoCodecs
                }
            }
        }
        playerSettings.preferredVideoCodecSettings = videoCodecSettings

        val audioCodecSettings = AudioCodecSettings()
        selectionPrefs.audioCodecs?.let {
            if (it.isNotEmpty()) {
                val audioCodecs = mutableListOf<PKAudioCodec>()
                for (codec in it) {
                    if (codec == EAC3) {
                        audioCodecs.add(PKAudioCodec.E_AC3)
                    } else if (codec == AC3) {
                        audioCodecs.add(PKAudioCodec.AC3)
                    } else if (codec == MP4A) {
                        audioCodecs.add(PKAudioCodec.AAC)
                    }
                }
                if (audioCodecs.size > 0) {
                    audioCodecSettings.codecPriorityList = audioCodecs
                }
            }
        }
        playerSettings.preferredAudioCodecSettings = audioCodecSettings

        return playerSettings
    }

    private fun downloadVideoTrack(pkTracks: PKTracks) {
        val videoExoTracks = mutableListOf<ExoTrack>()
        for (videoTrack in pkTracks.videoTracks) {
            if (videoTrack.bitrate == 0L) {
                continue
            }
            val exoTrack = ExoTrack(
                DownloadItem.TrackType.VIDEO,
                videoTrack.uniqueId,
                videoTrack.bitrate,
                videoTrack.width,
                videoTrack.height,
                videoTrack.codecName,
                null
            )
            videoExoTracks.add(exoTrack)
        }

        val selectedVideoTrack = selectVideoTrack(videoExoTracks)

        val trackUniqueIdArray = parseUniqueId((selectedVideoTrack as ExoTrack).uniqueId)
        val selectionOverrides: MutableList<SelectionOverride> = java.util.ArrayList()
        selectionOverrides.add(SelectionOverride(trackUniqueIdArray[1], trackUniqueIdArray[2]))
        for (periodIndex in 0 until downloadHelper.periodCount) {
            downloadHelper.clearTrackSelections(periodIndex)
            downloadHelper.addTrackSelectionForSingleRenderer(
                periodIndex,
                trackUniqueIdArray[0],
                buildExoParameters(),
                selectionOverrides
            )
        }
    }

    private fun downloadAudioTrack(pkTracks: PKTracks) {
        val audioExoTracks = mutableListOf<ExoTrack>()
        for (audioTrack in pkTracks.audioTracks) {
            if (audioTrack.bitrate == 0L) {
                continue
            }
            val exoTrack = ExoTrack(DownloadItem.TrackType.AUDIO,
                audioTrack.uniqueId,
                audioTrack.bitrate,
                -1, -1,
                audioTrack.codecName,
                audioTrack.language)
            audioExoTracks.add(exoTrack)
        }

        val selectionOverrides: MutableList<SelectionOverride> = java.util.ArrayList()

        if(!selectionPrefs.allAudioLanguages) {
            val selectedAudioTrack = selectAudioTrack(audioExoTracks)
            val trackUniqueIdArray = parseUniqueId((selectedAudioTrack as ExoTrack).uniqueId)
            selectionOverrides.add(SelectionOverride(trackUniqueIdArray[1], trackUniqueIdArray[2]))
            for (periodIndex in 0 until downloadHelper.periodCount) {
                //downloadHelper.clearTrackSelections(periodIndex)
                downloadHelper.addTrackSelectionForSingleRenderer(
                    periodIndex,
                    trackUniqueIdArray[0],
                    buildExoParameters(),
                    selectionOverrides
                )
            }
        } else {
            for (periodIndex in 0 until downloadHelper.periodCount) {
                //downloadHelper.clearTrackSelections(periodIndex)
                for (selectedAudioTrack in audioExoTracks) {
                    val trackUniqueIdArray = parseUniqueId((selectedAudioTrack as ExoTrack).uniqueId)
                    selectionOverrides.add(
                        SelectionOverride(
                            trackUniqueIdArray[1],
                            trackUniqueIdArray[2]
                        )
                    )

                    downloadHelper.addTrackSelectionForSingleRenderer(
                        periodIndex,
                        trackUniqueIdArray[0],
                        buildExoParameters(),
                        selectionOverrides
                    )
                }
            }
        }
    }

    private fun parseUniqueId(uniqueId: String): IntArray {
        val parsedUniqueId = IntArray(3)
        val splitUniqueId: String = removePrefix(uniqueId)
        val strArray = splitUniqueId.split(",").toTypedArray()

        val TRACK_ADAPTIVE = -1
        val TRACK_DISABLED = -2
        val NONE = "none"
        val ADAPTIVE = "adaptive"

        for (i in strArray.indices) {
            when (strArray[i]) {
                ADAPTIVE -> parsedUniqueId[i] = TRACK_ADAPTIVE
                NONE -> parsedUniqueId[i] = TRACK_DISABLED
                else -> parsedUniqueId[i] = strArray[i].toInt()
            }
        }
        return parsedUniqueId
    }

    private fun removePrefix(uniqueId: String): String {
        val strArray = uniqueId.split(":").toTypedArray()
        //always return the second element of the splitString.
        return strArray[1]
    }

    private fun selectVideoTrack(videoTracks: List<Track>): Track? {
        var tracks = videoTracks
        tracks = filterVideoTracks(tracks, selectionPrefs)
        val videoBitrates = videoBitratePrefsPerCodec()
        val tracksByCodec = mutableMapOf<CodecTag, List<Track>>()

        for (codec in TrackCodec.values()) {
            tracksByCodec[codec.tag()] = tracks.filter { it.codecs.split(".").firstOrNull() == codec.tag() }
        }

        for ((codec, bitrate) in videoBitrates) {
            val codecTracks = tracksByCodec[codec.tag()] ?: continue
            tracksByCodec[codec.tag()] = filterTracks(codecTracks, Track.bitrateComparator, { it.bitrate >= bitrate })
        }

        for (codecTag in fullVideoCodecPriority()) {
            val codecTracks = tracksByCodec[codecTag] ?: continue
            val first = codecTracks.firstOrNull()
            if (first != null) {
                return first
            }
        }

        return null
    }

    private fun selectAudioTrack(audioTracks: List<Track>): Track? {
        val tracks = audioTracks
        val tracksByCodec = mutableMapOf<CodecTag, List<Track>>()

        for (codec in TrackCodec.values()) {
            tracksByCodec[codec.tag()] = tracks.filter { it.codecs.split(".").firstOrNull() == codec.tag() }
        }

        for (codecTag in fullAudioCodecPriority()) {
            val codecTracks = tracksByCodec[codecTag] ?: continue
            val first = codecTracks.firstOrNull()
            if (first != null) {
                return first
            }
        }

        return null
    }


    private fun filterVideoTracks(tracks: List<Track>, selectionPrefs: SelectionPrefs): List<Track> {
        selectionPrefs.videoHeight?.let { videoHeight ->
            selectionPrefs.videoWidth?.let { videoWidth ->
                return filterTracks(
                    tracks,
                    Track.pixelComparator,
                    { it.width * it.height >= videoWidth * videoHeight })
            }
        }

        selectionPrefs.videoHeight?.let { videoHeight ->
            return filterTracks(tracks, Track.heightComparator, { it.height >= videoHeight })
        }

        selectionPrefs.videoWidth?.let { videoWidth ->
            return filterTracks(tracks, Track.widthComparator, { it.width >= videoWidth })
        }

        selectionPrefs.videoBitrate?.let { videoBitrate ->
            return filterTracks(tracks, Track.bitrateComparator, { it.bitrate >= videoBitrate })
        }

        return tracks
    }

    private fun videoBitratePrefsPerCodec(): HashMap<TrackCodec, Int> {
        val videoBitrates = HashMap(selectionPrefs.codecVideoBitrates ?: mapOf())
        if (videoBitrates[AVC1] == null) {
            videoBitrates[AVC1] = 180_000
        }
        if (videoBitrates[HEVC] == null) {
            videoBitrates[HEVC] = 120_000
        }
        return videoBitrates
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
        return if (filtered.isEmpty()) sorted.asReversed().subList(0, 1) else filtered
    }

    private fun TrackCodec.isAllowed() = when (this) {
        AVC1 -> true
        HEVC -> OMCodecSupport.hardwareHEVC || OMCodecSupport.anyHEVC && selectionPrefs.allowInefficientCodecs
        MP4A -> true
        AC3 -> OMCodecSupport.AC3
        EAC3 -> OMCodecSupport.EAC3
    }

    private fun TrackCodec.tag(): CodecTag = when (this) {
        AVC1 -> "avc1"
        HEVC -> "hvc1"
        MP4A -> "mp4a"
        AC3 -> "ac-3"
        EAC3 -> "ec-3"
    }

    private fun Track.codecTags(): Set<CodecTag> {
        // `codecs` is an array of String, but it's declared as a non-generic `Array`.
        // It might also be nil.
        val codecs = this.codecs ?: return setOf()
        val codecList = codecs.split(',')
        val tags = mutableSetOf<CodecTag>()
        for (c in codecList) {
            val tag = c.split('.').first().trim()
            tags.add(tag)
        }
        return tags
    }

    private fun Track.videoCodec(): CodecTag? {
        for (tag in codecTags()) {
            if (videoCodecs.any { it.tag() == tag }) {
                return tag
            }
        }
        return null
    }

    private fun Track.audioCodec(): CodecTag? {
        for (tag in codecTags()) {
            if (audioCodecs.any { it.tag() == tag }) {
                return tag
            }
        }
        return null
    }

    private fun fullVideoCodecPriority() : List<CodecTag> =
        fullCodecPriority(
            requestedTags = (selectionPrefs.videoCodecs ?: listOf()).map { it.tag() },
            allowedTags = allowedVideoCodecTags
        )

    private fun fullAudioCodecPriority() : List<CodecTag> =
        fullCodecPriority(
            requestedTags = (selectionPrefs.audioCodecs ?: listOf()).map { it.tag() },
            allowedTags = allowedAudioCodecTags
        )

    private fun fullCodecPriority(requestedTags: List<CodecTag>, allowedTags: List<CodecTag>) : List<CodecTag> {
        val codecPriority = ArrayList(requestedTags)
        for (codec in allowedTags) {
            if (!codecPriority.contains(codec)) {
                codecPriority.add(codec)
            }
        }
        return codecPriority
    }

    private fun downloadLanguageTracks(rendererIndex: Int, downloadHelper: DownloadHelper, selectionPrefs: SelectionPrefs) {
        log.d("downloadLanguageTracks")
        val mappedTrackInfo = downloadHelper.getMappedTrackInfo(0)
        for (periodIndex in 0 until downloadHelper.periodCount) {
            val selectionOverrides: MutableList<SelectionOverride> = java.util.ArrayList()
            val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)

            for (groupIndex in 0 until trackGroupArray.length) {
                //run through the all tracks in current trackGroup.
                val trackGroup = trackGroupArray[groupIndex]

                for (trackIndex in 0 until trackGroup.length) {
                    val format = trackGroup.getFormat(trackIndex)
                    if (rendererIndex == Consts.TRACK_TYPE_AUDIO) {
                        if (format.language == null ||
                            !selectionPrefs.allAudioLanguages &&
                            (selectionPrefs.audioLanguages == null || !selectionPrefs.audioLanguages!!.contains(format.language))) {
                            continue
                        } else {
                            log.d("audio language = " + format.language + " bitrate = " + format.bitrate)
                            if (!TextUtils.isEmpty(format.language)) {
                                selectionOverrides.add(SelectionOverride(groupIndex, trackIndex))
                            }
                        }
                    } else if (rendererIndex == Consts.TRACK_TYPE_TEXT) {
                        if (format.language == null ||
                            !selectionPrefs.allTextLanguages &&
                            (selectionPrefs.textLanguages == null || !selectionPrefs.textLanguages!!.contains(format.language))) {
                            continue
                        } else {
                            log.d("text language = " + format.language + " bitrate = " + format.bitrate)
                            selectionOverrides.add(SelectionOverride(groupIndex, trackIndex))
                        }
                    }
                }
            }

            downloadHelper.addTrackSelectionForSingleRenderer(
                periodIndex,
                rendererIndex,
                buildExoParameters(),
                selectionOverrides
            )
        }
    }

    private fun downloadAllTracks(downloadHelper: DownloadHelper) {
        log.d("downloadAllTracks")
        for (periodIndex in 0 until downloadHelper.periodCount) {
            val mappedTrackInfo = downloadHelper.getMappedTrackInfo(periodIndex)
            downloadHelper.clearTrackSelections(periodIndex)

            for (rendererIndex in listOf(Consts.TRACK_TYPE_VIDEO, Consts.TRACK_TYPE_AUDIO, Consts.TRACK_TYPE_TEXT) ) { // 0, 1, 2 run only over video audio and text tracks
                val selectionOverrides: MutableList<SelectionOverride> = java.util.ArrayList()
                val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)

                for (groupIndex in 0 until trackGroupArray.length) {
                    //run through the all tracks in current trackGroup.
                    val trackGroup = trackGroupArray[groupIndex]

                    for (trackIndex in 0 until trackGroup.length) {
                        val format = trackGroup.getFormat(trackIndex)
                        if (rendererIndex == Consts.TRACK_TYPE_VIDEO) { //video
                            log.d("video bitrate = " + format.bitrate + " codec = " + format.sampleMimeType)
                            selectionOverrides.add(SelectionOverride(groupIndex, trackIndex))
                        } else if (rendererIndex == Consts.TRACK_TYPE_AUDIO) {
                            log.d("audio language = " + format.language + " bitrate = " + format.bitrate)
                            selectionOverrides.add(SelectionOverride(groupIndex, trackIndex))
                        } else if (rendererIndex == Consts.TRACK_TYPE_TEXT) {
                            log.d("text language = " + format.language + " bitrate = " + format.bitrate)
                            selectionOverrides.add(SelectionOverride(groupIndex, trackIndex))
                        }
                    }
                }

                downloadHelper.addTrackSelectionForSingleRenderer(
                    periodIndex,
                    rendererIndex,
                    buildExoParameters(),
                    selectionOverrides
                )
            }
        }
    }

    private fun downloadAllTracksByTrackType(
        trackType: Int,
        downloadHelper: DownloadHelper,
        selectionPrefs: SelectionPrefs
    ) {
        for (periodIndex in 0 until downloadHelper.periodCount) {
            val mappedTrackInfo = downloadHelper.getMappedTrackInfo(periodIndex)
            downloadHelper.clearTrackSelections(periodIndex)
            val selectionOverrides: MutableList<SelectionOverride> = java.util.ArrayList()
            val trackGroupArray = mappedTrackInfo.getTrackGroups(trackType)

            for (groupIndex in 0 until trackGroupArray.length) {
                //run through the all tracks in current trackGroup.
                val trackGroup = trackGroupArray[groupIndex]

                for (trackIndex in 0 until trackGroup.length) {
                    selectionOverrides.add(SelectionOverride(groupIndex, trackIndex))
                }
                downloadHelper.addTrackSelectionForSingleRenderer(
                    periodIndex,
                    trackType,
                    buildExoParameters(),
                    selectionOverrides
                )
            }
        }
    }

    private fun downloadDefaultTracks(downloadHelper: DownloadHelper) {
        for (periodIndex in 0 until downloadHelper.periodCount) {
            downloadHelper.clearTrackSelections(periodIndex)

            for (i in 0 until downloadHelper.getMappedTrackInfo(periodIndex).rendererCount) {
                downloadHelper.addTrackSelectionForSingleRenderer(
                    periodIndex,  /* rendererIndex= */
                    i,
                    DownloadHelper.getDefaultTrackSelectorParameters(appContext), emptyList()
                )
            }
        }
    }

    private fun buildExoParameters(): DefaultTrackSelector.Parameters {
        return DownloadHelper.getDefaultTrackSelectorParameters(appContext)
    }

    private fun isDownloadableTrackType(mappedTrackInfo: MappingTrackSelector.MappedTrackInfo, rendererIndex: Int): Boolean {
        val trackGroupArray: TrackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)
        if (trackGroupArray.length === 0) {
            return false
        }
        val trackType: Int = mappedTrackInfo.getRendererType(rendererIndex)
        return isSupportedTrackType(
            trackType
        )
    }

    private fun isSupportedTrackType(trackType: Int): Boolean {
        return when (trackType) {
            C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_AUDIO, C.TRACK_TYPE_TEXT -> true
            else -> false
        }
    }
}

private val videoCodecs = listOf(HEVC, AVC1)
private val audioCodecs = listOf(EAC3, AC3, MP4A)

private typealias CodecTag = String

private object OMCodecSupport {
    val AC3 = CodecSupport.hasDecoder(MimeTypes.AUDIO_AC3, true, true)
    val EAC3 = CodecSupport.hasDecoder(MimeTypes.AUDIO_E_AC3, true, true)
    val hardwareHEVC = CodecSupport.hasDecoder(MimeTypes.VIDEO_H265, true, false)
    val anyHEVC = CodecSupport.hasDecoder(MimeTypes.VIDEO_H265, true, true)
}

