package com.kaltura.tvplayer.offline

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.AppCompatActivity
import com.kaltura.tvplayer.KalturaPlayer
import com.kaltura.tvplayer.OfflineManager
import com.kaltura.tvplayer.PlayerInitOptions

class PlayActivity : AppCompatActivity() {

    private lateinit var player: KalturaPlayer
    private lateinit var playDrawable: Drawable
    private lateinit var pauseDrawable: Drawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val options = PlayerInitOptions().apply {
            autoplay = true
        }

        player = KalturaPlayer.createBasicPlayer(this, options)
        player.setPlayerView(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        setContentView(player.playerView)


        val manager = OfflineManager.getInstance(this)

        intent.dataString?.let {
            manager.sendAssetToPlayer(it, player)
        } ?: run {
            Toast.makeText(this, "No asset id given", LENGTH_LONG)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        player.destroy()
    }
}
