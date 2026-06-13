package com.example.data.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*

class WebRTCManager(
    private val context: Context,
    val eglBaseContext: EglBase.Context,
    private val onIceCandidate: (IceCandidate) -> Unit,
    private val onAddStream: (MediaStream) -> Unit
) {
    private var peerConnectionFactory: PeerConnectionFactory? = null
    var peerConnection: PeerConnection? = null

    init {
        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(eglBaseContext, true, true)
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(eglBaseContext)
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .createPeerConnectionFactory()
    }

    fun getFactory(): PeerConnectionFactory? = peerConnectionFactory

    fun createPeerConnection() {
        if (peerConnectionFactory == null) return
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate) {
                onIceCandidate(candidate)
            }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream) {
                Log.d("WebRTCManager", "onAddStream CALLED. Video tracks: ${stream.videoTracks.size}, Audio tracks: ${stream.audioTracks.size}")
                onAddStream(stream)
            }
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        })
    }

    fun addLocalStream(localVideoTrack: VideoTrack?, localAudioTrack: AudioTrack?) {
        val stream = peerConnectionFactory?.createLocalMediaStream("ARDAMS")
        if (localAudioTrack != null) stream?.addTrack(localAudioTrack)
        if (localVideoTrack != null) stream?.addTrack(localVideoTrack)
        peerConnection?.addStream(stream)
    }

    fun createOffer(callback: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        callback(desc)
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, desc)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(reason: String?) {}
            override fun onSetFailure(reason: String?) {}
        }, constraints)
    }

    fun createAnswer(callback: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        callback(desc)
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, desc)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(reason: String?) {}
            override fun onSetFailure(reason: String?) {}
        }, constraints)
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d("WebRTCManager", "Remote Description set successfully!")
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, sdp)
    }

    fun addRemoteIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun dispose() {
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
    }
}
