package net.jitsi.sdktest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.track.RemoteVideoTrack

import kotlinx.coroutines.launch

import livekit.org.webrtc.SurfaceViewRenderer
import livekit.org.webrtc.EglBase

class LiveKitActivity : AppCompatActivity() {

    private lateinit var room: Room
    private lateinit var renderer: SurfaceViewRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        room = LiveKit.create(this)

        renderer = SurfaceViewRenderer(this)
        renderer.init(EglBase.create().eglBaseContext, null)

        setContentView(renderer)

        lifecycleScope.launch {

            // pra pegar esse token e a url tem q criar um projeto no site do livekit
            room.connect(
                "URL",
                "token"
            )

            room.localParticipant.setCameraEnabled(true)
            room.localParticipant.setMicrophoneEnabled(true)

            room.events.collect { event ->
                when (event) {

                    is RoomEvent.TrackPublished -> {
                        val track = event.publication.track

                        if (track is io.livekit.android.room.track.LocalVideoTrack) {
                            track.addRenderer(renderer)
                        }
                    }

                    is RoomEvent.TrackSubscribed -> {
                        val track = event.track

                        if (track is RemoteVideoTrack) {
                            track.addRenderer(renderer)
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        room.disconnect()
        renderer.release()
    }
}