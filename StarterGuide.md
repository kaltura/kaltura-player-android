<p>
<h2 style="text-align:center" >Started Guide / Design Checklist</h2>
</p>

Before delving into setting up player in your app or if you are currently using our player; we want you to go through this simple doc where we have explained the key features of player and will talk about critical points to take an account. 

[Kaltura-Player](https://kaltura.github.io/playkit/guide/android/migration/KalturaPlayer.html) - If you are using Kaltura backend then you might want to check 
[OTT Backend Customer Sample Code](https://github.com/kaltura/kaltura-player-android-samples/tree/develop/OTTSamples) OR
[OVP Backend Customer Sample Code](https://github.com/kaltura/kaltura-player-android-samples/tree/develop/OVPSamples).

In case, if you are not using Kaltura backend then also you might want to check, our [Basic Sample Code](https://github.com/kaltura/kaltura-player-android-samples/tree/develop/BasicSamples).

Kaltura player internally uses [Playkit](https://github.com/kaltura/playkit-android), [Kava Analytics](https://github.com/kaltura/playkit-android-kava), [Download To Go(Offline Downloads)](https://github.com/kaltura/playkit-dtg-android) and [Media Providers (Only for Kaltura backend customers)](https://github.com/kaltura/playkit-android-providers).

Apart from *Playkit Core Player*, to other libraries; we call *Plugins*. [To Know More check this](https://kaltura.github.io/playkit/#components)

In Kaltura Player, you can add extra plugins as per your requirement [IMA SDK](https://github.com/kaltura/playkit-android-ima), [Youbora Analytics](https://github.com/kaltura/playkit-android-youbora), [Chromecast](https://github.com/kaltura/playkit-android-googlecast), [VR/360](https://github.com/kaltura/playkit-android-vr)


<p>
<h3 style="text-align:center">&#x25ba;Using Kaltura Player and Plugins Standalone</h3>
</p>

1. As we know that **Kaltura-Player** uses [Playkit](https://github.com/kaltura/playkit-android), [Kava Analytics](https://github.com/kaltura/playkit-android-kava), [Download To Go(Offline Downloads)](https://github.com/kaltura/playkit-dtg-android) and [Providers(Only for Kaltura backend customers)](https://github.com/kaltura/playkit-android-providers). It has three Players ***Basic Player***, ***OTT Player*** and ***OVP Player***.

2. Intitalize the Player [Initialize OVP Player](https://github.com/kaltura/kaltura-player-android-samples/blob/78c1f6b4f3898c301d394947061b4a458ab284ea/OVPSamples/BasicSetup/app/src/main/java/com/kaltura/playkit/samples/basicsample/DemoApplication.kt#L13), [Initialize OTT Player](https://github.com/kaltura/kaltura-player-android-samples/blob/78c1f6b4f3898c301d394947061b4a458ab284ea/OTTSamples/BasicSetup/app/src/main/java/com/kaltura/playkit/samples/basicsample/DemoApplication.kt#L13). There is no need to do this for Basic Player.

3. You need to configure Plugins. [Code Sample](https://github.com/kaltura/kaltura-player-android-samples/blob/78c1f6b4f3898c301d394947061b4a458ab284ea/AdvancedSamples/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.kt#L674).

4. You need to create an object of `PlayerInitOptions`, this has the same properties like `PlayerSettings` of Playkit.

5. Create KalturaPlayer based on your requirement [Code Sample](https://github.com/kaltura/kaltura-player-android-samples/blob/78c1f6b4f3898c301d394947061b4a458ab284ea/AdvancedSamples/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.kt#L306).

6. Now load `KalturaPlayer`, [Use OTT Player](https://github.com/kaltura/kaltura-player-android-samples/blob/78c1f6b4f3898c301d394947061b4a458ab284ea/AdvancedSamples/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.kt#L366), [Use OVP Player](https://github.com/kaltura/kaltura-player-android-samples/blob/78c1f6b4f3898c301d394947061b4a458ab284ea/AdvancedSamples/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.kt#L391) and [Use Basic Player](https://github.com/kaltura/kaltura-player-android-samples/blob/78c1f6b4f3898c301d394947061b4a458ab284ea/AdvancedSamples/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.kt#L348).

7. Add your application's UI to `playerview`. For this simply define your layout in [xml layout](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/res/layout/activity_main.xml#L106).

	```
	player?.setPlayerView(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        val container = findViewById<ViewGroup>(R.id.player_view)
        container.addView(player?.playerView)
	```
8. Add the player and other plugin's listeners [Code Sample](https://github.com/kaltura/kaltura-player-android-samples/blob/78c1f6b4f3898c301d394947061b4a458ab284ea/AdvancedSamples/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.kt#L852).


<p>
<h3 style="text-align:center">&#x25ba;Using Playkit and Plugins Standalone</h3>
</p>

We have defined Steps to check and use the following while integrating to your application.

1. You can use our customized Log levels. [Check this Page](https://kaltura.github.io/playkit/guide/android/core/logging.html) [ Code Sample](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L117).
2. You need to check if the device supports DRM or not. If you are looking to play DRM protected content. [Check this page](https://kaltura.github.io/playkit/guide/android/core/drm.html) [ Code Sample](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L318).
3. You need to register the plugins as per your requirement. [Code Sample](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L287).
4. Get the media source and DRM info from Kaltura Backend. If you are a Kaltura backend customer,
 - **OTT Backend Customer** This uses our OTT backend. You need to pass, OTT partner Id, OTT BE url, OTT Kaltura Session(KS), format and MediaId [Code Sample](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L357).

 - **OVP Backend Customer:** This uses our OVP backend. You need to pass, OVP partner Id, OVP BE url, OVP Kaltura Session(KS) and EntryId [Code Sample](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L443).

 ###### For both OTT/OVP backend customers, you will get the call back after calling to the respective BE, [Code Sample](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L298).
 
5. You need to create MediaSource, MediaEntry.

 **OTT/OVP Backend Customers:** You will get `PKMediaEntry` in the callback of 3rd step. Now you need to create `PKMediaConfig`. [Code Sample](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L304).
 		
	`Player player = PlayKitManager.loadPlayer(this, pluginConfig);`
 		
 Now you can modify and various features of player using player's settings API, `player.getSettings()` 
 		
 [Please check this to know more about Settings](https://kaltura.github.io/playkit/guide/android/core/player-settings.html).
 
 **Non Kaltura backend customers:** Please check ***8th point***.
 		
6. **Add Player listeners:** [Please check this](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L916).

	It helps to get Player and its plugins' events to handle application's UI controls like, on `pause` player event you want to change the UI of player to pause ico and turn to play icon on `play` event.
 
7. Add your application's UI to `playerview`. For this simply define your layout in [xml layout](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/res/layout/activity_main.xml#L106).

	```
	FrameLayout layout = (FrameLayout) findViewById(R.id.player_view);
	layout.addView(player.getView());
	```
 		
8. **Non Kaltura Backend Customers:**  
 		
 	Here you go check out the following snippet
 
 ```
		List<PKDrmParams> pkDrmDataList = new ArrayList<>();
		String licenseUri = "DRM License URL";
		PKDrmParams pkDrmParams = new PKDrmParams(licenseUri,PKDrmParams.Scheme.WidevineCENC);
		pkDrmDataList.add(pkDrmParams);
		
		List<PKMediaSource> mediaSourceList = new ArrayList<>();
		PKMediaSource pkMediaSource = new PKMediaSource();
		pkMediaSource.setUrl("Media URL");
		pkMediaSource.setMediaFormat(PKMediaFormat.dash);
		
		pkMediaSource.setDrmData(pkDrmDataList);
		
		PKMediaEntry  pkMediaEntry = new PKMediaEntry();
		        
		mediaSourceList.add(pkMediaSource);
		        
		pkMediaEntry.setSources(mediaSourceList);
		
		PKMediaConfig config = new PKMediaConfig();
		config.setMediaEntry(pkMediaEntry);
		
		PKPluginConfigs pluginConfigs = new PKPluginConfigs();
		
		player = PlayKitManager.loadPlayer(this, pluginConfigs);
		
		player.getSettings().setAllowCrossProtocolRedirect(true);
 ```
	 	  
 	
 		
9. Now prepare the player using the mediaConfig which you prepared.

	```
	player.prepare(mediaConfig);
	player.play();
	```
 
10. Now there is an option to configure the plugins before preparing the plugin. *Make Sure to check 3rd step to register plugins* [Code Sample for player configuration](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L709).

11. You can use single Player instance and use the same for the next medias, It means if you want to change a media on next or previous button so just need to configure the session given in 4th step.

12. You can update the plugins on the change media, [Code Sample](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L532).

--

***&#x25ba;PlayerSettings:*** We are providing more details about the few settings to give more flexibility to the Player. For more details, [Check this page](https://kaltura.github.io/playkit/guide/android/core/player-settings.html).

- In case, if you want to provide customized referrer instead of default referrer in the manifest. Use if after ***Step-5***.

	`KalturaPlaybackRequestAdapter.install(player, "app://PlaykitTestApp");`

- In case, if you want to provide customized referrer instead of default referrer for DRM License request Adapter. Use if after ***Step-5***.

	`KalturaUDRMLicenseRequestAdapter.install(player, "app://PlaykitTestApp");`
	
- `setAllowCrossProtocolRedirect`: Decide if *player should do cross protocol redirect or not*. By default it will be always set to `false`.

- `allowClearLead`: If you want the player to use the initial clear chunks; by using this flag, player will play the *initial clear chunks available in manifest*.

- `enableDecoderFallback`: In case, if the device is not able to initialize the decoders then this flag if it is set to `true` will allow the player to use *low priority decoders.*

- `setSecureSurface`: Decide if player should use secure rendering on the surface. Known limitation - when `useTextureView` set to `true` and `isSurfaceSecured` set to `true` *secure rendering will have no effect*.

- `setABRSettings`: Adaptive bitrate settings. [Check it out](https://github.com/kaltura/playkit-android/blob/dev/playkit/src/main/java/com/kaltura/playkit/player/ABRSettings.java).

- `forceSinglePlayerEngine`*Useful when you use IMA ads*: Default value is `false`. In general, we prepare the content player when Ad Starts; but settings this flag `true` will force the playe not prepare with Ads. Useful for the devices where the low/less decoders.

- `setCustomLoadControlStrategy`: Load control strategy. [Check it out](https://github.com/kaltura/playkit-android/blob/dev/playkit/src/main/java/com/kaltura/playkit/player/LoadControlBuffers.java).

- `setMaxVideoSize`: Sets the maximum allowed video width and height.To set the maximum allowed video bitrate to sd resolution call:`setMaxVideoSize(new PKMaxVideoSize().setMaxVideoWidth(anySize).setMaxVideoHeight(anySize)`.
To reset call:`setMaxVideoSize(new PKMaxVideoSize().setMaxVideoWidth(Integer.MAX_VALUE).setMaxVideoHeight(Integer.MAX_VALUE)`.

- `setMaxVideoBitrate`: Sets the maximum allowed video bitrate. Maximum allowed video bitrate in bits per second.

- `setPreferredVideoCodecSettings`: Set Preferred codec for video track. If the video has multiple codes (Ex: HEVC, AVC, AV1, VP9, VP8). 
`player.getSettings().setPreferredVideoCodecSettings(new VideoCodecSettings().setCodecPriorityList(Collections.singletonList(PKVideoCodec.HEVC)));
`

- `setPreferredAudioCodecSettings`: Set Preferred codec for audio track. If the video has multiple codes (Ex: AAC, AC3, E_AC3, OPUS).
`player.getSettings().setPreferredAudioCodecSettings(new AudioCodecSettings().setCodecPriorityList(Collections.singletonList(PKAudioCodec.AAC)));
`

	In both the above preferred codec Settings, you can pass your custom preferred list Singleton. There are other setters as well for Codec Settings,
	
	* `VideoCodecSettings` 
	
		`allowSoftwareDecoder` to allow the player to user software decoder if hardware decoder is not at all available, by default it is `false`.
	
		`allowMixedCodecAdaptiveness` if it is true then it will override your preferred codec list. It will add all the video codec in the tracks. If media has multiple codec available for video.
		
	* `AudioCodecSettings`
	
		`allowMixedCodecs` if it is true then it will override your preferred codec list. It will add all the audio codec in the tracks. If media has multiple codec available for audio.


<p>
<h3 style="text-align:center">&#x2601; Key points to remember</h3>
</p>

1. You need to check if the device supports DRM or not. If you are lookging to play DRM protected content. An application that uses DRM should call the following API as early as possible (if applicable, in the splash screen):

	`MediaSupport.initializeDrm(Context, DrmInitCallback)`
	
	Calling this function makes sure that the DRM subsystem (in particular, Widevine) is ready before playback starts. In some cases, devices have to be provisioned to use Widevine, a process that involves connecting to a Google service over the network. [Check this out to know more](https://kaltura.github.io/playkit/guide/android/core/drm.html) [Code Sample](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L117)

2. Sometimes, developers face issue where they see that player is still alive and eventually may find any memory leak.   
&#x279f; To solve this, make sure you destroy the player and remove the player listeners while destorying the activity/fragment. [Check it out](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L907)

3. Make sure to call [`player.onApplicationPaused()`](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L895) in `onPause()` of call [`player.onApplicationResumed()`](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L1141) 

4. How to handle Internet connection/disconnection.   
&#x279f; Make sure that you check the internet before preparing the player. There shall be a case where internet can go off where player will retry for defined times[ It sends `Load_Error` in this case that is why it is *Recoverable* ] then it eventually will throw `Source_Error`[ It is *Fatal* error ].

5. We throw various Player Errors [check it out](https://github.com/kaltura/playkit-android/blob/dev/playkit/src/main/java/com/kaltura/playkit/player/PKPlayerErrorType.java).  
&#x279f; Application can get it in `PKError` object. We provide `Severity`, `message`, `exception`, `errorType` and `errorCategory`. 
`Severity` is `Recoverable` and some are `Fatal`. 
We send [`PKErrorCategory`](https://github.com/kaltura/playkit-android/blob/dev/playkit/src/main/java/com/kaltura/playkit/PKErrorCategory.java), which can be `Load` and `Play`.

6. Checkout out [FullDemo](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L916) to understand more about Events. 

7. Checkout out working and well maintained fully functional samples of [Playkit and its Plugins](https://github.com/kaltura/playkit-android-samples/tree/develop/FullDemo) and [Kaltura Player Sample](https://github.com/kaltura/kaltura-player-android-samples/tree/develop/AdvancedSamples/TestApp).


<p>
<h3 style="text-align:center">&#x2668; Important Information</h3>
</p>

1. If you are facing build conflicts, please check [this](https://kaltura.github.io/playkit/guide/android/migration/v3.6.0.html#plugin-interface).
2. If you want to check DRM info of Android device, [use our app](https://play.google.com/store/apps/details?id=com.kaltura.kalturadeviceinfo).
3. We have some know limitations. [Check it out](https://kaltura.github.io/playkit/guide/android/core/drm.html#known-limitations).
4. To know more about Player and Additional events. *For Step-6* [Check it out](https://kaltura.github.io/playkit/guide/android/core/events.html).
5. [Check the proguard configuration](https://kaltura.github.io/playkit/guide/android/core/proguard.html).
6. We have **warmup** feature. It helps to give quicker playback. [Check it out](https://kaltura.github.io/playkit/guide/android/core/http.html).
7. Profiling the player. [Check it out](https://kaltura.github.io/playkit/guide/android/core/profiler.html).
8. **Google Play Services Security Provider**. [Check it out](https://kaltura.github.io/playkit/guide/android/google/security-provider.html).
9. Check out about our [Offline-Manager](https://kaltura.github.io/playkit/guide/android/core/offline-manager.html) which is a successor of Dowload-To-Go.
10. Learn more about [Download-To-Go(Offline Downloader)](https://kaltura.github.io/playkit/guide/android/dtg/).
11. To know more about [***Kaltura Player***](https://kaltura.github.io/playkit/guide/android/migration/KalturaPlayer.html).
12. We support VR/360 content as well. [Check it out](https://kaltura.github.io/playkit/guide/android/core/vr.html).
13. If you are a Kaltura backend customer then you must be using our Backend so you can check our `GetPlaybackContext` API using the [OVP Endpoint check](https://kaltura.github.io/playkit/tools/gpc) and [OTT Endpoint check](https://kaltura.github.io/playkit/tools/gpc-ott).
14. Know more about our [Phoenix Media Provider](https://kaltura.github.io/playkit/guide/common/ott-media-provider.html).
15. Know more about [OVP Basics](https://kaltura.github.io/playkit/guide/common/ovp-basics.html).
16. If you want to understand more about the individual functionalities like, Subtitles, IMA Ads, Chromecast, Analytics. Please check [Playkit and its Plugins](https://github.com/kaltura/playkit-android-samples) and [Kaltura Player Sample](https://github.com/kaltura/kaltura-player-android-samples).

