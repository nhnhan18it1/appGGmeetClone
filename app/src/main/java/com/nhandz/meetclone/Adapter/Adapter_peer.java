package com.nhandz.meetclone.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhandz.meetclone.Obj.Connector;
import com.nhandz.meetclone.R;

import org.webrtc.AudioTrack;
import org.webrtc.EglBase;
import org.webrtc.PeerConnection;
import org.webrtc.RendererCommon;
import org.webrtc.VideoTrack;

import java.util.ArrayList;

public class Adapter_peer extends RecyclerView.Adapter<Adapter_peer.ViewHolder> {

    private ArrayList<Connector> connections;
    private Context context;
    private EglBase eglBase;
    private String TAG = getClass().getSimpleName();

    public Adapter_peer(ArrayList<Connector> connections, Context context, EglBase eglBase) {
        this.connections = connections;
        this.context = context;
        this.eglBase = eglBase;
        Log.e(TAG, "Adapter_peer: init" );
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater=LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.item_peer,parent,false);
        Log.e(TAG, "onCreateViewHolder: " );
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.e(TAG, "onBindViewHolder: setMirror" );

        if (connections!=null && connections.size()!=0){
            holder.viewRenderer.setMirror(true);
            holder.viewRenderer.setZOrderMediaOverlay(true);

            VideoTrack videoTrack = connections.get(position).getMediaStream().videoTracks.get(0);
            AudioTrack audioTrack = connections.get(position).getMediaStream().audioTracks.get(0);



            videoTrack.addSink(holder.viewRenderer);
            audioTrack.setVolume(5f);
            holder.txtNamePeer.setText(connections.get(position).getSocketId().toString());
        }

    }

    @Override
    public int getItemCount() {
        if (connections!=null){
            return connections.size();
        }
        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public org.webrtc.SurfaceViewRenderer viewRenderer;
        public TextView txtNamePeer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewRenderer=itemView.findViewById(R.id.itPeer_surface_rendeer);
            txtNamePeer=itemView.findViewById(R.id.itPeer_txtName);
            viewRenderer.init(eglBase.getEglBaseContext(), null);
        }
    }
}
