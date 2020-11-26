package com.nhandz.meetclone.Obj;

import android.util.Log;
import android.view.View;

import com.nhandz.meetclone.CustomPeerConnectionObserver;
import com.nhandz.meetclone.CustomSdpObserver;


import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioTrack;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoTrack;

import java.util.List;


import static com.nhandz.meetclone.SendCallActivity.peers;

public class SimplePeer  {

    private MediaStream localStream;
    private boolean initiator;
    private PeerConnection peerConnection;
    private PeerConnectionFactory peerConnectionFactory;
    private List<PeerConnection.IceServer> iceServers;
    private String socketId;
    private String TAG = getClass().getSimpleName();
    private MediaConstraints sdpConstraints;


    public SimplePeer(MediaStream localStream, boolean initiator, PeerConnectionFactory peerConnectionFactory, List<PeerConnection.IceServer> iceServers, String socketId) {
        this.localStream = localStream;
        this.initiator = initiator;
        this.peerConnectionFactory = peerConnectionFactory;
        this.iceServers = iceServers;
        this.socketId = socketId;
        //SignallingClient.getInstance().init(this);
        onTryToStart();
    }

    private void call(){
        sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")
        );
        sdpConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")
        );
        peerConnection.createOffer(new CustomSdpObserver("localCreateOffer"){
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                peerConnection.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"), sessionDescription);
                Log.e(TAG, "onCreateSuccess SignallingClient emit ");
                //SignallingClient.getInstance().emitMessage(sessionDescription,socketId);
            }

            @Override
            public void onCreateFailure(String s){
                super.onCreateFailure(s);
                Log.e(TAG, "onCreateFailure: "+s );
            }
        },sdpConstraints);
    }

    private void createPeerConnection(){
        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(iceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        peerConnection = peerConnectionFactory.createPeerConnection(iceServers, new CustomPeerConnectionObserver("localPeerCreation"){
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                onIceCandidateReceived(peerConnection,iceCandidate);
                Log.e(TAG, "createPeerConnection: "+iceCandidate );
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.e(TAG, "onAddStream: Received Remote stream" );
                super.onAddStream(mediaStream);
                gotRemoteStream(mediaStream);
            }
        });

        addStreamToLocalPeer();
    }

    private void addStreamToLocalPeer(){
        peerConnection.addStream(localStream);
    }

    private void gotRemoteStream(MediaStream stream) {
        Log.e(TAG, "run: gotRemoteStream"+stream.toString());
        //connectors.add(new Connector(peerConnection,socketId,stream));
        //adt.notifyDataSetChanged();


    }

    private void doAnswer(){
        peerConnection.createAnswer(
                new CustomSdpObserver("localCreateAns"){
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        super.onCreateSuccess(sessionDescription);
                        peerConnection.setLocalDescription(
                                new CustomSdpObserver("localSetLocal"),
                                sessionDescription
                        );
                        //SignallingClient.getInstance().emitMessage(sessionDescription,socketId);
                    }

                    @Override
                    public void onCreateFailure(String s) {
                        super.onCreateFailure(s);
                        Log.e(TAG, "onCreateFailure: "+s );
                    }
                },new MediaConstraints()
        );
    }

    public void onIceCandidateReceived(PeerConnection localPeer, IceCandidate iceCandidate) {
        //we have received ice candidate. We can set it to the other peer.
        //SignallingClient.getInstance().emitIceCandidate(iceCandidate);
    }


    //@Override
    public void onRemoteHangUp(String msg, String socketId) {

    }

    //@Override
    public void onOfferReceived(JSONObject data, String socketId) {
        if (peerConnection==null)createPeerConnection();
        try {
            peerConnection.setRemoteDescription(
                    new CustomSdpObserver("localSetRemote"),
                    new SessionDescription(
                            SessionDescription.Type.OFFER,
                            data.getString("sdp")
                    )
            );
            doAnswer();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //@Override
    public void onAnswerReceived(JSONObject data, String socketId) {
        Log.e(TAG, "onAnswerReceived: "+data);
        try {
            peerConnection.setRemoteDescription(
                    new CustomSdpObserver("localSetRemote"),
                    new SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(data.getString("type").toLowerCase()),
                            data.getString("sdp")
                    )
            );
        } catch (JSONException e) {
            Log.e(TAG, "onAnswerReceived: Err");
            e.printStackTrace();
        }
    }

    //@Override
    public void onIceCandidateReceived(JSONObject data, String socketId) {
        Log.e(TAG, "onIceCandidateReceived: "+data);
        try {
            peerConnection.addIceCandidate(
                    new IceCandidate(
                            data.getString("sdpMid"),
                            data.getInt("sdpMLineIndex"),
                            data.getString("candidate")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //@Override
    public void onTryToStart() {
        Log.e(TAG, "onTryToStart: ");
        createPeerConnection();
        if (initiator){
            Log.e(TAG, "onTryToStart: call() :: true" );
            call();
        }


    }

    //@Override
    public void onCreatedRoom() {

    }

    //@Override
    public void onJoinedRoom() {

    }

    //@Override
    public void onNewPeerJoined() {
        Log.e(TAG, "onNewPeerJoined: " );
    }
}
