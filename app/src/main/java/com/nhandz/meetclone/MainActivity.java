package com.nhandz.meetclone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.BootstrapEditText;
import com.nhandz.meetclone.Adapter.Adapter_Room;
import com.nhandz.meetclone.Obj.Room;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;

public class MainActivity extends AppCompatActivity {
    private String TAG = getClass().getSimpleName();
    public static String NodeServer = "https://ggmeet.herokuapp.com";
    public static Socket mSocket;
    private static final int ALL_PERMISSIONS_CODE = 1;

    private RecyclerView recyclerViewRoom;
    private ArrayList<Room> rooms;
    Adapter_Room adt;
    private BootstrapButton btnCreateRoom;
    private BootstrapEditText inputNameRoom;

    public void init() {
        try {
            mSocket = IO.socket(NodeServer);
            mSocket.connect();
            Log.e(TAG, "init: " + mSocket.connected());
            //mSocket.emit("CreateRoom","halo");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, ALL_PERMISSIONS_CODE);
        } else {
            // all permissions already granted

        }
        initView();
        socketEvent();
        btnEvent();

    }

    public void btnEvent(){
        btnCreateRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, "onClick: socket.connected="+mSocket.connected() );
                if (!inputNameRoom.getText().equals("")){
                    mSocket.emit("CreateRoom",inputNameRoom.getText().toString());
                    Intent intent = new Intent(MainActivity.this,SendCallActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    public void socketEvent(){
        mSocket.emit("getRoom");
        mSocket.on("Svs_getRoom",args -> {
            JSONArray jsonArray = (JSONArray) args[0];
            notifyRoom(jsonArray);
        });
    }

    public void initView() {
        recyclerViewRoom=findViewById(R.id.mainActivity_recycleviewRoom);
        btnCreateRoom=findViewById(R.id.mainActivity_btnCreateRoom);
        inputNameRoom=findViewById(R.id.mainActivity_inputCrateRoom);
        initRecycleViewRoom();
    }

    public void notifyRoom(JSONArray jsonArray){
        for (int i=0;i<jsonArray.length();i++){
            try {
                JSONObject jsonObject=(JSONObject) jsonArray.get(i);
                Log.e(TAG, "notifyRoom: "+jsonObject.toString() );
                rooms.add(new Room(String.valueOf(jsonObject.get("name")) ,String.valueOf(jsonObject.get("gId")) ,String.valueOf(jsonObject.get("key")) ));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adt.notifyDataSetChanged();
                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    public void initRecycleViewRoom(){
        recyclerViewRoom.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false);
        recyclerViewRoom.setLayoutManager(linearLayoutManager);
        rooms = new ArrayList<>();
        adt=new Adapter_Room(rooms,getApplicationContext());
        recyclerViewRoom.setAdapter(adt);
    }
}