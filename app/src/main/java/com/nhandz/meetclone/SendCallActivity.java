package com.nhandz.meetclone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhandz.meetclone.Adapter.Adapter_peer;
import com.nhandz.meetclone.Obj.Connector;
import com.nhandz.meetclone.Obj.SimplePeer;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import io.socket.emitter.Emitter;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SendCallActivity extends AppCompatActivity implements SignallingClient.SignalingInterface {

    private static final int ALL_PERMISSIONS_CODE = 1 ;
    private String TAG=getClass().getSimpleName();

    public static ArrayList<String> peers;
    public static String roomName;

    private RecyclerView rcvPeer;
    public static ArrayList<Connector> connectors;
    private static Adapter_peer adt;

    public static List<PeerConnection.IceServer> peericeServers=new ArrayList<>();
    List<IceServer> iceServers;
    public static PeerConnectionFactory peerConnectionFactory;
    MediaConstraints audioConstraints;
    MediaConstraints videoConstraints;
    MediaConstraints sdpConstraints;
    VideoSource videoSource;
    VideoTrack localvideoTrack;
    AudioSource audioSource;
    AudioTrack localAudioTrack;
    public static MediaStream localStream;
    SurfaceViewRenderer localVideoView;
    SurfaceViewRenderer remoteVideoView;
    VideoCapturer videoCapturer;
    EglBase eglBase;
    boolean gotUserMedia;

    PeerConnection localPeer, remotePeer;
    Button create, join, offer, trystart;
    EditText txtname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcall);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, ALL_PERMISSIONS_CODE);
        } else {
            // all permissions already granted
            start();
        }

        Intent intent=getIntent();
        roomName = intent.getStringExtra("romName");
        create=findViewById(R.id.button_create);
        join=findViewById(R.id.button_join);
        offer=findViewById(R.id.button_start);
        //txtname=findViewById(R.id.txt_name);
        trystart=findViewById(R.id.trytostart);

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignallingClient.getInstance().emitInitStatement_create("123");
                //doAnswer();
            }
        });
        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignallingClient.getInstance().emitInitStatement_join();
            }
        });
        offer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //SignallingClient.getInstance().emitMessage("get user media");
                SignallingClient.getInstance().connectorHashMap.forEach(new BiConsumer<String, SimplePeer>() {
                    @Override
                    public void accept(String s, SimplePeer simplePeer) {
                        simplePeer.call();
                    }
                });
                //call();
            }
        });
        trystart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignallingClient.getInstance().isStarted=false;
                onTryToStart();
            }
        });



    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == ALL_PERMISSIONS_CODE
                && grantResults.length == 2
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            // all permissions granted
            start();
        } else {
            //finish();
        }
    }

    private void getIceServers(SignallingClient.SignalingInterface signalingInterface) {
        byte[] data = new byte[0];
        try {
            data = ("nhavbnm:feffd4ec-afd5-11ea-b23e-0242ac150003").getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String authToken = "Basic " + Base64.encodeToString(data, Base64.NO_WRAP);//
        Utils.getInstance().getRetrofitInstance().getIceCandidates().enqueue(new Callback<TurnServerPojo>() {
            @Override
            public void onResponse(Call<TurnServerPojo> call, Response<TurnServerPojo> response) {
                TurnServerPojo body=response.body();

                if (body!=null){
                    iceServers=body.iceServerList.iceServers;
                }
                for (IceServer iceServer: iceServers){
                    if (iceServer.credential==null){
                        PeerConnection.IceServer iceServer1= PeerConnection.IceServer.builder(iceServer.url).createIceServer();
                        peericeServers.add(iceServer1);

                    }
                    else {
                        PeerConnection.IceServer iceServer1= PeerConnection.IceServer.builder(iceServer.url)
                                .setUsername(iceServer.username)
                                .setPassword(iceServer.credential)
                                .createIceServer();
                        peericeServers.add(iceServer1);
                    }
                    Log.e("onApiResponse", "IceServers--"+peericeServers.size()+"--");

                }
                SignallingClient.getInstance().context=getApplicationContext();
                SignallingClient.getInstance().init(signalingInterface);

            }



            @Override
            public void onFailure(Call<TurnServerPojo> call, Throwable t) {
            t.printStackTrace();
                Log.e("onApiResponse-false", "IceServers"+t.getMessage() );
            }
        });

    }

    private void initVideos() {
        eglBase = EglBase.create();
        localVideoView=findViewById(R.id.surface_rendeer);
        remoteVideoView = findViewById(R.id.Remote_surface_rendeer);
        localVideoView.init(eglBase.getEglBaseContext(), null);
        localVideoView.setEnableHardwareScaler(true);
        remoteVideoView.init(eglBase.getEglBaseContext(), null);
        localVideoView.setMirror(true);
        localVideoView.setZOrderMediaOverlay(true);
        rcvPeer = findViewById(R.id.rcv_listPeer);
        initRe();
//        remoteVideoView.setZOrderMediaOverlay(true);
    }

    public void initRe(){
        rcvPeer.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(getApplicationContext(),LinearLayoutManager.HORIZONTAL,false);
        rcvPeer.setLayoutManager(linearLayoutManager);
        connectors=new ArrayList<>();
        adt=new Adapter_peer(connectors,this,eglBase);

        rcvPeer.setAdapter(adt);
    }

    public void setLocalStream(){
        localStream = peerConnectionFactory.createLocalMediaStream("102");
        localStream.addTrack(localAudioTrack);
        localStream.addTrack(localvideoTrack);
    }

    private void start(){
        Log.e(TAG, "start: isStarted" );

        initVideos();
        videoCapturer=createCameraCapturer(new Camera1Enumerator(false));;
        PeerConnectionFactory.initialize(
                PeerConnectionFactory
                        .InitializationOptions
                        .builder(this)
                        .setEnableInternalTracer(true)
                        .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                        .createInitializationOptions());
        //peerConnectionFactory=PeerConnectionFactory.builder().createPeerConnectionFactory();



        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();


        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                eglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        peerConnectionFactory = PeerConnectionFactory.builder()

                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();



        MediaConstraints mediaConstraints=new MediaConstraints();
        if (videoCapturer != null) {
            SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
            videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
            videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        }

        localvideoTrack=peerConnectionFactory.createVideoTrack("100",videoSource);

        audioSource=peerConnectionFactory.createAudioSource(mediaConstraints);
        localAudioTrack=peerConnectionFactory.createAudioTrack("101",audioSource);


        localVideoView.setEnabled(true);
        remoteVideoView.setEnabled(true);
        if (videoCapturer != null) {
            videoCapturer.startCapture(1024, 720, 30);
        }


        ProxyVideoSink localVideoSink = new ProxyVideoSink();

        localvideoTrack.addSink(localVideoView);
        localVideoSink.setTarget(localVideoView);


        gotUserMedia=true;
        setLocalStream();
//        SignallingClient.getInstance().isInitiator=true;
        if (SignallingClient.getInstance().isInitiator){
            Log.e(TAG, "start: onTryToStart"+SignallingClient.getInstance().isInitiator );
            onTryToStart();
        }
        //onTryToStart();
        getIceServers(this);
    }


    private static class ProxyVideoSink implements VideoSink {
        private VideoSink target;

        @Override
        synchronized public void onFrame(VideoFrame frame) {
            if (target == null) {
                Log.e("TAG", "Dropping frame in proxy because target is null.");
                return;
            }

            target.onFrame(frame);
        }

        synchronized public void setTarget(VideoSink target) {
            this.target = target;
        }
    }



    public void call(){
        sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        localPeer.createOffer(new CustomSdpObserver("localCreateOffer") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"), sessionDescription);
                Log.e("onCreateSuccess", "SignallingClient emit ");
                SignallingClient.getInstance().emitMessage(sessionDescription);
            }

            @Override
            public void onCreateFailure(String s){
                super.onCreateFailure(s);
                Log.e("main 2", "onCreateFailure: "+s );
            }

        }, sdpConstraints);

    }


    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        Log.d("TAG", "Creating capturer using camera1 API.");
        videoCapturer = createCameraCapturer(new Camera2Enumerator(getApplicationContext()));

        return videoCapturer;
    }


    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Log.d("TAG", "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Log.d("TAG", "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }


        // Front facing camera not found, try something else
        Log.d("TAG", "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isBackFacing(deviceName)) {
                Log.d("TAG", "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    public void onIceCandidateReceived(PeerConnection localPeer, IceCandidate iceCandidate) {
        //we have received ice candidate. We can set it to the other peer.
        SignallingClient.getInstance().emitIceCandidate(iceCandidate);
    }

    @Override
    public void onBackPressed() {
        hangup();
        super.onBackPressed();
    }

    private void hangup() {
        if (localPeer!=null){
            localPeer.close();
        }
        if (remotePeer!=null)remotePeer.close();

        localPeer = null;
        remotePeer = null;
        //start.setEnabled(true);
        //call.setEnabled(false);
        //hangup.setEnabled(false);
    }

    private void gotRemoteStream(MediaStream stream) {
        //we have remote video stream. add to the renderer.
        VideoTrack videoTrack = stream.videoTracks.get(0);
        AudioTrack audioTrack = stream.audioTracks.get(0);
        Log.e(TAG, "gotRemoteStream: "+stream.toString() );
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
//                   remoteVideoView = new VideoRenderer(remoteVideoView);
                    remoteVideoView.setVisibility(View.VISIBLE);
                    videoTrack.addSink(remoteVideoView);
                    connectors.add(new Connector(localPeer,peers.get(0),stream));
                    adt.notifyDataSetChanged();
                    //peers.remove(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        call();
        Log.e(TAG, "gotRemoteStream: "+stream.toString() );

    }

    private void addStreamToLocalPeer() {
        //creating local mediastream
        MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localvideoTrack);
        localPeer.addStream(stream);
    }

    private void updateVideoViews(final boolean remoteVisible) {
        runOnUiThread(() -> {
            ViewGroup.LayoutParams params = localVideoView.getLayoutParams();
            if (remoteVisible) {
                params.height = 250;
                params.width = 250;
            } else {
                params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
            localVideoView.setLayoutParams(params);
        });
    }

    private void createPeerConnection() {

        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(peericeServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        localPeer = peerConnectionFactory.createPeerConnection(peericeServers, new CustomPeerConnectionObserver("localPeerCreation"){
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                onIceCandidateReceived(localPeer,iceCandidate);
                Log.e("main2", "createPeerConnection: "+iceCandidate );
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.e("main2", "onAddStream: Received Remote stream" );
                super.onAddStream(mediaStream);
                gotRemoteStream(mediaStream);
            }
        });

        addStreamToLocalPeer();


    }


    private void doAnswer() {
        localPeer.createAnswer(new CustomSdpObserver("localCreateAns") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(
                        new CustomSdpObserver("localSetLocal"),
                        sessionDescription
                );
                SignallingClient.getInstance().emitMessage(sessionDescription);
            }

            @Override
            public void onCreateFailure(String s) {
                super.onCreateFailure(s);
                Log.e("main2", "onCreateFailure: "+s );
            }
        }, new MediaConstraints());

        Log.e("Car","doanswer");
    }
    @Override
    public void onRemoteHangUp(String msg) {
        runOnUiThread(this::hangup);
    }

    @Override
    public void onOfferReceived(JSONObject data) {
        runOnUiThread(() -> {
            if (!SignallingClient.getInstance().isInitiator && !SignallingClient.getInstance().isStarted) {
                onTryToStart();
            }

            try {
                localPeer.setRemoteDescription(
                        new CustomSdpObserver("localSetRemote"),
                        new SessionDescription(
                                SessionDescription.Type.OFFER,
                                data.getString("sdp")
                        )
                );
                doAnswer();
                //updateVideoViews(true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }



    @Override
    public void onAnswerReceived(JSONObject data) {
        Log.e("main2", "onAnswerReceived: "+data);
        try {
            localPeer.setRemoteDescription(
                    new CustomSdpObserver("localSetRemote"),
                    new SessionDescription(
                            SessionDescription
                                    .Type.fromCanonicalForm(data.getString("type")
                                    .toLowerCase()),
                            data.getString("sdp")
                    )
            );
            updateVideoViews(true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onIceCandidateReceived(JSONObject data) {
        Log.e("main2", "onIceCandidateReceived: "+data );
        try {
            localPeer.addIceCandidate(new IceCandidate(
                    data.getString("sdpMid"),
                    data.getInt("sdpMLineIndex"),
                    data.getString("candidate")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTryToStart() {
        Log.e("main2", "onTryToStart: localvideoTrack="+localvideoTrack+"--isInitiator="+SignallingClient.getInstance().isInitiator+" isStart="+SignallingClient.getInstance().isStarted+" isChanelReady="+ SignallingClient.getInstance().isChannelReady);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!SignallingClient.getInstance().isStarted
                        &&localvideoTrack!=null
                        &&SignallingClient.getInstance().isChannelReady
                ){
                    createPeerConnection();
                    SignallingClient.getInstance().isStarted=true;
                    if (SignallingClient.getInstance().isInitiator){
                        call();
                    }
                }
            }
        });

    }

    @Override
    public void onCreatedRoom() {
        //Toast.makeText(getApplicationContext(), "You create a romm "+gotUserMedia, Toast.LENGTH_SHORT).show();
        Log.e("main2", "onCreatedRoom: "+gotUserMedia);
        if (gotUserMedia){
//            SignallingClient.getInstance().emitMessage("get user media");
        }
    }

    @Override
    public void onJoinedRoom() {
        //Toast.makeText(this, "You join a romm "+gotUserMedia, Toast.LENGTH_SHORT).show();
        Log.e("main2", "onJoinedRoom: "+gotUserMedia );
        if (gotUserMedia){
//            SignallingClient.getInstance().emitMessage("get user media");
        }
    }

    @Override
    public void onNewPeerJoined() {
//        Toast.makeText(this, "new peer join", Toast.LENGTH_SHORT).show();
        Log.e("mai2", "onNewPeerJoined: " );
    }

    public static class addRenderFromRemoteStram extends AsyncTask<Void,String,Void>{

        private Connector connector;

        public addRenderFromRemoteStram(Connector connector) {
            this.connector = connector;
        }

        @Override
        protected Void doInBackground(Void[] voids) {
            connectors.add(connector);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            adt.notifyDataSetChanged();
        }
    }
}
