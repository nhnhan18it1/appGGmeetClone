package com.nhandz.meetclone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.BootstrapEditText;
import com.nhandz.meetclone.Adapter.Adapter_Room;
import com.nhandz.meetclone.Obj.Room;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.nhandz.meetclone.MainActivity.mSocket;

public class RoomActivity extends AppCompatActivity {

    private RecyclerView recyclerViewRoom;
    private ArrayList<Room> rooms;
    Adapter_Room adt;
    private BootstrapButton btnCreateRoom;
    private BootstrapEditText inputNameRoom;
    private String TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
    }

    public void btnEvent(){
        btnCreateRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, "onClick: socket.connected="+mSocket.connected() );
                if (!inputNameRoom.getText().equals("")){
                    mSocket.emit("CreateRoom",inputNameRoom.getText().toString());
                    Intent intent = new Intent(RoomActivity.this,SendCallActivity.class);
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
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(RoomActivity.this, LinearLayoutManager.VERTICAL, false);
        recyclerViewRoom.setLayoutManager(linearLayoutManager);
        rooms = new ArrayList<>();
        adt=new Adapter_Room(rooms,getApplicationContext());
        recyclerViewRoom.setAdapter(adt);
    }
}