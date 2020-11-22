package com.nhandz.meetclone.Obj;

import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

import java.io.Serializable;

public class Connector implements Serializable {
    private PeerConnection peerConnection;
    private String socketId;
    private MediaStream mediaStream;

    public Connector(PeerConnection peerConnection, String socketId, MediaStream mediaStream) {
        this.peerConnection = peerConnection;
        this.socketId = socketId;
        this.mediaStream = mediaStream;
    }

    public MediaStream getMediaStream() {
        return mediaStream;
    }

    public void setMediaStream(MediaStream mediaStream) {
        this.mediaStream = mediaStream;
    }

    public PeerConnection getPeerConnection() {
        return peerConnection;
    }

    public void setPeerConnection(PeerConnection peerConnection) {
        this.peerConnection = peerConnection;
    }

    public String getSocketId() {
        return socketId;
    }

    public void setSocketId(String socketId) {
        this.socketId = socketId;
    }
}
