package net.jitsi.sdktest

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.widget.FrameLayout

import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.RemoteVideoTrack
import kotlinx.coroutines.delay

import kotlinx.coroutines.launch

import livekit.org.webrtc.SurfaceViewRenderer
import livekit.org.webrtc.EglBase

class LiveKitActivity : AppCompatActivity() {

    private lateinit var room: Room
    lateinit var localRenderer: SurfaceViewRenderer
    lateinit var remoteRenderer: SurfaceViewRenderer
    lateinit var eglBase: EglBase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.setProperty("org.webrtc.camera2.enabled", "false")

        room = LiveKit.create(this)

        eglBase = EglBase.create()

        localRenderer = SurfaceViewRenderer(this).apply {
            init(eglBase.eglBaseContext, null)
            setMirror(true)
        }

        remoteRenderer = SurfaceViewRenderer(this).apply {
            init(eglBase.eglBaseContext, null)
        }


        localRenderer.layoutParams = FrameLayout.LayoutParams(
            400, 600
        ).apply {
            marginStart = 50
            topMargin = 50
        }

        localRenderer.setZOrderMediaOverlay(true)

        localRenderer.post {
            Log.d("SIZE", "W: ${localRenderer.width} H: ${localRenderer.height}")
        }

        setContentView(localRenderer)

        lifecycleScope.launch {

            val identity = "user_" + System.currentTimeMillis()

            Log.d("TESTE", "ANTES FETCH")

            val token = fetchToken(identity, "sala_teste")

            Log.d("TESTE", "TOKEN: $token")

            launch {
                room.events.collect { event ->

                    when (event) {

                        is RoomEvent.Connected -> {

                            room.localParticipant.setCameraEnabled(true)
                            room.localParticipant.setMicrophoneEnabled(true)


                            val track = room.localParticipant.getOrCreateDefaultVideoTrack()

                            track.addRenderer(localRenderer)

                            room.localParticipant.publishVideoTrack(track)

                        }

                        is RoomEvent.TrackSubscribed -> {
                            val track = event.track

                            if (track is RemoteVideoTrack) {
                                track.addRenderer(remoteRenderer)
                                Log.d("LIVEKIT", "REMOTE VIDEO OK")
                            }
                        }

                        is RoomEvent.TrackPublicationFailed -> {
                            Log.e("LIVEKIT", " ERRO AO PUBLICAR TRACK:")
                        }

                        is RoomEvent.FailedToConnect -> {
                            Log.e("LIVEKIT", " FALHOU CONECTAR: ${event.error}")
                        }

                        is RoomEvent.Disconnected -> {
                            Log.e("LIVEKIT", " DESCONECTADO: ${event.error}")
                        }

                        else -> {}
                    }
                }
            }

            room.connect(
                "wss://migration-sample-poc-q1w2h4ro.livekit.cloud",
                token
            )
        }
    }

    suspend fun fetchToken(identity: String, room: String): String =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {

            val url = java.net.URL("http://192.168.3.81:3000/get-token")

            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            val json = """
            {
                "identity": "$identity",
                "room": "$room"
            }
        """.trimIndent()

            connection.outputStream.write(json.toByteArray())

            val response = connection.inputStream.bufferedReader().readText()

            return@withContext org.json.JSONObject(response).getString("token")
        }


    override fun onDestroy() {
        super.onDestroy()
        room.disconnect()
        localRenderer.release()
        remoteRenderer.release()
    }

}