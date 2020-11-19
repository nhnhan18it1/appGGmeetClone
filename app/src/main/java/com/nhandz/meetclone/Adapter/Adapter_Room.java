package com.nhandz.meetclone.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.nhandz.meetclone.MainActivity;
import com.nhandz.meetclone.Obj.Room;
import com.nhandz.meetclone.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Adapter_Room extends RecyclerView.Adapter<Adapter_Room.ViewHolder> {

    private ArrayList<Room> rooms;
    private Context context;

    public Adapter_Room(ArrayList<Room> rooms, Context context) {
        this.rooms = rooms;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View itemView = layoutInflater.inflate(R.layout.item_room, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtTitle.setText(rooms.get(position).getTitle());
        holder.btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject jsonObject=new JSONObject();
                try {
                    jsonObject.put("gId",rooms.get(position).getgId());
                    MainActivity.mSocket.emit("joinRoom",jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView txtTitle;
        public BootstrapButton btnJoin;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.itr_txtTitle);
            btnJoin = itemView.findViewById(R.id.itr_btnJoin);

        }
    }
}
