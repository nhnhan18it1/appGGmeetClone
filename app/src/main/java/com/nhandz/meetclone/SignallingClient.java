package com.nhandz.meetclone;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.nhandz.meetclone.Obj.Connector;
import com.nhandz.meetclone.Obj.SimplePeer;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

//import io.socket.client.IO;
//import io.socket.client.Socket;
//import io.socket.emitter.Emitter;

/**
 * Webrtc_Step3
 * Created by vivek-3102 on 11/03/17.
 */

class SignallingClient {
    private static SignallingClient instance;
    private String roomName = "123";
    public Socket socket= MainActivity.mSocket;
    private String ServerNode=MainActivity.NodeServer;
    boolean isChannelReady = true;
    boolean isInitiator = false;
    boolean isStarted = false;
    Context context;
    private SignalingInterface callback;
    private String TAG = getClass().getSimpleName();
    public  HashMap<String, SimplePeer> connectorHashMap;

    //This piece of code should not go into production!!
    //This will help in cases where the node server is running in non-https server and you want to ignore the warnings
    @SuppressLint("TrustAllX509TrustManager")
    private final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }

        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) {
        }

        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) {
        }
    }};

    public static SignallingClient getInstance() {
        if (instance == null) {
            instance = new SignallingClient();
        }
        if (instance.roomName == null) {
            //set the room name here
            instance.roomName = "123";
        }
        return instance;
    }

    public void init(SignalingInterface signalingInterface) {
        SendCallActivity.peers=new ArrayList<>();
        connectorHashMap = new HashMap<>();
        this.callback = signalingInterface;
        try {
//            SSLContext sslcontext = SSLContext.getInstance("TLS");
//            sslcontext.init(null, trustAllCerts, null);
//
//            IO.setDefaultHostnameVerifier((hostname, session) -> true);
//            IO.setDefaultSSLContext(sslcontext);
            //set the socket.io url here

            if (socket==null){
                socket = IO.socket(ServerNode);
                socket.connect();

            }


            Log.e("SignallingClient", "init() called "+socket.connected());

            if (!roomName.isEmpty()) {
                //emitInitStatement(roomName);
            }

            //room created event.
            socket.on("created", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e("SignallingClient-created", "created call() called with: args = [" + Arrays.toString(args) + "]");
                    isInitiator = true;
                    callback.onCreatedRoom();
                }
            });

            //peer joined event
            socket.on("initReceive", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    SendCallActivity.peers.add(String.valueOf(args[0])) ;
                    Log.e(TAG, "INIT RECEIVE "+connectorHashMap.size());
                    isChannelReady = true;
                    isInitiator=true;
                    //callback.onNewPeerJoined();
                    SimplePeer simplePeer=new SimplePeer(
                            SendCallActivity.localStream,
                            true,
                            SendCallActivity.peerConnectionFactory,
                            SendCallActivity.peericeServers,
                            String.valueOf(args[0])
                    );
                    connectorHashMap.put(String.valueOf(args[0]),simplePeer);
                    socket.emit("initSend",String.valueOf(args[0]));
                    Log.e(TAG, "INIT RECEIVE "+connectorHashMap.size());
                    Log.e(TAG, "init: initReceive "+String.valueOf(args[0]) );

                }
            });

            socket.on("initSend",args -> {
                SendCallActivity.peers.add(String.valueOf(args[0])) ;
                Log.e(TAG, "init: INIT SEND "+args[0] );
                isInitiator = true;
                callback.onCreatedRoom();
                SimplePeer simplePeer=new SimplePeer(
                        SendCallActivity.localStream,
                        true,
                        SendCallActivity.peerConnectionFactory,
                        SendCallActivity.peericeServers,
                        String.valueOf(args[0])
                );
                connectorHashMap.put(String.valueOf(args[0]).trim(),simplePeer);
                Log.e(TAG, "init: initSend "+String.valueOf(args[0]) );
                //callback.onTryToStart();
            });

            //when you joined a chat room successfully
            socket.on("joined", args -> {
                Log.e("SignallingClient-joined", "joined call() called with: args = [" + Arrays.toString(args) + "]");
                isChannelReady = true;
                callback.onJoinedRoom();
            });
            //bye event
            //socket.on("bye", args -> callback.onRemoteHangUp((String) args[0]));

            //messages - SDP and ICE candidates are transferred through this
            socket.on("signal", args -> {
                Log.e(TAG+"- signal", "message call() called with: args = [" + Arrays.toString(args) + "]");
                try {

//                    if (connectorHashMap.get(socketId)!=null){
//                        connectorHashMap.get(socketId).signal(data);
//                    }

                    if (args[0] instanceof String) {
                        Log.e("SignallingClient", "String received :: " + args[0]);
//                        String data = (String) args[0];
//                        if (data.equalsIgnoreCase("got user media")) {
//                            callback.onTryToStart();
//                        }
//                        if (data.equalsIgnoreCase("bye")) {
//                            callback.onRemoteHangUp(data);
//                        }
                    } else if (args[0] instanceof JSONObject) {
                        JSONObject dtSignal=(JSONObject) args[0];
                        JSONObject data =(JSONObject) dtSignal.get("signal");
                        String socketId = (String) dtSignal.get("socket_id");
                        String type = data.getString("type");
                        Log.e(TAG, "init: " + socketId);

                        Log.e(TAG, "init: connectorHashMap " + connectorHashMap.get(socketId));

                        //connectorHashMap.get(socketId).signal(data);
                        // if (type.equalsIgnoreCase("offer")) {
//                            connectorHashMap.get(socketId).onOfferReceived(data);
//                        } else if (type.equalsIgnoreCase("answer") && isStarted) {
//                            connectorHashMap.get(socketId).onAnswerReceived(data);
//                        } else if (type.equalsIgnoreCase("candidate") && isStarted) {
//                            connectorHashMap.get(socketId).onIceCandidateReceived(data);
//                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            });


            socket.on("joinRoomSucess",args -> {
               if ((int)args[0]!= -1){
                   Log.e(TAG, "init: JOIN ROOM SUCCESS "+args[0] );
                   socket.emit("clientReadyGroup",args[0]);
                   //callback.onTryToStart();
               }
            });
            emitInitStatement_join();
        } catch (URISyntaxException  e) {
            e.printStackTrace();
        }
    }


    public void emitInitStatement_create(String message) {
        Log.e("SignallingClient", "emitInitStatement() called with: event = [" + "create" + "], message = [" + message + "]");
        socket.emit("create", message);
    }

    public void emitInitStatement_join() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("gId",SendCallActivity.roomName);
            MainActivity.mSocket.emit("joinRoom",jsonObject);
            Log.e(TAG, "emitInitStatement_join: "+SendCallActivity.roomName );
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void emitMessage(SessionDescription message) {
        try {
            Log.e("SignallingClient", "emitMessage() called with: message = [" + message + "]");
            JSONObject obj = new JSONObject();
            obj.put("type", message.type.canonicalForm());
            obj.put("sdp", message.description);
            JSONObject signal=new JSONObject();
            signal.put("signal",obj);
            signal.put("socket_id",SendCallActivity.peers.get(0));
            Log.e("emitMessage-vivek1794", signal.toString());
            socket.emit("signal", signal);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void emitIceCandidate(IceCandidate iceCandidate) {
        try {
            JSONObject object = new JSONObject();
            object.put("type", "candidate");
            object.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            object.put("sdpMid", iceCandidate.sdpMid);
            object.put("candidate", iceCandidate.sdp);
            JSONObject signal = new JSONObject();
            signal.put("signal",object);
            signal.put("socket_id",socket.id());
            socket.emit("signal", signal);
            Log.e("Sinal-emitIce: " ," "+iceCandidate.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        socket.emit("bye", roomName);
        socket.disconnect();
        socket.close();
    }

    interface SignalingInterface {
        void onRemoteHangUp(String msg);

        void onOfferReceived(JSONObject data);

        void onAnswerReceived(JSONObject data);

        void onIceCandidateReceived(JSONObject data);

        void onTryToStart();

        void onCreatedRoom();

        void onJoinedRoom();

        void onNewPeerJoined();
    }

}