package com.nhandz.meetclone.Obj;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.nhandz.meetclone.CustomPeerConnectionObserver;
import com.nhandz.meetclone.CustomSdpObserver;
import com.nhandz.meetclone.MainActivity;
import com.nhandz.meetclone.SendCallActivity;


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


import io.socket.client.Socket;

import static com.nhandz.meetclone.SendCallActivity.peers;

public class SimplePeer  {

    private MediaStream localStream;
    private boolean initiator;
    private static PeerConnection peerConnection;
    private PeerConnectionFactory peerConnectionFactory;
    private List<PeerConnection.IceServer> iceServers;
    private String socketId;
    private String TAG = getClass().getSimpleName();
    private MediaConstraints sdpConstraints;
    private Socket socket;


    public SimplePeer(MediaStream localStream, boolean initiator, PeerConnectionFactory peerConnectionFactory, List<PeerConnection.IceServer> iceServers, String socketId) {
        this.localStream = localStream;
        this.initiator = initiator;
        this.peerConnectionFactory = peerConnectionFactory;
        this.iceServers = iceServers;
        this.socketId = socketId;
        socket = MainActivity.mSocket;
        //SignallingClient.getInstance().init(this);
        Log.e(TAG, "SimplePeer: "+socketId );
        new Start().execute();
        //onTryToStart();

    }

    public void init(){
        Log.e(TAG, "init: simplePeer :: "+socketId );
        socket.on("signal", args -> {
            Log.e(TAG+"- signal", "message call() called with: args = ");

            try {
                JSONObject dtSignal=(JSONObject) args[0];
                JSONObject data =(JSONObject) dtSignal.get("signal");
                Log.e("SignallingClient", "Json Received :: " + data.toString());
                String type = null;
                type = data.getString("type");
                if (dtSignal.getString("socket_id").equals(socketId)){
                    if (type.equalsIgnoreCase("offer")) {
                        onOfferReceived(data);
                    } else if (type.equalsIgnoreCase("answer")) {
                        onAnswerReceived(data);
                    } else if (type.equalsIgnoreCase("candidate")) {
                        onIceCandidateReceived(data);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        });
        onTryToStart();
    }

    public void signal(JSONObject jsonObject){
        try {
            String type = jsonObject.getString("type");
            Log.e(TAG, "signal: "+type );
            if (type.equalsIgnoreCase("offer")) {
                onOfferReceived(jsonObject);
            } else if (type.equalsIgnoreCase("answer")) {
                onAnswerReceived(jsonObject);
            } else if (type.equalsIgnoreCase("candidate")) {
                onIceCandidateReceived(jsonObject);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void emitMessage(SessionDescription message) {
        try {
            //Log.e(TAG , "emitMessage() called with: message = [" + message + "]");
            JSONObject obj = new JSONObject();
            obj.put("type", message.type.canonicalForm());
            obj.put("sdp", message.description);
            JSONObject signalx = new JSONObject();
            signalx.put("signal",obj);
            signalx.put("socket_id", socketId);
            Log.e("emitMessage-vivek1794", signalx.toString());
            socket.emit("signal", signalx);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void call(){
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
                emitMessage(sessionDescription);
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
                Log.e(TAG, "\n createPeerConnection: "+iceCandidate );
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.e(TAG, "\n onAddStream: Received Remote stream "+mediaStream );
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
        new SendCallActivity.addRenderFromRemoteStram(
                new Connector(
                        peerConnection,
                        socketId,
                        stream)).execute();
        call();
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
                        emitMessage(sessionDescription);
                    }

                    @Override
                    public void onCreateFailure(String s) {
                        super.onCreateFailure(s);
                        Log.e(TAG, "onCreateFailure: "+s );
                    }
                },new MediaConstraints()
        );
        Log.e(TAG, "\n doAnswer: \n" );
    }

    public void onIceCandidateReceived(PeerConnection localPeer, IceCandidate iceCandidate) {
        //we have received ice candidate. We can set it to the other peer.
        emitIceCandidate(iceCandidate);
    }

    public void emitIceCandidate(IceCandidate iceCandidate) {
        try {
            JSONObject object = new JSONObject();
            //object.put("type", "candidate");
            object.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            object.put("sdpMid", String.valueOf(iceCandidate.sdpMLineIndex));
            object.put("candidate", iceCandidate.sdp);
            JSONObject candidate = new JSONObject();
            candidate.put("candidate",object);
            JSONObject signal = new JSONObject();
            signal.put("signal",candidate);
            signal.put("socket_id",socket.id());
            socket.emit("signal", signal);
            Log.e(TAG ,"\n emitIceCandidate "+signal.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@Override
    public void onRemoteHangUp(String msg) {

    }

    //@Override
    public void onOfferReceived(JSONObject data) {
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
        Log.e(TAG, " \n onOfferReceived: "+data+" \n" );
    }

    //@Override
    public void onAnswerReceived(JSONObject data) {
        Log.e(TAG, "\n onAnswerReceived: "+data+"\n");
        try {
            peerConnection.setRemoteDescription(
                    new CustomSdpObserver("localSetRemote"),
                    new SessionDescription(
                            SessionDescription
                                    .Type
                                    .fromCanonicalForm(data.getString("type").toLowerCase()),
                            data.getString("sdp")
                    )
            );
        } catch (JSONException e) {
            Log.e(TAG, "onAnswerReceived: Err");
            e.printStackTrace();
        }
    }

    //@Override
    public void onIceCandidateReceived(JSONObject data) {
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
        Log.e(TAG, "onTryToStart: "+initiator);
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

    public class Start extends AsyncTask<Void,String,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            init();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.e(TAG, "onPostExecute: end Task" );
        }
    }

}
