package com.nhandz.meetclone;

import android.app.Notification;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface TurnServer {
    @GET("turnsv")///_turn/streamx
    Call<TurnServerPojo> getIceCandidates() ;//@Header("Authorization") String authkey
    @POST("api/loadnoti")
    @FormUrlEncoded
    Call<Notification[]> getNotification(@Field("ID") String ID);

}
