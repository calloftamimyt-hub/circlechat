package com.example.data.webrtc

import android.content.Context
import org.webrtc.*

class WebRTCManager(private val context: Context) {

    val eglBaseContext = EglBase.create().eglBaseContext
    private var peerConnectionFactory: PeerConnectionFactory? = null
    
    // ... we will add methods to initialize
    
    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(eglBaseContext, true, true)
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(eglBaseContext)
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .createPeerConnectionFactory()
    }
}
