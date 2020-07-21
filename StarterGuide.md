### Kaltura Player Design Checklist

Before delving into setting up player in your app. We have a list to check.

[Kaltura-Player](https://kaltura.github.io/playkit/guide/android/migration/KalturaPlayer.html) - If you are a Kaltura customer then you should check 
[OTT Customer](https://github.com/kaltura/kaltura-player-android-samples/tree/develop/OTTSamples)
[OVP Customer](https://github.com/kaltura/kaltura-player-android-samples/tree/develop/OVPSamples)

In case, if you are not Kaltura customer then also no need to get worried, 
Checkout our [Samples](https://github.com/kaltura/kaltura-player-android-samples/tree/develop/BasicSamples)

Kaltura player internally uses [Playkit](https://github.com/kaltura/playkit-android), [Kava Analytics](https://github.com/kaltura/playkit-android-kava), [Download To Go(Offline Downloads)] (https://github.com/kaltura/playkit-dtg-android) and [Providers(Only for Kaltura customers)](https://github.com/kaltura/playkit-android-providers)

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
 		
 		Now you can modify and various features of player using player's settings api, `player.getSettings()` [Please check this to know more](https://kaltura.github.io/playkit/guide/android/core/player-settings.html)
 		
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
	   PKDrmParams pkDrmParams = new PKDrmParams(licenseUri, 		PKDrmParams.Scheme.WidevineCENC);
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

        FrameLayout layout = (FrameLayout) findViewById(R.id.player_view);
        layout.addView(player.getView());

        controlsView = (PlaybackControlsView) this.findViewById(R.id.playerControls);
        controlsView.setPlayer(player);
 		```
 		
9. Now prepare the player using the mediaConfig which you prepared.

	```
	player.prepare(mediaConfig);
	player.play();
	```
 
10. Now there is an option to configure the plugins before preparing the plugin. *Make Sure to check 3rd step to register plugins* [Check out the player configuration](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L709)

11. You can use single Player instance and use the same for the next medias, It means if you want to change a media on next or previous button so just need to configure the session given in 4th step.

12. You can update the plugins on the change media, [Check it out](https://github.com/kaltura/playkit-android-samples/blob/1141bd1d95edf4dc172b3e8dad3b3c7eb78676ab/FullDemo/playkitdemo/src/main/java/com/kaltura/playkitdemo/MainActivity.java#L532)

