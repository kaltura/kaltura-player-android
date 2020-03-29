# Kaltura Player for Android

**Kaltura Player**  - This `Playkit` wrapper simplifies the player integration so that client applications will require less boilerplate code, which ensures a faster integration.

Gradle Dependency:  `implementation 'com.kaltura.player:tvplayer:4.x.x'`

This dependency already includes **Playkit, Kava Analytics Player Providers and Download-to-Go libraries** internally, so no need to add them to the client app's `build.gradle`.

[Kaltura Player Migration Guide](https://kaltura.github.io/playkit/guide/android)

[Kaltura Player Docs](https://developer.kaltura.com/player/android/getting-started-android)

[Kaltura Player Samples](https://github.com/kaltura/kaltura-player-android-samples/tree/master)

#### Pre-requisite

###### Player Initialization

Client application should call a mandatory initialization method calls at the time of application Launch or in the project's `Application` class file or the `SplashActivity`.

`KalturaPlayer.initializeOTT(this, OTT_PARNTER_ID, OTT_SERVER_URL);`
`KalturaPlayer.initializeOVP(this, OVP_PARNTER_ID, OVP_SERVER_URL);`

###### OTT Ex.
`public static final String OTT_SERVER_URL = "https://rest-us.ott.kaltura.com/v4_5/";`
`public static final int OTT_PARNTER_ID = 3009;`

#### Note: 
Without calling the initialization code on startup `KalturaPlayerNotInitializedError`, error will be fired on the `player.loadMedia` callback phase.


###### Warmup

Application can use the warmup connections for its specific `CDN` servers URLs where the medias are hosted so a connection to the hosts will be opened and ready for use so all handshake process will be saved and media playback will be faster.

There should be only one URL per host, and the URLs should resolve to a valid pathnames. A good choice might be `favicon.ico` or `crossdomain.xml` after the host name.

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

**Notes** 

The `playerInitOptions` allows the app to set player settings. Following is the example what we had then and what we have now.

In `playerInitOptions` the default value for player autoplay is true.

In `playerInitOptions` the default value for player media preload from the BE is true.
