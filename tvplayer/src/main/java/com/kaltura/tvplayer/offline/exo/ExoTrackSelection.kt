@file:Suppress("MoveLambdaOutsideParentheses")

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

fun selectTracks(
        appContext: Context,
        downloadHelper: DownloadHelper,
        selectionPrefs: SelectionPrefs) {
    if (selectionPrefs.downloadType == OfflineManager.DownloadType.PREFETCH) {
        ExoTrackSelection(appContext, downloadHelper, SelectionPrefs()).selectAllTracks()
    } else {
        ExoTrackSelection(appContext, downloadHelper, selectionPrefs).selectTracks()
    }
}

fun selectDefaultTracks(
    appContext: Context ,
    downloadHelper: DownloadHelper) {
    ExoTrackSelection(appContext, downloadHelper, SelectionPrefs()).selectDefaultTracks()
}

private val log = PKLog.get("ExoTrackSelection")

class ExoTrackSelection(
        private val appContext: Context,
        private val downloadHelper: DownloadHelper,
        private val selectionPrefs: SelectionPrefs) {

    private val allowedVideoCodecs = videoCodecs.filter { it.isAllowed() }
    private val allowedAudioCodecs = audioCodecs.filter { it.isAllowed() }

    private val allowedVideoCodecTags = allowedVideoCodecs.map { it.tag() }
    private val allowedAudioCodecTags = allowedAudioCodecs.map { it.tag() }

    private val allowedCodecTags = allowedVideoCodecTags + allowedAudioCodecTags

    private lateinit var trackSelectionHelper: TrackSelectionHelper
    private val lastSelectedTrackIds = arrayOf(
        "none",
        "none",
        "none",
        "none"
    )

    internal fun selectDefaultTracks() {
        downloadDefaultTracks(downloadHelper);
    }

    internal fun selectAllTracks() {
        downloadAllTracks(downloadHelper)
    }

    internal fun selectTracks() {
        val playerSettings = PlayerSettings()
        val trackSelector = DefaultTrackSelector(appContext)
        trackSelectionHelper = TrackSelectionHelper(appContext, trackSelector, lastSelectedTrackIds)
        trackSelectionHelper.setMappedTrackInfo(downloadHelper.getMappedTrackInfo(0))

        if (selectionPrefs.allowInefficientCodecs) {
            playerSettings.setPreferredVideoCodecSettings(VideoCodecSettings().setCodecPriorityList(
                listOf(PKVideoCodec.AVC)))
            playerSettings.setPreferredAudioCodecSettings(AudioCodecSettings().setCodecPriorityList(
                listOf(PKAudioCodec.AC3)))
            trackSelectionHelper.setPlayerSettings(playerSettings)
        }

        if (selectionPrefs.videoCodecs != null && selectionPrefs.videoCodecs!!.size > 0) {
            val videoCodecs = mutableListOf<PKVideoCodec>()
            for (code in selectionPrefs.videoCodecs ?: listOf()) {
                if (code == HEVC) {
                    videoCodecs.add(PKVideoCodec.HEVC)
                } else  if (code == AVC1) {
                    videoCodecs.add(PKVideoCodec.AVC)
                }
            }
            playerSettings.setPreferredVideoCodecSettings(VideoCodecSettings().setCodecPriorityList(videoCodecs))
        }

        if (selectionPrefs.audioCodecs != null && selectionPrefs.audioCodecs!!.size > 0) {
            val audioCodecs = mutableListOf<PKAudioCodec>()

            for (code in selectionPrefs.videoCodecs ?: listOf()) {
                if (code == EAC3) {
                    audioCodecs.add(PKAudioCodec.E_AC3)
                } else  if (code == AC3) {
                    audioCodecs.add(PKAudioCodec.AC3)
                } else  if (code == MP4A) {
                    audioCodecs.add(PKAudioCodec.AAC)
                }
            }
            playerSettings.setPreferredAudioCodecSettings(AudioCodecSettings().setCodecPriorityList(audioCodecs))
        }

        var pkTracks: PKTracks = trackSelectionHelper.buildTracks(listOf()) ?: return
        downloadVideoTrack(pkTracks)

        var audioExoTracks = mutableListOf<ExoTrack>()
        for (audioTrack in pkTracks.audioTracks) {
            var exoTrack = ExoTrack(DownloadItem.TrackType.AUDIO, audioTrack.uniqueId, audioTrack.bitrate, -1, -1, audioTrack.codecName, audioTrack.language);
            audioExoTracks.add(exoTrack)
        }

        downloadLanguageTracks(Consts.TRACK_TYPE_AUDIO, downloadHelper, selectionPrefs)
        downloadLanguageTracks(Consts.TRACK_TYPE_TEXT, downloadHelper, selectionPrefs)
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

        val trackUniqueIdidArray = parseUniqueId((selectedVideoTrack as ExoTrack).uniqueId)
        val selectionOverrides: MutableList<SelectionOverride> = java.util.ArrayList()
        selectionOverrides.add(SelectionOverride(trackUniqueIdidArray[1], trackUniqueIdidArray[2]))
        for (periodIndex in 0 until downloadHelper.periodCount) {
            downloadHelper.clearTrackSelections(periodIndex)
            downloadHelper.addTrackSelectionForSingleRenderer(
                periodIndex,
                trackUniqueIdidArray[0],
                buildExoParameters(),
                selectionOverrides
            )
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

        selectionPrefs.videoHeight?.let { videoHeight ->
            tracks = filterTracks(tracks, Track.heightComparator, { it.height >= videoHeight} )
        }

        selectionPrefs.videoWidth?.let { videoWidth ->
            tracks = filterTracks(tracks, Track.widthComparator, { it.width >= videoWidth} )
        }

        selectionPrefs.videoBitrate?.let { videoBitrate ->
            tracks = filterTracks(tracks, Track.bitrateComparator, { it.bitrate >= videoBitrate} )
        }


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

        return if (filtered.isEmpty()) sorted.subList(0, 1) else filtered
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
                                log.d("XXX audio language = " + format.language + " bitrate =" + format.bitrate)
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
                                log.d("XXX text language = " + format.language + " bitrate =" + format.bitrate)
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
                            log.d("XXX video bitrate = " + format.bitrate + " codec = " + format.sampleMimeType)
                            selectionOverrides.add(SelectionOverride(groupIndex, trackIndex))
                        } else if (rendererIndex == Consts.TRACK_TYPE_AUDIO) {
                            log.d("XXX audio language = " + format.language + " bitrate =" + format.bitrate)
                            selectionOverrides.add(SelectionOverride(groupIndex, trackIndex))
                        } else if (rendererIndex == Consts.TRACK_TYPE_TEXT) {
                            log.d("XXX text language = " + format.language + " bitrate =" + format.bitrate)
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

    private fun isDowonloadableTrackType(mappedTrackInfo: MappingTrackSelector.MappedTrackInfo, rendererIndex: Int): Boolean {
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


//private fun downloadAllTracks(downloadHelper: DownloadHelper, selectionPrefs: SelectionPrefs) {
//    log.d("downloadAllTracks")
//    val mappedTrackInfo = downloadHelper.getMappedTrackInfo(0)
//    for (periodIndex in 0 until downloadHelper.periodCount) {
//        downloadHelper.clearTrackSelections(periodIndex)
//        for (rendererIndex in listOf(Consts.TRACK_TYPE_VIDEO, Consts.TRACK_TYPE_AUDIO, Consts.TRACK_TYPE_TEXT) ) { // 0, 1, 2 run only over video audio and text tracks
//            val selectionOverrides: MutableList<SelectionOverride> = java.util.ArrayList()
//            val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)
//            for (groupIndex in 0 until trackGroupArray.length) {
//                //run through the all tracks in current trackGroup.
//                val trackGroup = trackGroupArray[groupIndex]
//                for (trackIndex in 0 until trackGroup.length) {
//                    val format = trackGroup.getFormat(trackIndex)
//                    if (rendererIndex == Consts.TRACK_TYPE_VIDEO) { //video
//                        log.d("XXX video bitrate = " + format.bitrate + " codec = " + format.sampleMimeType)
//                        if (selectionPrefs.videoBitrate != null) {
//                            var range = low
//                            if (downloadVideoQuality == DownloadVideoQuality.MEDIUM) {
//                                range = medium
//                            } else if (downloadVideoQuality == DownloadVideoQuality.HIGH) {
//                                range = high
//                            }
//                            if (format.bitrate >= range.first && format.bitrate <= range.second) {
//                                selectionOverrides.add(
//                                    SelectionOverride(
//                                        groupIndex,
//                                        trackIndex
//                                    )
//                                )
//                                break
//                            }
//                        } else {
//                            selectionOverrides.add(SelectionOverride(groupIndex, trackIndex))
//                        }
//                    } else if (rendererIndex == Consts.TRACK_TYPE_AUDIO) {
//                        if (format.language == null || !selectionPrefs.allAudioLanguages && (selectionPrefs.audioLanguages == null || !selectionPrefs.audioLanguages!!.contains(
//                                format.language
//                            ))
//                        ) {
//                            continue
//                        } else {
//                            log.d("XXX audio language = " + format.language + " bitrate =" + format.bitrate)
//                            if (!TextUtils.isEmpty(format.language)) {
//                                selectionOverrides.add(SelectionOverride(groupIndex, trackIndex))
//                            }
//                        }
//                    } else if (rendererIndex == Consts.TRACK_TYPE_TEXT) {
//                        if (format.language == null || !selectionPrefs.allTextLanguages && (selectionPrefs.textLanguages == null || !selectionPrefs.textLanguages!!.contains(
//                                format.language
//                            ))
//                        ) {
//                            continue
//                        } else {
//                            log.d("XXX text language = " + format.language + " bitrate =" + format.bitrate)
//                            selectionOverrides.add(SelectionOverride(groupIndex, trackIndex))
//                        }
//                    }
//                }
//            }
//
//            downloadHelper.addTrackSelectionForSingleRenderer(
//                periodIndex,
//                rendererIndex,
//                buildExoParameters(),
//                selectionOverrides
//            )
//        }
//    }
//}


//    private DefaultTrackSelector.Parameters buildExoParameters(SelectionPrefs selectionPrefs) {
//        //MappingTrackSelector.MappedTrackInfo mappedTrackInfo = downloadHelper.getMappedTrackInfo(/* periodIndex= */ 0);
//        //return new DefaultTrackSelector.ParametersBuilder(appContext).setMaxVideoSizeSd().build();
//        //return DefaultTrackSelector.Parameters.getDefaults(appContext);   // TODO: 2019-07-31
//        //return new DefaultTrackSelector.ParametersBuilder(appContext).build();
//        //return DownloadHelper.getDefaultTrackSelectorParameters(appContext);
//
//        //return  DefaultTrackSelector.Parameters.DEFAULT_WITHOUT_CONTEXT.buildUpon().setForceHighestSupportedBitrate(true).build();
//        return DownloadHelper.getDefaultTrackSelectorParameters(appContext);
//        //return  DefaultTrackSelector.Parameters.DEFAULT_WITHOUT_CONTEXT.buildUpon().build();
//
//    }
