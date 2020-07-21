### Kaltura Player Design Checklist

Before delving into setting up player in your app. We have a list to check.

[Kaltura-Player](https://kaltura.github.io/playkit/guide/android/migration/KalturaPlayer.html) - If you are a Kaltura customer then you should check 
[OTT Customer](https://github.com/kaltura/kaltura-player-android-samples/tree/develop/OTTSamples)
[OVP Customer](https://github.com/kaltura/kaltura-player-android-samples/tree/develop/OVPSamples)

In case, if you are not Kaltura customer then also no need to get worried, 
Checkout our [Samples](https://github.com/kaltura/kaltura-player-android-samples/tree/develop/BasicSamples)

Kaltura player internally uses [Playkit](https://github.com/kaltura/playkit-android), [Kava Analytics](https://github.com/kaltura/playkit-android-kava), [Download To Go(Offline Downloads)](https://github.com/kaltura/playkit-dtg-android) and [Providers(Only for Kaltura customers)](https://github.com/kaltura/playkit-android-providers)

Apart from *Playkit*, to other libraries; we call them *Plugins*. [To Know More check this](https://kaltura.github.io/playkit/#components)

Moving on there is another way to use player; so for that instead of using Kaltura Player, you can choose to use Playkit and based on your requirement, can choose for [IMA Ads Plugin](https://github.com/kaltura/playkit-android-ima), [Kava](https://github.com/kaltura/playkit-android-kava) / [Youbora Analytics Plugins](https://github.com/kaltura/playkit-android-youbora), [Offline Downloader](https://github.com/kaltura/playkit-dtg-android) or [Providers](https://github.com/kaltura/playkit-android-providers).

These plugins are written in very efficient and customised way to make sure that the applications need to write very less code to make the functionality to work.

#### Using Playkit and Plugins Standalone

1. You can use our customized Log levels. [Check this Page](https://kaltura.github.io/playkit/guide/android/core/logging.html) [ Code Sample](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L117)
2. You need to check if the device supports DRM or not. If you are lookging to play DRM protected content. [Check this page](https://kaltura.github.io/playkit/guide/android/core/drm.html) [ Code Sample](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L318)
3. You need to register the plugins as per your requirement. [Code Sample](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L287)
4. Get the media source and DRM info from Kaltura Backend. If you are a Kaltura Customer,
 - **OTT Customer** This uses our OTT backend. You need to pass, OTT partner Id, OTT BE url, OTT Kaltura Session(KS), format and MediaId [Please check this sample code](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L357)

 - **OVP Customer:** This uses our OVP backend. You need to pass, OVP partner Id, OVP BE url, OVP Kaltura Session(KS) and EntryId [Please check this sample code](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L443)

 ###### For both OTT/OVP customers, you will get the call back after calling to the respective BE, [get callback](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L298)
 
5. You need to create MediaSource, MediaEntry and DRMParams manually.

 - **OTT/OVP Customers:**
 
 		You will get `PKMediaEntry` in the callback of 3rd step. Now you need to create `PKMediaConfig`. 
 		
 		`Player player = PlayKitManager.loadPlayer(this, pluginConfig);`
 		
 		Now you can modify and various features of player using player's settings api, `player.getSettings()` [Please check this to know more](https://kaltura.github.io/playkit/guide/android/core/player-settings.html)  	&#x261f;&#x261f;&#x261f;
 		
6. **Add Player listeners:** [Please check this](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L916)

	It helps to get Player and its plugins' events to handle application's UI controls like, on `pause` player event you want to change the UI of player to pause ico and turn to play icon on `play` event.
 
7. Add your application's UI to `playerview`. For this simply define your layout in [xml layout](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/res/layout/activity_main.xml#L106)
 
	 	```
	 	FrameLayout layout = (FrameLayout) findViewById(R.id.player_view);
	 	layout.addView(player.getView());
	 	```
 		
8. **Non Kaltura Customers:**  
 		
 Here you go check out the following snippet,
 
 	```
		List<PKDrmParams> pkDrmDataList = new ArrayList<>();
		String licenseUri = "DRM License URL";
		PKDrmParams pkDrmParams = new PKDrmParams(licenseUri, PKDrmParams.Scheme.WidevineCENC);
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
 
10. Now there is an option to configure the plugins before preparing the plugin. *Make Sure to check 3rd step to register plugins* [Check out the player configuration](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L709)

11. You can use single Player instance and use the same for the next medias, It means if you want to change a media on next or previous button so just need to configure the session given in 4th step.

12. You can update the plugins on the change media, [Check it out](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L532)


&#x261f;&#x261f;&#x261f;

***PlayerSettings:*** We are providing more details about the few settings to give more flexibility to the Player. For more details, [Check it out](https://kaltura.github.io/playkit/guide/android/core/player-settings.html)

- In case, if you want to provide customized referrer instead of default referrer in the manifest. Use if after Step-5

	`KalturaPlaybackRequestAdapter.install(player, "app://PlaykitTestApp");`

- In case, if you want to provide customized referrer instead of default referrer for DRM License request Adapter. Use if after Step-5

	`KalturaUDRMLicenseRequestAdapter.install(player, "app://PlaykitTestApp");`
	
- `setAllowCrossProtocolRedirect`: Decide if *player should do cross protocol redirect or not*. By default it will be always set to `false`.

- `allowClearLead`: If you want the player to use the initial clear chunks; by using this flag, player will play the *initial clear chunks available in manifest*.

- `enableDecoderFallback`: In case, if the device is not able to initialize the decoders then this flag if it is set to `true` will allow the player to use *low priority decoders.*

- `setSecureSurface`: Decide if player should use secure rendering on the surface. Known limitation - when `useTextureView` set to `true` and `isSurfaceSecured` set to `true` *secure rendering will have no effect*.

- `setABRSettings`: Adaptive bitrate settings. [Check it out](https://github.com/kaltura/playkit-android/blob/dev/playkit/src/main/java/com/kaltura/playkit/player/ABRSettings.java)

- `forceSinglePlayerEngine`*Useful when you use IMA ads*: Default value is `false`. In general, we prepare the content player when Ad Starts; but settings this flag `true` will force the playe not prepare with Ads. Useful for the devices where the low/less decoders.

- `setCustomLoadControlStrategy`: Load control strategy. [Check it out](https://github.com/kaltura/playkit-android/blob/dev/playkit/src/main/java/com/kaltura/playkit/player/LoadControlBuffers.java)

- `setMaxVideoSize`: Sets the maximum allowed video width and height.To set the maximum allowed video bitrate to sd resolution call:`setMaxVideoSize(new PKMaxVideoSize().setMaxVideoWidth(anySize).setMaxVideoHeight(anySize)`
To reset call:`setMaxVideoSize(new PKMaxVideoSize().setMaxVideoWidth(Integer.MAX_VALUE).setMaxVideoHeight(Integer.MAX_VALUE)`

- `setMaxVideoBitrate`: Sets the maximum allowed video bitrate. Maximum allowed video bitrate in bits per second.


&#x2668;&#x2668;&#x2668;

**Additional Information**

1. If you are facing build conflicts, please check [this](https://kaltura.github.io/playkit/guide/android/migration/v3.6.0.html#plugin-interface)
2. If you want to check DRM info of Android device, [use our app](https://play.google.com/store/apps/details?id=com.kaltura.kalturadeviceinfo)
3. We have some know limitations. [Check it out](https://kaltura.github.io/playkit/guide/android/core/drm.html#known-limitations)
4. To know more about Player and Additional events. *For Step-6* [Check it out](https://kaltura.github.io/playkit/guide/android/core/events.html)
5. [Check the proguard configuration](https://kaltura.github.io/playkit/guide/android/core/proguard.html)
6. We have **warmup** feature. It helps to give quicker playback. [Check it out](https://kaltura.github.io/playkit/guide/android/core/http.html)
7. Profiling the player. [Check it out](https://kaltura.github.io/playkit/guide/android/core/profiler.html)
8. **Google Play Services Security Provider**. [Check it out](https://kaltura.github.io/playkit/guide/android/google/security-provider.html)
9. Check out about our [Offline-Manager](https://kaltura.github.io/playkit/guide/android/core/offline-manager.html) which is a successor of Dowload-To-Go.
10. Learn more about [Download-To-Go(Offline Downloader)](https://kaltura.github.io/playkit/guide/android/dtg/)
