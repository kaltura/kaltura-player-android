[![CI Status](https://github.com/kaltura/kaltura-player-android/actions/workflows/build.yml/badge.svg)](https://github.com/kaltura/kaltura-player-android/actions/workflows/build.yml)
[![Download](https://img.shields.io/maven-central/v/com.kaltura.player/tvplayer?label=Download)](https://search.maven.org/artifact/com.kaltura.player/tvplayer/)
[![License](https://img.shields.io/badge/license-AGPLv3-black.svg)](https://github.com/kaltura/playkit-android/blob/master/LICENSE)
![Android](https://img.shields.io/badge/platform-android-green.svg)

# Kaltura Player for Android

**Kaltura Player**  - This [`Playkit`](https://github.com/kaltura/playkit-android) wrapper simplifies the player integration so that client applications will require less boilerplate code, which ensures a faster integration.

## Kaltura Player Features:

#### Playback:

* DASH
* HLS
* MP4
* MP3
* Multicast(UDP)
* Multiple Codecs support
* Live / Live DVR
* TrackSelection (Video/Audio/Text)
* External subtitles / Subtitle positioning and styling
* Playback Rate
* ABR Configuration
* VR /360 media playback
* Dash Instream Thumbnails
* External Sprite Image support for Thumbnails
* Change Media
* Playlist
* ID3 Timed Metadata
* Screen recording blocking
* Playback Adapter - allow app to change the manifest url query param or headers changes

#### DRM:

* Widevine
* Playready
* DRM Adapter - allow app to change license url query param or headers changes

#### Analytics:

* Kaltura Kava (Kaltura Advanced Video Analytics)
* Phoenix Analytics
* Youbora (NPAW)

#### Monitization:
* IMA 
* DAI
* [Ad Layout Configuration](https://kaltura.github.io/playkit/guide/android/core/advertising-layout.html) 
  - Ad Warterfalling
  - Ad Schedualer
  - Ad-Hoc Ad playback

#### CDN:

* Broadpeak
* NPAW Smart Switch

#### Offline:
* Download to go 
* Prefetch (preloading)

#### Casting:
* Google Cast


----------


Gradle Dependency:  `implementation 'com.kaltura.player:tvplayer:4.x.x'` + add mavenCentral() in repositories section

This dependency already includes **Playkit, Kava Analytics Player Providers and Download-to-Go libraries** internally, so no need to add them to the client app's `build.gradle`.

[Kaltura Player Migration Guide](https://kaltura.github.io/playkit/guide/android/migration/KalturaPlayer.html)

[Kaltura Player Docs](https://developer.kaltura.com/player/android/getting-started-android)

[Kaltura Player OVP Samples](https://github.com/kaltura/kaltura-player-android-samples/tree/master/OVPSamples)

[Kaltura Player OTT Samples](https://github.com/kaltura/kaltura-player-android-samples/tree/master/OTTSamples)

[Kaltura Player Basic Player Samples](https://github.com/kaltura/kaltura-player-android-samples/tree/master/BasicSamples)

[Kaltura Player Advanced Samples](https://github.com/kaltura/kaltura-player-android-samples/tree/master/AdvancedSamples)

Kaltura Player Advanced Samples includes the advanced features like AdLayout, MediaPreviewSample for showing the thumbanail image on seekbar using external sprite image URL, RecyclerView Sample for showing media autoplay feature, DashThumbnail sample for showing in-stream image thumbnails.

[Kaltura Player Offline Sample](https://github.com/kaltura/kaltura-player-android-samples/tree/master/OfflineDemo)


#### Pre-requisite

###### Player Initialization

Client application should call a mandatory initialization method calls at the time of application Launch or in the project's `Application` class file or the `SplashActivity`.

`KalturaPlayer.initializeOTT(this, OTT_PARNTER_ID, OTT_SERVER_URL);`
`KalturaPlayer.initializeOVP(this, OVP_PARNTER_ID, OVP_SERVER_URL);`

###### OTT
`public static final String OTT_SERVER_URL = "https://url_of_kaltura_be";`
`public static final int OTT_PARNTER_ID = kaltura_partner_id;`

#### Note: 
Without calling the initialization code on startup `KalturaPlayerNotInitializedError`, error will be fired on the `player.loadMedia` callback phase.


###### Warmup

Application can use the warmup connections for its specific `CDN` servers URLs where the medias are hosted so a connection to the hosts will be opened and ready for use so all handshake process will be saved and media playback will be faster.

There should be only one URL per host, and the URLs should resolve to a valid path names. A good choice might be `favicon.ico` or `crossdomain.xml` after the host name.

If application wants to use connection warmer mechanism then this call will be placed at the time of application Launch or in the project's `Application` class file.

```java
/*
Set the http provider. Valid options are "system" (use the build-in {@linkplain java.net.HttpURLConnection}) 
and "okhttp" (use Square's <a href="https://square.github.io/okhttp/">OkHttp</a> library).
*/ 
PKHttpClientManager.setHttpProvider("okhttp");
PKHttpClientManager.warmUp (
      "https://https://rest-us.ott.kaltura.com/crossdomain.xml",
      "http://cdnapi.kaltura.com/favicon.ico",
      "https://cdnapisec.kaltura.com/favicon.ico",
      "https://cfvod.kaltura.com/favicon.ico"
      );     
```
## Kaltura Player configurations

##### Media Providers

1. OTT
2. OVP
3. Basic (For those client apps which don't use Kaltura Backend)

Please check the samples for Media Providers [here](https://github.com/kaltura/kaltura-player-android-samples/tree/release/v4.0.0)

Application should create `PlayerInitOptions`.

#### Initializing OTT/OVP Player
For initialization of OTT Player use `OTT_PARTNER_ID` and for initialization if OVP player use`OVP_PARTNER_ID` 

##### Example:

```

val mediaOptions = buildMediaOptions()
        player?.loadMedia(mediaOptions) { entry, loadError ->
            if (loadError != null) {
                Snackbar.make(findViewById(android.R.id.content), loadError.message, Snackbar.LENGTH_LONG).show()
            } else {
                log.d("onEntryLoadComplete  entry = " + entry.id)
            }
}
```

OR

#### Initializing Basic Player

##### Example:

```
  fun loadPlaykitPlayer(pkMediaEntry: PKMediaEntry) {
        val playerInitOptions = PlayerInitOptions()

        player = KalturaBasicPlayer.create(this@MainActivity, playerInitOptions)
        player?.setPlayerView(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        val container = player_root
        container.addView(player?.playerView)
        player?.setMedia(pkMediaEntry, START_POSITION)
    }
```

## `PlayerInitOptions`

`PlayerInitOptions` is an important API to get most of the Player. If you are currently using `Playkit` then it is similar to `PlayerSettings` there.

##### `setAutoPlay(Boolean autoplay)`
Default is `true`. Allows the playback to start automatically after the preparation. 
  
##### `setPreload(Boolean preload)`
Default is `true`. Allows the player to prepare itself. If set to `false` the after player load, App needs to call `prepare()`.
If autoplay is `true` then preload is being treated as `true`.

##### `setReferrer(String referrer)`
Default is empty. Referred string for the backend.

##### `setAudioLanguage(String audioLanguage, PKTrackConfig.Mode audioLanguageMode)` 
Preferred audio language for the player. If given then player will pick the given audio language automatically if available in the media.

`audioLanguageMode` is `OFF`, `AUTO` and `SELECTION`. Default is `OFF`. If selected `AUTO`, player will try to take the device langauge (Locale) and will pick the audio language if available in the media. If selected `Selection` then it will pick the language passed in the `audioLanguage` parameter.

> Similar to `setPreferredAudioTrack(PKTrackConfig preferredAudioTrackConfig)` of `PlayerSettings`.

##### `setTextLanguage(String textLanguage, PKTrackConfig.Mode textLanguageMode)`
Preferred text language for the player. If given then player will pick the given text language automatically if available in the media.

`textLanguageMode ` is `OFF`, `AUTO` and `SELECTION`. Default is `OFF`. If selected `AUTO`, player will try to take the device langauge (Locale) and will pick the text if available in the media. If selected `Selection` then it will pick the language passed in the `textLanguage ` parameter.

> Similar to `setPreferredTextTrack(PKTrackConfig preferredTextTrackConfig)` of `PlayerSettings`.

##### `setPreferredMediaFormat(PKMediaFormat preferredMediaFormat)`
If backend response contains multiple formats (Dash, HLS etc) then if this API is set with a specific format, Player will pick the given format.

> Similar to `setPreferredMediaFormat(PKMediaFormat preferredMediaFormat)` of `PlayerSettings`.

##### `setEnableDecoderFallback(Boolean enableDecoderFallback)`
Decide whether to enable fallback to lower-priority decoders if decoder initialization fails.

##### `useTextureView(Boolean useTextureView)`
Decide if player should use android.view.TextureView as primary surface
to render the video content. If set to `false`, will use the  android.view.SurfaceView instead. 

Note! Use this carefully, because android.view.TextureView is more expensive and not DRM protected. But it allows dynamic animations/scaling e.t.c on the player. By default it will be always set to `false`.

##### `setSecureSurface(Boolean secureSurface)`
Default is `false`. Decide if player should use secure rendering on the surface. 
Known limitation - when `useTextureView` set to `true` and `isSurfaceSecured` set to `true` then secure rendering will have no effect.

##### `setAdAutoPlayOnResume(Boolean adAutoPlayOnResume)`
Default is `true`. Decide the Ad will be auto played when comes to foreground from background.

##### `setVRSettings(VRSettings vrSettings)`
Set VR Settings on the player for VR/360 media.

##### `setVrPlayerEnabled(Boolean vrPlayerEnabled)`
Default is `true`. Set the Player's VR/360 support. If 360 media should be played on VR player or default player. If `setVRSettings(VRSettings vrSettings)` are set then only this flag works otherwise, treat it as no-op.

##### `setPKLowLatencyConfig(PKLowLatencyConfig pkLowLatencyConfig)`
Creates a Low Latency Live playback configuration. Please check `PKLowLatencyConfig` class' setter methods for more details.

##### `setPKRequestConfig(PKRequestConfig pkRequestConfig)`
Creates a request configuration for HttpDataSourceFactory used during player network calls. 

App can set cross protocol redirections (`http` to `https` or viceversa), read and connect timeouts. App can set tell player the maximum number of times to retry a load in the case of a load error, before propagating the error.

##### `setIsVideoViewHidden(Boolean isVideoViewHidden)`
In case of audio only content, app can hide the video view using this API. Set the flag which handles the video view.

##### `forceSinglePlayerEngine(Boolean forceSinglePlayerEngine)`
Default value is set to 'false'. Do not prepare the content player when the Ad starts(if exists); instead content player will be prepared when content_resume_requested is called.

This flag works where ad and content playback is there. In case, if because of the decoders content playback is jittery or having some issues then set this flag to `true`. Mostly handy in Android TV cases.

##### `allowChunklessPreparation(Boolean allowChunklessPreparation)`
This flag is only for the HLS Streams. Default is `true`. 
Player will only use the information in the multivariant playlist to prepare the stream, which works if the `#EXT-X-STREAM-INF` tags contain the `CODECS` attribute.

##### `forceWidevineL3Playback(Boolean forceWidevineL3Playback)`
If the device codec is known to fail if security level L1 is used then set flag to true, it will force the player to use Widevine L3. It will work only SDK level 18 or above.

##### `setDrmSettings(DRMSettings drmSettings)`
Creates a DRM playback configuration. Default is set to play the Widevine DRM content. In case, if App wants to use Playready DRM then set the DRMSchema to 
`PKDrmParams.Scheme.PlayReadyCENC`.

App can set the multi session DRM in this config. 
In stream DRM support can also be set using `setIsForceDefaultLicenseUri`. For more info please look at `DRMSettings` class.

##### `setSubtitleStyle(SubtitleStyleSettings setSubtitleStyle)`
Give styles to the subtitles. Works for both internal and external subtitles.
Can change font, typeface, text color, background color etc.

##### `setAspectRatioResizeMode(PKAspectRatioResizeMode aspectRatioResizeMode)`
Set the Player's AspectRatio resize Mode. It is very handy for different aspect ratio medias like full screen portrait medias.

##### `setContentRequestAdapter(PKRequestParams.Adapter contentRequestAdapter)`
Add headers on the manifest, chunk or the segments network requests. It can be set one time. We don't not support on the fly header manipulation.

##### `setLicenseRequestAdapter(PKRequestParams.Adapter licenseRequestAdapter)`
Add headers on the DRM server requests. 

##### `setLoadControlBuffers(LoadControlBuffers loadControlBuffers)`
Set the player buffers size. App can set the min/max buffer size before the actual playback starts/resume or seeked. For more info please check `LoadControlBuffers` class.

##### `setAbrSettings(ABRSettings abrSettings)`
Set the Player's ABR settings. ABR is supported based on Bitrate, height, width and Pixel level.

##### `setTVPlayerParams(TVPlayerParams tvPlayerParams)`
Sets the initial bitrate estimate in bits per second that should be assumed when a bandwidth estimate is unavailable. To reset it, set it to null.

##### `setCea608CaptionsEnabled(Boolean cea608CaptionsEnabled)`
Enable/disable cea-608 text tracks. By default they are disabled. Note! Once set, this value will be applied to all mediaSources for that instance of Player. In order to disable/enable it again, you should update that value once again. Otherwise it will stay in the previous state.

##### `setMpgaAudioFormatEnabled(Boolean mpgaAudioFormatEnabled)`
Enable/disable MPGA audio tracks. By default they are disabled. Note! Once set, this value will be applied to all mediaSources for that instance of Player. In order to disable/enable it again, you should update that value once again. Otherwise it will stay in the previous state.

##### `setVideoCodecSettings(VideoCodecSettings videoCodecSettings)`
Set Preferred codec for video track. App can give the codec priority list like HEVC, AV1, VP9, VP8, AVC.

App can pass `allowSoftwareDecoder` to `VideoCodecSettings`. Using this flag, app is giving freedom to player to choose the software based decoders as well. Default is `false`.

App can pass `allowMixedCodecAdaptiveness` to `VideoCodecSettings`, using this player will take all the available tracks of different codecs as well.

> Similar to `setPreferredVideoCodecSettings(VideoCodecSettings videoCodecSettings)` of `PlayerSettings`.

##### `setAudioCodecSettings(AudioCodecSettings audioCodecSettings)`
Set Preferred codec for audio track. App can give the codec priority list like E_AC3, AC3, OPUS, AAC.

App can pass `allowMixedCodecs` to `AudioCodecSettings `. Using this flag, app is giving freedom to player to take all the available audio codecs from the different adaptation set of the manifest. Default is `false`.

App can pass `allowMixedBitrates` to `AudioCodecSettings `, using this player will take all the available bitrate tracks.

> Similar to `setPreferredAudioCodecSettings(AudioCodecSettings audioCodecSettings)` of `PlayerSettings`.


##### `setTunneledAudioPlayback(Boolean isTunneledAudioPlayback)`
Default is `false`. Set Tunneled Audio Playback.

##### `setHandleAudioBecomingNoisy(Boolean handleAudioBecomingNoisyEnabled)`
Default is `false`. Set HandleAudioBecomingNoisy - Sets whether the player should pause automatically when audio is rerouted from a headset to device speakers.

##### `setHandleAudioFocus(Boolean handleAudioFocus)`
Default is `false`. Set HandleAudioFocus - Support for automatic audio focus handling

##### `setWakeMode(PKWakeMode wakeMode)`
Set WakeLock Mode  - Sets whether the player should not handle wakeLock or should handle a wake lock only or both wakeLock & wifiLock when the screen is off.

##### `setSubtitlePreference(PKSubtitlePreference subtitlePreference)`
Set preference to choose internal subtitles over external subtitles (Only in the case if the same language is present in both Internal and External subtitles) - Default is true (Internal is preferred).

- `PKSubtitlePreference.INTERNAL`, Internal will be present and External subtitle will be discarded
- `PKSubtitlePreference.EXTERNAL`, External will be present and Internal subtitle will be discarded
- `PKSubtitlePreference.OFF`, Both internal and external subtitles will be there

##### `setMaxAudioChannelCount(Integer maxAudioChannelCount)`
Sets the maximum allowed audio channel count. Default is `Integer.MAX_VALUE`

##### `setMulticastSettings(MulticastSettings multicastSettings)`
Sets the multicastSettings for udp streams. `maxPacketSize` default is 3000 & `socketTimeoutMillis` default is 10000.
Please check the `MulticastSettings` class for more details. 

##### `setMediaEntryCacheConfig(MediaEntryCacheConfig mediaEntryCacheConfig)`
##### `setOfflineProvider(OfflineManager.OfflineProvider offlineProvider)`


