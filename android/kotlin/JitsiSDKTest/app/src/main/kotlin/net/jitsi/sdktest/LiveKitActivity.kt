package net.jitsi.sdktest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.track.RemoteVideoTrack

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

import livekit.org.webrtc.SurfaceViewRenderer
import livekit.org.webrtc.EglBase

class LiveKitActivity : AppCompatActivity() {

    private lateinit var room: Room
    private lateinit var renderer: SurfaceViewRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa a Room usando a Factory do LiveKit
        room = LiveKit.create(this)
        
        // Usa o renderer do pacote livekit.org.webrtc para compatibilidade com o SDK
        renderer = SurfaceViewRenderer(this)

        renderer.init(
            EglBase.create().eglBaseContext,
            null
        )

        setContentView(renderer)

        lifecycleScope.launch {

            try {
                room.connect(
                    "wss://SEU-PROJETO.livekit.cloud",
                    "SEU_TOKEN"
                )

                room.localParticipant.setCameraEnabled(true)
                room.localParticipant.setMicrophoneEnabled(true)

                // room.events é um Flow<RoomEvent>
                room.events.collectLatest { event ->
                    when (event) {
                        is RoomEvent.TrackSubscribed -> {
                            val track = event.track
                            if (track is RemoteVideoTrack) {
                                track.addRenderer(renderer)
                            }
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        room.disconnect()
        renderer.release()
    }
}