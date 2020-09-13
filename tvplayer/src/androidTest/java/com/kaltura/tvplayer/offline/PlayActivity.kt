package com.kaltura.tvplayer.offline

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.AppCompatActivity
import com.kaltura.playkit.PKLog
import com.kaltura.playkit.PlayerEvent
import com.kaltura.playkit.PlayerEvent.*
import com.kaltura.tvplayer.KalturaBasicPlayer
import com.kaltura.tvplayer.KalturaPlayer
import com.kaltura.tvplayer.OfflineManager
import com.kaltura.tvplayer.PlayerInitOptions
import org.junit.Assert.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


private val log = PKLog.get("PlayActivity")

class PlayActivity : AppCompatActivity() {

    class Destroyed

    private lateinit var player: KalturaPlayer
    private lateinit var playDrawable: Drawable
    private lateinit var pauseDrawable: Drawable

    private val mainHandler = Handler(Looper.getMainLooper())

    private val handler = Handler(HandlerThread("playtest").apply { start() }.looper)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log.d("PlayActivity.onCreate.thread=${Thread.currentThread()}")


        val options = PlayerInitOptions().apply {
            autoplay = true
        }

        player = KalturaBasicPlayer.create(this, options)
        player.setPlayerView(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        setContentView(player.playerView)


        val manager = OfflineManager.getInstance(this)

        intent.dataString?.let {
            val entry = manager.getLocalPlaybackEntry(it)
            player.setMedia(entry)
        } ?: run {
            Toast.makeText(this, "No asset id given", LENGTH_LONG)
        }

        // Check duration
        player.addListener(this, durationChanged) {
            assertEquals(intent.getLongExtra("expectedDuration", 0), it.duration)
        }


        handler.post {

            log.d("PlayActivity.handler.thread=${Thread.currentThread()}")


            // Wait for playing
            val playingLatch = CountDownLatch(1)
            player.addListener(this, playing) {
                log.d("PlayActivity.handler.playing=${Thread.currentThread()}")
                playingLatch.countDown()
            }
            if (!playingLatch.await(1, TimeUnit.SECONDS)) {
                fail("Not playing")
            }
            log.d("Playing!")

            val duration = player.duration

            // Wait until position is 5 seconds
            val play5Latch = CountDownLatch(1)
            player.addListener(this, playheadUpdated) { if (it.position >= 5000) play5Latch.countDown() }
            if (!play5Latch.await(5, TimeUnit.SECONDS)) {
                fail("Didn't play 5 seconds")
            }
            log.d("Played for 5 seconds!")

            // Seek to the middle and wait
            val seek5Latch = CountDownLatch(1)
            player.seekTo(duration / 2)
            player.addListener(this, seeked) {seek5Latch.countDown()}
            seek5Latch.await(100, TimeUnit.MILLISECONDS)

            // Let it play for 1 second
            Thread.sleep(1000)

            // Seek to one second before the end
            val seekToEndLatch = CountDownLatch(1)
            player.seekTo(duration - 1000)
            player.addListener(this, seeked) {seekToEndLatch.countDown()}
            seekToEndLatch.await(100, TimeUnit.MILLISECONDS)

            // Wait for ended event
            val endedLatch = CountDownLatch(1)
            player.addListener(this, ended) { endedLatch.countDown() }
            endedLatch.await(1200, TimeUnit.MILLISECONDS)

        }

    }

    override fun onDestroy() {
        super.onDestroy()

        player.destroy()
    }
}
