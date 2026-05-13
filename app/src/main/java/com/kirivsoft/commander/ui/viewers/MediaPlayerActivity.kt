package com.kirivsoft.commander.ui.viewers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.kirivsoft.commander.databinding.ActivityMediaPlayerBinding
import java.io.File

class MediaPlayerActivity : AppCompatActivity() {

    private lateinit var b: ActivityMediaPlayerBinding
    private var player: ExoPlayer? = null

    companion object {
        fun open(context: Context, path: String) =
            context.startActivity(Intent(context, MediaPlayerActivity::class.java).putExtra("path", path))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMediaPlayerBinding.inflate(layoutInflater)
        setContentView(b.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val path = intent.getStringExtra("path") ?: run { finish(); return }
        title = File(path).name

        val isAudio = File(path).extension.lowercase() in setOf("mp3","flac","ogg","aac","wav","opus","m4a","wma")
        if (isAudio) b.artworkContainer.visibility = View.VISIBLE

        player = ExoPlayer.Builder(this).build().also { p ->
            b.playerView.player = p
            p.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(File(path))))
            p.prepare()
            p.playWhenReady = true
            p.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    b.progressBar.visibility =
                        if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                }
            })
        }
    }

    override fun onStop()    { super.onStop(); player?.pause() }
    override fun onStart()   { super.onStart(); player?.play() }
    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
}
